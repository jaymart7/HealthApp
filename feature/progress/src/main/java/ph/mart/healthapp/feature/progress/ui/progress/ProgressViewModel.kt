package ph.mart.healthapp.feature.progress.ui.progress

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureReading
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureRepository
import ph.mart.healthapp.core.data.cycle.CycleDay
import ph.mart.healthapp.core.data.cycle.CycleRepository
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.fasting.DEFAULT_FAST_GOAL_HOURS
import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.fasting.FastingRepository
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.health.HeartDay
import ph.mart.healthapp.core.data.health.DEFAULT_STEP_GOAL
import ph.mart.healthapp.core.data.health.HeartRepository
import ph.mart.healthapp.core.data.health.SleepNight
import ph.mart.healthapp.core.data.health.SleepRepository
import ph.mart.healthapp.core.data.health.StepDay
import ph.mart.healthapp.core.data.health.StepsRepository
import ph.mart.healthapp.core.data.mood.MoodRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.streak.loggedDays
import ph.mart.healthapp.core.data.streak.weightProgressKg
import ph.mart.healthapp.core.data.supplement.SupplementDay
import ph.mart.healthapp.core.data.supplement.SupplementRepository
import ph.mart.healthapp.core.data.water.WaterRepository

/** Read-only container — nothing on the Progress tab itself writes data (see [ProgressUiState]),
 * so there's no handleEvent/Event pair here, unlike the other screens in this feature. The water
 * repository is read for one thing only: the weekly recap's logged-day count. Exercise is read
 * twice — for that count, and for the Activity tab's burn series. */
class ProgressViewModel(
    progressRepository: ProgressRepository,
    profileRepository: ProfileRepository,
    foodRepository: FoodRepository,
    waterRepository: WaterRepository,
    exerciseRepository: ExerciseRepository,
    moodRepository: MoodRepository,
    cycleRepository: CycleRepository,
    sleepRepository: SleepRepository,
    heartRepository: HeartRepository,
    fastingRepository: FastingRepository,
    supplementRepository: SupplementRepository,
    bloodPressureRepository: BloodPressureRepository,
    stepsRepository: StepsRepository,
) : ViewModel(), OrbitContainerHost<ProgressUiState, ProgressUiState, Nothing> {

    override val container = orbitContainer<ProgressUiState, Nothing>(ProgressUiState()) {
        observeProgress(
            progressRepository, profileRepository, foodRepository, waterRepository, exerciseRepository,
            moodRepository, cycleRepository, sleepRepository, heartRepository, fastingRepository,
            supplementRepository, bloodPressureRepository, stepsRepository,
        )
    }

    private fun observeProgress(
        progressRepository: ProgressRepository,
        profileRepository: ProfileRepository,
        foodRepository: FoodRepository,
        waterRepository: WaterRepository,
        exerciseRepository: ExerciseRepository,
        moodRepository: MoodRepository,
        cycleRepository: CycleRepository,
        sleepRepository: SleepRepository,
        heartRepository: HeartRepository,
        fastingRepository: FastingRepository,
        supplementRepository: SupplementRepository,
        bloodPressureRepository: BloodPressureRepository,
        stepsRepository: StepsRepository,
    ) = intent {
        val progress = combine(
            progressRepository.observeWeightEntries(),
            progressRepository.observeMeasurements(),
            progressRepository.observePhotos(),
            profileRepository.observeProfile(),
            foodRepository.observeDailyNutrition(),
        ) { weightEntries, measurements, photos, profile, dailyNutrition ->
            ProgressUiState(
                weightEntries = weightEntries,
                measurements = measurements,
                photos = photos,
                goalWeightKg = profile?.targetWeightKg,
                goal = profile?.goal,
                preferredUnit = profile?.preferredUnit ?: UnitSystem.Metric,
                dailyNutrition = dailyNutrition,
                // Computed live off the profile, same as every other place targets are shown.
                targets = profile?.dailyTargets(),
                fastingGoalHours = profile?.fastingGoalHours ?: DEFAULT_FAST_GOAL_HOURS,
                stepGoal = profile?.stepGoal ?: DEFAULT_STEP_GOAL,
                // null means never asked, which is off — the reading `Profile.cycleTrackingOn` gives.
                cycleTrackingOn = profile?.cycleTrackingOn == true,
                // The same call HomeViewModel makes, from the one lambda that already holds both
                // halves — no extra flow, and the outer combine's arity is untouched.
                weightProgressKg = profile?.let {
                    weightProgressKg(weightEntries, it.goal, it.weightKg)
                },
            )
        }

        // Chained rather than widened: `combine` only has typed overloads up to five flows, and
        // the logged-day set is independent of the charts' data anyway. Same shape as
        // HomeViewModel.observeHome(), for the same reason.
        val activeDays = combine(
            foodRepository.observeDailyNutrition(),
            waterRepository.observeLoggedDays(),
            progressRepository.observeWeightEntries(),
            exerciseRepository.observeLoggedDays(),
            ::loggedDays,
        )

        // Sleep, heart, the supplement log and the blood pressure readings group up before the
        // outer combine, which is already at the five-flow arity the typed overloads stop at — the
        // same shape HomeViewModel uses. Supplements and blood pressure ride here for that reason
        // alone; neither has anything to do with a watch. A `Triple` can't take the fourth, hence
        // the tuple below.
        val sparseSeries = combine(
            sleepRepository.observeNights(),
            heartRepository.observeDays(),
            supplementRepository.observeDays(),
            bloodPressureRepository.observeReadings(),
            cycleRepository.observeDays(),
            ::SparseSeries,
        )

        // The fasts ride with the Activity tab's two series for the same arity reason, not because
        // they have anything to do with each other.
        val activity = combine(
            fastingRepository.observeSessions(),
            stepsRepository.observeDays(),
            exerciseRepository.observeRecentEntries(),
            ::ActivitySeries,
        )

        combine(
            progress,
            activeDays,
            moodRepository.observeDays(),
            sparseSeries,
            activity,
        ) { state, days, moodDays, sparse, activitySeries ->
            state.copy(
                activeDays = days,
                moodDays = moodDays,
                sleepNights = sparse.nights,
                heartDays = sparse.heartDays,
                fastSessions = activitySeries.fasts,
                supplementDays = sparse.supplementDays,
                bloodPressure = sparse.bloodPressure,
                cycleDays = sparse.cycleDays,
                stepDays = activitySeries.stepDays,
                exerciseEntries = activitySeries.exercise,
            )
        }.collect { newState -> reduce { newState } }
    }
}

/** The four sparse series, grouped so the outer combine stays inside the typed overloads'
 * five-flow arity. Private and structural — it never leaves this file. */
private data class ActivitySeries(
    val fasts: List<FastSession>,
    val stepDays: List<StepDay>,
    val exercise: List<ExerciseEntry>,
)

/** The four sparse series, grouped so the outer combine stays inside the typed overloads'
 * five-flow arity. Private and structural — it never leaves this file. */
private data class SparseSeries(
    val nights: List<SleepNight>,
    val heartDays: List<HeartDay>,
    val supplementDays: List<SupplementDay>,
    val bloodPressure: List<BloodPressureReading>,
    val cycleDays: List<CycleDay>,
)
