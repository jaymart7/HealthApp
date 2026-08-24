package ph.mart.healthapp.core.data.progress.local

import androidx.room3.Entity

/** One row per (part, date) — same replace-by-date-in-place shape as [WeightEntryEntity], scoped
 * per body part via the composite key. */
@Entity(tableName = "measurement_entry", primaryKeys = ["part", "date"])
internal data class MeasurementEntryEntity(
    val part: String,
    val date: Long,
    val valueCm: Double,
)
