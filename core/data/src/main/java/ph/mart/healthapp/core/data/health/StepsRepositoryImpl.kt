package ph.mart.healthapp.core.data.health

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ph.mart.healthapp.core.data.health.local.StepDayDao
import ph.mart.healthapp.core.data.health.local.StepDayEntity
import ph.mart.healthapp.core.data.todayEpochDay

internal class StepsRepositoryImpl(private val dao: StepDayDao) : StepsRepository {

    /** Today-only overload kept beside the dated one: Home genuinely means today. */
    override fun observeToday(): Flow<StepDay?> = observeSteps(todayEpochDay())

    override fun observeSteps(dateEpochDay: Long): Flow<StepDay?> =
        dao.observeForDate(dateEpochDay).map { it?.toStepDay() }

    override fun observeDays(): Flow<List<StepDay>> =
        dao.observeAll().map { entities -> entities.map { it.toStepDay() } }
}

private fun StepDayEntity.toStepDay() =
    StepDay(dateEpochDay = date, steps = steps, burnedKcal = burnedKcal)
