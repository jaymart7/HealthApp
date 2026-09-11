package ph.mart.healthapp.feature.progress.ui.progress

import androidx.annotation.StringRes
import kotlin.math.abs
import kotlin.math.sqrt
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.fasting.dateEpochDay
import ph.mart.healthapp.core.data.fasting.reachedGoal
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.health.SleepNight
import ph.mart.healthapp.core.data.health.StepDay
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.feature.progress.R

/**
 * "Does anything I log move with anything else I log" — the one question this app's data can
 * answer and no surface asks. Every other screen is single-subject: a chart per subject, a recap
 * that sums each one on its own, an insight card about weight alone.
 *
 * Derived, never stored — a fold over series the Progress tab already holds, the same way [recap]
 * and [ph.mart.healthapp.core.data.streak.streakStats] are. No table, no repository, no schema,
 * nothing written, and nothing sent to a model. Feature-local for [recap]'s reason: Progress is
 * the only screen that shows it and every input is a `:core:data` type, so no feature type leaks
 * down.
 *
 * **This is a comparison, not a claim.** Each pattern reports two averages and the day count
 * behind each, in the user's own log. No "because", no advice, no cause — which is the same line
 * the cycle tab holds when it refuses to derive a fertile window.
 *
 * **What is deliberately not compared.** Cycle days, heart rate and blood pressure: a line
 * relating any of the three to anything else is a clinical claim FitPulse cannot stand behind
 * (and cycle data stays on the phone by rule — see `CLAUDE.md`). Weight on either side: a
 * day-level weight is mostly water, and intake against the real weight trend is already owned,
 * honestly and over a proper window, by [ph.mart.healthapp.core.data.progress.goalProjection] and
 * the energy check-in. Supplements: a percent-taken split says nothing about anything. Water: the
 * tab holds no per-day water series, only the logged-day set.
 */

/** Matches [DEFAULT_CHART_RANGE] (three months). A month leaves too few paired days once both
 * series have to be present; a year spans diets the user abandoned in spring. */
const val PATTERN_WINDOW_DAYS = 90L

/** Days where **both** sides were logged. Below this the split is a coin toss with a caption. */
const val MIN_PAIRED_DAYS = 14

/** Per side of the split, so neither average is one good Tuesday. */
const val MIN_SIDE_DAYS = 5

/** The card draws every pattern it is given, so the cap is the card's: a fifth row is weaker than
 * the fourth and would push the grid off the screen. Nothing is hidden behind it — that is why
 * there is no "see all" door. */
const val MAX_PATTERNS = 4

/** What the outcome averages are measured in. The fold never formats — the screen does, because a
 * resource needs a Composable and this file has to stay testable on the JVM. */
enum class PatternUnit { Kcal, Grams, Score }

/**
 * [highDays]/[lowDays] are on screen beside the averages on purpose: "12 days against 9" is what
 * tells the reader how much to trust the line, and hiding it would make a comparison look like a
 * finding.
 *
 * [strength] ranks patterns against each other across different units and is never rendered.
 */
data class Pattern(
    @StringRes val title: Int,
    @StringRes val sentence: Int,
    val unit: PatternUnit,
    val highDays: Int,
    val lowDays: Int,
    val highOutcome: Double,
    val lowOutcome: Double,
    val strength: Double,
) {
    val delta: Double get() = highOutcome - lowOutcome
}

/** The series a spec's two extractors pick from — a bundle only because a spec is a pair of
 * lambdas and seven of them would otherwise take six parameters each. */
private data class PatternInputs(
    val dailyNutrition: List<DayNutrition>,
    val sleepNights: List<SleepNight>,
    val stepDays: List<StepDay>,
    val moodDays: List<MoodDay>,
    val exerciseEntries: List<ExerciseEntry>,
    val fastSessions: List<FastSession>,
)

/**
 * One comparison the app is willing to draw.
 *
 * [nextDay] shifts the outcome one day forward. Only fasting needs it: a
 * [FastSession.dateEpochDay] is the day the fast *ended*, so what it could plausibly show up in is
 * the next day's eating. Sleep needs no shift at all and that is not an oversight —
 * [SleepNight.dateEpochDay] is the day the sleep ended (the morning), so the night before day D is
 * already filed under D, beside D's food.
 *
 * [minDriverSpread] is the floor on the two sides' *driver* means: 7h01 against 6h59 is a median,
 * not a split. [minDelta] is the floor on the outcome gap, and is what keeps noise from being
 * reported as a pattern.
 */
