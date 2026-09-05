package ph.mart.healthapp.feature.progress.ui.weight.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ph.mart.healthapp.core.data.profile.EnergyCheckIn
import ph.mart.healthapp.core.data.profile.EnergyEstimate
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.MIN_MEANINGFUL_DELTA_KCAL
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
import ph.mart.healthapp.feature.progress.R
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
        caption = stringResource(R.string.progress_weight_today, unit.weightUnitLabel()),
    )

    FactChipRow(
        chips = listOfNotNull(
            windowDelta?.takeIf { filtered.size >= 2 }?.let { delta ->
                val direction = goalRelativeTrend(uiState.goal, delta)
                FactChip(
                    text = stringResource(
                        R.string.progress_weight_in_span,
                        formatKg(abs(delta).kgToDisplayUnit(unit)),
                        unit.weightUnitLabel(),
                        range.spanWords(),
                        direction.word(delta),
                    ),
                    leading = arrowFor(delta),
                    trend = if (abs(delta) < TREND_ARROW_DEADBAND_KG) TrendDirection.Neutral else direction,
                )
            },
            // Hides with the goal line and the projection insight — all three read the one
            // nullable target weight, so they can never disagree about whether there is a goal.
            uiState.goalWeightKg?.let {
                FactChip(
                    text = stringResource(
                        R.string.progress_weight_to_goal,
                        formatKg(abs(current - it).kgToDisplayUnit(unit)),
                        unit.weightUnitLabel(),
                    ),
                )
            },
        ),
    )

    ChartCard(
        title = stringResource(R.string.progress_weight_trend),
        range = range,
        onRangeChange = { state.setRange(Subject.Weight, it) },
        legend = listOfNotNull(
            LegendEntry(stringResource(R.string.progress_weight_daily), MaterialTheme.colorScheme.primary),
            LegendEntry(stringResource(R.string.progress_weight_average7), MaterialTheme.colorScheme.secondary),
            uiState.goalWeightKg?.let {
                LegendEntry(
                    label = stringResource(
                        R.string.progress_weight_goal_legend,
                        formatKg(it.kgToDisplayUnit(unit)),
                        unit.weightUnitLabel(),
                    ),
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
                label = stringResource(R.string.progress_weight_this_week),
                value = if (weekTrend.hasPrior) {
                    stringResource(
                        if (weekTrend.deltaKg < 0) R.string.progress_weight_down else R.string.progress_weight_up,
                        formatKg(abs(weekTrend.deltaKg).kgToDisplayUnit(unit)),
                        unit.weightUnitLabel(),
                    )
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
                label = stringResource(R.string.progress_weight_weekly_average),
                value = projection?.let {
                    stringResource(
                        if (it.kgPerWeek < 0) R.string.progress_weight_down else R.string.progress_weight_up,
                        formatKg(abs(it.kgPerWeek).kgToDisplayUnit(unit)),
                        unit.weightUnitLabel(),
                    )
                } ?: stringResource(R.string.progress_none),
            ),
            StatRow(
                label = stringResource(R.string.progress_weight_readings),
                value = range.days?.let { stringResource(R.string.progress_weight_readings_of, filtered.size, it) }
                    ?: pluralStringResource(R.plurals.progress_weight_readings_count, filtered.size, filtered.size),
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
            goalWeightLabel = stringResource(
                R.string.progress_weight_value,
                formatKg(it.goalWeightKg.kgToDisplayUnit(unit)),
                unit.weightUnitLabel(),
            ),
            targetEpochDay = it.targetEpochDay,
            reached = it.reached,
            windowDays = PROJECTION_WINDOW_DAYS,
        )
    }
    val estimate = checkIn?.estimate
    val checkInHeadline = when {
        checkIn == null -> null
        estimate == null -> stringResource(R.string.progress_weight_measuring)
        else -> stringResource(R.string.progress_weight_burning, estimate.maintenanceKcal)
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
        projectionLine != null -> checkInHeadline?.let {
            stringResource(R.string.progress_weight_combined, it, checkInNote.orEmpty())
        }
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
@Composable
private fun missingNote(checkIn: EnergyCheckIn) =
    pluralStringResource(
        R.plurals.progress_weight_checkin_progress,
        checkIn.weighIns,
        checkIn.daysLogged,
        checkIn.windowDays,
        checkIn.weighIns,
    )

@Composable
private fun deltaNote(estimate: EnergyEstimate): String {
    val delta = estimate.deltaKcal
    if (abs(delta) < MIN_MEANINGFUL_DELTA_KCAL) return stringResource(R.string.progress_weight_target_matches)
    return stringResource(
        if (delta > 0) R.string.progress_weight_target_under else R.string.progress_weight_target_over,
        abs(delta),
    )
}

/** "in 3 months", not "in 3M" — the chip is a sentence, the toggle above it is a control. */
@Composable
private fun ChartRange.spanWords(): String = when (this) {
    ChartRange.OneMonth -> stringResource(R.string.progress_span_month)
    ChartRange.ThreeMonths -> stringResource(R.string.progress_span_3months)
    ChartRange.SixMonths -> stringResource(R.string.progress_span_6months)
    ChartRange.OneYear -> stringResource(R.string.progress_span_year)
}

@Composable
private fun TrendDirection.word(deltaKg: Double): String = when {
    abs(deltaKg) < TREND_ARROW_DEADBAND_KG -> stringResource(R.string.progress_word_steady)
    this == TrendDirection.OnTrack -> stringResource(R.string.progress_word_on_track)
    this == TrendDirection.OffTrack -> stringResource(R.string.progress_word_off_track)
    else -> stringResource(R.string.progress_word_recorded)
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
