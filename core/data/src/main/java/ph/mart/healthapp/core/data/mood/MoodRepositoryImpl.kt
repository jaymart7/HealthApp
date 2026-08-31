package ph.mart.healthapp.core.data.mood

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ph.mart.healthapp.core.data.forToday
import ph.mart.healthapp.core.data.mood.local.MoodDayDao
import ph.mart.healthapp.core.data.mood.local.MoodDayEntity
import ph.mart.healthapp.core.data.todayEpochDay

internal class MoodRepositoryImpl(private val dao: MoodDayDao) : MoodRepository {

    /** Resolved here, not in a caller: `todayEpochDay()` is internal to this module, same as
     * `WaterRepositoryImpl.observeToday()`. */
    override fun observeToday(): Flow<MoodDay> = forToday { today ->
        dao.observeForDate(today).map { it?.toDomain() ?: MoodDay(today, mood = 0, energy = 0) }
    }

    override suspend fun setTodayMood(level: Int) {
        dao.setMood(todayEpochDay(), level.clampToScale())
    }

    override suspend fun setTodayEnergy(level: Int) {
        dao.setEnergy(todayEpochDay(), level.clampToScale())
    }

    override fun observeDays(): Flow<List<MoodDay>> =
        dao.observeLogged().map { entities -> entities.map { it.toDomain() } }

    override suspend fun upsertDay(day: MoodDay) {
        dao.upsert(
            MoodDayEntity(
                dateEpochDay = day.dateEpochDay,
                mood = day.mood.clampToScale(),
                energy = day.energy.clampToScale(),
            ),
        )
    }

    override suspend fun allDays(): List<MoodDay> = dao.allLogged().map { it.toDomain() }

    override suspend fun clearAllDays() {
        dao.clearAll()
    }
}

/** 0 stays 0 (unset); anything else lands inside [MOOD_SCALE] rather than reaching the table. */
private fun Int.clampToScale(): Int = if (this <= 0) 0 else coerceAtMost(MOOD_SCALE.last)

private fun MoodDayEntity.toDomain() = MoodDay(dateEpochDay = dateEpochDay, mood = mood, energy = energy)
