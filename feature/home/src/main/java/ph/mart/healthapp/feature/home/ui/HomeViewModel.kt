package ph.mart.healthapp.feature.home.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainer
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureReading
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureRepository
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.fasting.FastingRepository
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.dailyTotals
import ph.mart.healthapp.core.data.health.HeartRepository
import ph.mart.healthapp.core.data.health.SleepRepository
import ph.mart.healthapp.core.data.health.StepsRepository
import ph.mart.healthapp.core.data.health.dayBurnedKcal
import ph.mart.healthapp.core.data.health.stepsCreditKcal
import ph.mart.healthapp.core.data.insight.InsightRepository
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.mood.MoodRepository
import ph.mart.healthapp.core.data.network.NetworkMonitor
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.streak.loggedDays
import ph.mart.healthapp.core.data.supplement.SupplementRepository
import ph.mart.healthapp.core.data.supplement.SupplementToday
import ph.mart.healthapp.core.data.streak.streakStats
import ph.mart.healthapp.core.data.streak.weightProgressKg
import ph.mart.healthapp.core.data.fasting.DEFAULT_FAST_GOAL_HOURS
import ph.mart.healthapp.core.data.water.DEFAULT_WATER_GOAL_GLASSES
import ph.mart.healthapp.core.data.water.WaterRepository
import ph.mart.healthapp.core.data.todayEpochDay

/**
 * Near-read-only container: the water glasses, the day's mood/energy, the fasting timer and the
 * supplement ticks are the only things this screen writes. The FAB's sheets own every other write path.
 *
 * All five flows are combined rather than snapshotted: this is what stops Home from drifting from
 * the rest of the app (the prototype's Home briefly read a hardcoded profile instead of the shared
 * one). The photo list collapses to its newest date here so the full list never enters UI state.
 *
 * The streak's four day-series ride in a second combine chained onto the first — `combine` only
 * has typed overloads up to five flows, and the streak's inputs are independent of the day's
 * totals anyway. Today's exercise joins at that same outer combine for the same reason: the inner
 * one is already at the cap. Mood groups with the running fast and today's supplements, and sleep
 * with steps and heart rate, so that outer combine stays inside the arity too.
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
    private val supplementRepository: SupplementRepository,
    bloodPressureRepository: BloodPressureRepository,
    private val insightRepository: InsightRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel(), OrbitContainerHost<HomeUiState, HomeUiState, Nothing> {

    // Explicitly typed, unlike the other containers in this app: [requestInsight] reads
    // `container.stateFlow`, and a container whose type is inferred from a body that
    // mentions itself is a recursion the compiler can't unpick.
    override val container: OrbitContainer<HomeUiState, HomeUiState, Nothing> =
        orbitContainer<HomeUiState, Nothing>(HomeUiState()) {
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
                supplementRepository,
                bloodPressureRepository,
            )
            requestInsight()
        }

    fun handleEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OnSetWaterGlasses -> onSetWaterGlasses(event.glasses)
            is HomeEvent.OnSetMood -> onSetMood(event.level)
            is HomeEvent.OnSetEnergy -> onSetEnergy(event.level)
            is HomeEvent.OnSetSupplementTaken -> onSetSupplementTaken(event.id, event.taken)
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

    /** The card computes the next count; the repository clamps it against the supplement's own
     * target, so a stale emission can't write a dose that no longer exists. */
    private fun onSetSupplementTaken(id: Long, taken: Int) = intent {
        supplementRepository.setTakenToday(id, taken)
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
        supplementRepository: SupplementRepository,
        bloodPressureRepository: BloodPressureRepository,
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

        // Mood, the running fast, today's supplements and the latest blood pressure reading group
        // up for the same reason [fromWatch] does: the outer combine below is already at the
        // five-flow arity the typed overloads stop at. Supplements arrive pre-joined with today's
        // counts — the repository does that join so this file doesn't have to spend two of its
        // five slots on one card. A `Triple` can't take the fourth, hence the tuple below.
        val userLogged = combine(
            moodRepository.observeToday(),
            fastingRepository.observeActive(),
            supplementRepository.observeToday(),
            bloodPressureRepository.observeLatest(),
            ::UserLogged,
        )

        combine(
            todayState,
            activeDays,
            exerciseRepository.observeTodayEntries(),
            userLogged,
            fromWatch,
        ) { state, days, exercise, logged, (lastNight, steps, heart) ->
            state.copy(
                loaded = true,
                // Steps fold in here rather than in budgetKcal(), which stays the single place
                // burned calories reach the day.
                burnedKcal = dayBurnedKcal(exercise, steps),
                stepsCreditKcal = stepsCreditKcal(steps, exercise),
                lastNight = lastNight,
                steps = steps,
                heart = heart,
                moodLevel = logged.mood.mood,
                energyLevel = logged.mood.energy,
                activeFast = logged.activeFast,
                supplements = logged.supplements,
                latestBloodPressure = logged.bloodPressure,
                addExerciseToBudget = state.profile?.addExerciseToBudget != false,
                // Read on every emission, not once at flow-construction time, so the streak
                // doesn't freeze at whatever day the app happened to be opened.
                streak = days.streakStats(todayEpochDay()),
                weightProgressKg = state.profile?.let {
                    weightProgressKg(state.weightEntries, it.goal, it.weightKg)
                },
            )
        }.collect { newState ->
            // [newState] is rebuilt from the repositories on every emission, so a plain
            // `reduce { newState }` would drop the model's line the moment a glass of water was
            // tapped. The insight isn't derived from Room, so it is carried across by hand.
            reduce { newState.copy(aiInsight = state.aiInsight) }
        }
    }

    /**
     * One call per ViewModel, not per emission: Home re-reads its flows on every water tap and
     * every logged meal, and none of those is a new day's worth of news. The repository caches
     * per day on top of that, so leaving Home and coming back costs nothing either.
     *
     * Waits for a *populated* first state — the default [HomeUiState] is all-zero, which reads as
     * day one, and a model asked about a day with nothing in it writes a sentence about nothing.
     * Offline it never asks at all, and any null answer leaves [HomeUiState.aiInsight] null,
     * which `HomeCards` reads as "use [insightFor]".
     */
    private fun requestInsight() = intent {
        val ready = container.stateFlow.first { it.loaded && !it.isDayOne && it.profile != null }
        if (!networkMonitor.isOnline()) return@intent
        val request = ready.toInsightRequest() ?: return@intent
        val insight = insightRepository.dailyInsight(request, todayEpochDay()) ?: return@intent
        reduce { state.copy(aiInsight = insight) }
    }
}

/** The four things the user logs by hand, grouped so the outer combine stays inside the typed
 * overloads' five-flow arity. Private and structural — it never leaves this file. */
private data class UserLogged(
    val mood: MoodDay,
    val activeFast: FastSession?,
    val supplements: List<SupplementToday>,
    val bloodPressure: BloodPressureReading?,
)
