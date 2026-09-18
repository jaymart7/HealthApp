package ph.mart.healthapp.feature.food.ui.voice.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.RecognitionConfidence
import ph.mart.healthapp.core.designsystem.component.AIChip
import ph.mart.healthapp.core.designsystem.component.AIChipVariant
import ph.mart.healthapp.core.designsystem.component.DockedActionBar
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.components.MealTotalCard
import ph.mart.healthapp.feature.food.ui.shared.components.MealTypeChipRow
import ph.mart.healthapp.feature.food.ui.shared.components.MealTotal
import ph.mart.healthapp.feature.food.ui.shared.components.ReviewItemCard

/**
 * What the sentence became, before any of it is written.
 *
 * One meal slot for the whole batch: a sentence is one meal, and a slot per row would ask four
 * questions to log one breakfast. Rows are collapsed by default and open one at a time — the list
 * is the thing being checked, and a screen of five expanded forms is not a list. The card itself is
 * [ReviewItemCard], in `shared/` because the photo flow reviews a list of estimates too.
 *
 * This screen *is* the trust boundary on the numbers: every figure is shown and adjustable before
 * "Log" writes anything, which is why the parse itself needs no per-item calorie ceiling the way a
 * meal idea offered against a stated budget does.
 *
 * **The total is the headline the screen was missing.** Four rows of calories never answered the
 * question the user actually has. [MealTotal] is derived from the rows and nothing else, and the
 * Log button quotes the same figure — one derivation, so the card and the button cannot disagree.
 *
 * **Discard is a word, not a full-width button.** It sat below Log at the same width, which is the
 * wrong weight for the one destructive thing here. In the docked bar it is a text button at the
 * start and Log takes the rest; with the keyboard up the bar rises and Log stays reachable.
 */
@Composable
internal fun VoiceReviewScreen(
    items: List<AddEntryForm>,
    mealType: MealType,
    expandedIndex: Int?,
    onMealTypeSelect: (MealType) -> Unit,
    onItemChange: (Int, AddEntryForm) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onToggleExpanded: (Int) -> Unit,
    onLog: () -> Unit,
    onDiscard: () -> Unit,
    onSayAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = MealTotal.of(items)
    val unsureCount = items.count { it.confidence == RecognitionConfidence.Low }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // One row where three stacked text styles used to be. The chip says where the
                // numbers came from; the title says what to do; the body line said neither and is
                // gone — the rows themselves are the instruction.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp),
                ) {
                    Text(
                        text = stringResource(R.string.food_voice_review),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    AIChip(label = stringResource(R.string.food_voice_chip), variant = AIChipVariant.Default)
                }

                MealTypeChipRow(selected = mealType, onSelect = onMealTypeSelect)

                MealTotalCard(total = total)

                // Says how many, because "some of these" made the user check all of them. The rows
                // themselves now say which, so this is the summary rather than the whole signal.
                if (unsureCount > 0) {
                    LowConfidenceNotice(count = unsureCount)
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

                // Only where the parse found one thing. Four rows is a plate and the sentence
                // worked; one row after a whole meal was described usually means it didn't, and
                // the fix is the sentence rather than this screen. At four it would just be a
                // fifth thing to read.
                if (items.size == 1) {
                    SecondaryButton(
                        label = stringResource(R.string.food_voice_say_again),
                        onClick = onSayAgain,
                        icon = AppIcons.Mic,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            DockedActionBar {
                // Back already asks before throwing away edits; the button that means the same
                // thing asked nothing at all — the caller still confirms, this only demotes it.
                TextButton(
                    label = stringResource(R.string.food_discard),
                    onClick = onDiscard,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrimaryButton(
                    label = pluralStringResource(
                        R.plurals.food_voice_log_items,
                        items.size,
                        items.size,
                        total.calories,
                    ),
                    onClick = onLog,
                    enabled = items.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** The batch's summary of what the rows already flag individually. No "search instead" door,
 * unlike the photo flow's: the sentence is still one tap back. */
@Composable
private fun LowConfidenceNotice(count: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = AppIcons.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = pluralStringResource(R.plurals.food_voice_low_confidence, count, count),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

private val PREVIEW_ITEMS = listOf(
    AddEntryForm(
        MealType.Breakfast, "Toast", 1.0, "serving", 90, 3, 17, 1,
        confidence = RecognitionConfidence.Low, uncertainAbout = "a slice",
    ),
    AddEntryForm(MealType.Breakfast, "Scrambled eggs", 2.0, "serving", 182, 13, 2, 14),
    AddEntryForm(MealType.Breakfast, "Black coffee", 1.0, "cup", 2, 0, 0, 0),
)

/** The plate the flow exists for: several foods, one slot, the flagged row already at the top. */
@PreviewLightDark
@Composable
private fun VoiceReviewScreenPreview() {
    AppTheme {
        VoiceReviewScreen(
            items = PREVIEW_ITEMS,
            mealType = MealType.Breakfast,
            expandedIndex = null,
            onMealTypeSelect = {},
            onItemChange = { _, _ -> },
            onRemoveItem = {},
            onToggleExpanded = {},
            onLog = {},
            onDiscard = {},
            onSayAgain = {},
        )
    }
}

/** One row open for correction — the 2dp border, the macro cells and the footer's quoted phrase. */
@PreviewLightDark
@Composable
private fun VoiceReviewScreenEditingPreview() {
    AppTheme {
        VoiceReviewScreen(
            items = PREVIEW_ITEMS,
            mealType = MealType.Breakfast,
            expandedIndex = 0,
            onMealTypeSelect = {},
            onItemChange = { _, _ -> },
            onRemoveItem = {},
            onToggleExpanded = {},
            onLog = {},
            onDiscard = {},
            onSayAgain = {},
        )
    }
}

/** One item, no notice, and the door back to the sentence that the four-item screen hides. */
@PreviewLightDark
@Composable
private fun VoiceReviewScreenSingleItemPreview() {
    AppTheme {
        VoiceReviewScreen(
            items = listOf(PREVIEW_ITEMS[1]),
            mealType = MealType.Lunch,
            expandedIndex = null,
            onMealTypeSelect = {},
            onItemChange = { _, _ -> },
            onRemoveItem = {},
            onToggleExpanded = {},
            onLog = {},
            onDiscard = {},
            onSayAgain = {},
        )
    }
}
