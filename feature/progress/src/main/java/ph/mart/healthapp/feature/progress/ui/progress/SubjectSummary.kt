package ph.mart.healthapp.feature.progress.ui.progress

import ph.mart.healthapp.core.data.bloodpressure.averages
import ph.mart.healthapp.core.data.bloodpressure.byDay
import ph.mart.healthapp.core.data.cycle.cycleAverages
import ph.mart.healthapp.core.data.cycle.cycleDayNumber
import ph.mart.healthapp.core.data.cycle.periods
import ph.mart.healthapp.core.data.exercise.strengthTotals
import ph.mart.healthapp.core.data.exercise.volumeByDay
import ph.mart.healthapp.core.data.exercise.volumeLabel
import ph.mart.healthapp.core.data.exercise.withSets
import ph.mart.healthapp.core.data.fasting.dateEpochDay
import ph.mart.healthapp.core.data.fasting.durationMinutes
import ph.mart.healthapp.core.data.fasting.fastingAverages
import ph.mart.healthapp.core.data.food.averages
import ph.mart.healthapp.core.data.health.formatDuration
import ph.mart.healthapp.core.data.health.formatSteps
import ph.mart.healthapp.core.data.health.heartAverages
import ph.mart.healthapp.core.data.health.sleepAverages
import ph.mart.healthapp.core.data.health.stepAverages
import ph.mart.healthapp.core.data.mood.MOOD_SCALE
import ph.mart.healthapp.core.data.mood.moodAverages
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.TREND_ARROW_DEADBAND_KG
import ph.mart.healthapp.core.data.profile.TrendDirection
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.cmToDisplayUnit
import ph.mart.healthapp.core.data.profile.goalRelativeTrend
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.lengthUnitLabel
import ph.mart.healthapp.core.data.profile.trendVsSevenDaysAgo
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.streak.streakStats
import ph.mart.healthapp.core.data.supplement.adherenceByDay
import ph.mart.healthapp.core.data.supplement.averageAdherence
import ph.mart.healthapp.feature.progress.ui.achievement.badgeGroups
import ph.mart.healthapp.feature.progress.ui.measurement.components.formatCm
import ph.mart.healthapp.feature.progress.ui.weight.components.formatKg
import kotlin.math.abs
import kotlin.math.roundToInt

/** How many points a card's 26dp preview draws. Seven is a week, and a week is as much shape as
 * a strip that size can hold. */
const val PREVIEW_POINTS = 7

/** The three shapes a subject card's preview takes — the handoff's line, day bars and photo strip.
 * A subject with nothing to draw yet renders [None], not an empty box. */
sealed interface SubjectPreview {
    data class Line(val values: List<Double>) : SubjectPreview
    /** Zero-based day bars. A `0` is drawn as a stub rather than skipped, so a gap reads as a gap. */
    data class Bars(val values: List<Int>) : SubjectPreview
    data class PhotoStrip(val paths: List<String>) : SubjectPreview
    data object None : SubjectPreview
}

/** Which way a figure moved, for the glyph beside it. Separate from [TrendDirection], which says
 * whether that movement is *good* — the two disagree by design (down is on track for one goal and
 * off track for another), and meaning is never carried by colour alone. */
enum class TrendArrow { Down, Flat, Up }

/**
 * One subject as the overview draws it. [value] null is the whole of "nothing tracked yet": the
 * card renders dashed with "Nothing yet", the detail page renders its `FullScreenState`, and the
 * group counts it as empty. Every other field is then ignored.
 */
data class SubjectSummary(
    val subject: Subject,
    val value: String? = null,
    val unit: String? = null,
    val preview: SubjectPreview = SubjectPreview.None,
    val footnote: String = "",
    val arrow: TrendArrow? = null,
    val trend: TrendDirection = TrendDirection.Neutral,
) {
    val tracked: Boolean get() = value != null
}

/**
 * The single fold behind every subject card **and** its detail page's hero — which is the point:
 * each branch calls the derivation that subject's own tab already calls
 * (`sleepAverages()`, `stepAverages()`, `personalRecords()`, …), so a card and the page behind it
 * can never quote different numbers. Pure, so a JVM test can reach all thirteen branches.
 *
 * Windows differ per subject on purpose, and each footnote says which it used: dense series
 * (nutrition) average their last [PREVIEW_POINTS] days, sparse ones (sleep, heart, mood, fasting,
 * supplements, blood pressure) average whatever the repository returned, which is already the year
 * the charts draw. Nothing here re-slices to a chart range — the card is a standing summary, and
 * the range toggle belongs to the chart it sits in.
 */
