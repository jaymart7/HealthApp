package ph.mart.healthapp.feature.progress.ui.weight.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ph.mart.healthapp.core.data.profile.EnergyCheckIn
import ph.mart.healthapp.core.data.profile.EnergyEstimate
import ph.mart.healthapp.core.data.profile.MIN_MEANINGFUL_DELTA_KCAL
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.TREND_ARROW_DEADBAND_KG
import ph.mart.healthapp.core.data.profile.TrendDirection
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.goalRelativeTrend
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.trendVsSevenDaysAgo
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.data.progress.GoalProjection
import ph.mart.healthapp.core.data.progress.PROJECTION_WINDOW_DAYS
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.progress.inRange
import ph.mart.healthapp.core.data.progress.withMovingAverage
import ph.mart.healthapp.core.designsystem.component.AIInsightCard
import ph.mart.healthapp.core.designsystem.component.goalProjectionLine
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreenState
import ph.mart.healthapp.feature.progress.ui.progress.ProgressUiState
import ph.mart.healthapp.feature.progress.ui.progress.Subject
import ph.mart.healthapp.feature.progress.ui.progress.components.ChartCard
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChip
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChipRow
import ph.mart.healthapp.feature.progress.ui.progress.components.HeroValue
import ph.mart.healthapp.feature.progress.ui.progress.components.LegendEntry
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRow
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRowsCard

/**
 * The richest single-subject page, and the template the other twelve follow: hero, fact chips, a
 * chart card holding its own range toggle and legend, one insight card, stat rows.
 *
 * Its insight card is the only one in the app fed by two sources. The projection is the headline
 * when there is one; with no target weight (or a Maintain goal, or too few recent weigh-ins) the
 * **energy check-in takes the headline** rather than the card disappearing, because the check-in is
 * a measured figure that stands on its own. The card is gone only when neither has anything to say,
 * which is the rule the whole screen follows.
 */
@Composable
internal fun ColumnScope.WeightDetailBody(
    uiState: ProgressUiState,
    state: ProgressScreenState,
    checkIn: EnergyCheckIn?,
    projection: GoalProjection?,
) {
    val unit = uiState.preferredUnit
    val range = state.rangeFor(Subject.Weight)
    val sorted = uiState.weightEntries.sortedBy { it.dateEpochDay }
    val current = sorted.last().weightKg
    val filtered = uiState.weightEntries.inRange(range)
    val windowDelta = filtered.firstOrNull()?.let { current - it.weightKg }

    HeroValue(
        value = formatKg(current.kgToDisplayUnit(unit)),
        caption = "${unit.weightUnitLabel()} today",
    )

    FactChipRow(
        chips = listOfNotNull(
            windowDelta?.takeIf { filtered.size >= 2 }?.let { delta ->
                val direction = goalRelativeTrend(uiState.goal, delta)
                FactChip(
                    text = "${formatKg(abs(delta).kgToDisplayUnit(unit))} ${unit.weightUnitLabel()} " +
                        "in ${range.spanWords()} · ${direction.word(delta)}",
                    leading = arrowFor(delta),
                    trend = if (abs(delta) < TREND_ARROW_DEADBAND_KG) TrendDirection.Neutral else direction,
                )
            },
            // Hides with the goal line and the projection insight — all three read the one
            // nullable target weight, so they can never disagree about whether there is a goal.
            uiState.goalWeightKg?.let {
                FactChip(
                    text = "${formatKg(abs(current - it).kgToDisplayUnit(unit))} " +
                        "${unit.weightUnitLabel()} to goal",
                )
            },
        ),
    )

    ChartCard(
        title = "Trend",
        range = range,
        onRangeChange = { state.setRange(Subject.Weight, it) },
        legend = listOfNotNull(
            LegendEntry("Daily", MaterialTheme.colorScheme.primary),
            LegendEntry("7-day average", MaterialTheme.colorScheme.secondary),
            uiState.goalWeightKg?.let {
                LegendEntry(
                    label = "Goal ${formatKg(it.kgToDisplayUnit(unit))} ${unit.weightUnitLabel()}",
                    color = MaterialTheme.colorScheme.tertiary,
                    dashed = true,
                )
            },
        ),
    ) {
        WeightProgressChart(
            points = filtered.withMovingAverage(),
            goalWeightKg = uiState.goalWeightKg,
            unit = unit,
        )
    }

    InsightCard(uiState = uiState, checkIn = checkIn, projection = projection, onOpen = state::openEnergyCheckIn)

    val weekTrend = uiState.weightEntries.trendVsSevenDaysAgo(fallbackKg = current)
    StatRowsCard(
        rows = listOf(
            StatRow(
                label = "This week",
                value = if (weekTrend.hasPrior) {
                    "${formatKg(abs(weekTrend.deltaKg).kgToDisplayUnit(unit))} ${unit.weightUnitLabel()} " +
                        if (weekTrend.deltaKg < 0) "down" else "up"
                } else {
                    "—"
                },
                trend = if (weekTrend.hasPrior && abs(weekTrend.deltaKg) >= TREND_ARROW_DEADBAND_KG) {
                    goalRelativeTrend(uiState.goal, weekTrend.deltaKg)
                } else {
                    TrendDirection.Neutral
                },
            ),
            StatRow(
                label = "Weekly average",
                value = projection?.let {
                    "${formatKg(abs(it.kgPerWeek).kgToDisplayUnit(unit))} ${unit.weightUnitLabel()} " +
                        if (it.kgPerWeek < 0) "down" else "up"
                } ?: "—",
            ),
            StatRow(
                label = "Readings logged",
                value = range.days?.let { "${filtered.size} of $it days" } ?: "${filtered.size} readings",
            ),
        ),
    )
}

