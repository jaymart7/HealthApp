package ph.mart.healthapp.feature.progress.ui.cycle

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.cycle.CycleRepository

/**
 * The cycle sheet's container, and the one place this feature writes a cycle day —
 * `LogBloodPressureViewModel`'s shape and its reasoning, and it stays the writer now that the page
 * beside it has become a route with a read-only container of its own (`CycleViewModel`). The sheet
 * has two open sites — the Cycle page, and the overview's empty-card hint — so it is instantiated
 * under two `ViewModelStoreOwner`s, which is harmless: both write through the same repository, and
 * neither holds state.
 *
 * No state of its own: the days it seeds from are handed in by whichever surface opened it, and a
 * second copy here would give the sheet and that surface two sources that could disagree.
 */
class LogCycleViewModel(
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
