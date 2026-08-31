package ph.mart.healthapp.feature.home.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.fasting.FastingRepository
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.dailyTotals
import ph.mart.healthapp.core.data.health.HeartRepository
import ph.mart.healthapp.core.data.health.SleepRepository
import ph.mart.healthapp.core.data.health.StepsRepository
import ph.mart.healthapp.core.data.health.dayBurnedKcal
import ph.mart.healthapp.core.data.health.stepsCreditKcal
import ph.mart.healthapp.core.data.mood.MoodRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.streak.loggedDays
import ph.mart.healthapp.core.data.streak.streakStats
import ph.mart.healthapp.core.data.streak.weightProgressKg
import ph.mart.healthapp.core.data.fasting.DEFAULT_FAST_GOAL_HOURS
import ph.mart.healthapp.core.data.water.DEFAULT_WATER_GOAL_GLASSES
import ph.mart.healthapp.core.data.water.WaterRepository
import ph.mart.healthapp.core.data.todayEpochDay

/**
 * Near-read-only container: the water glasses, the day's mood/energy and the fasting timer are the
 * only things this screen writes. The FAB's sheets own every other write path.
 *
 * All five flows are combined rather than snapshotted: this is what stops Home from drifting from
 * the rest of the app (the prototype's Home briefly read a hardcoded profile instead of the shared
 * one). The photo list collapses to its newest date here so the full list never enters UI state.
 *
 * The streak's four day-series ride in a second combine chained onto the first — `combine` only
 * has typed overloads up to five flows, and the streak's inputs are independent of the day's
 * totals anyway. Today's exercise joins at that same outer combine for the same reason: the inner
 * one is already at the cap. Mood pairs with the running fast, and sleep with steps and heart rate,
 * so that outer combine stays inside the arity too.
 */
class HomeViewModel(
    profileRepository: ProfileRepository,
    foodRepository: FoodRepository,
    progressRepository: ProgressRepository,
    private val waterRepository: WaterRepository,
    exerciseRepository: ExerciseRepository,
    private val moodRepository: MoodRepository,
    private val fastingRepository: FastingRepository,
    sleepRepository: SleepRepository,
    stepsRepository: StepsRepository,
    heartRepository: HeartRepository,
) : ViewModel(), OrbitContainerHost<HomeUiState, HomeUiState, Nothing> {

    override val container = orbitContainer<HomeUiState, Nothing>(HomeUiState()) {
        observeHome(
            profileRepository,
            foodRepository,
            progressRepository,
            exerciseRepository,
            moodRepository,
            fastingRepository,
            sleepRepository,
            stepsRepository,
            heartRepository,
        )
    }

    fun handleEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OnSetWaterGlasses -> onSetWaterGlasses(event.glasses)
            is HomeEvent.OnSetMood -> onSetMood(event.level)
            is HomeEvent.OnSetEnergy -> onSetEnergy(event.level)
            HomeEvent.OnStartFast -> onStartFast()
            HomeEvent.OnEndFast -> intent { fastingRepository.stop() }
            HomeEvent.OnDiscardFast -> intent { fastingRepository.discardActive() }
        }
    }

    private fun onSetWaterGlasses(glasses: Int) = intent {
        waterRepository.setToday(glasses)
    }

    /** The goal is read off the profile here rather than passed down from the card, so the value
     * snapshotted onto the row is always the one Room holds — see `FastSessionEntity`. */
    private fun onStartFast() = intent {
        fastingRepository.start(state.fastingGoalHours)
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
        fastingRepository: FastingRepository,
        sleepRepository: SleepRepository,
        stepsRepository: StepsRepository,
        heartRepository: HeartRepository,
    ) = intent {
        val todayState = combine(
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
                fastingGoalHours = profile?.fastingGoalHours ?: DEFAULT_FAST_GOAL_HOURS,
            )
        }

        val activeDays = combine(
            foodRepository.observeDailyNutrition(),
            waterRepository.observeLoggedDays(),
            progressRepository.observeWeightEntries(),
            exerciseRepository.observeLoggedDays(),
            ::loggedDays,
        )

        // The three Google Health flows group up before the outer combine: it is already at the
        // five-flow arity the typed overloads stop at.
        val fromWatch = combine(
            sleepRepository.observeLastNight(),
            stepsRepository.observeToday(),
            heartRepository.observeToday(),
            ::Triple,
        )

        // Mood and the running fast pair up for the same reason [fromWatch] does: the outer
        // combine below is already at the five-flow arity the typed overloads stop at.
        val moodAndFast = combine(
            moodRepository.observeToday(),
            fastingRepository.observeActive(),
            ::Pair,
        )

        combine(
            todayState,
            activeDays,
            exerciseRepository.observeTodayEntries(),
            moodAndFast,
            fromWatch,
        ) { state, days, exercise, (mood, activeFast), (lastNight, steps, heart) ->
            state.copy(
                loaded = true,
                // Steps fold in here rather than in budgetKcal(), which stays the single place
                // burned calories reach the day.
                burnedKcal = dayBurnedKcal(exercise, steps),
                stepsCreditKcal = stepsCreditKcal(steps, exercise),
                lastNight = lastNight,
                steps = steps,
                heart = heart,
                moodLevel = mood.mood,
                energyLevel = mood.energy,
                activeFast = activeFast,
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
