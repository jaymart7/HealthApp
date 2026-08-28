package ph.mart.healthapp.core.data.water.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** One row per day, keyed by local-midnight epoch day. Un-logging a glass decrements [glasses] —
 * a row is never deleted, and a zeroed day is simply not exported. */
@Entity(tableName = "water_day")
internal data class WaterDayEntity(
    @PrimaryKey val dateEpochDay: Long,
    val glasses: Int,
)
