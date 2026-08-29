package ph.mart.healthapp.core.data.exercise

import kotlinx.coroutines.flow.Flow
import ph.mart.healthapp.core.data.streak.STREAK_WINDOW_DAYS

/** Shaped like `FoodRepository`: one row per logged activity, soft-deleted, today's rows observed
 * for the diary and Home. */
interface ExerciseRepository {
    fun observeTodayEntries(): Flow<List<ExerciseEntry>>

    /** One day's entries — the diary, which can be pointed at any past day. */
    fun observeEntries(dateEpochDay: Long): Flow<List<ExerciseEntry>>
    suspend fun addEntry(entry: ExerciseEntry)
    suspend fun deleteEntry(id: Long)

    /** Full history, oldest first — for data export. */
    suspend fun allEntries(): List<ExerciseEntry>

    /** Soft-deletes every entry, for import's replace-in-full semantics. */
    suspend fun deleteAllEntries()

    /** Days with at least one activity, within the last [STREAK_WINDOW_DAYS] — exercise's
     * contribution to the logging streak, same shape as `WaterRepository.observeLoggedDays()`. */
    fun observeLoggedDays(): Flow<Set<Long>>
}
