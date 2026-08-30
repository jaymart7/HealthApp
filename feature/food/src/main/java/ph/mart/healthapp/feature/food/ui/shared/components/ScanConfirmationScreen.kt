package ph.mart.healthapp.feature.food.ui.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.WindowInsetsRulers
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.component.MacroInputGroup
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.ui.shared.isValid
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm

/**
 * The barcode flow's review step. Same parts as [ConfirmationScreen][ph.mart.healthapp.feature.food.ui.photo.components.ConfirmationScreen] minus the photo and the AI
 * chrome: a barcode match is a database row, not an estimate, so there is no `AIChip` and no
 * confidence notice here — `tertiaryContainer` stays the AI accent alone.
 *
 * [subtitle] carries the per-100 g caveat on a found product, or the "we didn't have it" line when
 * the user landed here from the not-found state.
 */
@Composable
internal fun ScanConfirmationScreen(
    form: AddEntryForm,
    subtitle: String,
    onFormChange: (AddEntryForm) -> Unit,
    onMealTypeSelect: (MealType) -> Unit,
    onLogEntry: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .fitInside(WindowInsetsRulers.Ime.current)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Review this item",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            MealTypeChipRow(selected = form.mealType, onSelect = onMealTypeSelect)

            FoodItemRow(
                variant = FoodItemRowVariant.Editable,
                name = form.name,
                portionAmount = form.portionAmount,
                portionUnit = form.portionUnit,
                calories = form.calories,
                proteinG = form.proteinG,
                carbsG = form.carbsG,
                fatG = form.fatG,
                onNameChange = { onFormChange(form.copy(name = it)) },
                onPortionAmountChange = { onFormChange(form.copy(portionAmount = it)) },
                onPortionUnitChange = { onFormChange(form.copy(portionUnit = it)) },
                onCaloriesChange = { onFormChange(form.copy(calories = it)) },
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Macros",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MacroInputGroup(
                    proteinG = form.proteinG,
                    carbsG = form.carbsG,
                    fatG = form.fatG,
                    onProteinChange = { onFormChange(form.copy(proteinG = it)) },
                    onCarbsChange = { onFormChange(form.copy(carbsG = it)) },
                    onFatChange = { onFormChange(form.copy(fatG = it)) },
                )
            }

            PrimaryButton(
                label = "Log item",
                onClick = onLogEntry,
                enabled = form.isValid(),
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(label = "Discard", onClick = onDiscard, modifier = Modifier.fillMaxWidth())
        }
    }
}

@PreviewLightDark
@Composable
private fun ScanConfirmationScreenPreview() {
    AppTheme {
        ScanConfirmationScreen(
            form = AddEntryForm(
                mealType = MealType.Snacks,
                name = "Nutella",
                portionAmount = 100.0,
                portionUnit = "g",
                calories = 539,
                proteinG = 6,
                carbsG = 58,
                fatG = 31,
            ),
            subtitle = "Values are per 100 g — adjust the portion to match what you ate.",
            onFormChange = {},
            onMealTypeSelect = {},
            onLogEntry = {},
            onDiscard = {},
        )
    }
}
