package ph.mart.healthapp.core.data.exercise

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ph.mart.healthapp.core.data.exercise.local.ExerciseEntryDao
import ph.mart.healthapp.core.data.exercise.local.ExerciseEntryEntity
import ph.mart.healthapp.core.data.exercise.local.StrengthSetEntity
import ph.mart.healthapp.core.data.forToday
import ph.mart.healthapp.core.data.health.estimatedSteps
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.data.streak.STREAK_WINDOW_DAYS
import ph.mart.healthapp.core.data.todayEpochDay

internal class ExerciseRepositoryImpl(private val dao: ExerciseEntryDao) : ExerciseRepository {

    override fun observeTodayEntries(): Flow<List<ExerciseEntry>> = forToday(::observeEntries)

    override fun observeEntries(dateEpochDay: Long): Flow<List<ExerciseEntry>> = combine(
        dao.observeForDate(dateEpochDay),
        dao.observeSetsForDate(dateEpochDay),
        ::joinSets,
    )

    override suspend fun addEntry(entry: ExerciseEntry): Long {
        // A dated entry arrives from an import, or from the diary pointed at a past day; anything
        // logged without one is "today".
        val date = entry.dateEpochDay.takeIf { it > 0 } ?: todayEpochDay()
        // Filled here rather than at each call site so no write path can forget it. Manual
        // logging never knows a step count; the import supplies the watch's own, which is
        // non-zero and so wins over the estimate.
        val steps = entry.steps.takeIf { it > 0 } ?: estimatedSteps(entry.type, entry.minutes)
        return dao.insertWithSets(
            entry.copy(steps = steps).toEntity(date = date, loggedAt = System.currentTimeMillis()),
            entry.sets.toEntities(),
        )
    }

    override suspend fun updateEntry(entry: ExerciseEntry) {
        val date = entry.dateEpochDay.takeIf { it > 0 } ?: todayEpochDay()
        val loggedAt = dao.loggedAt(entry.id) ?: System.currentTimeMillis()
        // Unlike addEntry, steps are taken as given: re-deriving them would throw away the watch's
        // own figure on an imported workout, which is the one number here we didn't guess.
        // ponytail: so an edited duration doesn't move a hand-logged estimate either, and
        // stepsCreditKcal() subtracts a slightly stale count. Re-estimate here if that drift shows.
        // The sets travel with the row: replace() re-points them onto the new id, since the edit
        // supersedes rather than rewrites.
        dao.replace(entry.id, entry.toEntity(date = date, loggedAt = loggedAt), entry.sets.toEntities())
    }

    override suspend fun deleteEntry(id: Long) {
        dao.softDelete(id)
    }

    /** Window anchored here, not in the caller, for the same reason [observeLoggedDays]'s is. */
    override fun observeRecentEntries(): Flow<List<ExerciseEntry>> = forToday { today ->
        val from = today - ChartRange.OneYear.days!!
        combine(dao.observeSince(from), dao.observeSetsSince(from), ::joinSets)
    }

    override suspend fun allEntries(): List<ExerciseEntry> = joinSets(dao.allActive(), dao.allActiveSets())

    override suspend fun recentStrengthEntries(limit: Int): List<ExerciseEntry> {
        val entries = dao.recentStrength(limit)
        return joinSets(entries, dao.setsFor(entries.map { it.id }))
    }

    override suspend fun entry(id: Long): ExerciseEntry? {
        val entity = dao.entry(id) ?: return null
        return joinSets(listOf(entity), dao.setsFor(listOf(id))).firstOrNull()
    }

    override suspend fun deleteAllEntries() {
        // The sets go for real: a soft-deleted parent's children are unreachable but not gone, and
        // import is replace-in-full. Both calls run inside `DataTransferRepository.replaceAll`'s
        // single transaction.
        dao.deleteAllSets()
        dao.softDeleteAll()
    }

    /** Window anchored here, not in the caller — `todayEpochDay()` is internal to this module.
     * Re-pointed at each midnight, like every other today-derived window. */
    override fun observeLoggedDays(): Flow<Set<Long>> = forToday { today ->
        dao.observeLoggedDaysSince(today - STREAK_WINDOW_DAYS).map { it.toSet() }
    }
}

/** The one place the parent/child join happens — a fold over two lists rather than a Room
 * relation, the same call `FoodRepositoryImpl` makes for saved meals. Every read path goes
 * through it, so no query can quietly return a strength workout with its sets missing. */
private fun joinSets(
    entries: List<ExerciseEntryEntity>,
    sets: List<StrengthSetEntity>,
): List<ExerciseEntry> {
    val byEntry = sets.groupBy { it.entryId }
    return entries.map { it.toExerciseEntry(byEntry[it.id].orEmpty()) }
}

private fun ExerciseEntryEntity.toExerciseEntry(sets: List<StrengthSetEntity>) = ExerciseEntry(
    id = id,
    dateEpochDay = date,
    type = ExerciseType.valueOf(type),
    name = name,
    minutes = minutes,
    burnedKcal = burnedKcal,
    steps = steps,
    sets = sets.map { StrengthSet(it.exerciseName, it.reps, it.weightKg) },
)

/** [StrengthSetEntity.entryId] is filled in by `insertWithSets` once the row id exists. */
private fun List<StrengthSet>.toEntities(): List<StrengthSetEntity> = map {
    StrengthSetEntity(entryId = 0, exerciseName = it.exerciseName, reps = it.reps, weightKg = it.weightKg)
}

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
