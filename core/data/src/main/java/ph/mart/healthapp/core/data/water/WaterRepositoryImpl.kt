package ph.mart.healthapp.core.data.water

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ph.mart.healthapp.core.data.streak.STREAK_WINDOW_DAYS
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

    /** Window anchored here, not in the caller: `todayEpochDay()` is internal to this module,
     * same reasoning as `observeDailyNutrition()`. */
    override fun observeLoggedDays(): Flow<Set<Long>> =
        dao.observeLoggedDaysSince(todayEpochDay() - STREAK_WINDOW_DAYS).map { it.toSet() }

    override suspend fun allDays(): List<WaterDay> =
        dao.allNonZero().map { WaterDay(dateEpochDay = it.dateEpochDay, glasses = it.glasses) }

    override suspend fun clearAllDays() {
        dao.clearAll()
    }
}
