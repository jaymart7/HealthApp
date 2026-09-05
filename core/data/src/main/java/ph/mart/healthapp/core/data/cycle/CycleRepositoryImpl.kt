package ph.mart.healthapp.core.data.cycle

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ph.mart.healthapp.core.data.cycle.local.CycleDayDao
import ph.mart.healthapp.core.data.cycle.local.CycleDayEntity
import ph.mart.healthapp.core.data.forToday
import ph.mart.healthapp.core.data.todayEpochDay

internal class CycleRepositoryImpl(private val dao: CycleDayDao) : CycleRepository {

    /** Resolved here, not in a caller: `todayEpochDay()` is internal to this module, the same
     * reason `MoodRepositoryImpl.observeToday()` gives. */
    override fun observeToday(): Flow<CycleDay> = forToday { today ->
        dao.observeForDate(today).map { it?.toDomain() ?: CycleDay(today, flow = 0) }
    }

    override suspend fun setTodayFlow(flow: Int) {
        dao.setFlow(todayEpochDay(), flow.clampToScale())
    }

    override fun observeDays(): Flow<List<CycleDay>> =
        dao.observeLogged().map { entities -> entities.map { it.toDomain() } }

    override suspend fun upsertDay(day: CycleDay) {
        dao.upsert(day.toEntity())
    }

    override suspend fun importDays(days: List<CycleDay>): Int {
        var written = 0
        for (day in days) {
            if ((dao.flowOn(day.dateEpochDay) ?: 0) > 0) continue
            // setFlow, not upsert: a day the user tagged with a symptom but never gave a flow to
            // is still theirs, and an import must not blank the tags off it.
            dao.setFlow(day.dateEpochDay, day.flow.clampToScale())
            written++
        }
        return written
    }

    override suspend fun allDays(): List<CycleDay> = dao.allLogged().map { it.toDomain() }

    override suspend fun clearAllDays() {
        dao.clearAll()
    }
}

/** 0 stays 0 (not logged); anything else lands inside [FLOW_SCALE] rather than reaching the table. */
private fun Int.clampToScale(): Int = if (this <= 0) 0 else coerceIn(FLOW_SCALE)

private fun CycleDayEntity.toDomain() =
    CycleDay(dateEpochDay = dateEpochDay, flow = flow, symptoms = cycleSymptoms(symptoms))

private fun CycleDay.toEntity() = CycleDayEntity(
    dateEpochDay = dateEpochDay,
    flow = flow.clampToScale(),
    symptoms = encodeCycleSymptoms(symptoms),
)
