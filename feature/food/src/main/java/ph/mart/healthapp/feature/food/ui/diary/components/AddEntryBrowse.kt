package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.FoodSuggestion
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.diary.BrowseTab
import ph.mart.healthapp.feature.food.ui.recipe.components.RecipeRow

/** Roughly the height five rows would have taken, so the empty state sits where the list does
 * rather than collapsing the sheet around it. */
private val EmptyListHeight = 280.dp

private val PillShape = RoundedCornerShape(28.dp)
private val ChipShape = RoundedCornerShape(24.dp)

/**
 * "Which food?" — the state the sheet opens in.
 *
 * **Three doors above the fold, in frequency order, then one list.** The sheet used to answer this
 * question with four stacked panels of equal weight and a search field between them, which put the
 * commit two screens down and said nothing about where to look. Here the search, the quick add and
 * the list are the first three things on the sheet, and the four panels have become one tabbed list
 * — the same rows, one at a time, with a legend saying what tapping one does instead of three row
 * treatments hoping the difference reads.
 *
 * **The tabs are a filter, not a level.** Back does not step through them, which is why they live in
 * `browseTab` and not in the sheet's own view state.
 *
 * Recipes is always present because its "New recipe" row is the only way into the builder. Recents
 * and Saved meals hide their chip when empty, so a first run shows two chips rather than one full
 * chip and two dead ones.
 */
@Composable
internal fun AddEntryBrowse(
    mealType: MealType,
    tab: BrowseTab,
    suggestions: List<FoodSuggestion>,
    savedMeals: List<SavedMeal>,
    recipes: List<Recipe>,
    quickAddKcal: Int?,
    onTabChange: (BrowseTab) -> Unit,
    onQuickAddChange: (Int?) -> Unit,
    onQuickAdd: () -> Unit,
    onOpenSearch: () -> Unit,
    onSelectSuggestion: (FoodSuggestion) -> Unit,
    onLogAgain: (FoodSuggestion) -> Unit,
    onToggleFavorite: (FoodSuggestion, Boolean) -> Unit,
    onSelectRecipe: (Recipe) -> Unit,
    onDeleteRecipe: (Recipe) -> Unit,
    onNewRecipe: () -> Unit,
    onLogSavedMeal: (SavedMeal) -> Unit,
    onDeleteSavedMeal: (SavedMeal) -> Unit,
    modifier: Modifier = Modifier,
    onGetIdeas: (() -> Unit)? = null,
) {
    val tabs = visibleTabs(savedMeals.isNotEmpty())
    // A tab whose list emptied while the sheet was open — log the last recent away and the chip is
    // gone, so the selection has to fall back rather than show a list that is no longer offered.
    val selected = if (tab in tabs) tab else tabs.first()

    Column(modifier = modifier.fillMaxWidth()) {
        TitleRow(mealType = mealType, onGetIdeas = onGetIdeas)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SearchEntry(onClick = onOpenSearch)
            QuickAddPill(kcal = quickAddKcal, onKcalChange = onQuickAddChange, onLog = onQuickAdd)
        }
        TabChips(
            tabs = tabs,
            selected = selected,
            onSelect = onTabChange,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
        )
        Text(
            text = stringResource(legendFor(selected)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        )
        when (selected) {
            // The one tab with an empty state of its own: the other two either cannot be empty
            // (Recipes always has its "New recipe" row) or hide their chip when they are.
            BrowseTab.Recents -> if (suggestions.isEmpty()) {
                EmptyRecents()
            } else {
                RowList(suggestions) { suggestion ->
                    RecentRow(
                        suggestion = suggestion,
                        onSelect = { onSelectSuggestion(suggestion) },
                        onLogAgain = { onLogAgain(suggestion) },
                        onToggleFavorite = { onToggleFavorite(suggestion, !suggestion.isFavorite) },
                    )
                }
            }
            BrowseTab.Recipes -> Column {
                RowList(recipes) { recipe ->
                    RecipeRow(
                        recipe = recipe,
                        onSelect = { onSelectRecipe(recipe) },
                        onDelete = { onDeleteRecipe(recipe) },
                    )
                }
                if (recipes.isNotEmpty()) SheetRule()
                NewRecipeRow(onClick = onNewRecipe)
            }
            BrowseTab.Saved -> RowList(savedMeals) { meal ->
                SavedMealRow(
                    meal = meal,
                    onLog = { onLogSavedMeal(meal) },
                    onDelete = { onDeleteSavedMeal(meal) },
                )
            }
        }
    }
}

