package ph.mart.healthapp.core.data.progress.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** Multiple photos per date are allowed (unlike weight/measurement) — the prototype never
 * specified a same-date replace behavior for photos. */
@Entity(tableName = "progress_photo")
internal data class ProgressPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val filePath: String,
    val weightKg: Double?,
    /** Minutes past local midnight the reading was taken, `0..1439` — null on a row that never
     * carried one: written before the field existed, or brought in by a provider's sync. Not a
     * timestamp, because [date] is the key and a second copy of the day could drift from it. */
    val minuteOfDay: Int? = null,
)
