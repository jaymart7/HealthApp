package ph.mart.healthapp.feature.home.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.exercise.totalBurnedKcal
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.dailyTotals
import ph.mart.healthapp.core.data.mood.MoodRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.streak.loggedDays
import ph.mart.healthapp.core.data.streak.streakStats
import ph.mart.healthapp.core.data.streak.weightProgressKg
import ph.mart.healthapp.core.data.water.DEFAULT_WATER_GOAL_GLASSES
import ph.mart.healthapp.core.data.water.WaterRepository
import ph.mart.healthapp.core.designsystem.component.todayEpochDay

/**
 * Near-read-only container: the water glasses and the day's mood/energy are the only things this
 * screen writes. The FAB's sheets own every other write path.
 *
 * All five flows are combined rather than snapshotted: this is what stops Home from drifting from
 * the rest of the app (the prototype's Home briefly read a hardcoded profile instead of the shared
 * one). The photo list collapses to its newest date here so the full list never enters UI state.
 *
 * The streak's four day-series ride in a second combine chained onto the first — `combine` only
 * has typed overloads up to five flows, and the streak's inputs are independent of the day's
 * totals anyway. Today's exercise and today's mood join at that same outer combine for the same
 * reason: the inner one is already at the cap.
 */
class HomeViewModel(
    profileRepository: ProfileRepository,
    foodRepository: FoodRepository,
    progressRepository: ProgressRepository,
    private val waterRepository: WaterRepository,
    exerciseRepository: ExerciseRepository,
    private val moodRepository: MoodRepository,
) : ViewModel(), OrbitContainerHost<HomeUiState, HomeUiState, Nothing> {

    override val container = orbitContainer<HomeUiState, Nothing>(HomeUiState()) {
        observeHome(profileRepository, foodRepository, progressRepository, exerciseRepository, moodRepository)
    }

    fun handleEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OnSetWaterGlasses -> onSetWaterGlasses(event.glasses)
            is HomeEvent.OnSetMood -> onSetMood(event.level)
            is HomeEvent.OnSetEnergy -> onSetEnergy(event.level)
        }
    }

    private fun onSetWaterGlasses(glasses: Int) = intent {
        waterRepository.setToday(glasses)
    }

    private fun onSetMood(level: Int) = intent {
        moodRepository.setTodayMood(level)
    }

    private fun onSetEnergy(level: Int) = intent {
        moodRepository.setTodayEnergy(level)
    }

    private fun observeHome(
        profileRepository: ProfileRepository,
        foodRepository: FoodRepository,
        progressRepository: ProgressRepository,
        exerciseRepository: ExerciseRepository,
        moodRepository: MoodRepository,
    ) = intent {
        val today = combine(
            profileRepository.observeProfile(),
            foodRepository.observeTodayEntries(),
            progressRepository.observeWeightEntries(),
            progressRepository.observePhotos(),
            waterRepository.observeToday(),
        ) { profile, entries, weightEntries, photos, waterGlasses ->
            HomeUiState(
                profile = profile,
                totals = entries.dailyTotals(),
                foodEntryCount = entries.size,
                weightEntries = weightEntries,
                lastPhotoEpochDay = photos.maxOfOrNull { it.dateEpochDay },
                waterGlasses = waterGlasses,
                waterGoalGlasses = profile?.waterGoalGlasses ?: DEFAULT_WATER_GOAL_GLASSES,
            )
        }

        val activeDays = combine(
            foodRepository.observeDailyNutrition(),
            waterRepository.observeLoggedDays(),
            progressRepository.observeWeightEntries(),
            exerciseRepository.observeLoggedDays(),
            ::loggedDays,
        )

        combine(
            today,
            activeDays,
            exerciseRepository.observeTodayEntries(),
            moodRepository.observeToday(),
        ) { state, days, exercise, mood ->
            state.copy(
                loaded = true,
                burnedKcal = exercise.totalBurnedKcal(),
                moodLevel = mood.mood,
                energyLevel = mood.energy,
                addExerciseToBudget = state.profile?.addExerciseToBudget != false,
                // Read on every emission, not once at flow-construction time, so the streak
                // doesn't freeze at whatever day the app happened to be opened.
                streak = days.streakStats(todayEpochDay()),
                weightProgressKg = state.profile?.let {
                    weightProgressKg(state.weightEntries, it.goal, it.weightKg)
                },
            )
        }.collect { newState -> reduce { newState } }
    }
}
