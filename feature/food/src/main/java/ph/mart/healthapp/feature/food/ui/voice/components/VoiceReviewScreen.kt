package ph.mart.healthapp.feature.food.ui.voice.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.designsystem.component.AIChip
import ph.mart.healthapp.core.designsystem.component.AIChipVariant
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.components.MealTypeChipRow
import ph.mart.healthapp.feature.food.ui.shared.components.ReviewItemCard

/**
 * What the sentence became, before any of it is written.
 *
 * One meal slot for the whole batch: a sentence is one meal, and a slot per row would ask four
 * questions to log one breakfast. Rows are collapsed by default and open one at a time — the list
 * is the thing being checked, and a screen of five expanded forms is not a list. Every open row is
 * the same [FoodItemRow] and [MacroFieldGroup] pair the photo confirmation uses, so a
 * portion change reprices through the existing `withPortionAmount()`. The card itself is
 * [ReviewItemCard], in `shared/` because the photo flow reviews a list of estimates too.
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
                AIChip(label = stringResource(R.string.food_voice_chip), variant = AIChipVariant.Default)
                Text(
                    text = stringResource(R.string.food_voice_review),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.food_voice_review_body),
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
                label = pluralStringResource(R.plurals.food_voice_log_items, items.size, items.size),
                onClick = onLog,
                enabled = items.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(label = stringResource(R.string.food_discard), onClick = onDiscard, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** The batch's notice — one uncertain portion is a reason to read all of them. No "search instead"
 * door, unlike the photo flow's: the sentence is still one tap back. */
@Composable
private fun LowConfidenceNotice() {
    Text(
        text = stringResource(R.string.food_voice_low_confidence),
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
