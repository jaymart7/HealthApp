package ph.mart.healthapp.feature.profile.ui.supplement

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.supplement.SUPPLEMENT_TIMES_PER_DAY
import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.core.data.supplement.SupplementRepository

/**
 * Reads the list and writes the three things this screen can do to it. No side effects: an add, an
 * edit and a delete are all writes the flow reports back on its own — same shape as
 * [ph.mart.healthapp.feature.profile.ui.library.FoodLibraryViewModel].
 */
class SupplementsViewModel(
    private val supplementRepository: SupplementRepository,
) : ViewModel(), OrbitContainerHost<SupplementsUiState, SupplementsUiState, Nothing> {

    override val container = orbitContainer<SupplementsUiState, Nothing>(SupplementsUiState()) {
        observeSupplements()
    }

    fun handleEvent(event: SupplementsEvent) {
        when (event) {
            is SupplementsEvent.OnSave -> onSave(event.supplement)
            is SupplementsEvent.OnDelete -> onDelete(event.id)
        }
    }

    /** Sorted here rather than in the DAO: the order is this screen's presentation choice, and
     * Home's card reads the same flow and wants the list it already had. */
    private fun observeSupplements() = intent {
        supplementRepository.observeSupplements().collect { supplements ->
            reduce {
                state.copy(
                    supplements = supplements.sortedBy { it.name.lowercase() },
                    loaded = true,
                )
            }
        }
    }

    /** One entry point for both the add and the edit: `id == 0` is what tells Room to generate
     * one, so the sheet doesn't need to know which of the two it is. Blank names are rejected at
     * the sheet's Save button; trimming here is what stops a stray space becoming a name nothing
     * else in the app would have accepted. */
    private fun onSave(supplement: Supplement) = intent {
        val cleaned = supplement.copy(
            name = supplement.name.trim(),
            dose = supplement.dose.trim(),
            timesPerDay = supplement.timesPerDay.coerceIn(SUPPLEMENT_TIMES_PER_DAY),
        )
        // `days` needs no cleaning here: the sheet cannot empty the mask and the repository
        // normalises whatever does arrive, so `copy` carries it untouched.
        if (cleaned.name.isBlank()) return@intent
        if (cleaned.id == 0L) {
            supplementRepository.addSupplement(cleaned)
        } else {
            supplementRepository.updateSupplement(cleaned)
        }
    }

    private fun onDelete(id: Long) = intent {
        supplementRepository.deleteSupplement(id)
    }
}
