package ph.mart.healthapp.feature.progress.ui.cycle

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.cycle.CycleRepository

/**
 * The cycle flow's only container, and the one place this feature writes a cycle day —
 * `BloodPressureViewModel`'s shape and its reasoning: the tab and its sheet sit under one
 * `ViewModelStoreOwner`, so `koinViewModel()` hands them the same instance, and `ProgressViewModel`
 * stays the read-only container its KDoc says it is.
 *
 * No state of its own: the days are already on `ProgressUiState`, and a second copy here would
 * give the page and its sheet two sources that could disagree.
 */
class CycleViewModel(
    private val repository: CycleRepository,
) : ViewModel(), OrbitContainerHost<Unit, Unit, CycleSideEffect> {

    override val container = orbitContainer<Unit, CycleSideEffect>(Unit)

    fun handleEvent(event: CycleEvent) {
        when (event) {
            is CycleEvent.OnSave -> onSave(event.form)
        }
    }

    /** A whole-row write, unlike Home's flow tap: this sheet holds both halves of the day, so
     * saving it can't blank a value it never showed. */
    private fun onSave(form: CycleLogForm) = intent {
        repository.upsertDay(form.toDay())
        postSideEffect(CycleSideEffect.Saved)
    }
}
