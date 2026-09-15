package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.FoodSuggestion
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.food.totalKcal
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R

/**
 * The add-entry sheet's browse list, one row shape at a time.
 *
 * **They are rows in a list, not a stack of cards.** Each of the three used to be a
 * `surfaceContainerHighest` card inside its own titled panel, which is what a *stack of things*
 * looks like — three stacks of them, one under another, is why the sheet needed two screens of
 * scrolling. They are now one list under one tab, separated by start-inset rules, which is the
 * grammar [FoodItemRowVariant.Result] already brought to the search.
 *
 * **What each row does is stated once per tab, in the legend above it** — not by giving three rows
 * three different treatments and hoping the difference reads. What is left to the row itself is the
 * one thing a legend cannot say in advance: whether there is a `+` to log it *now*, beside the tap
 * that merely opens it. A recipe has none, because a recipe only ever seeds.
 */
/** 72dp so a list of these is scanned rather than read — the height the search's own rows use. */
private val RowMinHeight = 72.dp

/**
 * One recently logged or starred food.
 *
 * The name, the detail line and the calorie figure are [FoodItemRowVariant.Result] verbatim: a
 * recent and a search hit are the same thing being asked of the reader — which food is this, and
 * what does it come to — so they are the same row with the same coloured P/C/F markers, not two
 * dialects of one. The star and the `+` are what this list adds.
 */
@Composable
internal fun RecentRow(
    suggestion: FoodSuggestion,
    onSelect: () -> Unit,
    onLogAgain: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SheetRow(onClick = onSelect, modifier = modifier) {
        FoodItemRow(
            variant = FoodItemRowVariant.Result,
            name = suggestion.name,
            portionAmount = suggestion.portionAmount,
            portionUnit = suggestion.portionUnit,
            calories = suggestion.calories,
            proteinG = suggestion.proteinG,
            carbsG = suggestion.carbsG,
            fatG = suggestion.fatG,
            modifier = Modifier.weight(1f),
        )
        // Resolved here: a semantics lambda cannot read a resource, and an icon that changes
        // meaning with its state has to say which state it is in.
        val favoriteLabel = if (suggestion.isFavorite) {
            stringResource(R.string.food_favorite_remove, suggestion.name)
        } else {
            stringResource(R.string.food_favorite_add, suggestion.name)
        }
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = if (suggestion.isFavorite) AppIcons.Favorite.filled else AppIcons.Favorite.outlined,
                contentDescription = favoriteLabel,
                tint = if (suggestion.isFavorite) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        LogNowButton(
            contentDescription = stringResource(R.string.food_log_again, suggestion.name),
            onClick = onLogAgain,
        )
    }
}

/**
 * One saved meal. Tapping the row and tapping the `+` do the same thing — log every item at once —
 * so there is no chevron: there is nowhere else for the row to go.
 */
@Composable
internal fun SavedMealRow(
    meal: SavedMeal,
    onLog: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SheetRow(onClick = onLog, modifier = modifier) {
        RowText(
            name = meal.name,
            detail = stringResource(
                R.string.food_meal_summary,
                pluralStringResource(R.plurals.food_items, meal.items.size, meal.items.size),
                meal.totalKcal(),
            ),
            modifier = Modifier.weight(1f),
        )
        RowIconButton(
            icon = AppIcons.Delete,
            contentDescription = stringResource(R.string.food_delete_saved_meal, meal.name),
            onClick = onDelete,
        )
        LogNowButton(
            contentDescription = stringResource(R.string.food_log_meal, meal.name),
            onClick = onLog,
        )
    }
}

/** The way into the recipe builder, and the reason the Recipes tab is never hidden. */
@Composable
internal fun NewRecipeRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = AppIcons.Add,
            // The label beside it says what this is; two announcements for one control is noise.
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.food_new_recipe),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * The container every browse row shares: full-bleed so the pressed state runs the sheet's width,
 * no fill and no corner so a run of them reads as a list.
 */
@Composable
internal fun SheetRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = RowMinHeight)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** A name over one line of detail — every row but the recent, which borrows the search's. */
@Composable
internal fun RowText(name: String, detail: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * "Log this now", as opposed to the tap on the row beside it, which opens it.
 *
 * Filled `primaryContainer` because it is the only thing in the row that *writes*: the row itself
 * and every other glyph on it lead somewhere, and one of the three needs to look like a commit.
 */
@Composable
internal fun LogNowButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        // The app's one disabled treatment, from `Buttons.kt`. It stays in place rather than
        // appearing once there is something to log: a control that materialises under the finger
        // is a control that moves the row it is in.
        modifier = modifier.graphicsLayer(alpha = if (enabled) 1f else 0.4f).size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = AppIcons.Add, contentDescription = contentDescription)
        }
    }
}

/**
 * A row's quiet secondary glyph — delete, and the recipe's chevron.
 *
 * 48dp, not the 44 the handoff drew and not the 44 fourteen other sites in this app use: the app's
 * own rule is 48 and a row with three targets in its last 150dp is exactly where undershooting it
 * gets noticed.
 */
@Composable
internal fun RowIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@PreviewLightDark
@Composable
private fun AddEntryRowsPreview() {
    AppTheme {
        Surface {
            Column {
                RecentRow(
                    suggestion = FoodSuggestion("Greek yogurt", 170.0, "g", 100, 17, 6, 0, isFavorite = true),
                    onSelect = {},
                    onLogAgain = {},
                    onToggleFavorite = {},
                )
                RecentRow(
                    suggestion = FoodSuggestion("Grilled chicken breast", 150.0, "g", 210, 32, 2, 8, isFavorite = false),
                    onSelect = {},
                    onLogAgain = {},
                    onToggleFavorite = {},
                )
                SavedMealRow(
                    meal = SavedMeal(
                        id = 1,
                        name = "Usual breakfast",
                        items = listOf(
                            SavedMealItem("Greek yogurt", 1.0, "cup", 150, 20, 8, 4),
                            SavedMealItem("Oats", 60.0, "g", 230, 8, 40, 4),
                        ),
                    ),
                    onLog = {},
                    onDelete = {},
                )
                NewRecipeRow(onClick = {})
            }
        }
    }
}
