package ph.mart.healthapp.feature.progress.ui.progress.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ph.mart.healthapp.core.data.exercise.LiftRecord
import ph.mart.healthapp.core.data.exercise.StrengthTotals
import ph.mart.healthapp.core.data.exercise.formatLoad
import ph.mart.healthapp.core.data.exercise.volumeLabel
import ph.mart.healthapp.core.data.food.NutritionAverages
import ph.mart.healthapp.core.data.health.StepAverages
import ph.mart.healthapp.core.data.health.formatSteps
import ph.mart.healthapp.core.data.mood.MOOD_SCALE
import ph.mart.healthapp.core.data.mood.MoodAverages
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.TREND_ARROW_DEADBAND_KG
import ph.mart.healthapp.core.data.profile.TrendDirection
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.WeightTrendDisplay
import ph.mart.healthapp.core.data.profile.goalRelativeTrend
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.GoalProjection
import ph.mart.healthapp.core.data.progress.PROJECTION_WINDOW_DAYS
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.formatWeekday
import ph.mart.healthapp.core.designsystem.component.goalProjectionLine
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.progress.BestDay
import ph.mart.healthapp.feature.progress.ui.progress.Recap
import ph.mart.healthapp.feature.progress.ui.progress.RecapPeriod
import ph.mart.healthapp.feature.progress.ui.weight.components.StatCell
import ph.mart.healthapp.feature.progress.ui.weight.components.formatKg

/**
 * The rolling window at a glance — it spans nutrition, weight and consistency, so it belongs to no
 * single tab. Every number is derived in
 * [ph.mart.healthapp.feature.progress.ui.progress.recap]; this only formats.
 *
 * The weight colour comes from the shared [goalRelativeTrend], never a green-for-loss default,
 * and reads neutral below [TREND_ARROW_DEADBAND_KG] where the movement is too small to call.
 *
 * [projection] is the one figure here that is **not** a window figure: the weight cell says which
 * way the period went, and this says when it arrives. That is why the shared sentence names its
 * own window — a line under a "Last 7 days" heading that quietly reported a 30-day fit would be
 * the card contradicting itself. Null (no target weight, a Maintain goal, or too little recent
 * data) drops the line, the same way the weight cell drops to an em dash.
 *
 * The movement row is drawn for the longer periods only. A week's worth of workouts and steps is
 * a handful of numbers the Activity tab already shows better, and the card on the Progress screen
 * is the seven-day one — so what ships above the tab toggle is untouched by this.
 */
@Composable
fun RecapCard(
    recap: Recap,
    goal: Goal?,
    unit: UnitSystem,
    projection: GoalProjection?,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Text(
            text = recap.period.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatCell(label = "Days logged", value = "${recap.daysLogged}/${recap.period.days}")
            StatCell(
                label = "Avg calories",
                value = recap.targets?.let { "${recap.averages.calories} / ${it.calories}" }
                    ?: "${recap.averages.calories}",
            )
            WeightCell(trend = recap.weightTrend, goal = goal, unit = unit)
            StatCell(label = "Mood", value = moodValue(recap.moodAverages))
        }
        if (recap.period != RecapPeriod.Week) {
            // Three cells, not four: "12,450 kg" is a wider value than anything in the row above,
            // and volume reads as a note rather than a headline anyway.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatCell(label = "Workouts", value = "${recap.workouts}")
                StatCell(label = "Burned", value = "${recap.burnedKcal} kcal")
                StatCell(label = "Avg steps", value = recap.steps.averageSteps?.let(::formatSteps) ?: "—")
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            recap.bestDay?.let { Note("Best day — ${formatWeekday(it.dateEpochDay)}, ${it.calories} kcal") }
            // Two denominators in one card: the four-domain day count and the food-only average.
            // Spelled out when they differ, the same way NutritionAverageCard does.
            if (recap.averages.daysLogged != recap.daysLogged) {
                val days = recap.averages.daysLogged
                Note("Calories averaged over $days ${if (days == 1) "day" else "days"} with food logged.")
            }
            if (recap.weightTrend?.hasPrior != true) {
                Note("No weight change to compare yet.")
            }
            if (recap.period != RecapPeriod.Week) {
                recap.topLift?.let { Note(topLiftLine(it, unit)) }
                if (recap.strength.workouts > 0) Note(volumeLine(recap.strength, unit))
            }
            projection?.let {
                Note(
                    goalProjectionLine(
                        goalWeightLabel = "${formatKg(it.goalWeightKg.kgToDisplayUnit(unit))} ${unit.weightUnitLabel()}",
                        targetEpochDay = it.targetEpochDay,
                        reached = it.reached,
                        windowDays = PROJECTION_WINDOW_DAYS,
                    ),
                )
            }
        }
    }
}

/** A bodyweight best has no load to name, so it is reported in reps — the reading
 * [ph.mart.healthapp.core.data.exercise.loadLabel] gives a zero `weightKg` everywhere else. */
