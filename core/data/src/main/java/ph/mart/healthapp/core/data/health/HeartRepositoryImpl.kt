package ph.mart.healthapp.core.data.health

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ph.mart.healthapp.core.data.forToday
import ph.mart.healthapp.core.data.health.local.HeartDayDao
import ph.mart.healthapp.core.data.health.local.HeartDayEntity

internal class HeartRepositoryImpl(private val dao: HeartDayDao) : HeartRepository {

    /** Today-only, like `StepsRepository.observeToday()` — Home genuinely means today. */
    override fun observeToday(): Flow<HeartDay?> = forToday { today ->
        dao.observeForDate(today).map { it?.toHeartDay() }
    }

    override fun observeDays(): Flow<List<HeartDay>> =
        dao.observeAll().map { days -> days.map { it.toHeartDay() } }
}

private fun HeartDayEntity.toHeartDay() =
    HeartDay(dateEpochDay = date, averageBpm = averageBpm, minBpm = minBpm)
