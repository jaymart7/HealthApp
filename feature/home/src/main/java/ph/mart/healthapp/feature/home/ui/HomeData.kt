package ph.mart.healthapp.feature.home.ui

import kotlin.math.abs
import ph.mart.healthapp.core.data.fasting.DEFAULT_FAST_GOAL_HOURS
import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.food.DiaryTotals
import ph.mart.healthapp.core.data.health.HeartDay
import ph.mart.healthapp.core.data.health.SleepNight
import ph.mart.healthapp.core.data.health.StepDay
import ph.mart.healthapp.core.data.insight.InsightRequest
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.TREND_ARROW_DEADBAND_KG
import ph.mart.healthapp.core.data.profile.trendVsSevenDaysAgo
import ph.mart.healthapp.core.data.profile.WeightTrendDisplay
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.streak.StreakStats
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
    /** Today's steps, from Google Health. Hidden the same way [lastNight] is, for the same reason. */
    val steps: StepDay? = null,
    /** Today's heart rate, from Google Health. Hidden the same way [lastNight] is, for the same
     * reason — FitPulse cannot count a heartbeat itself, so an absent row is an absent watch. */
    val heart: HeartDay? = null,
    /** The part of [burnedKcal] that came from [steps] — what the card shows, and already net of
     * any workout that had claimed those steps. */
    val stepsCreditKcal: Int = 0,
    /** From the profile — whether [burnedKcal] raises the calorie ring's goal. */
    val addExerciseToBudget: Boolean = true,
    val streak: StreakStats = StreakStats(current = 0, best = 0, totalDaysLogged = 0),
    /** Null when the goal is Maintain (no direction to move) or nothing has been weighed yet. */
    val weightProgressKg: Double? = null,
    /** The model's line, once it answers. Null until then and null forever offline or on a failed
     * call — `HomeCards` falls back to [insightFor], so the card never waits on the network. */
    val aiInsight: String? = null,
)

/** All Home writes: today's glass count, today's mood/energy, the fasting timer and today's
 * supplement ticks. Everything
 * else on the screen is read-only — the FAB's sheets own every other write path. A level of 0
 * clears that row. */
sealed interface HomeEvent {
    data class OnSetSupplementTaken(val id: Long, val taken: Int) : HomeEvent
    data class OnSetWaterGlasses(val glasses: Int) : HomeEvent
    data class OnSetMood(val level: Int) : HomeEvent
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
 * The one insight line, derived from the day's real numbers rather than a hardcoded string.
 * First matching rule wins; null hides the card entirely. Deliberately not a model call — a
 * Gemini-backed insight is a separate piece of work, not Phase 7 assembly.
 */
fun insightFor(totals: DiaryTotals, targets: DailyTargets, trend: WeightTrendDisplay): String? = when {
    totals.calories > targets.calories ->
        "You're ${totals.calories - targets.calories} kcal over today's target."
    targets.proteinG > 0 && totals.calories > 0 && totals.proteinG < targets.proteinG * 0.6 ->
        "You're ${targets.proteinG - totals.proteinG}g short on protein today."
    trend.hasPrior && abs(trend.deltaKg) >= TREND_ARROW_DEADBAND_KG ->
        "${formatDelta(trend.deltaKg)} kg over the last week — keep it steady."
    else -> null
}

/** Signed, one decimal, tabular-friendly — e.g. "-0.6", "+1.2". */
fun formatDelta(deltaKg: Double): String = "%+.1f".format(deltaKg)

/**
 * The same two derivations `HomeCards` makes to draw the day — `dailyTargets()` and
 * `trendVsSevenDaysAgo()` — handed to the model instead of to the cards, so the sentence it
 * writes is priced off exactly the numbers on screen.
 *
 * Null with no profile: there is no target to be over or under, and Home has nothing to show yet
 * either.
 */
internal fun HomeUiState.toInsightRequest(): InsightRequest? {
    val profile = profile ?: return null
    val targets = profile.dailyTargets()
    val trend = weightEntries.trendVsSevenDaysAgo(fallbackKg = profile.weightKg)
    return InsightRequest(
        goal = profile.goal,
        caloriesConsumed = totals.calories,
        caloriesTarget = targets.calories,
        proteinG = totals.proteinG,
        proteinTargetG = targets.proteinG,
        carbsG = totals.carbsG,
        carbsTargetG = targets.carbsG,
        fatG = totals.fatG,
        fatTargetG = targets.fatG,
        waterGlasses = waterGlasses,
        waterGoalGlasses = waterGoalGlasses,
        streakDays = streak.current,
        // Null rather than 0.0 with nothing to compare against — see [InsightRequest].
        weightDeltaKg = trend.deltaKg.takeIf { trend.hasPrior },
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