private fun topLiftLine(lift: LiftRecord, unit: UnitSystem): String =
    if (lift.bestWeightKg <= 0.0) {
        "Top lift — ${lift.exerciseName}, ${lift.bestReps} reps bodyweight"
    } else {
        val load = "${formatLoad(lift.bestWeightKg.kgToDisplayUnit(unit))} ${unit.weightUnitLabel()}"
        "Top lift — ${lift.exerciseName}, $load × ${lift.bestReps}"
    }

private fun volumeLine(totals: StrengthTotals, unit: UnitSystem): String =
    "Lifted ${volumeLabel(totals.volumeKg, unit)} across ${totals.sets} " +
        if (totals.sets == 1) "set." else "sets."

/** Mood only — the recap row is already four cells wide, and energy has its own cell on the
 * Mood tab. An em dash when the window has weight and food but no reflection. */
private fun moodValue(averages: MoodAverages?): String =
    averages?.mood?.let { "%.1f/${MOOD_SCALE.last}".format(it) } ?: "—"

@Composable
private fun WeightCell(trend: WeightTrendDisplay?, goal: Goal?, unit: UnitSystem) {
    if (trend == null || !trend.hasPrior) {
        StatCell(label = "Weight", value = "—")
        return
    }
    val delta = trend.deltaKg
    val neutral = MaterialTheme.colorScheme.onSurfaceVariant
    StatCell(
        label = "Weight",
        value = "${if (delta > 0) "+" else ""}${formatKg(delta.kgToDisplayUnit(unit))} ${unit.weightUnitLabel()}",
        valueColor = if (abs(delta) < TREND_ARROW_DEADBAND_KG) {
            neutral
        } else {
            when (goalRelativeTrend(goal, delta)) {
                TrendDirection.OnTrack -> MaterialTheme.colorScheme.primary
                TrendDirection.OffTrack -> MaterialTheme.colorScheme.error
                TrendDirection.Neutral -> neutral
            }
        },
    )
}

/** Shared with [GoalProjectionCard] — same screen, same feature, so it stays here rather than
 * being promoted to `:core:designsystem` (that rule is about components used across screens). */
@Composable
internal fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private val PREVIEW_TARGETS =
    DailyTargets(calories = 2000, proteinG = 150, carbsG = 200, fatG = 67, floor = 1500)

@PreviewLightDark
@Composable
private fun RecapCardPreview() {
    AppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                RecapCard(
                    recap = Recap(
                        period = RecapPeriod.Week,
                        daysLogged = 7,
                        averages = NutritionAverages(1940, 141, 196, 68, daysLogged = 7),
                        targets = PREVIEW_TARGETS,
                        weightTrend = WeightTrendDisplay(currentKg = 76.0, deltaKg = -0.6, hasPrior = true),
                        moodAverages = MoodAverages(mood = 3.6, energy = 3.1, daysLogged = 6),
                        bestDay = BestDay(dateEpochDay = 20_690, calories = 1985),
                    ),
                    goal = Goal.Lose,
                    unit = UnitSystem.Metric,
                    projection = GoalProjection(
                        goalWeightKg = 72.0,
                        kgPerWeek = -0.4,
                        targetEpochDay = 20_760,
                        reached = false,
                    ),
                )
                // A month, with the movement row the week never draws.
                RecapCard(
                    recap = Recap(
                        period = RecapPeriod.Month,
                        daysLogged = 24,
                        averages = NutritionAverages(1885, 138, 189, 64, daysLogged = 21),
                        targets = PREVIEW_TARGETS,
                        weightTrend = WeightTrendDisplay(currentKg = 75.2, deltaKg = -0.4, hasPrior = true),
                        moodAverages = MoodAverages(mood = 3.9, energy = 3.4, daysLogged = 18),
                        bestDay = BestDay(dateEpochDay = 20_690, calories = 1985),
                        strength = StrengthTotals(workouts = 9, sets = 74, volumeKg = 41_820.0),
                        topLift = LiftRecord(
                            exerciseName = "Squat",
                            bestWeightKg = 100.0,
                            bestReps = 5,
                            bestOneRepMaxKg = 116.7,
                            dateEpochDay = 20_685,
                            sets = 27,
                        ),
                        burnedKcal = 8_940,
                        workouts = 12,
                        steps = StepAverages(averageSteps = 8_420, bestSteps = 16_002, daysHitGoal = 9, days = 26),
                    ),
                    goal = Goal.Lose,
                    unit = UnitSystem.Metric,
                    projection = null,
                )
                // Sparse week: water-only days pad the count, and nothing has been weighed — so
                // there is nothing to fit a rate over either.
                RecapCard(
                    recap = Recap(
                        period = RecapPeriod.Week,
                        daysLogged = 4,
                        averages = NutritionAverages(1720, 118, 170, 61, daysLogged = 2),
                        targets = PREVIEW_TARGETS,
                        weightTrend = null,
                        moodAverages = null,
                        bestDay = null,
                    ),
                    goal = Goal.Lose,
                    unit = UnitSystem.Metric,
                    projection = null,
                )
            }
        }
    }
}
