package ph.mart.healthapp.core.data.exercise

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ph.mart.healthapp.core.data.exercise.local.ExerciseEntryDao
import ph.mart.healthapp.core.data.exercise.local.ExerciseEntryEntity
import ph.mart.healthapp.core.data.health.estimatedSteps
import ph.mart.healthapp.core.data.streak.STREAK_WINDOW_DAYS
import ph.mart.healthapp.core.data.todayEpochDay

internal class ExerciseRepositoryImpl(private val dao: ExerciseEntryDao) : ExerciseRepository {

    override fun observeTodayEntries(): Flow<List<ExerciseEntry>> = observeEntries(todayEpochDay())

    override fun observeEntries(dateEpochDay: Long): Flow<List<ExerciseEntry>> =
        dao.observeForDate(dateEpochDay).map { entities -> entities.map { it.toExerciseEntry() } }

    override suspend fun addEntry(entry: ExerciseEntry): Long {
        // A dated entry arrives from an import, or from the diary pointed at a past day; anything
        // logged without one is "today".
        val date = entry.dateEpochDay.takeIf { it > 0 } ?: todayEpochDay()
        // Filled here rather than at each call site so no write path can forget it. Manual
        // logging never knows a step count; the import supplies the watch's own, which is
        // non-zero and so wins over the estimate.
        val steps = entry.steps.takeIf { it > 0 } ?: estimatedSteps(entry.type, entry.minutes)
        return dao.insert(
            entry.copy(steps = steps).toEntity(date = date, loggedAt = System.currentTimeMillis()),
        )
    }

    override suspend fun updateEntry(entry: ExerciseEntry) {
        val date = entry.dateEpochDay.takeIf { it > 0 } ?: todayEpochDay()
        val loggedAt = dao.loggedAt(entry.id) ?: System.currentTimeMillis()
        // Unlike addEntry, steps are taken as given: re-deriving them would throw away the watch's
        // own figure on an imported workout, which is the one number here we didn't guess.
        // ponytail: so an edited duration doesn't move a hand-logged estimate either, and
        // stepsCreditKcal() subtracts a slightly stale count. Re-estimate here if that drift shows.
        dao.replace(entry.id, entry.toEntity(date = date, loggedAt = loggedAt))
    }

    override suspend fun deleteEntry(id: Long) {
        dao.softDelete(id)
    }

    override suspend fun allEntries(): List<ExerciseEntry> = dao.allActive().map { it.toExerciseEntry() }

    override suspend fun deleteAllEntries() {
        dao.softDeleteAll()
    }

    /** Window anchored here, not in the caller — `todayEpochDay()` is internal to this module. */
    override fun observeLoggedDays(): Flow<Set<Long>> =
        dao.observeLoggedDaysSince(todayEpochDay() - STREAK_WINDOW_DAYS).map { it.toSet() }
}

private fun ExerciseEntryEntity.toExerciseEntry() = ExerciseEntry(
    id = id,
    dateEpochDay = date,
    type = ExerciseType.valueOf(type),
    name = name,
    minutes = minutes,
    burnedKcal = burnedKcal,
    steps = steps,
)

private fun ExerciseEntry.toEntity(date: Long, loggedAt: Long) = ExerciseEntryEntity(
    id = id,
    type = type.name,
    name = name,
    date = date,
    loggedAt = loggedAt,
    minutes = minutes,
    burnedKcal = burnedKcal,
    steps = steps,
)
