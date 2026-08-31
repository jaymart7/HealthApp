package ph.mart.healthapp.core.data.health

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ph.mart.healthapp.core.data.forToday
import ph.mart.healthapp.core.data.health.local.SleepDayDao
import ph.mart.healthapp.core.data.health.local.SleepDayEntity

internal class SleepRepositoryImpl(private val dao: SleepDayDao) : SleepRepository {

    /** Today-only, like `FoodRepository.observeTodayEntries()` — Home genuinely means today. */
    override fun observeLastNight(): Flow<SleepNight?> = forToday { today ->
        dao.observeForDate(today).map { it?.toSleepNight() }
    }

    override fun observeNights(): Flow<List<SleepNight>> =
        dao.observeAll().map { nights -> nights.map { it.toSleepNight() } }
}

private fun SleepDayEntity.toSleepNight() = SleepNight(dateEpochDay = date, minutesAsleep = minutesAsleep)
