package ph.mart.healthapp.core.data.progress.local

import androidx.room3.Entity

/** One row per (part, date) — same replace-by-date-in-place shape as [WeightEntryEntity], scoped
 * per body part via the composite key. The key being a plain string is what let body fat join as a
 * sixth part with no migration.
 *
 * [valueCm] keeps its name and holds what the part says it holds: centimetres for a circumference,
 * percent for body fat. Renaming a column is a migration, and this one buys nothing — the domain's
 * [MeasurementEntry][ph.mart.healthapp.core.data.progress.MeasurementEntry] calls it `value`. */
@Entity(tableName = "measurement_entry", primaryKeys = ["part", "date"])
internal data class MeasurementEntryEntity(
    val part: String,
    val date: Long,
    val valueCm: Double,
)
