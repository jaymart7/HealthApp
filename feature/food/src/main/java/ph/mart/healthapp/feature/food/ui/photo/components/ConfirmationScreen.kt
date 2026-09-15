package ph.mart.healthapp.feature.food.ui.photo.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.RecognitionConfidence
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
 * What the camera saw, before any of it is written.
 *
 * A list, because a plate is rice *and* chicken *and* greens and the flow used to log the first of
 * those and throw the rest away. One meal slot for the whole plate, for the voice review screen's
 * reason: it is one meal, and a slot per row would ask four questions to log one lunch. Rows are
 * collapsed and opened one at a time — except a plate that came back as a single food, which the
 * caller opens for you, since collapsing the only row there is would put a tap in front of the form
 * this screen has always opened on.
 *
 * This screen *is* the trust boundary on the estimate: every figure is shown and adjustable before
 * "Log" writes anything.
 */
@Composable
internal fun ConfirmationScreen(
    photo: Bitmap,
    items: List<AddEntryForm>,
    mealType: MealType,
    expandedIndex: Int?,
    confidence: RecognitionConfidence,
    onItemChange: (Int, AddEntryForm) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onToggleExpanded: (Int) -> Unit,
    onViewPhoto: () -> Unit,
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
                // A 64dp centre-crop is the worst look at the plate the numbers were read off,
                // so it opens: tappable, and therefore described rather than decorative.
                Image(
                    bitmap = photo.asImageBitmap(),
                    contentDescription = stringResource(R.string.food_photo_view),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onViewPhoto),
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AIChip(label = stringResource(R.string.food_photo_chip), variant = AIChipVariant.Default)
                    Text(text = stringResource(R.string.food_photo_review), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = stringResource(R.string.food_photo_review_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            MealTypeChipRow(selected = mealType, onSelect = onMealTypeSelect)

            if (confidence == RecognitionConfidence.Low) {
                LowConfidenceNotice(onSearchInstead = onSearchInstead)
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
                label = pluralStringResource(R.plurals.food_photo_log_items, items.size, items.size),
                onClick = onLogMeal,
                // Removing every row is the same statement as Discard, so the button goes dead
                // rather than logging nothing. A blank *name* is fine — `toFoodEntry` degrades it
                // to a quick add, which is the rule `isValid()` already documents.
                enabled = items.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(label = stringResource(R.string.food_discard), onClick = onDiscard, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** About the plate rather than one row — one uncertain portion is a reason to read all of them.
 * Keeps the "search instead" door the voice flow's twin has no need of: there is no sentence to go
 * back and fix here. */
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

private fun previewPhoto(): Bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)

/** The plate the flow exists for: several foods, one slot, nothing open yet. */
@PreviewLightDark
@Composable
private fun ConfirmationScreenPreview() {
    AppTheme {
        ConfirmationScreen(
            photo = previewPhoto(),
            items = listOf(
                AddEntryForm(MealType.Lunch, "Grilled chicken breast", 150.0, "g", 210, 32, 2, 8),
                AddEntryForm(MealType.Lunch, "Steamed rice", 1.0, "cup", 205, 4, 45, 0),
                AddEntryForm(MealType.Lunch, "Sautéed greens", 1.0, "cup", 55, 3, 6, 2),
            ),
            mealType = MealType.Lunch,
            expandedIndex = null,
            confidence = RecognitionConfidence.High,
            onItemChange = { _, _ -> }, onRemoveItem = {}, onToggleExpanded = {},
            onViewPhoto = {}, onMealTypeSelect = {}, onSearchInstead = {}, onLogMeal = {}, onDiscard = {},
        )
    }
}

/** One food, so its row opens — and the plate flagged, so the notice and its door are up. */
@PreviewLightDark
@Composable
private fun ConfirmationScreenLowConfidencePreview() {
    AppTheme {
        ConfirmationScreen(
            photo = previewPhoto(),
            items = listOf(
                AddEntryForm(MealType.Breakfast, "Mixed berries", 1.0, "cup", 85, 1, 21, 0),
            ),
            mealType = MealType.Breakfast,
            expandedIndex = 0,
            confidence = RecognitionConfidence.Low,
            onItemChange = { _, _ -> }, onRemoveItem = {}, onToggleExpanded = {},
            onViewPhoto = {}, onMealTypeSelect = {}, onSearchInstead = {}, onLogMeal = {}, onDiscard = {},
        )
    }
}
