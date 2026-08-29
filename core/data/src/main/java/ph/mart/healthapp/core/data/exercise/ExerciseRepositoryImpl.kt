package ph.mart.healthapp.core.data.exercise

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ph.mart.healthapp.core.data.exercise.local.ExerciseEntryDao
import ph.mart.healthapp.core.data.exercise.local.ExerciseEntryEntity
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
        return dao.insert(entry.toEntity(date = date, loggedAt = System.currentTimeMillis()))
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
)

private fun ExerciseEntry.toEntity(date: Long, loggedAt: Long) = ExerciseEntryEntity(
    id = id,
    type = type.name,
    name = name,
    date = date,
    loggedAt = loggedAt,
    minutes = minutes,
    burnedKcal = burnedKcal,
)