/**
 * Which chips there are to show.
 *
 * Recipes is unconditional — its "New recipe" row is the only door to the builder, so hiding it when
 * there are no recipes would hide the way to make one. Saved meals is a list of things the user has
 * done and nothing else, so an empty one is a chip leading to a dead end and is not drawn.
 *
 * **Recents stays, empty or not**, which is a deliberate departure from the handoff. It says both
 * lists hide when empty *and* specifies a first-run empty state for Recents — with the chip hidden
 * that state is unreachable, and it is the only thing on a first run that explains what the three
 * doors above it are for. First run still shows two chips, not three, and the default tab is still
 * Recents; what changes is which of the two is the one being dropped.
 */
private fun visibleTabs(hasSavedMeals: Boolean): List<BrowseTab> = buildList {
    add(BrowseTab.Recents)
    add(BrowseTab.Recipes)
    if (hasSavedMeals) add(BrowseTab.Saved)
}

/** What tapping a row does, said once per tab. */
private fun legendFor(tab: BrowseTab): Int = when (tab) {
    BrowseTab.Recents -> R.string.food_sheet_legend_recents
    BrowseTab.Recipes -> R.string.food_sheet_legend_recipes
    BrowseTab.Saved -> R.string.food_sheet_legend_saved
}

/**
 * The list, ruled between rows and nowhere else.
 *
 * Fully materialized and capped by count upstream — `MAX_SUGGESTIONS` is five — because nothing
 * inside a bottom sheet may be a lazy list. Empty draws nothing at all: what an empty list *means*
 * differs per tab, so saying it is the caller's job.
 */
@Composable
private fun <T> RowList(items: List<T>, row: @Composable (T) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            if (index > 0) SheetRule()
            row(item)
        }
    }
}

/** A rule between two rows, inset so it reads as a separator rather than a border. */
@Composable
private fun SheetRule() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = 16.dp),
    )
}

/**
 * First run, on the Recents tab.
 *
 * **No call to action inside it.** The three doors it names — search, quick add, add it yourself —
 * are all on screen already, and a fourth button repeating one of them would be the empty state
 * arguing with the sheet around it.
 */
@Composable
private fun EmptyRecents() {
    FullScreenState(
        icon = {
            Icon(
                imageVector = AppIcons.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        },
        heading = stringResource(R.string.food_sheet_empty_title),
        body = stringResource(R.string.food_sheet_empty_body),
        modifier = Modifier.heightIn(min = EmptyListHeight),
    )
}

/**
 * The sheet's title, and the one button that must not cost the column any height when it is absent.
 *
 * "Meal ideas" is hidden rather than disabled when there is no profile or nothing left in the
 * budget — a control that cannot answer should not be there — and putting it *in the title row*
 * rather than above the content is what makes hiding it free.
 */
@Composable
private fun TitleRow(mealType: MealType, onGetIdeas: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.food_add_to, stringResource(mealType.labelRes)),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (onGetIdeas != null) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onGetIdeas)
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = AppIcons.AiSparkle,
                    // The label is right beside it.
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.food_get_ideas),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * The way into the search state — a button that looks like a field, not a field.
 *
 * It was a real text field with a 280dp scrolling results box beneath it, inside a sheet that was
 * itself scrolling. Tapping this instead hands the whole sheet height to one list, which is what
 * stops the two scrollers fighting: they are never on screen together any more.
 */
@Composable
private fun SearchEntry(onClick: () -> Unit) {
    val label = stringResource(R.string.food_sheet_search_open)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = AppIcons.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.food_search_placeholder),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

/**
 * A bare calorie figure, logged without reaching the form at all.
 *
 * The quick add has always been supported — `toFoodEntry()` fills a blank name with
 * `QUICK_ADD_NAME` — but reaching it meant scrolling past four panels to a form and leaving its
 * name empty, which is a thing you have to already know. The pill says it out loud.
 *
 * **It never seeds the form.** The `+` writes the entry and closes the sheet, the same "logs it
 * now" contract every row's filled button has; an em dash while unset, for the reason every other
 * figure on this sheet prints one.
 */
