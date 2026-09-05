package ph.mart.healthapp.feature.home.ui

import ph.mart.healthapp.core.data.fasting.DEFAULT_FAST_GOAL_HOURS
import ph.mart.healthapp.core.data.health.DEFAULT_STEP_GOAL
import ph.mart.healthapp.core.data.exercise.PlanDay
import ph.mart.healthapp.core.data.exercise.Routine
import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.cycle.CycleDay
import ph.mart.healthapp.core.data.food.DiaryTotals
import ph.mart.healthapp.core.data.health.HeartDay
import ph.mart.healthapp.core.data.health.SleepNight
import ph.mart.healthapp.core.data.health.StepDay
import ph.mart.healthapp.core.data.insight.InsightRequest
import ph.mart.healthapp.core.data.insight.insightRequest
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.streak.StreakStats
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureReading
import ph.mart.healthapp.core.data.supplement.SupplementToday
import ph.mart.healthapp.core.data.water.DEFAULT_WATER_GOAL_GLASSES

/**
 * Read model. Every field traces back to a repository interface: [profile] from
 * `ProfileRepository`, [totals] from `FoodRepository`, [weightEntries] and [lastPhotoEpochDay]
 * from `ProgressRepository`, [waterGlasses] from `WaterRepository`, [burnedKcal] from
 * `ExerciseRepository` and `StepsRepository` together, [lastNight], [steps] and [heart] from
 * `SleepRepository`/`StepsRepository`/`HeartRepository`. Targets are never stored here — they're derived
 * from [profile] at read time via `dailyTargets()`, so they can't drift from the inputs that
 * produce them. [streak] and [weightProgressKg] are derived the same way, from all three
 * repositories at once — nothing about consistency is stored.
 */
data class HomeUiState(
    /** False until the repositories' first combined emission. The all-zero default below is
     * indistinguishable from a genuinely empty day, so nothing may be rendered from it. */
    val loaded: Boolean = false,
    val profile: Profile? = null,
    val totals: DiaryTotals = DiaryTotals(0, 0, 0, 0),
    val foodEntryCount: Int = 0,
    val weightEntries: List<WeightEntry> = emptyList(),
    val lastPhotoEpochDay: Long? = null,
    val waterGlasses: Int = 0,
    val waterGoalGlasses: Int = DEFAULT_WATER_GOAL_GLASSES,
    /** Today's reflection, 1-5 each; 0 means the user hasn't tapped that row. */
    val moodLevel: Int = 0,
    val energyLevel: Int = 0,
    /** The fast currently running, or null. Not part of [isDayOne] — see its KDoc. */
    val activeFast: FastSession? = null,
    /** The profile's target, used only to price a fast that hasn't started; a running one carries
     * its own snapshot. */
    val fastingGoalHours: Int = DEFAULT_FAST_GOAL_HOURS,
    /** Today's supplements with their counts. Empty hides the card — Profile is where the list is
     * authored, and an empty checklist is not an invitation. Not part of [isDayOne], see its KDoc. */
    val supplements: List<SupplementToday> = emptyList(),
    val burnedKcal: Int = 0,
    /** Last night, from Google Health. Null when nothing was imported — the card is hidden, not
     * zeroed, because FitPulse has no way to measure sleep itself. */
    val lastNight: SleepNight? = null,
    /** The most recent reading, whenever it was taken — not today's, unlike the three watch
     * fields. Null before the first one, which hides the card. See [BloodPressureCard]. */
    val latestBloodPressure: BloodPressureReading? = null,
    /** Today's steps, from Google Health. Hidden the same way [lastNight] is, for the same reason. */
    val steps: StepDay? = null,
    /** Today's heart rate, from Google Health. Hidden the same way [lastNight] is, for the same
     * reason — FitPulse cannot count a heartbeat itself, so an absent row is an absent watch. */
    val heart: HeartDay? = null,
    /** The profile's daily step target — [StepsCard]'s denominator. */
    val stepGoal: Int = DEFAULT_STEP_GOAL,
    /** The part of [burnedKcal] that came from [steps] — what the card shows, and already net of
     * any workout that had claimed those steps. */
    val stepsCreditKcal: Int = 0,
    /** From the profile — whether [burnedKcal] raises the calorie ring's goal. */
    val addExerciseToBudget: Boolean = true,
    /** Every saved routine, for the training-plan card: which are planned for today, and whether
     * anything is planned at all (an empty plan hides the card, `SupplementsCard`'s rule). The
     * card derives both from this rather than storing a second copy — the `targets`/`projection`
     * treatment. Not part of [isDayOne]: a plan is intent, not a logged day. */
    val routines: List<Routine> = emptyList(),
    /** This Monday-to-Sunday week, scored against the plan — see `trainingWeek()`, which is the
     * only place adherence is defined. */
    val trainingWeek: List<PlanDay> = emptyList(),
    val streak: StreakStats = StreakStats(current = 0, best = 0, totalDaysLogged = 0),
    /** Null when the goal is Maintain (no direction to move) or nothing has been weighed yet. */
    val weightProgressKg: Double? = null,
    /** The model's line, once it answers. Null until then and null forever offline or on a failed
     * call — `HomeCards` falls back to [insightFor], so the card never waits on the network. */
    val aiInsight: String? = null,
    /**
     * Every logged cycle day, oldest first — the card folds its own day number and prediction out
     * of this, the treatment `targets` and `projection` already get. Collected whether or not the
     * Profile switch is on, the rule every hidden Home card's flows follow; the card is gated on
     * the switch rather than on the list, since an empty list with tracking on is a card asking
     * for the first tap.
     *
     * Not part of [isDayOne], for mood's reason — this is not one of the streak's four domains.
     */
    val cycleDays: List<CycleDay> = emptyList(),
)

