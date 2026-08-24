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
)
