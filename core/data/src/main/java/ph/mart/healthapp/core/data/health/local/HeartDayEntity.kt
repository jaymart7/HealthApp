package ph.mart.healthapp.core.data.health.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One day of heart-rate readings, keyed by the local day they were taken.
 *
 * Lives under `health/` beside [SleepDayEntity] and [StepDayEntity], and for the same reason: it
 * only exists as an import, so there is no manual write path and no soft-delete column —
 * disconnecting removes the rows outright.
 *
 * Both figures are aggregates of the day's samples, because the API reports heart rate intra-day
 * and FitPulse stores a day. [minBpm] is the day's *lowest* reading and is labelled that way
 * everywhere it is shown — it is not a resting heart rate, which is a measurement this app has no
 * way to take.
 */
@Entity(tableName = "heart_day")
internal data class HeartDayEntity(
    @PrimaryKey val date: Long,
    val averageBpm: Int,
    val minBpm: Int,
)
