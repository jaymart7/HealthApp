package ph.mart.healthapp.core.data.supplement

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ph.mart.healthapp.core.data.forToday
import ph.mart.healthapp.core.data.supplement.local.SupplementDao
import ph.mart.healthapp.core.data.supplement.local.SupplementDayEntity
import ph.mart.healthapp.core.data.supplement.local.SupplementEntity
import ph.mart.healthapp.core.data.todayEpochDay

internal class SupplementRepositoryImpl(private val dao: SupplementDao) : SupplementRepository {

    override fun observeSupplements(): Flow<List<Supplement>> =
        dao.observeActive().map { rows -> rows.map { it.toSupplement() } }

    /**
     * The join happens here rather than in the two feature ViewModels: both of their `combine`
     * blocks are already at the five-flow arity the typed overloads stop at, and a supplement
     * without today's count is not a thing either screen wants.
     */
    override fun observeToday(): Flow<List<SupplementToday>> = forToday { today ->
        combine(dao.observeActive(), dao.observeForDate(today)) { supplements, days ->
            val taken = days.associate { it.supplementId to it.taken }
            supplements.map { SupplementToday(it.toSupplement(), taken[it.id] ?: 0) }
        }
    }

    override fun observeDays(): Flow<List<SupplementDay>> =
        dao.observeAllDays().map { rows -> rows.map { it.toSupplementDay() } }

    override suspend fun addSupplement(supplement: Supplement) {
        dao.upsert(supplement.toEntity(createdAt = System.currentTimeMillis()).copy(id = 0))
    }

    override suspend fun updateSupplement(supplement: Supplement) {
        dao.upsert(supplement.toEntity())
    }

    override suspend fun deleteSupplement(id: Long) {
        dao.softDelete(id)
    }

    /** Clamped to the supplement's own target rather than the caller's: the card and a stale
     * flow emission can disagree about how many doses a row still has. */
    override suspend fun setTakenToday(supplementId: Long, taken: Int) {
        val times = dao.active().firstOrNull { it.id == supplementId }?.timesPerDay ?: return
        dao.setTakenOn(todayEpochDay(), supplementId, taken.coerceIn(0, times))
    }

    override suspend fun allSupplements(): List<Supplement> = dao.all().map { it.toSupplement() }

    override suspend fun allDays(): List<SupplementDay> = dao.allTakenDays().map { it.toSupplementDay() }

    override suspend fun upsertSupplement(supplement: Supplement) {
        dao.upsert(supplement.toEntity())
    }

    override suspend fun upsertDay(day: SupplementDay) {
        dao.upsertDay(
            SupplementDayEntity(
                dateEpochDay = day.dateEpochDay,
                supplementId = day.supplementId,
                taken = day.taken.coerceAtLeast(0),
                dueTimes = day.dueTimes.coerceAtLeast(1),
            ),
        )
    }

    override suspend fun clearAll() {
        dao.clearDays()
        dao.clearSupplements()
    }
}

private fun SupplementEntity.toSupplement() = Supplement(
    id = id,
    name = name,
    dose = dose,
    timesPerDay = timesPerDay,
    deleted = deleted,
    createdAt = createdAt,
)

private fun Supplement.toEntity(createdAt: Long = this.createdAt) = SupplementEntity(
    id = id,
    name = name,
    dose = dose,
    timesPerDay = timesPerDay.coerceIn(SUPPLEMENT_TIMES_PER_DAY),
    deleted = deleted,
    createdAt = createdAt,
)

private fun SupplementDayEntity.toSupplementDay() = SupplementDay(
    dateEpochDay = dateEpochDay,
    supplementId = supplementId,
    taken = taken,
    dueTimes = dueTimes,
)
