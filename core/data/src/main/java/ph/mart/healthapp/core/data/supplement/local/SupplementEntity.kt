package ph.mart.healthapp.core.data.supplement.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** The user's own list. [deleted] is the soft delete — the row stays so past `supplement_day`
 * rows keep a name to render. */
@Entity(tableName = "supplement")
internal data class SupplementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dose: String,
    val timesPerDay: Int,
    val deleted: Boolean,
    val createdAt: Long,
)
