package ph.mart.healthapp.feature.food.ui.diary

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.SavedMealItem
import ph.mart.healthapp.core.data.health.StepsRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.data.profile.nutrientTargets
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.data.todayFlow
import ph.mart.healthapp.core.data.water.DEFAULT_WATER_GOAL_GLASSES
import ph.mart.healthapp.core.data.water.WaterDay
import ph.mart.healthapp.core.data.water.WaterRepository
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.toFoodEntry
import ph.mart.healthapp.feature.food.ui.shared.toSuggestion

/** No side effects: the add-entry sheet dismisses itself optimistically in [FoodScreen], same
 * pattern [ph.mart.healthapp.ui.QuickActionSheet] already uses — nothing here needs to round-trip
 * through a SideEffect. */
sealed interface FoodSideEffect

class FoodViewModel(
    private val foodRepository: FoodRepository,
    profileRepository: ProfileRepository,
    private val waterRepository: WaterRepository,
    private val exerciseRepository: ExerciseRepository,
    stepsRepository: StepsRepository,
) : ViewModel(), OrbitContainerHost<FoodUiState, FoodUiState, FoodSideEffect> {

    /** The diary's day, and the only thing that re-points the three dated flows below. */
    private val selectedDate = MutableStateFlow(todayEpochDay())

    /** The day a copy is being taken from — null whenever the copy sheet is closed, which is what
     * keeps the three reads behind it off the diary's own path. */
    private val copySourceDate = MutableStateFlow<Long?>(null)

    override val container = orbitContainer<FoodUiState, FoodSideEffect>(FoodUiState()) {
        observeDiary(foodRepository, profileRepository, waterRepository, exerciseRepository, stepsRepository)
        followMidnight()
    }

    fun handleEvent(event: FoodEvent) {
        when (event) {
            is FoodEvent.OnSelectDate -> selectedDate.value = event.dateEpochDay
            is FoodEvent.OnAddEntry -> onAddEntry(event.form)
            is FoodEvent.OnUpdateEntry -> onUpdateEntry(event.id, event.form)
            is FoodEvent.OnDeleteEntry -> onDeleteEntry(event.id)
            is FoodEvent.OnRestoreEntry -> onRestoreEntry(event.entry)
            is FoodEvent.OnToggleFavorite -> onToggleFavorite(event)
            is FoodEvent.OnSaveMyFood -> onSaveMyFood(event.form)
            is FoodEvent.OnSetWaterGlasses -> onSetWaterGlasses(event.glasses)
            is FoodEvent.OnDeleteExercise -> onDeleteExercise(event.id)
            is FoodEvent.OnRestoreExercise -> onRestoreExercise(event.entry)
            is FoodEvent.OnSaveMeal -> onSaveMeal(event.name, event.items)
            is FoodEvent.OnLogSavedMeal -> onLogSavedMeal(event)
            is FoodEvent.OnDeleteSavedMeal -> onDeleteSavedMeal(event.id)
            is FoodEvent.OnDeleteRecipe -> onDeleteRecipe(event.id)
            is FoodEvent.OnPickCopySource -> onPickCopySource(event.dateEpochDay)
            is FoodEvent.OnCopyDay -> onCopyDay(event)
        }
    }

    /**
     * A diary left open overnight stayed pointed at yesterday while every write went to the real
     * today — so an entry added at 00:05 landed on a day the screen wasn't showing.
     *
     * `compareAndSet` is the whole guard: the day only advances when the diary is still on what
     * *was* today. A user who deliberately stepped back to Tuesday stays on Tuesday.
     */
    private fun followMidnight() = intent {
        todayFlow().collect { today -> selectedDate.compareAndSet(today - 1, today) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeDiary(
        foodRepository: FoodRepository,
        profileRepository: ProfileRepository,
        waterRepository: WaterRepository,
        exerciseRepository: ExerciseRepository,
        stepsRepository: StepsRepository,
    ) = intent {
        // Saved meals and recipes belong to no day, so they combine outside the date switch —
        // which also keeps the inner combine at the five-flow arity the typed overloads stop at.
        val dated = selectedDate.flatMapLatest { date ->
            combine(
                foodRepository.observeEntries(date),
                profileRepository.observeProfile(),
                foodRepository.observeSuggestions(),
                waterRepository.observeDay(date),
                // The day's burn comes from two sources now, so they pair up before the combine
                // that is already at its typed-overload arity.
                combine(
                    exerciseRepository.observeEntries(date),
                    stepsRepository.observeSteps(date),
                    ::Pair,
                ),
            ) { entries, profile, suggestions, waterGlasses, (exercise, steps) ->
                FoodUiState(
                    selectedDate = date,
                    entries = entries,
                    exercise = exercise,
                    steps = steps,
                    addExerciseToBudget = profile?.addExerciseToBudget != false,
                    targets = profile?.dailyTargets(),
                    nutrientTargets = profile?.let { nutrientTargets(it, it.dailyTargets()) },
                    goal = profile?.goal,
                    diet = profile?.dietaryPreference,
                    suggestions = suggestions,
                    waterGlasses = waterGlasses,
                    waterGoalGlasses = profile?.waterGoalGlasses ?: DEFAULT_WATER_GOAL_GLASSES,
                    unit = profile?.preferredUnit ?: UnitSystem.Metric,
                )
            }
        }
        combine(
            dated,
            foodRepository.observeSavedMeals(),
            foodRepository.observeRecipes(),
            copySource(),
        ) { newState, savedMeals, recipes, copySource ->
            newState.copy(savedMeals = savedMeals, recipes = recipes, copySource = copySource)
        }.collect { newState -> reduce { newState } }
    }

    /**
     * The source day, read through the same three dated flows the diary itself uses — a copy needs
     * no query of its own. Null while nothing is being copied, so the reads only exist while the
     * sheet is open.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun copySource(): Flow<CopyDay?> = copySourceDate.flatMapLatest { date ->
        if (date == null) {
            flowOf(null)
        } else {
            combine(
                foodRepository.observeEntries(date),
                exerciseRepository.observeEntries(date),
                waterRepository.observeDay(date),
            ) { entries, exercise, glasses -> CopyDay(date, entries, exercise, glasses) }
        }
    }

    /** Copying a day onto itself would only double it, so the day already being shown is not a
     * source: the calendar draws it selected, and a tap on it does nothing. */
    private fun onPickCopySource(dateEpochDay: Long?) {
        if (dateEpochDay == selectedDate.value) return
        copySourceDate.value = dateEpochDay
    }

    /**
     * One batched food write plus a workout each, so a copied day lands in as few emissions as a
     * saved meal does.
     *
     * Water is *set* rather than added, and only from a day that had some — a copy must never zero
     * a count already standing on the day being copied onto.
     */
    private fun onCopyDay(event: FoodEvent.OnCopyDay) = intent {
        val source = state.copySource ?: return@intent
        val target = selectedDate.value
        foodRepository.addEntries(source.foodOnto(target, event.meals))
        if (event.exercise) {
            source.exerciseOnto(target).forEach { exerciseRepository.addEntry(it) }
        }
        if (event.water && source.waterGlasses > 0) {
            waterRepository.upsertDay(WaterDay(dateEpochDay = target, glasses = source.waterGlasses))
        }
        copySourceDate.value = null
    }

    // Dated writes, not the "today" convenience overloads — on a past day those would silently
    // land on the wrong row.
    private fun onSetWaterGlasses(glasses: Int) = intent {
        waterRepository.upsertDay(WaterDay(dateEpochDay = selectedDate.value, glasses = glasses))
    }

    private fun onAddEntry(form: AddEntryForm) = intent {
        foodRepository.addEntry(form.toFoodEntry(dateEpochDay = selectedDate.value))
    }

    private fun onUpdateEntry(id: Long, form: AddEntryForm) = intent {
        foodRepository.updateEntry(form.toFoodEntry(dateEpochDay = selectedDate.value).copy(id = id))
    }

    private fun onDeleteEntry(id: Long) = intent {
        foodRepository.deleteEntry(id)
    }

    /** `id = 0` so the repository inserts rather than collides; the entry keeps its own day, so
     * an undo on Tuesday's diary restores to Tuesday. */
    private fun onRestoreEntry(entry: FoodEntry) = intent {
        foodRepository.addEntry(entry.copy(id = 0))
    }

    private fun onRestoreExercise(entry: ExerciseEntry) = intent {
        exerciseRepository.addEntry(entry.copy(id = 0))
    }

    private fun onDeleteExercise(id: Long) = intent {
        exerciseRepository.deleteEntry(id)
    }

    private fun onToggleFavorite(event: FoodEvent.OnToggleFavorite) = intent {
        foodRepository.setFavorite(event.suggestion, event.favorite)
    }

    /** The star's write, from the form instead of a suggestion row. */
    private fun onSaveMyFood(form: AddEntryForm) = intent {
        foodRepository.setFavorite(form.toSuggestion(), favorite = true)
    }

    private fun onSaveMeal(name: String, items: List<SavedMealItem>) = intent {
        foodRepository.saveMeal(name.trim(), items)
    }

    /** One batched write, so the whole meal appears in the diary at once. */
    private fun onLogSavedMeal(event: FoodEvent.OnLogSavedMeal) = intent {
        foodRepository.addEntries(
            event.meal.items.map { it.toFoodEntry(event.mealType, selectedDate.value) },
        )
    }

    private fun onDeleteSavedMeal(id: Long) = intent {
        foodRepository.deleteSavedMeal(id)
    }

    private fun onDeleteRecipe(id: Long) = intent {
        foodRepository.deleteRecipe(id)
    }
}
