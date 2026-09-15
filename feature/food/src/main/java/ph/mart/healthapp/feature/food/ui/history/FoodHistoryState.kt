package ph.mart.healthapp.feature.food.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.feature.food.ui.barcode.BarcodeScanScreenState
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm

@Composable
internal fun rememberFoodHistoryScreen(): FoodHistoryScreenState = remember { FoodHistoryScreenState() }

/**
 * Which of this screen's two levels is showing, and the form on the second of them.
 *
 * Screen-local, and plain `remember` for the same reason [BarcodeScanScreenState] is: a
 * half-finished review isn't meaningful to restore across process death, and the row it was seeded
 * from is one tap away in the list behind it. The query and its results are the ViewModel's — they
 * survive a rotation, and a review does not have to.
 */
internal class FoodHistoryScreenState {
    /** Non-null exactly while a row is being reviewed; null is the results list. */
    var form: AddEntryForm? by mutableStateOf(null)

    fun review(entry: FoodEntry) {
        form = entry.toReviewForm()
    }

    fun selectMealType(mealType: MealType) {
        form = form?.copy(mealType = mealType)
    }

    fun dismiss() {
        form = null
    }
}
