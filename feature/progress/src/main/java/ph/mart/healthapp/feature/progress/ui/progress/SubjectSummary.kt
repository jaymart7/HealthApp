package ph.mart.healthapp.feature.progress.ui.progress

import androidx.annotation.StringRes
import ph.mart.healthapp.core.data.Phrase
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
import ph.mart.healthapp.core.data.phrase
import ph.mart.healthapp.core.data.plural
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.TREND_ARROW_DEADBAND_KG
import ph.mart.healthapp.core.data.profile.TrendDirection
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.goalRelativeTrend
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.lengthUnitLabel
import ph.mart.healthapp.core.data.profile.trendVsSevenDaysAgo
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.data.progress.toDisplay
import ph.mart.healthapp.core.data.progress.unitLabel
import ph.mart.healthapp.core.data.progress.weightArc
import ph.mart.healthapp.core.data.streak.streakStats
import ph.mart.healthapp.core.data.supplement.adherenceByDay
import ph.mart.healthapp.core.data.supplement.averageAdherence
import ph.mart.healthapp.core.data.water.waterAverages
import ph.mart.healthapp.core.designsystem.component.formatDecimals
import ph.mart.healthapp.core.designsystem.component.formatOneDecimal
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.achievement.badgeGroups
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
    val unit: Phrase? = null,
    val preview: SubjectPreview = SubjectPreview.None,
    val footnote: Phrase? = null,
    val arrow: TrendArrow? = null,
    val trend: TrendDirection = TrendDirection.Neutral,
) {
    val tracked: Boolean get() = value != null
}

/**
 * The single fold behind every subject card **and** its detail page's hero — which is the point:
 * each branch calls the derivation that subject's own tab already calls
 * (`sleepAverages()`, `stepAverages()`, `personalRecords()`, …), so a card and the page behind it
 * can never quote different numbers. Pure, so a JVM test can reach all fifteen branches.
 *
 * Windows differ per subject on purpose, and each footnote says which it used: dense series
 * (nutrition) average their last [PREVIEW_POINTS] days, sparse ones (sleep, heart, mood, fasting,
 * supplements, blood pressure) average whatever the repository returned, which is already the year
 * the charts draw. Nothing here re-slices to a chart range — the card is a standing summary, and
 * the range toggle belongs to the chart it sits in.
 *
 * The words are `progress_summary_*`, carried as [Phrase]s the card resolves; a figure and its unit
 * symbol travel pre-formatted as one argument, exactly as they read.
 */
