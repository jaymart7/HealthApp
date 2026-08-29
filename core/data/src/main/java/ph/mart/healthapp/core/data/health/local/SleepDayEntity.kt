package ph.mart.healthapp.core.data.health.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One row per night, keyed by the day the sleep *ended* — that is the day a user means by "last
 * night", and it is what makes Home's lookup a plain equality against today.
 *
 * Lives under `health/` rather than beside the app's own domains because it only exists as an
 * import: FitPulse has no way to measure sleep itself, so there is no manual write path and no
 * soft-delete column — disconnecting removes the row outright.
 */
@Entity(tableName = "sleep_day")
internal data class SleepDayEntity(
    @PrimaryKey val date: Long,
    val minutesAsleep: Int,
    val startMillis: Long,
    val endMillis: Long,
)
