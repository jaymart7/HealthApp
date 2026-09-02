package ph.mart.healthapp.feature.profile.ui.routine

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.exercise.RoutineRepository

/**
 * `FoodLibraryViewModel`'s twin, one domain over: reads the unlimited list and writes the two
 * things this screen can do to it. No side effects — a rename and a delete are both writes the
 * flow reports back on its own.
 */
class RoutinesViewModel(
    private val routineRepository: RoutineRepository,
) : ViewModel(), OrbitContainerHost<RoutinesUiState, RoutinesUiState, Nothing> {

    override val container = orbitContainer<RoutinesUiState, Nothing>(RoutinesUiState()) {
        observeRoutines()
    }

    fun handleEvent(event: RoutinesEvent) {
        when (event) {
            is RoutinesEvent.OnDelete -> onDelete(event.id)
            is RoutinesEvent.OnRename -> onRename(event.id, event.name)
            is RoutinesEvent.OnSetDays -> onSetDays(event.id, event.days)
        }
    }

    private fun observeRoutines() = intent {
        routineRepository.observeRoutines().collect { routines ->
            reduce { state.copy(routines = routines) }
        }
    }

    private fun onDelete(id: Long) = intent {
        routineRepository.deleteRoutine(id)
    }

    private fun onRename(id: Long, name: String) = intent {
        routineRepository.renameRoutine(id, name)
    }

    private fun onSetDays(id: Long, days: Int) = intent {
        routineRepository.setRoutineDays(id, days)
    }
}
