package ph.mart.healthapp.core.data.note.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** One row per day, keyed by local-midnight epoch day. Clearing a note writes `""` rather than
 * deleting the row, and a blank day is simply not read back. */
@Entity(tableName = "note_day")
internal data class NoteDayEntity(
    @PrimaryKey val dateEpochDay: Long,
    val text: String,
)
