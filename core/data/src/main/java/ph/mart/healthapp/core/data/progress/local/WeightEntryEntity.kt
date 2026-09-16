package ph.mart.healthapp.core.data.progress.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** One row per date — backdating an existing date replaces in place via a plain REPLACE insert,
 * no separate update query needed. */
@Entity(tableName = "weight_entry")
internal data class WeightEntryEntity(
    @PrimaryKey val date: Long,
    val weightKg: Double,
    val note: String,
    /** Minutes past local midnight the reading was taken, `0..1439` — null on a row that never
     * carried one: written before the field existed, or brought in by a provider's sync. Not a
     * timestamp, because [date] is the key and a second copy of the day could drift from it. */
    val minuteOfDay: Int? = null,
)
