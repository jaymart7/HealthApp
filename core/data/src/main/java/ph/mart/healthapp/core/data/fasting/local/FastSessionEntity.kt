package ph.mart.healthapp.core.data.fasting.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One fast, as a session rather than a day: a fast crosses midnight by design, so there is no
 * epoch day that describes it. [endMillis] null *is* the active-fast marker — there is no status
 * column and no "currently fasting" flag on the profile, so the two can never disagree.
 *
 * [goalHours] is snapshotted from the profile at start rather than read back live, the same rule
 * `step_day.burnedKcal` and `exercise_entry.burnedKcal` follow: raising your target next month
 * must not retroactively un-hit a fast you already finished.
 */
@Entity(tableName = "fast_session")
internal data class FastSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMillis: Long,
    val endMillis: Long?,
    val goalHours: Int,
)
