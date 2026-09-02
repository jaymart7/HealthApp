package ph.mart.healthapp.core.data.exercise

import kotlinx.coroutines.flow.Flow
import ph.mart.healthapp.core.data.streak.STREAK_WINDOW_DAYS

/** Shaped like `FoodRepository`: one row per logged activity, soft-deleted, today's rows observed
 * for the diary and Home. */
interface ExerciseRepository {
    fun observeTodayEntries(): Flow<List<ExerciseEntry>>

    /** One day's entries — the diary, which can be pointed at any past day. */
    fun observeEntries(dateEpochDay: Long): Flow<List<ExerciseEntry>>
    /** Returns the new row id, which the Google Health import records against the data point. */
    suspend fun addEntry(entry: ExerciseEntry): Long
    /** Twin of `FoodRepository.updateEntry`: the corrected row supersedes the old one, so the id
     * changes and the row's place in the day does not. [ExerciseEntry.steps] is carried across
     * untouched — an imported workout's step count is the watch's, not something to re-estimate. */
    suspend fun updateEntry(entry: ExerciseEntry)
    suspend fun deleteEntry(id: Long)

    /** The last year's entries, oldest first — the Progress Activity tab's burn series. Windowed
     * in the impl, like [observeLoggedDays], because `todayEpochDay()` is internal to this module. */
    fun observeRecentEntries(): Flow<List<ExerciseEntry>>

    /** Full history, oldest first — for data export. */
    suspend fun allEntries(): List<ExerciseEntry>

    /** The newest strength workouts, newest first — the strength screen's "repeat last workout"
     * seed (the first of them) and its lift-name chips. A one-shot read rather than a flow: the
     * screen asks once on entry, and neither figure changes while it is open. */
    suspend fun recentStrengthEntries(limit: Int = RECENT_STRENGTH_WORKOUTS): List<ExerciseEntry>

    /** One workout by id, sets included — the strength route carries an id, not the row, so the
     * screen has to resolve it itself. Null once it has been deleted. */
    suspend fun entry(id: Long): ExerciseEntry?

    /** Soft-deletes every entry, for import's replace-in-full semantics. */
    suspend fun deleteAllEntries()

    /** Days with at least one activity, within the last [STREAK_WINDOW_DAYS] — exercise's
     * contribution to the logging streak, same shape as `WaterRepository.observeLoggedDays()`. */
    fun observeLoggedDays(): Flow<Set<Long>>
}
