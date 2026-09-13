package ph.mart.healthapp.feature.progress.ui.activity

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.health.DEFAULT_STEP_GOAL
import ph.mart.healthapp.core.data.health.StepDay
import ph.mart.healthapp.core.data.health.burnSeries
import ph.mart.healthapp.core.data.health.formatSteps
import ph.mart.healthapp.core.data.health.inRange
import ph.mart.healthapp.core.data.health.stepAverages
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.AppTopBar
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.progress.Subject
import ph.mart.healthapp.feature.progress.ui.progress.components.ChartCard
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChip
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChipRow
import ph.mart.healthapp.feature.progress.ui.progress.components.HeroValue
import ph.mart.healthapp.feature.progress.ui.progress.components.LegendEntry
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRow
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRowsCard
import ph.mart.healthapp.feature.progress.ui.progress.components.SubjectSwitcher
import ph.mart.healthapp.feature.progress.ui.shared.components.DayBar
import ph.mart.healthapp.feature.progress.ui.shared.components.DayBarChart

/** Steps and burn over the picked window — a route of its own, `SleepScreen`'s shape. Steps are
 * imported and workouts are logged elsewhere, so this page only reads. */
@Composable
internal fun ActivityScreen(
    onSwitchSubject: (Subject) -> Unit,
    onOpenRecap: () -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    ActivityContent(
        stepDays = uiState.stepDays,
        exerciseEntries = uiState.exerciseEntries,
        stepGoal = uiState.stepGoal,
        onSwitchSubject = onSwitchSubject,
        onOpenRecap = onOpenRecap,
        onExitFlow = onExitFlow,
        modifier = modifier,
    )
}

@Composable
private fun ActivityContent(
    stepDays: List<StepDay>,
    exerciseEntries: List<ExerciseEntry>,
    stepGoal: Int,
    onSwitchSubject: (Subject) -> Unit,
    onOpenRecap: () -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    state: ActivityState = rememberActivityState(),
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Zero insets: this bar sits in the scaffold's content, which its `innerPadding` has
            // already cleared of the status bar. See `SleepScreen`.
            AppTopBar(
                title = stringResource(Subject.Activity.label),
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
            // Steps, not the burn series: this page is the watch's, and a burn chart drawn from
            // logged workouts alone is the Strength page's story told worse. The empty card on the
            // overview says the same thing, off the same field.
            if (stepDays.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        FullScreenState(
                            icon = { MascotAvatar(state = MascotState.Idle, size = 64.dp) },
                            heading = stringResource(R.string.progress_empty_activity_heading),
                            body = stringResource(R.string.progress_empty_activity_body),
                        )
                    }
                    SubjectSwitcher(
                        subject = Subject.Activity,
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
                    ActivityBody(
                        stepDays = stepDays,
                        exerciseEntries = exerciseEntries,
                        stepGoal = stepGoal,
                        state = state,
                    )
                    SubjectSwitcher(subject = Subject.Activity, onSelect = onSwitchSubject)
                }
            }
        }
    }
}

/**
 * Two charts, because steps and calories share no axis — and one range toggle between them, on the
 * first card. Two identical toggles would be two controls for one value.
 *
 * The burn series folds imported step days together with logged workouts through the same
 * `dayBurnedKcal()` the diary uses, so a walk the watch already counted is never counted twice. It
 * shows what was *burned* and therefore ignores the "add exercise to my budget" switch: that switch
 * decides what reaches the calorie budget, not what happened.
 *
 * The step goal is the profile's current one and is not snapshotted per day — `step_day` rows are
 * replaced wholesale on every re-sync, so a target stored beside them would be overwritten. The
 * stat says "today's goal" for that reason.
 */
@Composable
private fun ColumnScope.ActivityBody(
    stepDays: List<StepDay>,
    exerciseEntries: List<ExerciseEntry>,
    stepGoal: Int,
    state: ActivityState,
) {
    val range = state.range
    val today = todayEpochDay()
    val from = today - (range.days ?: ChartRange.OneYear.days!!)
    val steps = stepDays.inRange(range, today)
    val stepStats = steps.stepAverages(stepGoal)
    val burn = burnSeries(stepDays, exerciseEntries).inRange(range, today)

    HeroValue(value = stepStats.averageSteps?.let(::formatSteps) ?: stringResource(R.string.progress_none), caption = stringResource(R.string.progress_activity_hero))
    FactChipRow(
        chips = listOf(FactChip(stringResource(R.string.progress_activity_goal_days, stepStats.daysHitGoal, stepStats.days, formatSteps(stepGoal)))),
    )
    ChartCard(
        title = stringResource(R.string.progress_activity_steps),
        range = range,
        onRangeChange = { state.range = it },
        legend = listOf(
            LegendEntry(stringResource(R.string.progress_activity_steps), MaterialTheme.colorScheme.primary),
            LegendEntry(stringResource(R.string.progress_activity_goal, formatSteps(stepGoal)), MaterialTheme.colorScheme.onSurfaceVariant, dashed = true),
        ),
    ) {
        DayBarChart(
            bars = steps.map { DayBar(it.dateEpochDay, it.steps) },
            fromEpochDay = from,
            toEpochDay = today,
            minAxisValue = stepGoal,
            goalValue = stepGoal,
        )
    }
    ChartCard(
        title = stringResource(R.string.progress_activity_burned_title),
        range = null,
        onRangeChange = null,
        legend = listOf(LegendEntry(stringResource(R.string.progress_activity_burned_legend), MaterialTheme.colorScheme.primary)),
    ) {
        DayBarChart(
            bars = burn.map { DayBar(it.dateEpochDay, it.burnedKcal) },
            fromEpochDay = from,
            toEpochDay = today,
        )
    }
    StatRowsCard(
        rows = listOf(
            StatRow(stringResource(R.string.progress_activity_average), stepStats.averageSteps?.let(::formatSteps) ?: stringResource(R.string.progress_none)),
            StatRow(stringResource(R.string.progress_activity_best), stepStats.bestSteps?.let(::formatSteps) ?: stringResource(R.string.progress_none)),
            StatRow(stringResource(R.string.progress_activity_hit_goal), stringResource(R.string.progress_of, stepStats.daysHitGoal, stepStats.days)),
            StatRow(stringResource(R.string.progress_activity_burned), stringResource(R.string.progress_kcal, burn.sumOf { it.burnedKcal })),
        ),
    )
}

private fun stepDaysPreview(today: Long): List<StepDay> =
    listOf(8_200, 11_400, 6_050, 12_900, 9_100).mapIndexed { index, steps ->
        StepDay(today - 4 + index, steps = steps, burnedKcal = steps / 22)
    }

@PreviewLightDark
@Composable
private fun ActivityScreenPreview() {
    AppTheme {
        ActivityContent(
            stepDays = stepDaysPreview(todayEpochDay()),
            exerciseEntries = emptyList(),
            stepGoal = DEFAULT_STEP_GOAL,
            onSwitchSubject = {},
            onOpenRecap = {},
            onExitFlow = {},
        )
    }
}

/** Nothing imported yet — the page is still a real page, with the way on to its siblings. */
@PreviewLightDark
@Composable
private fun ActivityScreenEmptyPreview() {
    AppTheme {
        ActivityContent(
            stepDays = emptyList(),
            exerciseEntries = emptyList(),
            stepGoal = DEFAULT_STEP_GOAL,
            onSwitchSubject = {},
            onOpenRecap = {},
            onExitFlow = {},
        )
    }
}