private class PatternSpec(
    @StringRes val title: Int,
    @StringRes val sentence: Int,
    val unit: PatternUnit,
    val nextDay: Boolean = false,
    val minDriverSpread: Double,
    val minDelta: Double,
    val driver: (PatternInputs, LongRange) -> Map<Long, Double>,
    val outcome: (PatternInputs) -> Map<Long, Double>,
)

// --- series, each keyed by day and carrying only days that were really logged ------------------

/** A zero-calorie day means "didn't open the app", never "ate nothing" — [DayNutrition.isLogged]
 * is the only honest signal in a dense series, and the streak reads it the same way. */
private fun PatternInputs.food(value: (DayNutrition) -> Double): Map<Long, Double> =
    dailyNutrition.filter { it.isLogged }.associate { it.dateEpochDay to value(it) }

private fun PatternInputs.sleep(): Map<Long, Double> =
    sleepNights.filter { it.minutesAsleep > 0 }.associate { it.dateEpochDay to it.minutesAsleep.toDouble() }

private fun PatternInputs.steps(): Map<Long, Double> =
    stepDays.filter { it.steps > 0 }.associate { it.dateEpochDay to it.steps.toDouble() }

private fun PatternInputs.mood(value: (MoodDay) -> Int): Map<Long, Double> =
    moodDays.filter { value(it) > 0 }.associate { it.dateEpochDay to value(it).toDouble() }

/**
 * A yes/no driver, spread across every day in the window so the "no" side exists at all. It needs
 * no special case downstream: the median of a 0/1 series lands on whichever answer is commoner,
 * and the split rule below puts the other one on its own side.
 */
private fun flag(window: LongRange, days: Set<Long>): Map<Long, Double> =
    window.associateWith { if (it in days) 1.0 else 0.0 }

private val SPECS = listOf(
    PatternSpec(
        title = R.string.progress_pattern_sleep_calories_title,
        sentence = R.string.progress_pattern_sleep_calories,
        unit = PatternUnit.Kcal,
        minDriverSpread = 30.0,
        minDelta = 150.0,
        driver = { inputs, _ -> inputs.sleep() },
        outcome = { it.food { day -> day.calories.toDouble() } },
    ),
    PatternSpec(
        title = R.string.progress_pattern_sleep_mood_title,
        sentence = R.string.progress_pattern_sleep_mood,
        unit = PatternUnit.Score,
        minDriverSpread = 30.0,
        minDelta = 0.4,
        driver = { inputs, _ -> inputs.sleep() },
        outcome = { it.mood { day -> day.mood } },
    ),
    PatternSpec(
        title = R.string.progress_pattern_steps_calories_title,
        sentence = R.string.progress_pattern_steps_calories,
        unit = PatternUnit.Kcal,
        minDriverSpread = 1_500.0,
        minDelta = 150.0,
        driver = { inputs, _ -> inputs.steps() },
        outcome = { it.food { day -> day.calories.toDouble() } },
    ),
    PatternSpec(
        title = R.string.progress_pattern_steps_mood_title,
        sentence = R.string.progress_pattern_steps_mood,
        unit = PatternUnit.Score,
        minDriverSpread = 1_500.0,
        minDelta = 0.4,
        driver = { inputs, _ -> inputs.steps() },
        outcome = { it.mood { day -> day.mood } },
    ),
    PatternSpec(
        title = R.string.progress_pattern_training_protein_title,
        sentence = R.string.progress_pattern_training_protein,
        unit = PatternUnit.Grams,
        minDriverSpread = 0.5,
        minDelta = 10.0,
        driver = { inputs, window ->
            flag(window, inputs.exerciseEntries.map { it.dateEpochDay }.toSet())
        },
        outcome = { it.food { day -> day.proteinG.toDouble() } },
    ),
    PatternSpec(
        title = R.string.progress_pattern_protein_energy_title,
        sentence = R.string.progress_pattern_protein_energy,
        unit = PatternUnit.Score,
        minDriverSpread = 15.0,
        minDelta = 0.4,
        driver = { inputs, _ -> inputs.food { day -> day.proteinG.toDouble() } },
        outcome = { it.mood { day -> day.energy } },
    ),
    PatternSpec(
        title = R.string.progress_pattern_fasting_calories_title,
        sentence = R.string.progress_pattern_fasting_calories,
        unit = PatternUnit.Kcal,
        nextDay = true,
        minDriverSpread = 0.5,
        minDelta = 150.0,
        driver = { inputs, window ->
            flag(
                window,
                inputs.fastSessions.filter { it.reachedGoal(nowMillis = 0) }.map { it.dateEpochDay }.toSet(),
            )
        },
        outcome = { it.food { day -> day.calories.toDouble() } },
    ),
)

