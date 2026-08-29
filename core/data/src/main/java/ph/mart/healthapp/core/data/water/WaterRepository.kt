package ph.mart.healthapp.core.data.water

import kotlinx.coroutines.flow.Flow
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.streak.STREAK_WINDOW_DAYS

/** One day's hydration, as a glass count. Days the user never logged simply have no row. */
data class WaterDay(val dateEpochDay: Long, val glasses: Int)

/** A glass is a fixed serving, not a measured pour — the count is what both screens show, and
 * only the label below it converts to the user's unit. */
const val GLASS_ML = 250
const val GLASS_FL_OZ = 8

const val DEFAULT_WATER_GOAL_GLASSES = 8

/** Below 4 the row stops being a useful target; above 20 it stops fitting on a phone. */
val WATER_GOAL_GLASSES = 4..20

/** e.g. "1.5 L" (metric) or "48 fl oz" (imperial). */
fun waterVolumeLabel(glasses: Int, unit: UnitSystem): String = when (unit) {
    UnitSystem.Imperial -> "${glasses * GLASS_FL_OZ} fl oz"
    UnitSystem.Metric -> {
        val ml = glasses * GLASS_ML
        if (ml < 1000) "$ml ml" else "%.1f L".format(ml / 1000.0)
    }
}

/**
 * Water is stored one row per day holding a count — not one row per glass. Correcting a
 * miscount is then an update rather than a delete, which is what keeps this domain inside the
 * project's soft-delete-only rule without a deleted flag.
 */
interface WaterRepository {
    fun observeToday(): Flow<Int>
    suspend fun setToday(glasses: Int)

    /** Backdated write — only an import or the debug seed ever uses it. */
    suspend fun upsertDay(day: WaterDay)

    /** Every day with a non-zero count, oldest first — for data export. */
    suspend fun allDays(): List<WaterDay>

    /** Days with at least one glass, within the last [STREAK_WINDOW_DAYS] — water's contribution
     * to the logging streak. */
    fun observeLoggedDays(): Flow<Set<Long>>

    /** Zeroes every day, for import's replace-in-full semantics. */
    suspend fun clearAllDays()
}
