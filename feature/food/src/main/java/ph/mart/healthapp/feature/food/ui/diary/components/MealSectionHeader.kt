package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.Motion
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.food.R

/**
 * Screen-specific to the food diary — the collapsible row at the top of a section card: chevron,
 * an optional [leadingIcon], the name, the subtotal, an optional "save as meal" bookmark, and "+".
 * [onSave] is null for a section with nothing in it — there is nothing to snapshot yet.
 *
 * It draws **no divider and no background of its own**: it sits on a card now, and a header that
 * painted its own container would fight the one under it.
 *
 * [subtotalText] is formatted by the caller and null prints nothing. That is what lets the exercise
 * section sign its own figure — four sections above it report calories eaten in this exact slot and
 * that one reports calories *spent*, raising the day's budget rather than filling it. Five numbers
 * down one column that look identical and mean opposite things is a misreading waiting to happen,
 * and the cost of it is believing you ate 903 kcal you did not. A zero subtotal passes null: on an
 * empty day, four "0 kcal" labels are four repetitions of the absence the empty state already says
 * once.
 *
 * [contentDescription] is likewise the caller's, because the exercise section has a different
 * sentence to say about the same layout.
 */
@Composable
internal fun MealSectionHeader(
    label: String,
    expanded: Boolean,
    contentDescription: String,
    onToggle: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    subtotalText: String? = null,
    onSave: (() -> Unit)? = null,
    leadingIcon: ImageVector? = null,
) {
    val expandedLabel = stringResource(R.string.food_section_expanded)
    val collapsedLabel = stringResource(R.string.food_section_collapsed)
    // One glyph turned, not two swapped: the rotation is what says the row folded rather than that
    // a different row arrived.
    val chevronAngle by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        animationSpec = tween(durationMillis = Motion.State, easing = Motion.Standard),
        label = "sectionChevron",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onToggle)
            // Without this a screen reader gets "Breakfast 325 kcal" and an activatable row, with
            // no word about it being a section or which way activating it goes.
            .semantics {
                role = Role.Button
                stateDescription = if (expanded) expandedLabel else collapsedLabel
                this.contentDescription = contentDescription
            }
            .padding(start = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = AppIcons.ChevronDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            // Explicitly square, because the entry rows below indent to clear exactly this.
            modifier = Modifier.size(24.dp).rotate(chevronAngle),
        )
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp).size(20.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        if (subtotalText != null) {
            Text(
                text = subtotalText,
                style = MaterialTheme.typography.bodySmall.tabularNums,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onSave != null) {
            IconButton(onClick = onSave, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = AppIcons.Bookmark,
                    contentDescription = stringResource(R.string.food_section_save_meal, label),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        IconButton(onClick = onAdd, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = AppIcons.Add,
                contentDescription = stringResource(R.string.food_section_add_to, label),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun MealSectionHeaderPreview() {
    AppTheme {
        Surface {
            MealSectionHeader(
                label = "Breakfast",
                expanded = true,
                contentDescription = "Breakfast, 470 kilocalories",
                subtotalText = "470 kcal",
                onToggle = {},
                onAdd = {},
                onSave = {},
            )
        }
    }
}

/** Collapsed, nothing logged, and the exercise variant — the three the meal row never showed. */
@PreviewLightDark
@Composable
private fun MealSectionHeaderVariantsPreview() {
    AppTheme {
        Surface {
            Column {
                MealSectionHeader(
                    label = "Lunch",
                    expanded = false,
                    contentDescription = "Lunch, 480 kilocalories",
                    subtotalText = "480 kcal",
                    onToggle = {},
                    onAdd = {},
                    onSave = {},
                )
                MealSectionHeader(
                    label = "Dinner",
                    expanded = true,
                    contentDescription = "Dinner, 0 kilocalories",
                    onToggle = {},
                    onAdd = {},
                )
                MealSectionHeader(
                    label = "Exercise",
                    expanded = true,
                    contentDescription = "Exercise, 903 kilocalories burned, added to today's budget",
                    subtotalText = "+903 kcal",
                    leadingIcon = AppIcons.Run,
                    onToggle = {},
                    onAdd = {},
                )
            }
        }
    }
}