/**
 * The strongest comparisons the log can honestly support, strongest first and at most
 * [MAX_PATTERNS]. An empty list is the ordinary answer on a young account, and the card is then
 * omitted entirely rather than drawn saying it has nothing — the recap card's rule.
 */
fun patterns(
    dailyNutrition: List<DayNutrition>,
    sleepNights: List<SleepNight>,
    stepDays: List<StepDay>,
    moodDays: List<MoodDay>,
    exerciseEntries: List<ExerciseEntry>,
    fastSessions: List<FastSession>,
    todayEpochDay: Long,
): List<Pattern> {
    val inputs = PatternInputs(dailyNutrition, sleepNights, stepDays, moodDays, exerciseEntries, fastSessions)
    val window = (todayEpochDay - PATTERN_WINDOW_DAYS)..todayEpochDay
    return SPECS.mapNotNull { it.evaluate(inputs, window) }
        .sortedByDescending { it.strength }
        .take(MAX_PATTERNS)
}

/** One day's driver value beside the outcome it is being compared against. */
private class Paired(val driver: Double, val outcome: Double)

/**
 * Null wherever there is nothing honest to say — too few days, a side too thin, a driver that
 * didn't really split, or an outcome gap small enough to be noise.
 *
 * The split is at the driver's **median** rather than a fixed cutoff, because a cutoff would mean
 * this app deciding what "enough sleep" is; the median only says "your better half against your
 * worse half". Ties go to the low side, unless that would empty a side (a mostly-1 yes/no driver),
 * in which case they go high — either way the two groups are the two real ones.
 *
 * ponytail: a median split with an effect-size floor, not a significance test. If real logs start
 * producing findings that read as flukes, a Welch t or a Spearman rho over the same pairs is the
 * upgrade path — same inputs, same specs, one more guard here.
 */
private fun PatternSpec.evaluate(inputs: PatternInputs, window: LongRange): Pattern? {
    val driverDays = driver(inputs, window)
    val outcomeDays = outcome(inputs)
    val offset = if (nextDay) 1L else 0L
    val pairs = driverDays.mapNotNull { (day, value) ->
        if (day !in window) null else outcomeDays[day + offset]?.let { Paired(value, it) }
    }
    if (pairs.size < MIN_PAIRED_DAYS) return null

    val median = pairs.map { it.driver }.sorted().median()
    var low = pairs.filter { it.driver <= median }
    var high = pairs.filter { it.driver > median }
    if (low.isEmpty() || high.isEmpty()) {
        low = pairs.filter { it.driver < median }
        high = pairs.filter { it.driver >= median }
    }
    if (low.size < MIN_SIDE_DAYS || high.size < MIN_SIDE_DAYS) return null
    if (high.map { it.driver }.average() - low.map { it.driver }.average() < minDriverSpread) return null

    val highOutcome = high.map { it.outcome }.average()
    val lowOutcome = low.map { it.outcome }.average()
    val delta = highOutcome - lowOutcome
    if (abs(delta) < minDelta) return null

    val spread = pooledSd(high.map { it.outcome }, low.map { it.outcome })
    return Pattern(
        title = title,
        sentence = sentence,
        unit = unit,
        highDays = high.size,
        lowDays = low.size,
        highOutcome = highOutcome,
        lowOutcome = lowOutcome,
        // Two sides that never overlap have no spread to divide by, and that is the strongest a
        // split can be — not the weakest, which is what a zero here would otherwise rank it as.
        strength = if (spread > 0.0) abs(delta) / spread else Double.MAX_VALUE,
    )
}

/** Called on a sorted, non-empty list. */
private fun List<Double>.median(): Double =
    if (size % 2 == 1) this[size / 2] else (this[size / 2 - 1] + this[size / 2]) / 2

/** Both sides are at least [MIN_SIDE_DAYS] long by the time this is reached, so the denominator
 * can't vanish. */
private fun pooledSd(a: List<Double>, b: List<Double>): Double {
    val variance = ((a.size - 1) * a.variance() + (b.size - 1) * b.variance()) / (a.size + b.size - 2)
    return sqrt(variance)
}

private fun List<Double>.variance(): Double {
    val mean = average()
    return sumOf { (it - mean) * (it - mean) } / (size - 1)
}
