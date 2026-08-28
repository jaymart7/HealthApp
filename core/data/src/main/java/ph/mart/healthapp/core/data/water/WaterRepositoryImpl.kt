package ph.mart.healthapp.core.data.water

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.data.water.local.WaterDayDao
import ph.mart.healthapp.core.data.water.local.WaterDayEntity

internal class WaterRepositoryImpl(private val dao: WaterDayDao) : WaterRepository {

    /** No row yet is 0 glasses, not an empty card — the UI never has to special-case a null. */
    override fun observeToday(): Flow<Int> =
        dao.observeForDate(todayEpochDay()).map { it?.glasses ?: 0 }

    override suspend fun setToday(glasses: Int) {
        upsertDay(WaterDay(dateEpochDay = todayEpochDay(), glasses = glasses))
    }

    override suspend fun upsertDay(day: WaterDay) {
        dao.upsert(WaterDayEntity(dateEpochDay = day.dateEpochDay, glasses = day.glasses.coerceAtLeast(0)))
    }

    override suspend fun allDays(): List<WaterDay> =
        dao.allNonZero().map { WaterDay(dateEpochDay = it.dateEpochDay, glasses = it.glasses) }

    override suspend fun clearAllDays() {
        dao.clearAll()
    }
}
