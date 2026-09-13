package ph.mart.healthapp.feature.progress.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.EnergyCheckIn
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.profile.TREND_ARROW_DEADBAND_KG
import ph.mart.healthapp.core.data.profile.TrendDirection
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.energyCheckIn
import ph.mart.healthapp.core.data.profile.goalRelativeTrend
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.trendVsSevenDaysAgo
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.data.progress.GoalProjection
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.progress.bmiCategoryOf
import ph.mart.healthapp.core.data.progress.bmiOf
import ph.mart.healthapp.core.data.progress.goalProjection
import ph.mart.healthapp.core.data.progress.inRange
import ph.mart.healthapp.core.data.progress.withMovingAverage
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.AppTopBar
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.energy.EnergyCheckInEvent
import ph.mart.healthapp.feature.progress.ui.energy.EnergyCheckInScreen
import ph.mart.healthapp.feature.progress.ui.energy.EnergyCheckInViewModel
import ph.mart.healthapp.feature.progress.ui.progress.Subject
import ph.mart.healthapp.feature.progress.ui.progress.components.ChartCard
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChip
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChipRow
import ph.mart.healthapp.feature.progress.ui.progress.components.HeroValue
import ph.mart.healthapp.feature.progress.ui.progress.components.LegendEntry
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRow
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRowsCard
import ph.mart.healthapp.feature.progress.ui.progress.components.SubjectSwitcher
import ph.mart.healthapp.feature.progress.ui.weight.components.WeightInsightCard
import ph.mart.healthapp.feature.progress.ui.weight.components.WeightProgressChart
import ph.mart.healthapp.feature.progress.ui.weight.components.formatKg

/**
 * The richest subject page, and the one the other twelve were modelled on: hero, fact chips, a
 * chart card holding its own range toggle and legend, one insight card, stat rows.
 *
 * A route of its own, `SleepScreen`'s shape. It is the only page that draws a full-screen overlay
 * of its own — the energy check-in — which is why it reaches for a second container: reading is
 * [WeightViewModel]'s and the check-in's Apply is
 * [EnergyCheckInViewModel]'s, both under this route's owner.
 */
@Composable
internal fun WeightScreen(
    onSwitchSubject: (Subject) -> Unit,
    onOpenRecap: () -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeightViewModel = koinViewModel(),
    energyViewModel: EnergyCheckInViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    WeightContent(
        entries = uiState.entries,
        dailyNutrition = uiState.dailyNutrition,
        profile = uiState.profile,
        onApplyTarget = { kcal -> energyViewModel.handleEvent(EnergyCheckInEvent.OnApply(kcal)) },
        onSwitchSubject = onSwitchSubject,
        onOpenRecap = onOpenRecap,
        onExitFlow = onExitFlow,
        modifier = modifier,
    )
}

