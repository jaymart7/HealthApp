package ph.mart.healthapp.core.data.exercise

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import ph.mart.healthapp.core.data.exercise.local.RoutineDao
import ph.mart.healthapp.core.data.exercise.local.RoutineEntity
import ph.mart.healthapp.core.data.exercise.local.RoutineLiftEntity

internal class RoutineRepositoryImpl(private val dao: RoutineDao) : RoutineRepository {

    override fun observeRoutines(): Flow<List<Routine>> =
        combine(dao.observeRoutines(), dao.observeLifts(), ::joinLifts)

    override suspend fun addRoutine(name: String, lifts: List<RoutineLift>) {
        dao.insertWithLifts(
            RoutineEntity(name = name.trim(), createdAt = System.currentTimeMillis()),
            lifts.map { RoutineLiftEntity(routineId = 0, exerciseName = it.exerciseName, sets = it.sets, reps = it.reps) },
        )
    }

    override suspend fun renameRoutine(id: Long, name: String) {
        dao.rename(id, name.trim())
    }

    override suspend fun setRoutineDays(id: Long, days: Int) {
        dao.setDays(id, days)
    }

    override suspend fun deleteRoutine(id: Long) {
        dao.softDelete(id)
    }
}

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