/**
 * The page's one `tertiaryContainer` card, and the only insight card in the app fed by two
 * sources — which is what let the standalone projection and energy-check-in cards go.
 *
 * The projection takes the headline when there is one. Without a target weight there is no
 * projection at all, and the check-in takes it instead: a measured maintenance is a figure that
 * stands on its own, and it is the reason the card doesn't simply vanish for anyone who never set a
 * goal weight. The check-in reports what it is still missing rather than going quiet, because a
 * card that says nothing gives nobody a reason to keep weighing in.
 */
@Composable
private fun ColumnScope.InsightCard(
    uiState: ProgressUiState,
    checkIn: EnergyCheckIn?,
    projection: GoalProjection?,
    onOpen: () -> Unit,
) {
    val unit = uiState.preferredUnit
    val projectionLine = projection?.let {
        goalProjectionLine(
            goalWeightLabel = "${formatKg(it.goalWeightKg.kgToDisplayUnit(unit))} ${unit.weightUnitLabel()}",
            targetEpochDay = it.targetEpochDay,
            reached = it.reached,
            windowDays = PROJECTION_WINDOW_DAYS,
        )
    }
    val estimate = checkIn?.estimate
    val checkInHeadline = when {
        checkIn == null -> null
        estimate == null -> "Measuring what you actually burn"
        else -> "You're burning about ${estimate.maintenanceKcal} kcal a day"
    }
    val checkInNote = when {
        checkIn == null -> null
        estimate == null -> missingNote(checkIn)
        else -> deltaNote(estimate)
    }

    val headline = projectionLine ?: checkInHeadline ?: return
    val subline = when {
        // The projection already has the headline, so the check-in becomes the second line and
        // brings its own "tap to review" with it.
        projectionLine != null -> checkInHeadline?.let { "$it. $checkInNote" }
        else -> checkInNote
    }
    AIInsightCard(
        text = headline,
        subline = subline,
        onClick = if (checkIn != null) onOpen else null,
        headlineStyle = MaterialTheme.typography.titleMedium,
    )
}

/** Both counts every time, so the user can tell which one is holding the measurement up. */
private fun missingNote(checkIn: EnergyCheckIn) =
    "${checkIn.daysLogged} of ${checkIn.windowDays} days logged · " +
        "${checkIn.weighIns} ${if (checkIn.weighIns == 1) "weigh-in" else "weigh-ins"}. Keep going."

private fun deltaNote(estimate: EnergyEstimate): String {
    val delta = estimate.deltaKcal
    if (abs(delta) < MIN_MEANINGFUL_DELTA_KCAL) return "Your calorie target already matches. Tap for the detail."
    return "Your target is ${abs(delta)} kcal ${if (delta > 0) "under" else "over"} what this suggests. " +
        "Tap to review."
}

/** "in 3 months", not "in 3M" — the chip is a sentence, the toggle above it is a control. */
private fun ChartRange.spanWords(): String = when (this) {
    ChartRange.OneMonth -> "a month"
    ChartRange.ThreeMonths -> "3 months"
    ChartRange.SixMonths -> "6 months"
    ChartRange.OneYear -> "a year"
}

private fun TrendDirection.word(deltaKg: Double): String = when {
    abs(deltaKg) < TREND_ARROW_DEADBAND_KG -> "steady"
    this == TrendDirection.OnTrack -> "on track"
    this == TrendDirection.OffTrack -> "off track"
    else -> "recorded"
}

private fun arrowFor(delta: Double) = when {
    abs(delta) < TREND_ARROW_DEADBAND_KG -> AppIcons.TrendFlat
    delta < 0 -> AppIcons.TrendDown
    else -> AppIcons.TrendUp
}

@PreviewLightDark
@Composable
private fun WeightDetailPreview() {
    val today = 20_700L
    val entries = (0..10).map {
        WeightEntry(dateEpochDay = today - (10 - it) * 8, weightKg = 84.8 - it * 0.21)
    }
    val uiState = ProgressUiState(
        weightEntries = entries,
        goalWeightKg = 82.0,
        goal = Goal.Lose,
        preferredUnit = UnitSystem.Metric,
    )
    AppTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WeightDetailBody(
                    uiState = uiState,
                    state = ProgressScreenState(),
                    checkIn = null,
                    projection = GoalProjection(
                        goalWeightKg = 82.0,
                        kgPerWeek = -0.4,
                        targetEpochDay = today + 20,
                        reached = false,
                    ),
                )
            }
        }
    }
}
