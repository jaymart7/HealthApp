package ph.mart.healthapp.core.data.exercise

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import ph.mart.healthapp.core.data.exercise.local.RoutineDao
import ph.mart.healthapp.core.data.exercise.local.RoutineEntity
import ph.mart.healthapp.core.data.exercise.local.RoutineLiftEntity

internal class RoutineRepositoryImpl(private val dao: RoutineDao) : RoutineRepository {

    override fun observeRoutines(): Flow<List<Routine>> =
        combine(dao.observeRoutines(), dao.observeLifts(), ::joinLifts)

    override suspend fun addRoutine(name: String, lifts: List<RoutineLift>, days: Int) {
        dao.insertWithLifts(routineEntity(name, days), lifts.map(::liftEntity))
    }

    override suspend fun updateRoutine(id: Long, name: String, lifts: List<RoutineLift>, days: Int) {
        dao.replace(id, routineEntity(name, days), lifts.map(::liftEntity))
    }

    override suspend fun setRoutineDays(id: Long, days: Int) {
        dao.setDays(id, days)
    }

    override suspend fun deleteRoutine(id: Long) {
        dao.softDelete(id)
    }
}

private fun routineEntity(name: String, days: Int) =
    RoutineEntity(name = name.trim(), createdAt = System.currentTimeMillis(), days = days)

/** `routineId` is stamped by `RoutineDao.insertWithLifts` once the parent has one. */
private fun liftEntity(lift: RoutineLift) =
    RoutineLiftEntity(routineId = 0, exerciseName = lift.exerciseName, sets = lift.sets, reps = lift.reps)

/** The one place the parent/child join happens, so no read path can return a routine with its
 * lifts missing — `ExerciseRepositoryImpl.joinSets`' reasoning, one table over. */
private fun joinLifts(routines: List<RoutineEntity>, lifts: List<RoutineLiftEntity>): List<Routine> {
    val byRoutine = lifts.groupBy { it.routineId }
    return routines.map { routine ->
        Routine(
            id = routine.id,
            name = routine.name,
            lifts = byRoutine[routine.id].orEmpty().map { RoutineLift(it.exerciseName, it.sets, it.reps) },
            days = routine.days,
        )
    }
}
