package ph.mart.healthapp.core.data.progress

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ph.mart.healthapp.core.data.progress.local.MeasurementEntryDao
import ph.mart.healthapp.core.data.progress.local.MeasurementEntryEntity
import ph.mart.healthapp.core.data.progress.local.ProgressPhotoDao
import ph.mart.healthapp.core.data.progress.local.ProgressPhotoEntity
import ph.mart.healthapp.core.data.progress.local.WeightEntryDao
import ph.mart.healthapp.core.data.progress.local.WeightEntryEntity

internal class ProgressRepositoryImpl(
    private val context: Context,
    private val weightDao: WeightEntryDao,
    private val measurementDao: MeasurementEntryDao,
    private val photoDao: ProgressPhotoDao,
) : ProgressRepository {

    override fun observeWeightEntries(): Flow<List<WeightEntry>> =
        weightDao.observeAll().map { entities -> entities.map { it.toWeightEntry() } }

    override suspend fun upsertWeightEntry(entry: WeightEntry) {
        weightDao.upsert(WeightEntryEntity(date = entry.dateEpochDay, weightKg = entry.weightKg, note = entry.note))
    }

    override fun observeMeasurements(): Flow<Map<MeasurementPart, List<MeasurementEntry>>> =
        measurementDao.observeAll().map { entities ->
            entities.map { it.toMeasurementEntry() }.groupBy { it.part }
        }

    override suspend fun upsertMeasurementEntry(entry: MeasurementEntry) {
        measurementDao.upsert(
            MeasurementEntryEntity(part = entry.part.name, date = entry.dateEpochDay, valueCm = entry.valueCm),
        )
    }

    override fun observePhotos(): Flow<List<ProgressPhoto>> =
        photoDao.observeAll().map { entities -> entities.map { it.toProgressPhoto() } }

    override suspend fun addPhoto(bitmap: Bitmap, dateEpochDay: Long, weightKg: Double?) {
        val dir = File(context.filesDir, "progress_photos").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }
        photoDao.insert(ProgressPhotoEntity(date = dateEpochDay, filePath = file.absolutePath, weightKg = weightKg))
    }
}

private fun WeightEntryEntity.toWeightEntry() = WeightEntry(dateEpochDay = date, weightKg = weightKg, note = note)

private fun MeasurementEntryEntity.toMeasurementEntry() =
    MeasurementEntry(part = MeasurementPart.valueOf(part), dateEpochDay = date, valueCm = valueCm)

private fun ProgressPhotoEntity.toProgressPhoto() =
    ProgressPhoto(id = id, dateEpochDay = date, filePath = filePath, weightKg = weightKg)
