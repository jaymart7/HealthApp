package ph.mart.healthapp.feature.food.ui.photo.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.WindowInsetsRulers
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.RecognitionConfidence
import ph.mart.healthapp.core.designsystem.component.AIChip
import ph.mart.healthapp.core.designsystem.component.AIChipVariant
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.component.MacroInputGroup
import ph.mart.healthapp.core.designsystem.component.MicronutrientInputGroup
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.components.MealTypeChipRow
import ph.mart.healthapp.feature.food.ui.shared.isValid
import ph.mart.healthapp.feature.food.ui.shared.withPortionAmount

@Composable
internal fun ConfirmationScreen(
    photo: Bitmap,
    form: AddEntryForm,
    confidence: RecognitionConfidence,
    onFormChange: (AddEntryForm) -> Unit,
    onMealTypeSelect: (MealType) -> Unit,
    onSearchInstead: () -> Unit,
    onLogMeal: () -> Unit,
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    bitmap = photo.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AIChip(label = stringResource(R.string.food_photo_chip), variant = AIChipVariant.Default)
                    Text(text = stringResource(R.string.food_photo_review), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            MealTypeChipRow(selected = form.mealType, onSelect = onMealTypeSelect)

            if (confidence == RecognitionConfidence.Low) {
                LowConfidenceNotice(onSearchInstead = onSearchInstead)
            }

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
                onPortionAmountChange = { onFormChange(form.withPortionAmount(it)) },
                onPortionUnitChange = { onFormChange(form.copy(portionUnit = it)) },
                onCaloriesChange = { onFormChange(form.copy(calories = it)) },
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.food_macros), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MacroInputGroup(
                    proteinG = form.proteinG,
                    carbsG = form.carbsG,
                    fatG = form.fatG,
                    onProteinChange = { onFormChange(form.copy(proteinG = it)) },
                    onCarbsChange = { onFormChange(form.copy(carbsG = it)) },
                    onFatChange = { onFormChange(form.copy(fatG = it)) },
                )
                MicronutrientInputGroup(
                    fiberG = form.fiberG,
                    sugarG = form.sugarG,
                    sodiumMg = form.sodiumMg,
                    onFiberChange = { onFormChange(form.copy(fiberG = it)) },
                    onSugarChange = { onFormChange(form.copy(sugarG = it)) },
                    onSodiumChange = { onFormChange(form.copy(sodiumMg = it)) },
                )
            }

            PrimaryButton(label = stringResource(R.string.food_photo_log_meal), onClick = onLogMeal, enabled = form.isValid(), modifier = Modifier.fillMaxWidth())
            TextButton(label = stringResource(R.string.food_discard), onClick = onDiscard, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun LowConfidenceNotice(onSearchInstead: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.food_photo_low_confidence),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        TextButton(label = stringResource(R.string.food_photo_search_instead), onClick = onSearchInstead)
    }
}

@PreviewLightDark
@Composable
private fun ConfirmationScreenPreview() {
    AppTheme {
        ConfirmationScreen(
            photo = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888),
            form = AddEntryForm(
                mealType = MealType.Lunch, name = "Grilled chicken breast", portionAmount = 150.0,
                portionUnit = "g", calories = 210, proteinG = 32, carbsG = 2, fatG = 8,
            ),
            confidence = RecognitionConfidence.High,
            onFormChange = {}, onMealTypeSelect = {}, onSearchInstead = {}, onLogMeal = {}, onDiscard = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ConfirmationScreenLowConfidencePreview() {
    AppTheme {
        ConfirmationScreen(
            photo = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888),
            form = AddEntryForm(
                mealType = MealType.Breakfast, name = "Mixed berries", portionAmount = 1.0,
                portionUnit = "cup", calories = 85, proteinG = 1, carbsG = 21, fatG = 0,
            ),
            confidence = RecognitionConfidence.Low,
            onFormChange = {}, onMealTypeSelect = {}, onSearchInstead = {}, onLogMeal = {}, onDiscard = {},
        )
    }
}
