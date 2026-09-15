package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import ph.mart.healthapp.core.data.food.FoodSuggestion
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.SavedMeal
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.food.ScannedProduct
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.component.TonalButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.diary.AddEntryView
import ph.mart.healthapp.feature.food.ui.diary.BrowseTab
import ph.mart.healthapp.feature.food.ui.search.FoodSearchScreen
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.isSaveableFood
import ph.mart.healthapp.feature.food.ui.shared.isValid

/**
 * The diary's log-a-food sheet: **two questions and a docked answer**, plus the search that owns
 * the sheet's whole height while it is showing.
 *
 * It used to be one column of five same-weight blocks — recipes, saved meals, recents, a search
 * with its own 280dp scroller inside this scrolling sheet, and then the form — with the commit
 * underneath all of it. That put Add two screens down, said nothing about where to look, and put
 * two scrollers on screen at once fighting over the same drag.
 *
 * Now: [AddEntryView.Browse] answers "which food?" with three doors above the fold and one tabbed
 * list, [AddEntryView.Form] answers "how much?", and [AddEntryView.Search] takes the full height so
 * the nested scroller is gone — the two lists are never on screen together any more.
 *
 * **Back walks the levels, one at a time.** Search → Browse, Form → Browse, Browse → closed. A
 * correction opens straight into the form and so closes from there: there is no browse state behind
 * it to return to. The tab chips and the micronutrient disclosure are controls, not levels, and back
 * leaves both alone.
 *
 * [editing] turns the same sheet into the correct-a-logged-row sheet. Browse goes with it, because
 * every door on it seeds or writes a *new* log, and two of them write the moment they are tapped —
 * which is not something that can happen while one row is being corrected.
 */
@Composable
internal fun AddEntrySheet(
    mealType: MealType,
    form: AddEntryForm,
    view: AddEntryView,
    browseTab: BrowseTab,
    quickAddKcal: Int?,
    seededFromProduct: Boolean,
    saveMyFood: Boolean,
    suggestions: List<FoodSuggestion>,
    savedMeals: List<SavedMeal>,
    recipes: List<Recipe>,
    onViewChange: (AddEntryView) -> Unit,
    onTabChange: (BrowseTab) -> Unit,
    onQuickAddChange: (Int?) -> Unit,
    onQuickAdd: () -> Unit,
    onSaveMyFoodChange: (Boolean) -> Unit,
    onSelectRecipe: (Recipe) -> Unit,
    onDeleteRecipe: (Recipe) -> Unit,
    onNewRecipe: () -> Unit,
    onLogSavedMeal: (SavedMeal) -> Unit,
    onDeleteSavedMeal: (SavedMeal) -> Unit,
    onFormChange: (AddEntryForm) -> Unit,
    onSelectProduct: (ScannedProduct) -> Unit,
    onSelectSuggestion: (FoodSuggestion) -> Unit,
    onLogAgain: (FoodSuggestion) -> Unit,
    onToggleFavorite: (FoodSuggestion, Boolean) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    /** Null when there is no day to suggest against — no profile yet, or nothing left in the
     * budget. Hidden rather than disabled: a control that can't answer shouldn't be there. */
    onGetIdeas: (() -> Unit)? = null,
    editing: Boolean = false,
    /** When the row being corrected was logged, for the subtitle that says which row it is. */
    loggedAt: Long? = null,
) {
    // One handler, always mounted, dispatching on the state — the shape the photo and voice flows
    // use. `onBack` steps a level and falls through to `onDismiss` when there is none left.
    val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(state = navigationState, onBackCompleted = onBack)

    val scroll = rememberScrollState()
    // Derived so the sheet re-composes when the *answer* changes rather than on every scrolled
    // pixel. `form.name` is deliberately outside it: a derivedStateOf captures a non-state value at
    // the composition it was remembered in, which would freeze the handover on whatever the name
    // was when the sheet opened.
    val pastTitle by remember { derivedStateOf { scrolledPastTitle(scroll.value) } }

    AppBottomSheet(
        onDismiss = onDismiss,
        // Rows run the sheet's full width so their pressed state does too; every other block pads
        // itself by the same 16dp the gutter would have applied.
        horizontalPadding = 0.dp,
        expanded = view == AddEntryView.Search,
        // The search state brings its own scroller and wants the height handed to it, which is the
        // whole point of it being a state rather than a panel.
        scrollable = view != AddEntryView.Search,
        scrollState = scroll,
        bottomBar = {
            when (view) {
                AddEntryView.Browse -> BrowseActionBar(onAddYourself = { onViewChange(AddEntryView.Form) })
                AddEntryView.Form -> FormActionBar(
                    form = form,
                    editing = editing,
                    saveMyFood = saveMyFood,
                    onSaveMyFoodChange = onSaveMyFoodChange,
                    onAdd = onAdd,
                    onCancel = onDismiss,
                )
                // The search draws its own count-and-escape bar at the foot of its list.
                AddEntryView.Search -> Unit
            }
        },
    ) {
        when (view) {
            AddEntryView.Browse -> AddEntryBrowse(
                mealType = mealType,
                tab = browseTab,
                suggestions = suggestions,
                savedMeals = savedMeals,
                recipes = recipes,
                quickAddKcal = quickAddKcal,
                onTabChange = onTabChange,
                onQuickAddChange = onQuickAddChange,
                onQuickAdd = onQuickAdd,
                onOpenSearch = { onViewChange(AddEntryView.Search) },
                onSelectSuggestion = onSelectSuggestion,
                onLogAgain = onLogAgain,
                onToggleFavorite = onToggleFavorite,
                onSelectRecipe = onSelectRecipe,
                onDeleteRecipe = onDeleteRecipe,
                onNewRecipe = onNewRecipe,
                onLogSavedMeal = onLogSavedMeal,
                onDeleteSavedMeal = onDeleteSavedMeal,
                onGetIdeas = onGetIdeas,
            )
            AddEntryView.Search -> FoodSearchScreen(
                onSelectProduct = onSelectProduct,
                // Giving up on the search is the same door "Add it yourself" is, so it lands in
                // the same place: a form with whatever is in it, ready to be typed into.
                onEnterManually = { onViewChange(AddEntryView.Form) },
                onBack = onBack,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                // ModalBottomSheet has already applied the IME inset; applying it again would lift
                // the docked bar twice by the height of the keyboard.
                imeAware = false,
                modifier = Modifier.fillMaxSize(),
            )
            AddEntryView.Form -> AddEntryFormView(
                form = form,
                mealType = mealType,
                editing = editing,
                seededFromProduct = seededFromProduct,
                scrolledPastTitle = pastTitle,
                loggedAt = loggedAt,
                onFormChange = onFormChange,
                onBack = onBack,
            )
        }
    }
}