@Suppress("CyclomaticComplexMethod", "LongMethod")
// Stays in Kotlin, with `trendWord` and `daysAgo` below it: a pure fold with a JVM test over its
// exact wording, the same reading `insightFor()` and `goalProjectionLine()` got. Moving it means
// returning a case type per branch for the card to resolve, which is its own decision.
fun summarize(
    subject: Subject,
    uiState: ProgressUiState,
    todayEpochDay: Long,
): SubjectSummary {
    val unit = uiState.preferredUnit
    return when (subject) {
        Subject.Weight -> {
            val entries = uiState.weightEntries.sortedBy { it.dateEpochDay }
            if (entries.isEmpty()) return SubjectSummary(subject)
            val trend = uiState.weightEntries.trendVsSevenDaysAgo(fallbackKg = 0.0)
            SubjectSummary(
                subject = subject,
                value = formatKg(entries.last().weightKg.kgToDisplayUnit(unit)),
                unit = unit.weightUnitLabel(),
                preview = SubjectPreview.Line(
                    entries.takeLast(PREVIEW_POINTS).map { it.weightKg.kgToDisplayUnit(unit) },
                ),
                footnote = if (trend.hasPrior) {
                    "${formatKg(abs(trend.deltaKg).kgToDisplayUnit(unit))} ${unit.weightUnitLabel()} " +
                        "this week · ${trendWord(uiState.goal, trend.deltaKg)}"
                } else {
                    "One reading so far"
                },
                arrow = if (trend.hasPrior) arrowFor(trend.deltaKg, TREND_ARROW_DEADBAND_KG) else null,
                trend = if (trend.hasPrior) goalRelativeTrend(uiState.goal, trend.deltaKg) else TrendDirection.Neutral,
            )
        }

        Subject.Photos -> {
            val photos = uiState.photos
            if (photos.isEmpty()) return SubjectSummary(subject)
            val newest = photos.maxOf { it.dateEpochDay }
            SubjectSummary(
                subject = subject,
                value = "${photos.size}",
                unit = if (photos.size == 1) "shot" else "shots",
                preview = SubjectPreview.PhotoStrip(
                    photos.sortedByDescending { it.dateEpochDay }.take(3).map { it.filePath },
                ),
                footnote = "Last one ${daysAgo(newest, todayEpochDay)}",
            )
        }

        Subject.Measurements -> {
            // The part measured most recently leads, because that is the one being worked on.
            // Ties break to declaration order, so a day with two readings is stable.
            val tracked = uiState.measurements.filterValues { it.isNotEmpty() }
            val lead = tracked.entries
                .maxByOrNull { (_, entries) -> entries.maxOf { it.dateEpochDay } }
                ?: return SubjectSummary(subject)
            val history = lead.value.sortedBy { it.dateEpochDay }
            val delta = history.deltaCm()
            SubjectSummary(
                subject = subject,
                value = formatCm(history.last().valueCm.cmToDisplayUnit(unit)),
                unit = "${unit.lengthUnitLabel()} ${lead.key.name.lowercase()}",
                preview = SubjectPreview.Line(
                    history.takeLast(PREVIEW_POINTS).map { it.valueCm.cmToDisplayUnit(unit) },
                ),
                footnote = buildString {
                    if (delta != null) {
                        append("${formatCm(abs(delta).cmToDisplayUnit(unit))} ${unit.lengthUnitLabel()} · ")
                    }
                    append("${tracked.size} ${if (tracked.size == 1) "part" else "parts"}")
                },
                arrow = delta?.let { arrowFor(it, deadband = 0.0) },
                // Shrinking reads as progress here, the rule `MeasurementRow` already draws by —
                // measurements have no per-goal direction the way weight does.
                trend = when {
                    delta == null || delta == 0.0 -> TrendDirection.Neutral
                    delta < 0 -> TrendDirection.OnTrack
                    else -> TrendDirection.OffTrack
                },
            )
        }

        Subject.Nutrition -> {
            val week = uiState.dailyNutrition.takeLast(PREVIEW_POINTS)
            val averages = week.averages()
            if (averages.daysLogged == 0) return SubjectSummary(subject)
            val target = uiState.targets?.calories
            SubjectSummary(
                subject = subject,
                value = "${averages.calories}",
                unit = "kcal avg",
                preview = SubjectPreview.Bars(week.map { it.calories }),
                footnote = when {
                    target == null -> "${averages.daysLogged} of ${week.size} days logged"
                    averages.calories < target -> "${target - averages.calories} kcal under target"
                    averages.calories > target -> "${averages.calories - target} kcal over target"
                    else -> "On target"
                },
            )
        }

        Subject.Fasting -> {
            val sessions = uiState.fastSessions
            if (sessions.isEmpty()) return SubjectSummary(subject)
            val averages = sessions.fastingAverages()
            val average = averages.averageMinutes ?: return SubjectSummary(subject)
            SubjectSummary(
                subject = subject,
                value = formatDuration(average),
                unit = "avg",
                preview = SubjectPreview.Bars(
                    sessions.takeLast(PREVIEW_POINTS).map { it.durationMinutes(nowMillis = 0) },
                ),
                footnote = "${averages.goalsHit} of ${averages.count} goals hit",
            )
        }

        Subject.Supplements -> {
            val days = uiState.supplementDays
            val adherence = days.averageAdherence() ?: return SubjectSummary(subject)
            val byDay = days.adherenceByDay()
            SubjectSummary(
                subject = subject,
                value = "${(adherence * 100).roundToInt()}",
                unit = "% taken",
                preview = SubjectPreview.Bars(
                    byDay.takeLast(PREVIEW_POINTS).map { (_, ratio) -> (ratio * 100).roundToInt() },
                ),
                footnote = "${byDay.size} ${if (byDay.size == 1) "day" else "days"} logged",
            )
        }

        Subject.Activity -> {
            val days = uiState.stepDays
            val averages = days.stepAverages(uiState.stepGoal)
            val average = averages.averageSteps ?: return SubjectSummary(subject)
            SubjectSummary(
                subject = subject,
                value = formatSteps(average),
                unit = "steps",
                preview = SubjectPreview.Bars(days.takeLast(PREVIEW_POINTS).map { it.steps }),
                footnote = "Daily average · ${averages.daysHitGoal} of ${averages.days} hit goal",
            )
        }

        Subject.Strength -> {
            val lifted = uiState.exerciseEntries.withSets()
            if (lifted.isEmpty()) return SubjectSummary(subject)
            val totals = lifted.strengthTotals()
            SubjectSummary(
                subject = subject,
                value = "${totals.workouts}",
                unit = if (totals.workouts == 1) "workout" else "workouts",
                preview = SubjectPreview.Bars(
                    lifted.volumeByDay().takeLast(PREVIEW_POINTS).map { it.volumeKg.roundToInt() },
                ),
                footnote = "Lifted ${volumeLabel(totals.volumeKg, unit)}",
            )
        }

        Subject.Sleep -> {
            val nights = uiState.sleepNights
            val averages = nights.sleepAverages()
            val average = averages.averageMinutes ?: return SubjectSummary(subject)
            SubjectSummary(
                subject = subject,
                value = formatDuration(average),
                unit = "avg",
                preview = SubjectPreview.Bars(nights.takeLast(PREVIEW_POINTS).map { it.minutesAsleep }),
                footnote = "From your watch · ${averages.nights} nights",
            )
        }

        Subject.Mood -> {
            val days = uiState.moodDays
            val averages = days.moodAverages()
            val mood = averages.mood ?: return SubjectSummary(subject)
            SubjectSummary(
                subject = subject,
                value = "%.1f".format(mood),
                unit = "/ ${MOOD_SCALE.last}",
                preview = SubjectPreview.Bars(days.takeLast(PREVIEW_POINTS).map { it.mood }),
                footnote = "${averages.daysLogged} ${if (averages.daysLogged == 1) "day" else "days"} logged",
            )
        }

        // Every figure the page shows at its default range, folded by the same calls: the card
        // says where you are, the page says the rest.
        Subject.Cycle -> {
            val days = uiState.cycleDays
            val periods = days.periods()
            val cycleDay = periods.cycleDayNumber(todayEpochDay) ?: return SubjectSummary(subject)
            val averages = days.cycleAverages(todayEpochDay)
            SubjectSummary(
                subject = subject,
                value = "$cycleDay",
                unit = "day of cycle",
                // The last week's flow, gaps drawn as stubs — a week is what the strip can hold.
                preview = SubjectPreview.Bars(
                    (todayEpochDay - PREVIEW_POINTS + 1..todayEpochDay).map { day ->
                        days.firstOrNull { it.dateEpochDay == day }?.flow ?: 0
                    },
                ),
                footnote = averages.cycleDays
                    ?.let { "Average cycle ${it.roundToInt()} days" }
                    ?: "${averages.daysLogged} ${if (averages.daysLogged == 1) "day" else "days"} logged",
            )
        }

        Subject.Heart -> {
            val days = uiState.heartDays
            val averages = days.heartAverages()
            val average = averages.averageBpm ?: return SubjectSummary(subject)
            SubjectSummary(
                subject = subject,
                value = "$average",
                unit = "bpm avg",
                preview = SubjectPreview.Bars(days.takeLast(PREVIEW_POINTS).map { it.averageBpm }),
                footnote = averages.lowestBpm?.let { "Lowest $it bpm" } ?: "From your watch",
            )
        }

        Subject.BloodPressure -> {
            val readings = uiState.bloodPressure
            val averages = readings.averages()
            val systolic = averages.systolic ?: return SubjectSummary(subject)
            SubjectSummary(
                subject = subject,
                value = "$systolic/${averages.diastolic}",
                unit = "mmHg avg",
                preview = SubjectPreview.Bars(readings.byDay().takeLast(PREVIEW_POINTS).map { it.systolic }),
                footnote = "${averages.readings} ${if (averages.readings == 1) "reading" else "readings"}",
            )
        }

        Subject.Badges -> {
            val tally = badgeTally(uiState, todayEpochDay)
            if (uiState.activeDays.isEmpty()) return SubjectSummary(subject)
            SubjectSummary(
                subject = subject,
                value = "${tally.earned}",
                unit = "of ${tally.total} earned",
                footnote = "${tally.families} ${if (tally.families == 1) "family" else "families"}",
            )
        }
    }
}

