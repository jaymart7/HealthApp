package ph.mart.healthapp.core.data.progress

import android.graphics.Bitmap
import androidx.annotation.StringRes
import kotlinx.coroutines.flow.Flow
import ph.mart.healthapp.core.data.R
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.cmToDisplayUnit
import ph.mart.healthapp.core.data.profile.displayUnitToCm
import ph.mart.healthapp.core.data.profile.lengthUnitLabel

/**
 * Five tape-measure sites and one percentage. [percent] is the whole of the difference: a body fat
 * reading is stored as it is typed and drawn as it is typed, where a circumference is stored in cm
 * and drawn in whichever unit the profile prefers. Callers ask the *part*, never the display
 * toggle — [toDisplay], [fromDisplay] and [unitLabel] below are the only three places that branch.
 *
 * `name` is the stored token: the `measurement_entry` primary key and the export's part field. So
 * the display name is a resource beside it, and a sixth value costs no migration — the table is
 * keyed on that string, so `BodyFat` simply starts writing rows of its own.
 */
enum class MeasurementPart(@StringRes val label: Int, val percent: Boolean = false) {
    Chest(R.string.data_measurement_chest),
    Waist(R.string.data_measurement_waist),
    Hips(R.string.data_measurement_hips),
    Arms(R.string.data_measurement_arms),
    Thighs(R.string.data_measurement_thighs),
    BodyFat(R.string.data_measurement_body_fat, percent = true),
}

fun MeasurementPart.toDisplay(value: Double, unit: UnitSystem): Double =
    if (percent) value else value.cmToDisplayUnit(unit)

fun MeasurementPart.fromDisplay(value: Double, unit: UnitSystem): Double =
    if (percent) value else value.displayUnitToCm(unit)

/** "%" is a unit symbol, like kg and cm — not copy, and not a resource. */
fun MeasurementPart.unitLabel(unit: UnitSystem): String = if (percent) "%" else unit.lengthUnitLabel()

/** Stored units, both of them: what the stepper opens at with nothing on record, and the clamp
 * either side of it. Two kinds rather than six constants — a body fat under 1% or over 70% is a
 * slipped finger, and so is a 5cm waist. */
fun MeasurementPart.defaultValue(): Double = if (percent) 20.0 else 80.0

fun MeasurementPart.range(): ClosedFloatingPointRange<Double> = if (percent) 1.0..70.0 else 10.0..250.0

data class WeightEntry(val dateEpochDay: Long, val weightKg: Double, val note: String = "")

/** [value] is centimetres for a circumference and percent for [MeasurementPart.BodyFat] — the part
 * is what says which. The Room column and the export key are both still named `valueCm`: renaming
 * a column is a migration, and renaming a wire field is a schema version, neither bought by a name. */
data class MeasurementEntry(val part: MeasurementPart, val dateEpochDay: Long, val value: Double)

data class ProgressPhoto(
    val id: Long = 0,
    val dateEpochDay: Long,
    val filePath: String,
    val weightKg: Double? = null,
)

/** What a run of progress photos adds up to: kilograms gained or lost between its ends, and the
 * days between them. */
data class PhotoWeightArc(val deltaKg: Double, val days: Long)

/**
 * The arc between the oldest and the newest photo that carry a weight — null unless two shots do
 * and they fall on different dates. The field is optional on a shot (the Add photo sheet's stepper
 * opens at none), and a delta "over 0 days" is not a change over time.
 *
 * Here rather than beside either card that draws it: Home's `ProgressPhotoReminderCard` and the
 * Progress overview's Photos card both report this run, `:feature:*` modules never import each
 * other, and two folds would be two answers to one question.
 */
fun List<ProgressPhoto>.weightArc(): PhotoWeightArc? {
    val weighed = mapNotNull { photo -> photo.weightKg?.let { photo.dateEpochDay to it } }
        .sortedBy { (day, _) -> day }
    val (firstDay, firstKg) = weighed.firstOrNull() ?: return null
    val (lastDay, lastKg) = weighed.last()
    return if (lastDay == firstDay) null else PhotoWeightArc(lastKg - firstKg, lastDay - firstDay)
}

enum class ChartRange(@StringRes val label: Int, val days: Int?) {
    OneMonth(R.string.data_chart_range_1m, 30),
    ThreeMonths(R.string.data_chart_range_3m, 90),
    SixMonths(R.string.data_chart_range_6m, 180),
    OneYear(R.string.data_chart_range_1y, 365),
}

data class WeightPoint(val dateEpochDay: Long, val weightKg: Double, val movingAverageKg: Double)

/** 2-point trailing moving average, computed live on every read — never persisted per-row. This
 * is what makes backdating "just work": inserting a past-dated entry recomputes the whole series
 * from the sorted list rather than needing a stored average to patch. */
fun List<WeightEntry>.withMovingAverage(): List<WeightPoint> {
    val sorted = sortedBy { it.dateEpochDay }
    return sorted.mapIndexed { index, entry ->
        val windowStart = (index - 1).coerceAtLeast(0)
        val window = sorted.subList(windowStart, index + 1)
        WeightPoint(
            dateEpochDay = entry.dateEpochDay,
            weightKg = entry.weightKg,
            movingAverageKg = window.sumOf { it.weightKg } / window.size,
        )
    }
}

fun List<WeightEntry>.inRange(range: ChartRange): List<WeightEntry> {
    val days = range.days ?: return this
    val latest = maxOfOrNull { it.dateEpochDay } ?: return this
    return filter { it.dateEpochDay >= latest - days }
}

interface ProgressRepository {
    fun observeWeightEntries(): Flow<List<WeightEntry>>
    suspend fun upsertWeightEntry(entry: WeightEntry)

    /** Removes one day's weigh-in. Only the Google Health disconnect calls this, to take back
     * exactly the entries it imported. */
    suspend fun deleteWeightEntry(dateEpochDay: Long)

    fun observeMeasurements(): Flow<Map<MeasurementPart, List<MeasurementEntry>>>
    suspend fun upsertMeasurementEntry(entry: MeasurementEntry)

    fun observePhotos(): Flow<List<ProgressPhoto>>
    suspend fun addPhoto(bitmap: Bitmap, dateEpochDay: Long, weightKg: Double?)
}
