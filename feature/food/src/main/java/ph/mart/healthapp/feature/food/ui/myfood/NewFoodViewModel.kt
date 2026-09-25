package ph.mart.healthapp.feature.food.ui.myfood

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.toSuggestion

/** Reads nothing — a food is authored, not loaded — so, like the recipe builder's, the container's
 * state is empty and the only thing crossing it is "the write landed, leave the screen". */
data object NewFoodUiState

sealed interface NewFoodSideEffect {
    data object Saved : NewFoodSideEffect
}

sealed interface NewFoodEvent {
    data class OnSave(val form: AddEntryForm) : NewFoodEvent
}

class NewFoodViewModel(
    private val foodRepository: FoodRepository,
) : ViewModel(), OrbitContainerHost<NewFoodUiState, NewFoodUiState, NewFoodSideEffect> {

    override val container = orbitContainer<NewFoodUiState, NewFoodSideEffect>(NewFoodUiState)

    fun handleEvent(event: NewFoodEvent) {
        when (event) {
            is NewFoodEvent.OnSave -> onSave(event.form)
        }
    }

    /** "Save as my food"'s own write — the same map and the same row — so a food made here and one
     * kept from the add-entry sheet cannot differ. Saving onto a name that exists replaces it,
     * because the name is the row's identity. */
    private fun onSave(form: AddEntryForm) = intent {
        foodRepository.setFavorite(form.toSuggestion(), favorite = true)
        postSideEffect(NewFoodSideEffect.Saved)
    }
}
