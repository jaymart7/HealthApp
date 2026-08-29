package ph.mart.healthapp.core.data.mood.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** One row per day, keyed by local-midnight epoch day. Clearing a tap writes 0 rather than
 * deleting the row, and an all-zero day is simply not read back. */
@Entity(tableName = "mood_day")
internal data class MoodDayEntity(
    @PrimaryKey val dateEpochDay: Long,
    val mood: Int,
    val energy: Int,
)
