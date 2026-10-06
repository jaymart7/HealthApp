package ph.mart.healthapp.feature.profile.ui.routine

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.exercise.ExerciseParseRepository
import ph.mart.healthapp.core.data.exercise.Routine
import ph.mart.healthapp.core.data.exercise.RoutineRepository
import ph.mart.healthapp.core.data.network.NetworkMonitor

/**
 * `FoodLibraryViewModel`'s twin, one domain over: reads the unlimited list and writes what this
 * screen can do to it. An edit and a delete are writes the flow reports back on its own; the one
 * side effect is a New routine design, which the sheet previews before anything is written.
 */
class RoutinesViewModel(
    private val routineRepository: RoutineRepository,
    private val exerciseParseRepository: ExerciseParseRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel(), OrbitContainerHost<RoutinesUiState, RoutinesUiState, RoutinesSideEffect> {

    /** `LogExerciseViewModel.parseJob`'s reason: cancelling the design must not cancel the list. */
    private var designJob: Job? = null

    override val container = orbitContainer<RoutinesUiState, RoutinesSideEffect>(RoutinesUiState()) {
        observeRoutines()
    }

    /** Asked at the moment of the tap, `LogExerciseViewModel.isOnline`'s rule — the offline line is
     * the sheet's, so an offline tap never reaches an intent. */
    fun isOnline(): Boolean = networkMonitor.isOnline()

    fun handleEvent(event: RoutinesEvent) {
        when (event) {
            is RoutinesEvent.OnDelete -> onDelete(event.id)
            is RoutinesEvent.OnUpdate -> onUpdate(event.routine)
            is RoutinesEvent.OnSetDays -> onSetDays(event.id, event.days)
            is RoutinesEvent.OnDesign -> onDesign(event.request)
            RoutinesEvent.OnCancelDesign -> onCancelDesign()
            is RoutinesEvent.OnSaveDesigned -> onSaveDesigned(event.routine)
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

    private fun onUpdate(routine: Routine) = intent {
        routineRepository.updateRoutine(routine.id, routine.name, routine.lifts, routine.days)
    }

    private fun onSetDays(id: Long, days: Int) = intent {
        routineRepository.setRoutineDays(id, days)
    }

    /** The repository swallows everything but a cancellation, so [onCancelDesign] is the only way
     * out that skips the last two lines — and it lowers the flag itself. */
    private fun onDesign(request: String) {
        designJob = intent {
            reduce { state.copy(designing = true) }
            val result = exerciseParseRepository.designRoutine(request)
            reduce { state.copy(designing = false) }
            postSideEffect(RoutinesSideEffect.Designed(result))
        }
    }

    /** `LogExerciseViewModel.onCancelParse`'s two steps, for its reason: the cancel kills the
     * coroutine the reduce would have run in. */
    private fun onCancelDesign() {
        designJob?.cancel()
        intent { reduce { state.copy(designing = false) } }
    }

    /** The list observes the table, so the new row is on screen as soon as this lands. */
    private fun onSaveDesigned(routine: Routine) = intent {
        routineRepository.addRoutine(routine.name, routine.lifts, routine.days)
    }
}