@Composable
private fun WeightContent(
    entries: List<WeightEntry>,
    dailyNutrition: List<DayNutrition>,
    profile: Profile?,
    onApplyTarget: (Int) -> Unit,
    onSwitchSubject: (Subject) -> Unit,
    onOpenRecap: () -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    state: WeightState = rememberWeightState(),
) {
    val today = todayEpochDay()
    val unit = profile?.preferredUnit ?: UnitSystem.Metric
    // Folded here rather than in the container: both read `today`, so a recomposition is what picks
    // up a day boundary.
    val checkIn = profile?.let {
        remember(dailyNutrition, entries, it, today) {
            energyCheckIn(dailyNutrition, entries, it, today)
        }
    }
    val projection = goalProjection(
        weightEntries = entries,
        goalWeightKg = profile?.targetWeightKg,
        goal = profile?.goal,
        todayEpochDay = today,
    )

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Zero insets: this bar sits in the scaffold's content, which its `innerPadding` has
            // already cleared of the status bar. See `SleepScreen`.
            AppTopBar(
                title = stringResource(Subject.Weight.label),
                onBack = onExitFlow,
                windowInsets = WindowInsets(0),
                actions = {
                    IconButton(onClick = onOpenRecap) {
                        Icon(
                            imageVector = AppIcons.Share,
                            contentDescription = stringResource(R.string.progress_recap),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
            if (entries.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        FullScreenState(
                            icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
                            heading = stringResource(R.string.progress_empty_weight_heading),
                            body = stringResource(R.string.progress_empty_weight_body),
                        )
                    }
                    SubjectSwitcher(
                        subject = Subject.Weight,
                        onSelect = onSwitchSubject,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    WeightBody(
                        entries = entries,
                        goal = profile?.goal,
                        goalWeightKg = profile?.targetWeightKg,
                        heightCm = profile?.heightCm,
                        unit = unit,
                        checkIn = checkIn,
                        projection = projection,
                        state = state,
                    )
                    SubjectSwitcher(subject = Subject.Weight, onSelect = onSwitchSubject)
                }
            }
        }
    }

    if (state.checkInOpen && checkIn != null) {
        EnergyCheckInScreen(
            checkIn = checkIn,
            unit = unit,
            addExerciseToBudget = profile?.addExerciseToBudget ?: true,
            onApply = onApplyTarget,
            onClose = { state.checkInOpen = false },
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun ColumnScope.WeightBody(
    entries: List<WeightEntry>,
    goal: Goal?,
    goalWeightKg: Double?,
    heightCm: Double?,
    unit: UnitSystem,
    checkIn: EnergyCheckIn?,
    projection: GoalProjection?,
    state: WeightState,
) {
    val range = state.range
    val current = entries.maxByOrNull { it.dateEpochDay }!!.weightKg
    val filtered = entries.inRange(range)
    val windowDelta = filtered.firstOrNull()?.let { current - it.weightKg }

    HeroValue(
        value = formatKg(current.kgToDisplayUnit(unit)),
        caption = stringResource(R.string.progress_weight_today, unit.weightUnitLabel()),
    )

    FactChipRow(
        chips = listOfNotNull(
            windowDelta?.takeIf { filtered.size >= 2 }?.let { delta ->
                val direction = goalRelativeTrend(goal, delta)
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
            goalWeightKg?.let {
                FactChip(
                    text = stringResource(
                        R.string.progress_weight_to_goal,
                        formatKg(abs(current - it).kgToDisplayUnit(unit)),
                        unit.weightUnitLabel(),
                    ),
                )
            },
            // Neutral and iconless on purpose. A band is not a direction, and the chip's own
            // contract is never a trend without an arrow to say which way — so it carries neither.
            // Absent rather than zeroed when the profile has no height, like the goal chip above it.
            heightCm?.let { bmiOf(current, it) }?.let { bmi ->
                FactChip(
                    text = stringResource(
                        R.string.progress_weight_bmi,
                        formatBmi(bmi),
                        stringResource(bmiCategoryOf(bmi).label),
                    ),
                )
            },
        ),
    )

    ChartCard(
        title = stringResource(R.string.progress_weight_trend),
        range = range,
        onRangeChange = { state.range = it },
        legend = listOfNotNull(
            LegendEntry(stringResource(R.string.progress_weight_daily), MaterialTheme.colorScheme.primary),
            LegendEntry(stringResource(R.string.progress_weight_average7), MaterialTheme.colorScheme.secondary),
            goalWeightKg?.let {
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
            goalWeightKg = goalWeightKg,
            unit = unit,
        )
    }

    WeightInsightCard(
        checkIn = checkIn,
        projection = projection,
        unit = unit,
        onOpen = { state.checkInOpen = true },
    )

    val weekTrend = entries.trendVsSevenDaysAgo(fallbackKg = current)
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
                    goalRelativeTrend(goal, weekTrend.deltaKg)
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

/** Always one decimal, unlike [formatKg] beside it — a weight reads fine as "84", but "BMI 23"
 * next to "BMI 23.4" looks like two different precisions of the same figure. */
private fun formatBmi(value: Double): String = "%.1f".format(value)

private fun arrowFor(delta: Double) = when {
    abs(delta) < TREND_ARROW_DEADBAND_KG -> AppIcons.TrendFlat
    delta < 0 -> AppIcons.TrendDown
    else -> AppIcons.TrendUp
}

private fun entriesPreview(today: Long): List<WeightEntry> = (0..10).map {
    WeightEntry(dateEpochDay = today - (10 - it) * 8, weightKg = 84.8 - it * 0.21)
}

private fun profilePreview() = Profile(
    sex = Sex.Male,
    age = 34,
    heightCm = 178.0,
    weightKg = 82.6,
    activityLevel = ActivityLevel.Moderate,
    goal = Goal.Lose,
    targetWeightKg = 82.0,
)

@PreviewLightDark
@Composable
private fun WeightScreenPreview() {
    AppTheme {
        WeightContent(
            entries = entriesPreview(20_700L),
            dailyNutrition = emptyList(),
            profile = profilePreview(),
            onApplyTarget = {},
            onSwitchSubject = {},
            onOpenRecap = {},
            onExitFlow = {},
        )
    }
}

/** Nothing weighed yet — the page is still a real page, with the way on to its siblings. */
@PreviewLightDark
@Composable
private fun WeightScreenEmptyPreview() {
    AppTheme {
        WeightContent(
            entries = emptyList(),
            dailyNutrition = emptyList(),
            profile = profilePreview(),
            onApplyTarget = {},
            onSwitchSubject = {},
            onOpenRecap = {},
            onExitFlow = {},
        )
    }
}