/** All Home writes: today's glass count, today's mood/energy, the fasting timer and today's
 * supplement ticks. Everything
 * else on the screen is read-only — the FAB's sheets own every other write path. A level of 0
 * clears that row. */
sealed interface HomeEvent {
    data class OnSetSupplementTaken(val id: Long, val taken: Int) : HomeEvent
    data class OnSetWaterGlasses(val glasses: Int) : HomeEvent
    data class OnSetMood(val level: Int) : HomeEvent

    /** A [ph.mart.healthapp.core.data.cycle.FlowLevel] value for today, or 0 to clear it. */
    data class OnSetCycleFlow(val flow: Int) : HomeEvent
    data class OnSetEnergy(val level: Int) : HomeEvent
    data object OnStartFast : HomeEvent
    data object OnEndFast : HomeEvent
    data object OnDiscardFast : HomeEvent
}

/**
 * Day one = nothing logged anywhere yet. The profile alone doesn't count — it always exists by
 * the time Home is reachable.
 *
 * Mood, fasting and supplements are deliberately absent: none is one of the streak's four
 * domains, so a reflection, a running timer or a ticked pill alone doesn't make a day "logged"
 * here either. The cost is that the mood card only appears once
 * something real has been logged, which is the right order anyway.
 */
val HomeUiState.isDayOne: Boolean
    get() = foodEntryCount == 0 && weightEntries.isEmpty() && lastPhotoEpochDay == null &&
        waterGlasses == 0 && burnedKcal == 0

/**
 * What Home is actually showing.
 *
 * [Loading] exists because [HomeUiState]'s default is all-zero, which reads as day one. Without
 * this the day-one empty state renders for the frames before Room's first emission — a hard cut
 * hid it, but Home crossfades between phases now, so a user with months of history would watch
 * "Let's log your first meal" fade away on every cold start.
 */
enum class HomePhase { Loading, DayOne, Populated }

fun homePhase(state: HomeUiState): HomePhase = when {
    !state.loaded -> HomePhase.Loading
    state.isDayOne -> HomePhase.DayOne
    else -> HomePhase.Populated
}

/** Days between the most recent photo and today. Null when there are no photos at all. */
fun daysSincePhoto(lastPhotoEpochDay: Long?, todayEpochDay: Long): Long? =
    lastPhotoEpochDay?.let { (todayEpochDay - it).coerceAtLeast(0) }

/**
 * Today's payload for the model, off state Home has already combined for its cards — the coach
 * builds the identical request from flows instead (`observeInsightRequest`), and both land in
 * [insightRequest] so the two can't drift.
 *
 * Null with no profile: there is no target to be over or under, and Home has nothing to show yet
 * either.
 */
internal fun HomeUiState.toInsightRequest(): InsightRequest? = profile?.let {
    insightRequest(
        profile = it,
        totals = totals,
        weightEntries = weightEntries,
        waterGlasses = waterGlasses,
        waterGoalGlasses = waterGoalGlasses,
        streakDays = streak.current,
    )
}

/** Greeting copy is the prototype's verbatim, keyed off the local hour. */
fun greetingFor(hour: Int): String {
    val part = when {
        hour < 12 -> "morning"
        hour < 18 -> "afternoon"
        else -> "evening"
    }
    val line = when {
        hour < 12 -> "Ready for breakfast?"
        hour < 18 -> "How's the day going?"
        else -> "Almost there for today."
    }
    return "Good $part! $line"
}
