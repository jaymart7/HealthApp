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
)