@Composable
private fun QuickAddPill(kcal: Int?, onKcalChange: (Int?) -> Unit, onLog: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val editLabel = stringResource(R.string.food_sheet_quick_add_edit)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(PillShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, PillShape)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.food_quick_add),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .clickable(onClickLabel = editLabel) { focusRequester.requestFocus() }
                .padding(horizontal = 8.dp),
        ) {
            // Sized to its digits for the reason every other figure on this sheet is: a field left
            // to fill takes the whole row and "kcal" then measures against zero.
            Box(modifier = Modifier.width(IntrinsicSize.Min)) {
                if (kcal == null) {
                    Text(
                        text = "—",
                        style = ValueStyle(),
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                BasicTextField(
                    value = kcal?.toString().orEmpty(),
                    onValueChange = { raw -> onKcalChange(raw.filter { it.isDigit() }.take(5).toIntOrNull()) },
                    singleLine = true,
                    textStyle = ValueStyle().copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier
                        .defaultMinSize(minWidth = 28.dp)
                        .focusRequester(focusRequester),
                )
            }
            Text(
                // Not copy.
                text = "kcal",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
        }
        LogNowButton(
            contentDescription = stringResource(R.string.food_sheet_quick_add_log, kcal ?: 0),
            onClick = onLog,
            enabled = kcal != null,
        )
    }
}

@Composable
private fun ValueStyle() =
    MaterialTheme.typography.titleLarge.tabularNums.copy(fontWeight = FontWeight.SemiBold)

/**
 * The three lists, as one single-select row.
 *
 * Equal width so the row reads as a set rather than a sentence, and the app's two border weights —
 * the same pair the meal chips and the portion presets use, because this app has exactly two.
 */
@Composable
private fun TabChips(
    tabs: List<BrowseTab>,
    selected: BrowseTab,
    onSelect: (BrowseTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tabs.forEach { tab ->
            val isSelected = tab == selected
            Surface(
                onClick = { onSelect(tab) },
                shape = ChipShape,
                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .semantics {
                        role = Role.Tab
                        this.selected = isSelected
                    },
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                    Text(text = stringResource(labelFor(tab)), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private fun labelFor(tab: BrowseTab): Int = when (tab) {
    BrowseTab.Recents -> R.string.food_recents
    BrowseTab.Recipes -> R.string.food_recipes
    BrowseTab.Saved -> R.string.food_saved_meals
}

@PreviewLightDark
@Composable
private fun AddEntryBrowsePreview() {
    AppTheme {
        Surface {
            AddEntryBrowse(
                mealType = MealType.Breakfast,
                tab = BrowseTab.Recents,
                suggestions = listOf(
                    FoodSuggestion("Greek yogurt", 170.0, "g", 100, 17, 6, 0, isFavorite = true),
                    FoodSuggestion("Grilled chicken breast", 150.0, "g", 210, 32, 2, 8, isFavorite = false),
                ),
                savedMeals = listOf(
                    SavedMeal(1, "Usual breakfast", listOf(SavedMealItem("Oats", 60.0, "g", 230, 8, 40, 4))),
                ),
                recipes = emptyList(),
                quickAddKcal = null,
                onTabChange = {},
                onQuickAddChange = {},
                onQuickAdd = {},
                onOpenSearch = {},
                onSelectSuggestion = {},
                onLogAgain = {},
                onToggleFavorite = { _, _ -> },
                onSelectRecipe = {},
                onDeleteRecipe = {},
                onNewRecipe = {},
                onLogSavedMeal = {},
                onDeleteSavedMeal = {},
                onGetIdeas = {},
            )
        }
    }
}

/** The recipes tab. Its "New recipe" row is why this chip never hides. */
@PreviewLightDark
@Composable
private fun AddEntryBrowseRecipesPreview() {
    AppTheme {
        Surface {
            AddEntryBrowse(
                mealType = MealType.Dinner,
                tab = BrowseTab.Recipes,
                suggestions = emptyList(),
                savedMeals = emptyList(),
                recipes = listOf(
                    Recipe(1, "Chili", 4, listOf(SavedMealItem("Beans", 400.0, "g", 480, 28, 80, 4))),
                ),
                quickAddKcal = null,
                onTabChange = {},
                onQuickAddChange = {},
                onQuickAdd = {},
                onOpenSearch = {},
                onSelectSuggestion = {},
                onLogAgain = {},
                onToggleFavorite = { _, _ -> },
                onSelectRecipe = {},
                onDeleteRecipe = {},
                onNewRecipe = {},
                onLogSavedMeal = {},
                onDeleteSavedMeal = {},
            )
        }
    }
}

/** First run: two chips, not three, and an empty state that names the doors already on screen. */
@PreviewLightDark
@Composable
private fun AddEntryBrowseEmptyPreview() {
    AppTheme {
        Surface {
            AddEntryBrowse(
                mealType = MealType.Breakfast,
                tab = BrowseTab.Recents,
                suggestions = emptyList(),
                savedMeals = emptyList(),
                recipes = emptyList(),
                quickAddKcal = 320,
                onTabChange = {},
                onQuickAddChange = {},
                onQuickAdd = {},
                onOpenSearch = {},
                onSelectSuggestion = {},
                onLogAgain = {},
                onToggleFavorite = { _, _ -> },
                onSelectRecipe = {},
                onDeleteRecipe = {},
                onNewRecipe = {},
                onLogSavedMeal = {},
                onDeleteSavedMeal = {},
            )
        }
    }
}