/** What the Badges row prints. Its own type rather than three strings on [SubjectSummary],
 * because no other subject has a "how many of how many" to report. */
data class BadgeTally(val earned: Int, val total: Int, val families: Int)

/** The [badgeGroups] fold the Badges tab already makes, counted rather than drawn — so the row on
 * the overview and the page behind it can never disagree about how many are lit. */
fun badgeTally(uiState: ProgressUiState, todayEpochDay: Long): BadgeTally {
    val groups = badgeGroups(
        streak = uiState.activeDays.streakStats(todayEpochDay),
        weightProgressKg = uiState.weightProgressKg,
        workoutCount = uiState.exerciseEntries.size,
        fasts = uiState.fastSessions,
        photoCount = uiState.photos.size,
    )
    return BadgeTally(
        earned = groups.sumOf { it.earnedCount },
        total = groups.sumOf { it.tiers.size },
        families = groups.size,
    )
}

/** Every subject at once — the overview folds this one and reads it everywhere, rather than
 * summarizing the same subject twice for its card and its group's count. */
fun summarizeAll(uiState: ProgressUiState, todayEpochDay: Long): Map<Subject, SubjectSummary> =
    Subject.entries.associateWith { summarize(it, uiState, todayEpochDay) }

/** Latest minus the reading before it, or null when there is only one — the reading
 * `MeasurementRow` gives a single entry, rather than a false 0.0. */
private fun List<MeasurementEntry>.deltaCm(): Double? =
    if (size >= 2) last().valueCm - this[size - 2].valueCm else null

private fun arrowFor(delta: Double, deadband: Double): TrendArrow = when {
    abs(delta) < deadband || delta == 0.0 -> TrendArrow.Flat
    delta < 0 -> TrendArrow.Down
    else -> TrendArrow.Up
}

/** The words beside the arrow. Colour never carries this on its own. */
private fun trendWord(goal: Goal?, deltaKg: Double): String =
    if (abs(deltaKg) < TREND_ARROW_DEADBAND_KG) {
        "steady"
    } else {
        when (goalRelativeTrend(goal, deltaKg)) {
            TrendDirection.OnTrack -> "on track"
            TrendDirection.OffTrack -> "off track"
            TrendDirection.Neutral -> "steady"
        }
    }

private fun daysAgo(dateEpochDay: Long, todayEpochDay: Long): String = when (val days = todayEpochDay - dateEpochDay) {
    0L -> "today"
    1L -> "yesterday"
    else -> "$days days ago"
}