/**
 * Browse's one action: the door to typing it in yourself.
 *
 * **Tonal, not filled.** Nothing has been picked at this point, so this is a way through rather
 * than a commit — a filled button here would be the sheet's loudest control sitting on the option
 * fewest people want. No Cancel beside it either: the drag handle and the scrim both dismiss, and a
 * cancel button under a list of things to pick is a button for a decision nobody is making.
 */
@Composable
private fun BrowseActionBar(onAddYourself: () -> Unit) {
    SheetActionBar {
        TonalButton(
            label = stringResource(R.string.food_add_yourself),
            onClick = onAddYourself,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        )
    }
}

/**
 * The form's commit, with the keep-this-food switch pinned above it.
 *
 * **Cancel leaves while the keyboard is up** — the review screen's rule, for the same reason: a
 * full-width discard directly under the IME is a button standing where a mis-swipe at the
 * suggestion bar lands. Back still closes the sheet.
 */
@Composable
private fun FormActionBar(
    form: AddEntryForm,
    editing: Boolean,
    saveMyFood: Boolean,
    onSaveMyFoodChange: (Boolean) -> Unit,
    onAdd: () -> Unit,
    onCancel: () -> Unit,
) {
    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    Column(modifier = Modifier.fillMaxWidth()) {
        // Absent while correcting a row, for the reason Browse is: it keeps a *new* food.
        if (!editing) {
            SaveMyFoodRow(
                checked = saveMyFood && form.isSaveableFood(),
                enabled = form.isSaveableFood(),
                onCheckedChange = onSaveMyFoodChange,
            )
        }
        SheetActionBar {
            PrimaryButton(
                // The label is the only thing telling the user a nameless entry will be accepted;
                // the button itself is enabled the moment there are calories.
                label = when {
                    editing -> stringResource(R.string.food_save)
                    form.name.isBlank() -> stringResource(R.string.food_quick_add)
                    else -> stringResource(R.string.food_add)
                },
                onClick = onAdd,
                enabled = form.isValid(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            )
            if (!imeOpen) {
                TextButton(
                    label = stringResource(R.string.food_cancel),
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** The docked bar's chrome. Ruled off rather than floated: the content behind it is a scroll with
 * an edge, and a shadow would only blur that edge. */
@Composable
private fun SheetActionBar(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )
    }
}

@PreviewLightDark
@Composable
private fun AddEntrySheetBrowsePreview() {
    AppTheme {
        AddEntrySheet(
            mealType = MealType.Breakfast,
            form = AddEntryForm(),
            view = AddEntryView.Browse,
            browseTab = BrowseTab.Recents,
            quickAddKcal = null,
            seededFromProduct = false,
            saveMyFood = false,
            suggestions = listOf(
                FoodSuggestion("Greek yogurt", 170.0, "g", 100, 17, 6, 0, isFavorite = true),
                FoodSuggestion("Grilled chicken breast", 150.0, "g", 210, 32, 2, 8, isFavorite = false),
            ),
            savedMeals = listOf(
                SavedMeal(1, "Usual breakfast", listOf(SavedMealItem("Oats", 60.0, "g", 230, 8, 40, 4))),
            ),
            recipes = emptyList(),
            onViewChange = {},
            onTabChange = {},
            onQuickAddChange = {},
            onQuickAdd = {},
            onSaveMyFoodChange = {},
            onSelectRecipe = {},
            onDeleteRecipe = {},
            onNewRecipe = {},
            onLogSavedMeal = {},
            onDeleteSavedMeal = {},
            onFormChange = {},
            onSelectProduct = {},
            onSelectSuggestion = {},
            onLogAgain = {},
            onToggleFavorite = { _, _ -> },
            onBack = {},
            onDismiss = {},
            onAdd = {},
            onGetIdeas = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun AddEntrySheetFormPreview() {
    AppTheme {
        AddEntrySheet(
            mealType = MealType.Snacks,
            form = AddEntryForm(
                name = "Nutella",
                portionAmount = 150.0,
                portionUnit = "g",
                calories = 809,
                proteinG = 9,
                carbsG = 87,
                fatG = 47,
                servingSize = "1 tbsp (15 g)",
            ),
            view = AddEntryView.Form,
            browseTab = BrowseTab.Recents,
            quickAddKcal = null,
            seededFromProduct = true,
            saveMyFood = false,
            suggestions = emptyList(),
            savedMeals = emptyList(),
            recipes = emptyList(),
            onViewChange = {},
            onTabChange = {},
            onQuickAddChange = {},
            onQuickAdd = {},
            onSaveMyFoodChange = {},
            onSelectRecipe = {},
            onDeleteRecipe = {},
            onNewRecipe = {},
            onLogSavedMeal = {},
            onDeleteSavedMeal = {},
            onFormChange = {},
            onSelectProduct = {},
            onSelectSuggestion = {},
            onLogAgain = {},
            onToggleFavorite = { _, _ -> },
            onBack = {},
            onDismiss = {},
            onAdd = {},
        )
    }
}

/** The search, with the sheet's whole height and one list in it — the state that removed the
 * 280dp scroller inside a scrolling sheet. */
@PreviewLightDark
@Composable
private fun AddEntrySheetSearchPreview() {
    AppTheme {
        AddEntrySheet(
            mealType = MealType.Breakfast,
            form = AddEntryForm(),
            view = AddEntryView.Search,
            browseTab = BrowseTab.Recents,
            quickAddKcal = null,
            seededFromProduct = false,
            saveMyFood = false,
            suggestions = emptyList(),
            savedMeals = emptyList(),
            recipes = emptyList(),
            onViewChange = {},
            onTabChange = {},
            onQuickAddChange = {},
            onQuickAdd = {},
            onSaveMyFoodChange = {},
            onSelectRecipe = {},
            onDeleteRecipe = {},
            onNewRecipe = {},
            onLogSavedMeal = {},
            onDeleteSavedMeal = {},
            onFormChange = {},
            onSelectProduct = {},
            onSelectSuggestion = {},
            onLogAgain = {},
            onToggleFavorite = { _, _ -> },
            onBack = {},
            onDismiss = {},
            onAdd = {},
        )
    }
}

/** Correcting a logged row: no browse state behind it, no keeping a food from here. */
@PreviewLightDark
@Composable
private fun AddEntrySheetEditPreview() {
    AppTheme {
        AddEntrySheet(
            mealType = MealType.Lunch,
            form = AddEntryForm(
                name = "Grilled chicken breast",
                portionAmount = 150.0,
                portionUnit = "g",
                calories = 210,
                proteinG = 32,
                carbsG = 2,
                fatG = 8,
            ),
            view = AddEntryView.Form,
            browseTab = BrowseTab.Recents,
            quickAddKcal = null,
            seededFromProduct = false,
            saveMyFood = false,
            suggestions = emptyList(),
            savedMeals = emptyList(),
            recipes = emptyList(),
            onViewChange = {},
            onTabChange = {},
            onQuickAddChange = {},
            onQuickAdd = {},
            onSaveMyFoodChange = {},
            onSelectRecipe = {},
            onDeleteRecipe = {},
            onNewRecipe = {},
            onLogSavedMeal = {},
            onDeleteSavedMeal = {},
            onFormChange = {},
            onSelectProduct = {},
            onSelectSuggestion = {},
            onLogAgain = {},
            onToggleFavorite = { _, _ -> },
            onBack = {},
            onDismiss = {},
            onAdd = {},
            editing = true,
            loggedAt = 1_757_925_720_000,
        )
    }
}
