package ph.mart.healthapp.feature.progress.ui.heart

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
import ph.mart.healthapp.core.data.health.HeartDay
import ph.mart.healthapp.core.data.health.heartAverages
import ph.mart.healthapp.core.data.health.inRange
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.AppTopBar
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.heart.components.HeartTrendChart
import ph.mart.healthapp.feature.progress.ui.progress.Subject
import ph.mart.healthapp.feature.progress.ui.progress.components.ChartCard
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChip
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChipRow
import ph.mart.healthapp.feature.progress.ui.progress.components.HeroValue
import ph.mart.healthapp.feature.progress.ui.progress.components.LegendEntry
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRow
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRowsCard
import ph.mart.healthapp.feature.progress.ui.progress.components.SubjectSwitcher

/** Imported heart-rate days, charted — a route of its own, `SleepScreen`'s shape. Import-only,
 * so there is no sheet and no call to action on the empty state. */
@Composable
internal fun HeartScreen(
    onSwitchSubject: (Subject) -> Unit,
    onOpenRecap: () -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HeartViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    HeartContent(
        days = uiState.days,
        cycleTrackingOn = uiState.cycleTrackingOn,
        onSwitchSubject = onSwitchSubject,
        onOpenRecap = onOpenRecap,
        onExitFlow = onExitFlow,
        modifier = modifier,
    )
}

@Composable
private fun HeartContent(
    days: List<HeartDay>,
    cycleTrackingOn: Boolean,
    onSwitchSubject: (Subject) -> Unit,
    onOpenRecap: () -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    state: HeartState = rememberHeartState(),
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Zero insets: this bar sits in the scaffold's content, which its `innerPadding` has
            // already cleared of the status bar. See `SleepScreen`.
            AppTopBar(
                title = stringResource(Subject.Heart.label),
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
            if (days.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        FullScreenState(
                            icon = { MascotAvatar(state = MascotState.Idle, size = 64.dp) },
                            heading = stringResource(R.string.progress_empty_heart_heading),
                            body = stringResource(R.string.progress_empty_heart_body),
                        )
                    }
                    SubjectSwitcher(
                        subject = Subject.Heart,
                        cycleTracking = cycleTrackingOn,
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
                    HeartBody(days = days, state = state)
                    SubjectSwitcher(
                        subject = Subject.Heart,
                        cycleTracking = cycleTrackingOn,
                        onSelect = onSwitchSubject,
                    )
                }
            }
        }
    }
}

/**
 * The one chart in the app whose bars are not zero-based — nobody's heart visits 0–45 bpm, so a
 * zero-based axis would squash the beats that actually differ. Each bar spans that day's lowest
 * reading up to its average, which is what the legend names.
 *
 * "Lowest" is never called a resting rate: FitPulse aggregates whatever samples the watch happened
 * to take, and calling a minimum "resting" would claim a measurement nobody made.
 */
@Composable
private fun ColumnScope.HeartBody(days: List<HeartDay>, state: HeartState) {
    val range = state.range
    val today = todayEpochDay()
    val from = today - (range.days ?: ChartRange.OneYear.days!!)
    val inWindow = days.inRange(range, today)
    val averages = inWindow.heartAverages()

    HeroValue(value = averages.averageBpm?.toString() ?: stringResource(R.string.progress_none), caption = stringResource(R.string.progress_heart_hero))
    FactChipRow(chips = listOf(FactChip(stringResource(R.string.progress_from_watch))))
    ChartCard(
        title = stringResource(R.string.progress_heart_range),
        range = range,
        onRangeChange = { state.range = it },
        legend = listOf(LegendEntry(stringResource(R.string.progress_heart_legend), MaterialTheme.colorScheme.secondary)),
    ) {
        HeartTrendChart(days = inWindow, fromEpochDay = from, toEpochDay = today)
    }
    StatRowsCard(
        rows = listOf(
            StatRow(stringResource(R.string.progress_heart_average), averages.averageBpm?.let { stringResource(R.string.progress_heart_bpm, it) } ?: stringResource(R.string.progress_none)),
            StatRow(stringResource(R.string.progress_heart_lowest), averages.lowestBpm?.let { stringResource(R.string.progress_heart_bpm, it) } ?: stringResource(R.string.progress_none)),
            StatRow(stringResource(R.string.progress_heart_days), "${averages.days}"),
        ),
    )
}

@PreviewLightDark
@Composable
private fun HeartScreenPreview() {
    val today = todayEpochDay()
    AppTheme {
        HeartContent(
            days = listOf(68 to 52, 71 to 55, 66 to 49, 74 to 58).mapIndexed { index, (average, low) ->
                HeartDay(today - 3 + index, averageBpm = average, minBpm = low)
            },
            cycleTrackingOn = true,
            onSwitchSubject = {},
            onOpenRecap = {},
            onExitFlow = {},
        )
    }
}

/** Nothing imported yet — the page is still a real page, with the way on to its siblings. */
@PreviewLightDark
@Composable
private fun HeartScreenEmptyPreview() {
    AppTheme {
        HeartContent(
            days = emptyList(),
            cycleTrackingOn = true,
            onSwitchSubject = {},
            onOpenRecap = {},
            onExitFlow = {},
        )
    }
}
