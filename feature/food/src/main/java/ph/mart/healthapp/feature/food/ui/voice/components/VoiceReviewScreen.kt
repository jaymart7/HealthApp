package ph.mart.healthapp.feature.food.ui.voice.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.designsystem.component.AIChip
import ph.mart.healthapp.core.designsystem.component.AIChipVariant
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.component.MacroInputGroup
import ph.mart.healthapp.core.designsystem.component.MicronutrientInputGroup
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.components.MealTypeChipRow
import ph.mart.healthapp.feature.food.ui.shared.withPortionAmount

/**
 * What the sentence became, before any of it is written.
 *
 * One meal slot for the whole batch: a sentence is one meal, and a slot per row would ask four
 * questions to log one breakfast. Rows are collapsed by default and open one at a time — the list
 * is the thing being checked, and a screen of five expanded forms is not a list. Every open row is
 * the same [FoodItemRow] and [MacroInputGroup] pair the photo and barcode confirmations use, so a
 * portion change reprices through the existing `withPortionAmount()`.
 *
 * This screen *is* the trust boundary on the numbers: every figure is shown and adjustable before
 * "Log" writes anything, which is why the parse itself needs no per-item calorie ceiling the way a
 * meal idea offered against a stated budget does.
 */
@Composable
internal fun VoiceReviewScreen(
    items: List<AddEntryForm>,
    mealType: MealType,
    expandedIndex: Int?,
    lowConfidence: Boolean,
    onMealTypeSelect: (MealType) -> Unit,
    onItemChange: (Int, AddEntryForm) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onToggleExpanded: (Int) -> Unit,
    onLog: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AIChip(label = "AI estimated", variant = AIChipVariant.Default)
                Text(
                    text = "Review what you ate",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Tap a row to adjust it, or remove anything you didn't eat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            MealTypeChipRow(selected = mealType, onSelect = onMealTypeSelect)

            if (lowConfidence) {
                LowConfidenceNotice()
            }

            items.forEachIndexed { index, item ->
                ReviewItemCard(
                    item = item,
                    expanded = expandedIndex == index,
                    onToggleExpanded = { onToggleExpanded(index) },
                    onChange = { onItemChange(index, it) },
                    onRemove = { onRemoveItem(index) },
                )
            }

            PrimaryButton(
                label = if (items.size == 1) "Log 1 item" else "Log ${items.size} items",
                onClick = onLog,
                enabled = items.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(label = "Discard", onClick = onDiscard, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ReviewItemCard(
    item: AddEntryForm,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onChange: (AddEntryForm) -> Unit,
    onRemove: () -> Unit,
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = onToggleExpanded,
                color = Color.Transparent,
                modifier = Modifier.weight(1f),
            ) {
                FoodItemRow(
                    variant = FoodItemRowVariant.Display,
                    name = item.name,
                    portionAmount = item.portionAmount,
                    portionUnit = item.portionUnit,
                    calories = item.calories,
                    proteinG = item.proteinG,
                    carbsG = item.carbsG,
                    fatG = item.fatG,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = AppIcons.Close,
                    contentDescription = "Remove ${item.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                FoodItemRow(
                    variant = FoodItemRowVariant.Editable,
                    name = item.name,
                    portionAmount = item.portionAmount,
                    portionUnit = item.portionUnit,
                    calories = item.calories,
                    proteinG = item.proteinG,
                    carbsG = item.carbsG,
                    fatG = item.fatG,
                    onNameChange = { onChange(item.copy(name = it)) },
                    onPortionAmountChange = { onChange(item.withPortionAmount(it)) },
                    onPortionUnitChange = { onChange(item.copy(portionUnit = it)) },
                    onCaloriesChange = { onChange(item.copy(calories = it)) },
                )
                MacroInputGroup(
                    proteinG = item.proteinG,
                    carbsG = item.carbsG,
                    fatG = item.fatG,
                    onProteinChange = { onChange(item.copy(proteinG = it)) },
                    onCarbsChange = { onChange(item.copy(carbsG = it)) },
                    onFatChange = { onChange(item.copy(fatG = it)) },
                )
                MicronutrientInputGroup(
                    fiberG = item.fiberG,
                    sugarG = item.sugarG,
                    sodiumMg = item.sodiumMg,
                    onFiberChange = { onChange(item.copy(fiberG = it)) },
                    onSugarChange = { onChange(item.copy(sugarG = it)) },
                    onSodiumChange = { onChange(item.copy(sodiumMg = it)) },
                )
            }
        }
    }
}

/** The photo flow's notice, about the batch rather than one plate — one uncertain portion is a
 * reason to read all of them. No "search instead" door: the sentence is still one tap back. */
@Composable
private fun LowConfidenceNotice() {
    Text(
        text = "Not fully sure about some of these — double-check the portions.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    )
}

private val PREVIEW_ITEMS = listOf(
    AddEntryForm(MealType.Breakfast, "Scrambled eggs", 2.0, "serving", 182, 13, 2, 14),
    AddEntryForm(MealType.Breakfast, "Toast", 1.0, "serving", 90, 3, 17, 1),
    AddEntryForm(MealType.Breakfast, "Black coffee", 1.0, "cup", 2, 0, 0, 0),
)

@PreviewLightDark
@Composable
private fun VoiceReviewScreenPreview() {
    AppTheme {
        VoiceReviewScreen(
            items = PREVIEW_ITEMS,
            mealType = MealType.Breakfast,
            expandedIndex = null,
            lowConfidence = false,
            onMealTypeSelect = {},
            onItemChange = { _, _ -> },
            onRemoveItem = {},
            onToggleExpanded = {},
            onLog = {},
            onDiscard = {},
        )
    }
}

/** One row open for correction, and the batch flagged. */
@PreviewLightDark
@Composable
private fun VoiceReviewScreenEditingPreview() {
    AppTheme {
        VoiceReviewScreen(
            items = PREVIEW_ITEMS,
            mealType = MealType.Breakfast,
            expandedIndex = 1,
            lowConfidence = true,
            onMealTypeSelect = {},
            onItemChange = { _, _ -> },
            onRemoveItem = {},
            onToggleExpanded = {},
            onLog = {},
            onDiscard = {},
        )
    }
}