@Suppress("CyclomaticComplexMethod", "LongMethod")
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
                value = formatOneDecimal(entries.last().weightKg.kgToDisplayUnit(unit)),
                unit = Phrase.Raw(unit.weightUnitLabel()),
                preview = SubjectPreview.Line(
                    entries.takeLast(PREVIEW_POINTS).map { it.weightKg.kgToDisplayUnit(unit) },
                ),
                footnote = if (trend.hasPrior) {
                    phrase(
                        R.string.progress_summary_weight_week,
                        "${formatOneDecimal(abs(trend.deltaKg).kgToDisplayUnit(unit))} ${unit.weightUnitLabel()}",
                        phrase(trendWord(uiState.goal, trend.deltaKg)),
                    )
                } else {
                    phrase(R.string.progress_summary_one_reading)
                },
                arrow = if (trend.hasPrior) arrowFor(trend.deltaKg, TREND_ARROW_DEADBAND_KG) else null,
                trend = if (trend.hasPrior) goalRelativeTrend(uiState.goal, trend.deltaKg) else TrendDirection.Neutral,
            )
        }

        Subject.Photos -> {
            val photos = uiState.photos
            if (photos.isEmpty()) return SubjectSummary(subject)
            val newest = photos.maxOf { it.dateEpochDay }
            val ago = daysAgo(newest, todayEpochDay)
            // The weight the shots themselves carry, oldest to newest — the same field and the
            // same goal-relative call `ComparisonHeadline` makes over a hand-picked pair, so the
            // card and the overlay cannot read one run two ways. Absent it, the date alone, which
            // is what this card said before there was anything else to say.
            val arc = photos.weightArc()
            SubjectSummary(
                subject = subject,
                value = "${photos.size}",
                unit = plural(R.plurals.progress_summary_shots, photos.size),
                preview = SubjectPreview.PhotoStrip(
                    photos.sortedByDescending { it.dateEpochDay }.take(3).map { it.filePath },
                ),
                footnote = arc?.let { (deltaKg, days) ->
                    phrase(
                        R.string.progress_summary_photos_arc,
                        "${formatOneDecimal(abs(deltaKg).kgToDisplayUnit(unit))} ${unit.weightUnitLabel()}",
                        plural(R.plurals.progress_summary_over_days, days.toInt(), days),
                        ago,
                    )
                } ?: phrase(R.string.progress_summary_last_one, ago),
                // Direction is the arrow and the judgement is the colour — the split `TrendArrow`
                // and `TrendDirection` exist for. The text stays absolute, the Weight card's rule.
                arrow = arc?.let { (deltaKg, _) -> arrowFor(deltaKg, TREND_ARROW_DEADBAND_KG) },
                trend = arc?.let { (deltaKg, _) -> goalRelativeTrend(uiState.goal, deltaKg) }
                    ?: TrendDirection.Neutral,
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
            val delta = history.delta()
            SubjectSummary(
                subject = subject,
                value = formatOneDecimal(lead.key.toDisplay(history.last().value, unit)),
                // Body fat is a percentage, so it carries its own words rather than the unit
                // toggle's. Every other part reads "cm waist": the symbol, then the part's noun.
                unit = if (lead.key.percent) {
                    phrase(R.string.progress_summary_unit_body_fat)
                } else {
                    phrase(R.string.progress_summary_unit_part, unit.lengthUnitLabel(), phrase(nounFor(lead.key)))
                },
                preview = SubjectPreview.Line(
                    history.takeLast(PREVIEW_POINTS).map { lead.key.toDisplay(it.value, unit) },
                ),
                footnote = plural(R.plurals.progress_summary_parts, tracked.size, tracked.size).let { parts ->
                    if (delta == null) {
                        parts
                    } else {
                        phrase(
                            R.string.progress_summary_measure_delta,
                            "${formatOneDecimal(lead.key.toDisplay(abs(delta), unit))} ${lead.key.unitLabel(unit)}",
                            parts,
                        )
                    }
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
                unit = phrase(R.string.progress_summary_unit_kcal_avg),
                preview = SubjectPreview.Bars(week.map { it.calories }),
                footnote = when {
                    target == null -> phrase(R.string.progress_summary_days_logged_of, averages.daysLogged, week.size)
                    averages.calories < target -> phrase(R.string.progress_summary_kcal_under, target - averages.calories)
                    averages.calories > target -> phrase(R.string.progress_summary_kcal_over, averages.calories - target)
                    else -> phrase(R.string.progress_summary_on_target)
                },
            )
        }

        Subject.Water -> {
            val days = uiState.waterDays
            val averages = days.waterAverages(uiState.waterGoalGlasses)
            val average = averages.averageGlasses ?: return SubjectSummary(subject)
            SubjectSummary(
                subject = subject,
                value = formatDecimals(average, decimals = 1),
                unit = phrase(R.string.progress_summary_unit_glasses_avg),
                preview = SubjectPreview.Bars(days.takeLast(PREVIEW_POINTS).map { it.glasses }),
                footnote = plural(R.plurals.progress_summary_water_goal_days, averages.daysLogged, averages.daysHitGoal, averages.daysLogged),
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
                unit = phrase(R.string.progress_summary_unit_avg),
                preview = SubjectPreview.Bars(
                    sessions.takeLast(PREVIEW_POINTS).map { it.durationMinutes(nowMillis = 0) },
                ),
                footnote = phrase(R.string.progress_summary_fast_goals, averages.goalsHit, averages.count),
            )
        }

        Subject.Supplements -> {
            val days = uiState.supplementDays
            val adherence = days.averageAdherence() ?: return SubjectSummary(subject)
            val byDay = days.adherenceByDay()
            SubjectSummary(
                subject = subject,
                value = "${(adherence * 100).roundToInt()}",
                unit = phrase(R.string.progress_supplements_hero),
                preview = SubjectPreview.Bars(
                    byDay.takeLast(PREVIEW_POINTS).map { (_, ratio) -> (ratio * 100).roundToInt() },
                ),
                footnote = plural(R.plurals.progress_summary_days_logged, byDay.size, byDay.size),
            )
        }

        Subject.Activity -> {
            val days = uiState.stepDays
            val averages = days.stepAverages(uiState.stepGoal)
            val average = averages.averageSteps ?: return SubjectSummary(subject)
            SubjectSummary(
                subject = subject,
                value = formatSteps(average),
                unit = phrase(R.string.progress_summary_unit_steps),
                preview = SubjectPreview.Bars(days.takeLast(PREVIEW_POINTS).map { it.steps }),
                footnote = phrase(R.string.progress_summary_steps_goal, averages.daysHitGoal, averages.days),
            )
        }

        Subject.Strength -> {
            val lifted = uiState.exerciseEntries.withSets()
            if (lifted.isEmpty()) return SubjectSummary(subject)
            val totals = lifted.strengthTotals()
            SubjectSummary(
                subject = subject,
                value = "${totals.workouts}",
                unit = plural(R.plurals.progress_summary_workouts, totals.workouts),
                preview = SubjectPreview.Bars(
                    lifted.volumeByDay().takeLast(PREVIEW_POINTS).map { it.volumeKg.roundToInt() },
                ),
                footnote = phrase(R.string.progress_summary_lifted, volumeLabel(totals.volumeKg, unit)),
            )
        }

        Subject.Sleep -> {
            val nights = uiState.sleepNights
            val averages = nights.sleepAverages()
            val average = averages.averageMinutes ?: return SubjectSummary(subject)
            SubjectSummary(
                subject = subject,
                value = formatDuration(average),
                unit = phrase(R.string.progress_summary_unit_avg),
                preview = SubjectPreview.Bars(nights.takeLast(PREVIEW_POINTS).map { it.minutesAsleep }),
                footnote = plural(R.plurals.progress_summary_watch_nights, averages.nights, averages.nights),
            )
        }

        Subject.Mood -> {
            val days = uiState.moodDays
            val averages = days.moodAverages()
            val mood = averages.mood ?: return SubjectSummary(subject)
            SubjectSummary(
                subject = subject,
                value = formatDecimals(mood, decimals = 1),
                unit = Phrase.Raw("/ ${MOOD_SCALE.last}"),
                preview = SubjectPreview.Bars(days.takeLast(PREVIEW_POINTS).map { it.mood }),
                footnote = plural(R.plurals.progress_summary_days_logged, averages.daysLogged, averages.daysLogged),
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
                unit = phrase(R.string.progress_summary_unit_cycle_day),
                // The last week's flow, gaps drawn as stubs — a week is what the strip can hold.
                preview = SubjectPreview.Bars(
                    (todayEpochDay - PREVIEW_POINTS + 1..todayEpochDay).map { day ->
                        days.firstOrNull { it.dateEpochDay == day }?.flow ?: 0
                    },
                ),
                footnote = averages.cycleDays
                    ?.let { phrase(R.string.progress_summary_avg_cycle, it.roundToInt()) }
                    ?: plural(R.plurals.progress_summary_days_logged, averages.daysLogged, averages.daysLogged),
            )
        }

        Subject.Heart -> {
            val days = uiState.heartDays
            val averages = days.heartAverages()
            val average = averages.averageBpm ?: return SubjectSummary(subject)
            SubjectSummary(
                subject = subject,
                value = "$average",
                unit = phrase(R.string.progress_summary_unit_bpm_avg),
                preview = SubjectPreview.Bars(days.takeLast(PREVIEW_POINTS).map { it.averageBpm }),
                footnote = averages.lowestBpm?.let { phrase(R.string.progress_summary_lowest_bpm, it) }
                    ?: phrase(R.string.progress_summary_from_watch),
            )
        }

        Subject.BloodPressure -> {
            val readings = uiState.bloodPressure
            val averages = readings.averages()
            val systolic = averages.systolic ?: return SubjectSummary(subject)
            SubjectSummary(
                subject = subject,
                value = "$systolic/${averages.diastolic}",
                unit = phrase(R.string.progress_summary_unit_mmhg_avg),
                preview = SubjectPreview.Bars(readings.byDay().takeLast(PREVIEW_POINTS).map { it.systolic }),
                footnote = plural(R.plurals.progress_summary_readings, averages.readings, averages.readings),
            )
        }

        Subject.Badges -> {
            val tally = badgeTally(uiState, todayEpochDay)
            if (uiState.activeDays.isEmpty()) return SubjectSummary(subject)
            SubjectSummary(
                subject = subject,
                value = "${tally.earned}",
                unit = phrase(R.string.progress_summary_badges_of, tally.total),
                footnote = plural(R.plurals.progress_summary_families, tally.families, tally.families),
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
 * `MeasurementRow` gives a single entry, rather than a false 0.0. In stored units, so the caller
 * converts it through the part like every other figure on the card. */
private fun List<MeasurementEntry>.delta(): Double? =
    if (size >= 2) last().value - this[size - 2].value else null

private fun arrowFor(delta: Double, deadband: Double): TrendArrow = when {
    abs(delta) < deadband || delta == 0.0 -> TrendArrow.Flat
    delta < 0 -> TrendArrow.Down
    else -> TrendArrow.Up
}

/** The words beside the arrow. Colour never carries this on its own. */
@StringRes
private fun trendWord(goal: Goal?, deltaKg: Double): Int =
    if (abs(deltaKg) < TREND_ARROW_DEADBAND_KG) {
        R.string.progress_summary_trend_steady
    } else {
        when (goalRelativeTrend(goal, deltaKg)) {
            TrendDirection.OnTrack -> R.string.progress_summary_trend_on_track
            TrendDirection.OffTrack -> R.string.progress_summary_trend_off_track
            TrendDirection.Neutral -> R.string.progress_summary_trend_steady
        }
    }

/** Lowercase on purpose — it sits mid-sentence, after "last one". */
private fun daysAgo(dateEpochDay: Long, todayEpochDay: Long): Phrase = when (val days = todayEpochDay - dateEpochDay) {
    0L -> phrase(R.string.progress_summary_ago_today)
    1L -> phrase(R.string.progress_summary_ago_yesterday)
    else -> plural(R.plurals.progress_summary_ago_days, days.toInt(), days)
}

/** The part as a noun inside "cm waist" — lowercase, unlike [MeasurementPart.label], which heads a
 * row. Body fat never reaches here (it reads "% body fat"), but the `when` stays total. */
@StringRes
private fun nounFor(part: MeasurementPart): Int = when (part) {
    MeasurementPart.Chest -> R.string.progress_summary_part_chest
    MeasurementPart.Waist -> R.string.progress_summary_part_waist
    MeasurementPart.Hips -> R.string.progress_summary_part_hips
    MeasurementPart.Arms -> R.string.progress_summary_part_arms
    MeasurementPart.Thighs -> R.string.progress_summary_part_thighs
    MeasurementPart.BodyFat -> R.string.progress_summary_part_body_fat
}
