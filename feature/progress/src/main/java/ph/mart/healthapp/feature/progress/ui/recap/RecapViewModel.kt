package ph.mart.healthapp.feature.progress.ui.recap

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.health.DEFAULT_STEP_GOAL
import ph.mart.healthapp.core.data.health.StepDay
import ph.mart.healthapp.core.data.health.StepsRepository
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.mood.MoodRepository
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.progress.goalProjection
import ph.mart.healthapp.core.data.streak.loggedDays
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.data.water.WaterRepository
import ph.mart.healthapp.feature.progress.ui.shared.DEFAULT_RECAP_PERIOD
import ph.mart.healthapp.feature.progress.ui.shared.recap

/**
 * The recap flow's container. It writes nothing: the seven repositories are read so the report is
 * folded once, per period, where the period lives — which is what leaves `RecapUiState` a single
 * `Recap` rather than a second copy of every series the tab already holds. Seven of these flows
 * are also `ProgressViewModel`'s, and that duplication is the price of the recap being a screen
 * with its own package. See `DECISIONS.md` → **Charts & the recap**.
 *
 * It stays an **overlay**, not a route — that half of the original argument is untouched.
 */
class RecapViewModel(
    progressRepository: ProgressRepository,
    profileRepository: ProfileRepository,
    foodRepository: FoodRepository,
    waterRepository: WaterRepository,
    exerciseRepository: ExerciseRepository,
    moodRepository: MoodRepository,
    stepsRepository: StepsRepository,
) : ViewModel(), OrbitContainerHost<RecapUiState, RecapUiState, Nothing> {

    /** The window on show, and the only thing that re-folds the report below. It sits here rather
     * than in `RecapState` so a period picked once survives closing and reopening the overlay. */
    private val period = MutableStateFlow(DEFAULT_RECAP_PERIOD)

    override val container = orbitContainer<RecapUiState, Nothing>(RecapUiState()) {
        observeRecap(
            progressRepository, profileRepository, foodRepository, waterRepository,
            exerciseRepository, moodRepository, stepsRepository,
        )
    }

    fun handleEvent(event: RecapEvent) {
        when (event) {
            is RecapEvent.OnPeriodChange -> period.value = event.period
        }
    }

    private fun observeRecap(
        progressRepository: ProgressRepository,
        profileRepository: ProfileRepository,
        foodRepository: FoodRepository,
        waterRepository: WaterRepository,
        exerciseRepository: ExerciseRepository,
        moodRepository: MoodRepository,
        stepsRepository: StepsRepository,
    ) = intent {
        val body = combine(
            progressRepository.observeWeightEntries(),
            progressRepository.observePhotos(),
            profileRepository.observeProfile(),
            ::RecapBody,
        )

        // The four-domain definition, so this page can never disagree with the streak about what a
        // logged day is — `ProgressViewModel.observeProgress()`'s call, chained for its reason.
        val activeDays = combine(
            foodRepository.observeDailyNutrition(),
            waterRepository.observeLoggedDays(),
            progressRepository.observeWeightEntries(),
            exerciseRepository.observeLoggedDays(),
            ::loggedDays,
        )

        // Grouped before the outer combine, which is already at the five-flow arity the typed
        // overloads stop at. Mood rides with movement for that reason alone.
        val movement = combine(
            exerciseRepository.observeRecentEntries(),
            stepsRepository.observeDays(),
            moodRepository.observeDays(),
            ::RecapMovement,
        )

        combine(
            body,
            activeDays,
            movement,
            foodRepository.observeDailyNutrition(),
            period,
        ) { entries, days, moves, dailyNutrition, window ->
            val profile = entries.profile
            val today = todayEpochDay()
            RecapUiState(
                report = recap(
                    period = window,
                    dailyNutrition = dailyNutrition,
                    activeDays = days,
                    weightEntries = entries.weightEntries,
                    moodDays = moves.moodDays,
                    targets = profile?.dailyTargets(),
                    todayEpochDay = today,
                    exerciseEntries = moves.exercise,
                    stepDays = moves.stepDays,
                    stepGoal = profile?.stepGoal ?: DEFAULT_STEP_GOAL,
                    photos = entries.photos,
                ),
                period = window,
                goal = profile?.goal,
                unit = profile?.preferredUnit ?: UnitSystem.Metric,
                projection = goalProjection(
                    weightEntries = entries.weightEntries,
                    goalWeightKg = profile?.targetWeightKg,
                    goal = profile?.goal,
                    todayEpochDay = today,
                ),
            )
        }.collect { newState -> reduce { newState } }
    }
}

/** Grouped so the outer combine stays inside the typed overloads' five-flow arity. Private and
 * structural — it never leaves this file. */
private data class RecapBody(
    val weightEntries: List<WeightEntry>,
    val photos: List<ProgressPhoto>,
    val profile: Profile?,
)

/** Grouped so the outer combine stays inside the typed overloads' five-flow arity. Private and
 * structural — it never leaves this file. */
private data class RecapMovement(
    val exercise: List<ExerciseEntry>,
    val stepDays: List<StepDay>,
    val moodDays: List<MoodDay>,
)
