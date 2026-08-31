package ph.mart.healthapp.core.data.bloodpressure

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ph.mart.healthapp.core.data.bloodpressure.local.BloodPressureReadingDao
import ph.mart.healthapp.core.data.bloodpressure.local.BloodPressureReadingEntity

internal class BloodPressureRepositoryImpl(
    private val dao: BloodPressureReadingDao,
) : BloodPressureRepository {

    override fun observeLatest(): Flow<BloodPressureReading?> =
        dao.observeLatest().map { it?.toReading() }

    override fun observeReadings(): Flow<List<BloodPressureReading>> =
        dao.observeAll().map { readings -> readings.map { it.toReading() } }

    /** Clamped rather than rejected: a stepper held down or a stray digit lands on the nearest
     * reading a cuff could have produced, instead of failing a save the user can't diagnose. */
    override suspend fun addReading(reading: BloodPressureReading) {
        dao.insert(
            BloodPressureReadingEntity(
                takenAtMillis = reading.takenAtMillis,
                systolic = reading.systolic.coerceIn(SYSTOLIC_RANGE),
                diastolic = reading.diastolic.coerceIn(DIASTOLIC_RANGE),
                // Zero stays zero — it means "not entered", and clamping it into the range would
                // invent a pulse the cuff never showed.
                pulseBpm = if (reading.pulseBpm <= 0) 0 else reading.pulseBpm.coerceIn(PULSE_RANGE),
            ),
        )
    }

    override suspend fun deleteReading(id: Long) = dao.softDelete(id)

    override suspend fun allReadings(): List<BloodPressureReading> = dao.allActive().map { it.toReading() }

    override suspend fun clearAllReadings() = dao.softDeleteAll()
}

private fun BloodPressureReadingEntity.toReading() = BloodPressureReading(
    id = id,
    takenAtMillis = takenAtMillis,
    systolic = systolic,
    diastolic = diastolic,
    pulseBpm = pulseBpm,
)
