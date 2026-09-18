package ph.mart.healthapp.feature.food.ui.diary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealIdea
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.toAddEntryForm

/**
 * Which of the add-entry sheet's three states is showing.
 *
 * They are levels, not tabs: back walks Search → Browse → closed and Form → Browse → closed, which
 * is what [FoodScreenState.backFromSheet] returns. An edit opens straight into [Form] and back from
 * there closes the sheet — there is no browse state behind a correction to return to.
 */
internal enum class AddEntryView { Browse, Search, Form }

/**
 * Which list the browse state is showing. A filter, **not** a level — back does not step through it,
 * which is the whole reason it is here and not in [AddEntryView].
 */
internal enum class BrowseTab { Recents, Recipes, Saved }

@Composable
internal fun rememberFoodScreenState(): FoodScreenState =
    rememberSaveable(saver = FoodScreenState.Saver()) { FoodScreenState() }

/** Screen-local UI state — which meal sheet is open, its in-progress form, section expand/collapse,
 * and the diary search query. None of it has meaning outside this screen, per the skill's
 * "UI-only flag" rule; the loaded diary itself lives in [FoodUiState] (the Orbit container). */
internal class FoodScreenState(
    activeMealSheet: MealType? = null,
    addForm: AddEntryForm = AddEntryForm(),
    searchQuery: String = "",
    filterExpanded: Boolean = false,
    expandedMeals: Map<MealType, Boolean> = MealType.entries.associateWith { true },
    exerciseExpanded: Boolean = true,
    calendarOpen: Boolean = false,
    saveMealFor: MealType? = null,
    savedMealName: String = "",
    editingEntryId: Long? = null,
    ideasFor: MealType? = null,
    shareOpen: Boolean = false,
    copyPickerOpen: Boolean = false,
    sheetView: AddEntryView = AddEntryView.Browse,
    browseTab: BrowseTab = BrowseTab.Recents,
    quickAddKcal: Int? = null,
    seededFromProduct: Boolean = false,
    saveMyFood: Boolean = false,
    noteSheetOpen: Boolean = false,
    noteDraft: String = "",
) {
    var activeMealSheet: MealType? by mutableStateOf(activeMealSheet)
    var addForm: AddEntryForm by mutableStateOf(addForm)
    var searchQuery: String by mutableStateOf(searchQuery)

    /** Whether the date header is showing the filter field instead of the date controls.
     * Closing it clears [searchQuery] through [closeFilter] — a filter you can no longer see is
     * one you will not remember is hiding rows. */
    var filterExpanded: Boolean by mutableStateOf(filterExpanded)
    var expandedMeals: Map<MealType, Boolean> by mutableStateOf(expandedMeals)
    var exerciseExpanded: Boolean by mutableStateOf(exerciseExpanded)
    var calendarOpen: Boolean by mutableStateOf(calendarOpen)

    /** Which meal the ideas overlay is suggesting for — null when it's closed. It replaces the
     * sheet rather than sitting over it: the sheet is where a picked idea comes back to. */
    var ideasFor: MealType? by mutableStateOf(ideasFor)

    /**
     * Which logged row the add-entry sheet is *correcting* rather than adding to — null means a
     * new entry.
     *
     * An id rather than the row itself: the screen resolves it back off the loaded day, which
     * keeps this saveable across a rotation without teaching the saver another record shape.
     */
    var editingEntryId: Long? by mutableStateOf(editingEntryId)

    /** Whether the day's share sheet is over the diary. UI-only: what it shows is the day the
     * screen is already holding, so there is nothing to restore but the fact that it was open. */
    var shareOpen: Boolean by mutableStateOf(shareOpen)

    /**
     * Whether the calendar that picks a day to copy *from* is showing. Only the picker: which day
     * was picked, and what is on it, live in [FoodUiState.copySource] — loading a day is a read.
     *
     * Separate from [calendarOpen] because the two calendars answer different questions, and at
     * expanded width one of them is a permanent pane while this one is still a sheet.
     */
    var copyPickerOpen: Boolean by mutableStateOf(copyPickerOpen)

    /** Which meal section's entries the "save this meal" sheet is naming, and the name so far. */
    var saveMealFor: MealType? by mutableStateOf(saveMealFor)
    var savedMealName: String by mutableStateOf(savedMealName)

    /** Which of the add-entry sheet's three states is showing. */
    var sheetView: AddEntryView by mutableStateOf(sheetView)

    /** Which list the browse state is showing. Survives a close-and-reopen on purpose: somebody who
     * logs out of their saved meals twice a day should not retap the chip every time. */
    var browseTab: BrowseTab by mutableStateOf(browseTab)

    /** The bare calorie figure typed into the browse state's quick-add pill. Null is "nobody has
     * said", which is the em dash — the same rule the form's four figures follow. It never seeds
     * [addForm]: the pill logs and closes, or it does nothing. */
    var quickAddKcal: Int? by mutableStateOf(quickAddKcal)

    /**
     * Whether [addForm] was seeded from a per-100 g database row — a search hit or a barcode match.
     *
     * It drives two things that are only true of such a row: the portion control's preset chips
     * (50 g / 150 g / the package's own serving are amounts to preset *against* per-100 g figures)
     * and the "Database values are per 100 g." caveat. A recipe, a recent and a meal idea all seed
     * figures that are already for the portion shown, so they leave it false and get the caveat that
     * says so.
     */
    var seededFromProduct: Boolean by mutableStateOf(seededFromProduct)

    /** Whether the form's "Save as my food" switch is on. Held here rather than committed on the
     * spot because a switch states an intention and [FoodEvent.OnSaveMyFood] acts on it when Add
     * does — which is what a switch beside a button means. */
    var saveMyFood: Boolean by mutableStateOf(saveMyFood)

    /** Whether the day's note is being written, and the text so far. The draft is held here rather
     * than committed per keystroke for the reason [saveMyFood] is: a sheet with a Save button means
     * nothing is written until it is tapped, and backing out of a half-typed note leaves the day's
     * note as it was. */
    var noteSheetOpen: Boolean by mutableStateOf(noteSheetOpen)
    var noteDraft: String by mutableStateOf(noteDraft)

    /** Seeded from what the day already holds, so the sheet opens on the note being corrected. */
    fun openNoteSheet(current: String) {
        noteDraft = current
        noteSheetOpen = true
    }

    fun closeNoteSheet() {
        noteSheetOpen = false
        noteDraft = ""
    }

    fun openSheet(mealType: MealType) {
        addForm = AddEntryForm(mealType = mealType)
        editingEntryId = null
        sheetView = AddEntryView.Browse
        quickAddKcal = null
        seededFromProduct = false
        saveMyFood = false
        activeMealSheet = mealType
    }

    /** The same sheet, seeded from a row that already exists — it saves over that row. Straight into
     * the form: there is nothing to browse for, the food has already been picked. */
    fun openEditSheet(entry: FoodEntry) {
        addForm = entry.toAddEntryForm()
        editingEntryId = entry.id
        sheetView = AddEntryView.Form
        quickAddKcal = null
        seededFromProduct = false
        saveMyFood = false
        activeMealSheet = entry.mealType
    }

    /**
     * A pick, from any of the four doors that seed rather than log.
     *
     * [fromProduct] is true only for a search hit or a barcode match — see [seededFromProduct].
     */
    fun seedForm(form: AddEntryForm, fromProduct: Boolean) {
        addForm = form
        seededFromProduct = fromProduct
        sheetView = AddEntryView.Form
    }

    /**
     * One back press inside the sheet. Returns false when there is no level left to step back to,
     * which is the caller's cue to close the sheet.
     *
     * The tab chips and the micronutrient disclosure are deliberately absent: they are controls, not
     * levels, and undoing a filter is not what anyone means by back.
     */
    fun backFromSheet(): Boolean = when (sheetView) {
        AddEntryView.Search -> { sheetView = AddEntryView.Browse; true }
        // A correction opened straight into the form, so there is no browse state behind it.
        AddEntryView.Form -> if (editingEntryId != null) false else { sheetView = AddEntryView.Browse; true }
        AddEntryView.Browse -> false
    }

    fun closeSheet() {
        activeMealSheet = null
        editingEntryId = null
        sheetView = AddEntryView.Browse
        quickAddKcal = null
        seededFromProduct = false
        saveMyFood = false
    }

    /** Straight from the add-entry sheet, which closes behind it — the handover "New recipe" and
     * "Log sets instead" both make, so back from the overlay lands on the diary rather than
     * reopening a form the user has walked away from. */
    fun openIdeas(mealType: MealType) {
        closeSheet()
        ideasFor = mealType
    }

    fun closeIdeas() {
        ideasFor = null
    }

    /**
     * An idea is a seed, never a row: it reopens the sheet it came from with the fields filled in,
     * where the portion stepper reprices it and Add commits it. That is the same landing a recipe,
     * a recent and a search hit already have, which is why picking one writes nothing.
     */
    fun selectIdea(idea: MealIdea) {
        val mealType = ideasFor ?: return
        ideasFor = null
        editingEntryId = null
        activeMealSheet = mealType
        // An idea's figures are for the serving it describes, not per 100 g — so it seeds like a
        // recipe, not like a search hit.
        seedForm(idea.toAddEntryForm(mealType), fromProduct = false)
    }

    fun openSaveMealSheet(mealType: MealType) {
        savedMealName = mealType.name
        saveMealFor = mealType
    }

    fun closeSaveMealSheet() {
        saveMealFor = null
    }

    fun closeFilter() {
        filterExpanded = false
        searchQuery = ""
    }

    fun toggleExpanded(mealType: MealType) {
        expandedMeals = expandedMeals + (mealType to (expandedMeals[mealType] != true))
    }

    companion object {
        fun Saver(): Saver<FoodScreenState, Any> = listSaver(
            save = {
                val f = it.addForm
                listOf(
                    it.activeMealSheet?.name, it.searchQuery,
                    f.mealType.name, f.name, f.portionAmount, f.portionUnit,
                    f.calories, f.proteinG, f.carbsG, f.fatG,
                ) + MealType.entries.map { m -> it.expandedMeals[m] != false } +
                    listOf(
                        it.exerciseExpanded, it.calendarOpen,
                        it.saveMealFor?.name, it.savedMealName,
                        it.editingEntryId, it.ideasFor?.name,
                        // Appended, never inserted: every index below is positional, so a new
                        // field in the middle would silently re-point all of them.
                        it.filterExpanded,
                        it.shareOpen,
                        it.copyPickerOpen,
                        it.sheetView.name,
                        it.browseTab.name,
                        it.quickAddKcal,
                        it.seededFromProduct,
                        it.saveMyFood,
                        it.noteSheetOpen,
                        it.noteDraft,
                    )
            },
            restore = { saved ->
                FoodScreenState(
                    activeMealSheet = (saved[0] as String?)?.let(MealType::valueOf),
                    searchQuery = saved[1] as String,
                    addForm = AddEntryForm(
                        mealType = MealType.valueOf(saved[2] as String),
                        name = saved[3] as String,
                        portionAmount = saved[4] as Double,
                        portionUnit = saved[5] as String,
                        calories = saved[6] as Int?,
                        proteinG = saved[7] as Int?,
                        carbsG = saved[8] as Int?,
                        fatG = saved[9] as Int?,
                    ),
                    expandedMeals = MealType.entries.mapIndexed { index, m -> m to (saved[10 + index] as Boolean) }.toMap(),
                    exerciseExpanded = saved[10 + MealType.entries.size] as Boolean,
                    calendarOpen = saved[11 + MealType.entries.size] as Boolean,
                    saveMealFor = (saved[12 + MealType.entries.size] as String?)?.let(MealType::valueOf),
                    savedMealName = saved[13 + MealType.entries.size] as String,
                    editingEntryId = saved[14 + MealType.entries.size] as Long?,
                    ideasFor = (saved[15 + MealType.entries.size] as String?)?.let(MealType::valueOf),
                    filterExpanded = saved[16 + MealType.entries.size] as Boolean,
                    shareOpen = saved[17 + MealType.entries.size] as Boolean,
                    copyPickerOpen = saved[18 + MealType.entries.size] as Boolean,
                    sheetView = AddEntryView.valueOf(saved[19 + MealType.entries.size] as String),
                    browseTab = BrowseTab.valueOf(saved[20 + MealType.entries.size] as String),
                    quickAddKcal = saved[21 + MealType.entries.size] as Int?,
                    seededFromProduct = saved[22 + MealType.entries.size] as Boolean,
                    saveMyFood = saved[23 + MealType.entries.size] as Boolean,
                    noteSheetOpen = saved[24 + MealType.entries.size] as Boolean,
                    noteDraft = saved[25 + MealType.entries.size] as String,
                )
            },
        )
    }
}
