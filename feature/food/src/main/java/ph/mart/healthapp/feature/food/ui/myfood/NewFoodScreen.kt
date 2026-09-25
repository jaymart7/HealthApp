package ph.mart.healthapp.feature.food.ui.myfood

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.data.food.Nutrients
import ph.mart.healthapp.core.designsystem.component.DiscardConfirmDialog
import ph.mart.healthapp.core.designsystem.component.DockedActionBar
import ph.mart.healthapp.core.designsystem.component.MacroBar
import ph.mart.healthapp.core.designsystem.component.MacroFieldGroup
import ph.mart.healthapp.core.designsystem.component.MicronutrientInputGroup
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.components.CardLabel
import ph.mart.healthapp.feature.food.ui.shared.components.SubjectCard
import ph.mart.healthapp.feature.food.ui.shared.isSaveableFood

/**
 * Authors a food the user owns, reached from Profile → Food library. The same subject card, macro
 * tiles and micronutrient group the scan review draws, minus the meal chips and the Log button:
 * keeping a food and logging it are two intentions, and this screen only has the first — logging
 * needs a meal slot and a day, and nothing here has either.
 *
 * Not a second food form. The fields are the review screen's parts and the write is "Save as my
 * food"'s, so a food made here is the same `favorite_food` row the add-entry sheet would make.
 */
@Composable
fun NewFoodScreen(
    onExit: () -> Unit,
    viewModel: NewFoodViewModel = koinViewModel(),
) {
    val state = rememberNewFoodState()
    viewModel.collectSideEffect { effect ->
        when (effect) {
            NewFoodSideEffect.Saved -> onExit()
        }
    }
    NewFoodContent(
        state = state,
        onSave = { viewModel.handleEvent(NewFoodEvent.OnSave(state.form)) },
        onExit = onExit,
    )
}

@Composable
private fun NewFoodContent(
    state: NewFoodState,
    onSave: () -> Unit,
    onExit: () -> Unit,
) {
    // The recipe builder's rule: back only asks once there is something to lose.
    if (state.isDirty) {
        val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(
            state = navigationState,
            onBackCompleted = { state.discardOpen = true },
        )
    }

    val form = state.form
    val onFormChange: (AddEntryForm) -> Unit = { state.form = it }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                // Nothing seeded it, so the caveat is "enter the values for this portion".
                SubjectCard(form = form, manualEntry = true, onFormChange = onFormChange)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardLabel(stringResource(R.string.food_macros))
                    MacroBar(
                        proteinG = form.proteinG ?: 0,
                        carbsG = form.carbsG ?: 0,
                        fatG = form.fatG ?: 0,
                    )
                    MacroFieldGroup(
                        proteinG = form.proteinG,
                        carbsG = form.carbsG,
                        fatG = form.fatG,
                        onProteinChange = { onFormChange(form.copy(proteinG = it)) },
                        onCarbsChange = { onFormChange(form.copy(carbsG = it)) },
                        onFatChange = { onFormChange(form.copy(fatG = it)) },
                    )
                }

                MicronutrientInputGroup(
                    fiberG = form.nutrients.fiberG.takeIf { it > 0 },
                    sugarG = form.nutrients.sugarG.takeIf { it > 0 },
                    sodiumMg = form.nutrients.sodiumMg.takeIf { it > 0 },
                    onFiberChange = { onFormChange(form.copy(nutrients = form.nutrients.copy(fiberG = it ?: 0))) },
                    onSugarChange = { onFormChange(form.copy(nutrients = form.nutrients.copy(sugarG = it ?: 0))) },
                    onSodiumChange = { onFormChange(form.copy(nutrients = form.nutrients.copy(sodiumMg = it ?: 0))) },
                )
            }

            DockedActionBar {
                PrimaryButton(
                    label = stringResource(R.string.food_new_food_save),
                    onClick = onSave,
                    // A name and some calories — the rule that shows "Save as my food" on the sheet.
                    enabled = form.isSaveableFood(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (state.discardOpen) {
            DiscardConfirmDialog(
                title = stringResource(R.string.food_new_food_discard_title),
                body = stringResource(R.string.food_new_food_not_saved),
                onConfirm = {
                    state.discardOpen = false
                    onExit()
                },
                onDismiss = { state.discardOpen = false },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun NewFoodBlankPreview() {
    AppTheme { NewFoodContent(state = NewFoodState(), onSave = {}, onExit = {}) }
}

@PreviewLightDark
@Composable
private fun NewFoodFilledPreview() {
    AppTheme {
        NewFoodContent(
            state = NewFoodState(
                form = AddEntryForm(
                    name = "Mum's adobo",
                    portionAmount = 1.0,
                    portionUnit = "serving",
                    calories = 420,
                    proteinG = 28,
                    carbsG = 12,
                    fatG = 28,
                    nutrients = Nutrients(sodiumMg = 900),
                ),
            ),
            onSave = {},
            onExit = {},
        )
    }
}
