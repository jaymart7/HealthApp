package ph.mart.healthapp.feature.food.ui.myfood

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.food.Nutrients
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm

@Composable
internal fun rememberNewFoodState(): NewFoodState =
    rememberSaveable(saver = NewFoodState.Saver()) { NewFoodState() }

/** The draft food — UI-only, the `RecipeBuilderState` rule: nothing reaches Room until Save, so it
 * has to survive a rotation on its own. The form's meal slot is carried and ignored: a food the
 * user owns belongs to no meal, and `toSuggestion()` drops it. */
internal class NewFoodState(
    form: AddEntryForm = AddEntryForm(),
    discardOpen: Boolean = false,
) {
    var form: AddEntryForm by mutableStateOf(form)
    var discardOpen: Boolean by mutableStateOf(discardOpen)

    val isDirty: Boolean get() = form != AddEntryForm()

    companion object {
        /** Only the three micronutrients this screen can type are kept; the other four are
         * always zero here. */
        fun Saver(): Saver<NewFoodState, Any> = listSaver(
            save = {
                val f = it.form
                listOf(
                    f.name, f.portionAmount, f.portionUnit,
                    f.calories, f.proteinG, f.carbsG, f.fatG,
                    f.nutrients.fiberG, f.nutrients.sugarG, f.nutrients.sodiumMg,
                    it.discardOpen,
                )
            },
            restore = { saved ->
                NewFoodState(
                    form = AddEntryForm(
                        name = saved[0] as String,
                        portionAmount = saved[1] as Double,
                        portionUnit = saved[2] as String,
                        calories = saved[3] as Int?,
                        proteinG = saved[4] as Int?,
                        carbsG = saved[5] as Int?,
                        fatG = saved[6] as Int?,
                        nutrients = Nutrients(
                            fiberG = saved[7] as Int,
                            sugarG = saved[8] as Int,
                            sodiumMg = saved[9] as Int,
                        ),
                    ),
                    discardOpen = saved[10] as Boolean,
                )
            },
        )
    }
}
