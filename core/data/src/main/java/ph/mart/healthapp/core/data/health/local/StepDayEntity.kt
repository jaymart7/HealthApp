package ph.mart.healthapp.core.data.health.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One row per day of walking, keyed by the local day the steps were taken.
 *
 * Lives under `health/` beside [SleepDayEntity] and for the same reason: it only exists as an
 * import, so there is no manual write path and no soft-delete column — disconnecting removes the
 * rows outright.
 *
 * [burnedKcal] is stored rather than recomputed on read, the same rule `exercise_entry` follows:
 * a later weigh-in must not silently rewrite what a past day burned. It is the burn for the
 * *whole* [steps] count; the share a logged workout already claims is subtracted at read time by
 * `stepsCreditKcal()`.
 */
@Entity(tableName = "step_day")
internal data class StepDayEntity(
    @PrimaryKey val date: Long,
    val steps: Int,
    val burnedKcal: Int,
)
