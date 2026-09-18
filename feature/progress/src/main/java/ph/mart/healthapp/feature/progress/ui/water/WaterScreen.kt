package ph.mart.healthapp.feature.progress.ui.water

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
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.data.water.WaterDay
import ph.mart.healthapp.core.data.water.inRange
import ph.mart.healthapp.core.data.water.waterAverages
import ph.mart.healthapp.core.data.water.waterVolumeLabel
import ph.mart.healthapp.core.designsystem.component.AppTopBar
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.progress.Subject
import ph.mart.healthapp.feature.progress.ui.progress.components.AskCoachAction
import ph.mart.healthapp.feature.progress.ui.progress.components.ChartCard
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChip
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChipRow
import ph.mart.healthapp.feature.progress.ui.progress.components.HeroValue
import ph.mart.healthapp.feature.progress.ui.progress.components.LegendEntry
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRow
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRowsCard
import ph.mart.healthapp.feature.progress.ui.shared.components.DayBar
import ph.mart.healthapp.feature.progress.ui.shared.components.DayBarChart

/** Glasses a day over the picked window — a route of its own, `FastingScreen`'s shape. Logging a
 * glass is Home's card and the diary's row, so this page only reads. */
@Composable
internal fun WaterScreen(
    onOpenRecap: () -> Unit,
    onAskCoach: (question: String, source: String) -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WaterViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    WaterContent(
        days = uiState.days,
        goalGlasses = uiState.goalGlasses,
        unit = uiState.unit,
        onOpenRecap = onOpenRecap,
        onAskCoach = onAskCoach,
        onExitFlow = onExitFlow,
        modifier = modifier,
    )
}

@Composable
private fun WaterContent(
    days: List<WaterDay>,
    goalGlasses: Int,
    unit: UnitSystem,
    onOpenRecap: () -> Unit,
    onAskCoach: (question: String, source: String) -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    state: WaterState = rememberWaterState(),
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Zero insets: this bar sits in the scaffold's content, which its `innerPadding` has
            // already cleared of the status bar. See `FastingScreen`.
            AppTopBar(
                title = stringResource(Subject.Water.label),
                onBack = onExitFlow,
                windowInsets = WindowInsets(0),
                actions = {
                    AskCoachAction(subject = Subject.Water, onAskCoach = onAskCoach)
                    IconButton(onClick = onOpenRecap) {
                        Icon(
                            imageVector = AppIcons.Share,
                            contentDescription = stringResource(R.string.progress_recap),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
            if (days.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    FullScreenState(
                        icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
                        heading = stringResource(R.string.progress_empty_water_heading),
                        body = stringResource(R.string.progress_empty_water_body),
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
                    WaterBody(days = days, goalGlasses = goalGlasses, unit = unit, state = state)
                }
            }
        }
    }
}

/**
 * One bar per day that had a glass on it. A day with no row is drawn as a gap rather than a zero —
 * the series is sparse, the reading `waterAverages()` takes: an untracked day is not a day someone
 * drank nothing.
 *
 * The dashed line is the profile's *current* goal, and the bars are the counts that were logged.
 * Raising the goal moves the line and prices tomorrow; it never un-hits a day already drawn, which
 * is the Fasting page's rule for the same reason.
 */
@Composable
private fun ColumnScope.WaterBody(
    days: List<WaterDay>,
    goalGlasses: Int,
    unit: UnitSystem,
    state: WaterState,
) {
    val range = state.range
    val today = todayEpochDay()
    val from = today - range.days
    val inWindow = days.inRange(range, today)
    val averages = inWindow.waterAverages(goalGlasses)

    HeroValue(
        value = averages.averageGlasses?.let { "%.1f".format(it) } ?: stringResource(R.string.progress_none),
        caption = stringResource(R.string.progress_water_hero),
    )
    FactChipRow(
        chips = listOf(
            FactChip(stringResource(R.string.progress_water_goal, goalGlasses, waterVolumeLabel(goalGlasses, unit))),
        ),
    )
    ChartCard(
        title = stringResource(R.string.progress_water_title),
        range = range,
        onRangeChange = { state.range = it },
        legend = listOf(
            LegendEntry(stringResource(R.string.progress_water_legend), MaterialTheme.colorScheme.primary),
            LegendEntry(stringResource(R.string.progress_water_goal_legend), MaterialTheme.colorScheme.onSurfaceVariant, dashed = true),
        ),
    ) {
        DayBarChart(
            bars = inWindow.map { DayBar(it.dateEpochDay, it.glasses) },
            fromEpochDay = from,
            toEpochDay = today,
            // The goal is the scale's floor, so a short day draws short rather than filling the
            // card on its own — the reading Fasting's `FULL_DAY_MINUTES` gives that chart.
            minAxisValue = goalGlasses,
            goalValue = goalGlasses,
        )
    }
    StatRowsCard(
        rows = listOf(
            StatRow(
                stringResource(R.string.progress_water_average),
                averages.averageGlasses?.let { "%.1f".format(it) } ?: stringResource(R.string.progress_none),
            ),
            StatRow(
                stringResource(R.string.progress_water_best),
                averages.bestGlasses?.let { stringResource(R.string.progress_water_glasses, it) }
                    ?: stringResource(R.string.progress_none),
            ),
            StatRow(
                stringResource(R.string.progress_water_goals_hit),
                stringResource(R.string.progress_of, averages.daysHitGoal, averages.daysLogged),
            ),
        ),
    )
}

private fun daysPreview(today: Long): List<WaterDay> =
    listOf(8, 6, 9, 5, 8, 7, 10).mapIndexed { index, glasses ->
        WaterDay(dateEpochDay = today - 6 + index, glasses = glasses)
    }

@PreviewLightDark
@Composable
private fun WaterScreenPreview() {
    AppTheme {
        WaterContent(
            days = daysPreview(todayEpochDay()),
            goalGlasses = 8,
            unit = UnitSystem.Metric,
            onOpenRecap = {},
            onAskCoach = { _, _ -> },
            onExitFlow = {},
        )
    }
}

/** Nothing logged yet — the page is still a real page, with the way on to its siblings. */
@PreviewLightDark
@Composable
private fun WaterScreenEmptyPreview() {
    AppTheme {
        WaterContent(
            days = emptyList(),
            goalGlasses = 8,
            unit = UnitSystem.Metric,
            onOpenRecap = {},
            onAskCoach = { _, _ -> },
            onExitFlow = {},
        )
    }
}
