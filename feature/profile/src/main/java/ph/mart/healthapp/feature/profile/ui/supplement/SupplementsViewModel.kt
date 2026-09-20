package ph.mart.healthapp.feature.profile.ui.supplement

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.network.NetworkMonitor
import ph.mart.healthapp.core.data.supplement.SUPPLEMENT_TIMES_PER_DAY
import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.core.data.supplement.SupplementRepository
import ph.mart.healthapp.core.data.supplement.SupplementScanRepository
import ph.mart.healthapp.core.data.supplement.SupplementScanResult
import ph.mart.healthapp.feature.profile.R

/**
 * Reads the list and writes the three things this screen can do to it, plus the one thing it asks
 * a model. The add, the edit and the delete still report themselves through the list they change —
 * [ph.mart.healthapp.feature.profile.ui.library.FoodLibraryViewModel]'s shape — and the lookup is
 * why there are side effects at all: its answer lands in a sheet rather than in the list.
 *
 * It reaches for `SupplementScanRepository` rather than a repository of its own, because a name and
 * a photograph are the same question with different evidence — that interface argues the split.
 */
class SupplementsViewModel(
    private val supplementRepository: SupplementRepository,
    private val supplementScanRepository: SupplementScanRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel(), OrbitContainerHost<SupplementsUiState, SupplementsUiState, SupplementsSideEffect> {

    override val container = orbitContainer<SupplementsUiState, SupplementsSideEffect>(SupplementsUiState()) {
        observeSupplements()
    }

    fun handleEvent(event: SupplementsEvent) {
        when (event) {
            is SupplementsEvent.OnSave -> onSave(event.supplement)
            is SupplementsEvent.OnDelete -> onDelete(event.id)
            is SupplementsEvent.OnLookUp -> onLookUp(event.name)
        }
    }

    /**
     * The online recheck is here rather than in the repository for the reason every AI call site in
     * this app makes it: `isOnline()` is asked at the moment a request is about to be spent, and the
     * answer is a sentence the user reads rather than a failure to retry.
     *
     * `NoLabelFound` is the model declining to invent a formula it does not know, which is what the
     * prompt asks for and the one answer worth having from a name it has never seen.
     */
    private fun onLookUp(name: String) = intent {
        val typed = name.trim()
        if (typed.isBlank() || state.lookingUp) return@intent
        if (!networkMonitor.isOnline()) {
            postSideEffect(SupplementsSideEffect.LookupFailed(R.string.profile_supplements_lookup_offline))
            return@intent
        }
        reduce { state.copy(lookingUp = true) }
        val result = supplementScanRepository.lookUp(typed)
        reduce { state.copy(lookingUp = false) }
        postSideEffect(
            when (result) {
                is SupplementScanResult.Found -> SupplementsSideEffect.LookedUp(result.reading)
                SupplementScanResult.NoLabelFound ->
                    SupplementsSideEffect.LookupFailed(R.string.profile_supplements_lookup_unknown)

                SupplementScanResult.Failed ->
                    SupplementsSideEffect.LookupFailed(R.string.profile_supplements_lookup_failed)
            },
        )
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
