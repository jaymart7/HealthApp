package ph.mart.healthapp.core.data.progress

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow

enum class MeasurementPart { Chest, Waist, Hips, Arms, Thighs }

data class WeightEntry(val dateEpochDay: Long, val weightKg: Double, val note: String = "")

data class MeasurementEntry(val part: MeasurementPart, val dateEpochDay: Long, val valueCm: Double)

data class ProgressPhoto(
    val id: Long = 0,
    val dateEpochDay: Long,
    val filePath: String,
    val weightKg: Double? = null,
)

enum class ChartRange(val label: String, val days: Int?) {
    OneMonth("1M", 30),
    ThreeMonths("3M", 90),
    SixMonths("6M", 180),
    OneYear("1Y", 365),
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
