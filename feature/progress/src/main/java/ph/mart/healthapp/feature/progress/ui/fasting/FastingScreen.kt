package ph.mart.healthapp.feature.progress.ui.fasting

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
import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.fasting.dateEpochDay
import ph.mart.healthapp.core.data.fasting.durationMinutes
import ph.mart.healthapp.core.data.fasting.fastingAverages
import ph.mart.healthapp.core.data.fasting.inRange
import ph.mart.healthapp.core.data.health.formatDuration
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

/** A fast is read against the day it sits in, not against the longest one in the window. */
private const val FULL_DAY_MINUTES = 24 * 60

/** Completed fasts over the picked window — a route of its own, `SleepScreen`'s shape. Starting
 * and ending a fast is Home's card, so this page only reads. */
@Composable
internal fun FastingScreen(
    onSwitchSubject: (Subject) -> Unit,
    onOpenRecap: () -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FastingViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    FastingContent(
        sessions = uiState.sessions,
        goalHours = uiState.goalHours,
        onSwitchSubject = onSwitchSubject,
        onOpenRecap = onOpenRecap,
        onExitFlow = onExitFlow,
        modifier = modifier,
    )
}

@Composable
private fun FastingContent(
    sessions: List<FastSession>,
    goalHours: Int,
    onSwitchSubject: (Subject) -> Unit,
    onOpenRecap: () -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    state: FastingState = rememberFastingState(),
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Zero insets: this bar sits in the scaffold's content, which its `innerPadding` has
            // already cleared of the status bar. See `SleepScreen`.
            AppTopBar(
                title = stringResource(Subject.Fasting.label),
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
            if (sessions.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        FullScreenState(
                            icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
                            heading = stringResource(R.string.progress_empty_fasting_heading),
                            body = stringResource(R.string.progress_empty_fasting_body),
                        )
                    }
                    SubjectSwitcher(
                        subject = Subject.Fasting,
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
                    FastingBody(sessions = sessions, goalHours = goalHours, state = state)
                    SubjectSwitcher(subject = Subject.Fasting, onSelect = onSwitchSubject)
                }
            }
        }
    }
}

/**
 * Completed fasts only, each placed on the day it **ended** — a 16-hour fast started at 20:00 is
 * yesterday evening's discipline paying off at lunchtime.
 *
 * The dashed line is the profile's *current* target, while every bar was scored against the target
 * in force when it was logged. Raising the goal next month moves the line and prices the next fast;
 * it never un-hits one already drawn.
 */
@Composable
private fun ColumnScope.FastingBody(
    sessions: List<FastSession>,
    goalHours: Int,
    state: FastingState,
) {
    val range = state.range
    val today = todayEpochDay()
    val from = today - (range.days ?: ChartRange.OneYear.days!!)
    val inWindow = sessions.inRange(range, today)
    val averages = inWindow.fastingAverages()

    HeroValue(value = averages.averageMinutes?.let(::formatDuration) ?: stringResource(R.string.progress_none), caption = stringResource(R.string.progress_fasting_hero))
    FactChipRow(chips = listOf(FactChip(stringResource(R.string.progress_fasting_goal, goalHours))))
    ChartCard(
        title = stringResource(R.string.progress_fasting_title),
        range = range,
        onRangeChange = { state.range = it },
        legend = listOf(
            LegendEntry(stringResource(R.string.progress_fasting_legend), MaterialTheme.colorScheme.primary),
            LegendEntry(stringResource(R.string.progress_fasting_goal, goalHours), MaterialTheme.colorScheme.onSurfaceVariant, dashed = true),
        ),
    ) {
        DayBarChart(
            // Priced with nowMillis = 0: only completed fasts reach here, so no clock enters this.
            bars = inWindow.map { DayBar(it.dateEpochDay, it.durationMinutes(nowMillis = 0)) },
            fromEpochDay = from,
            toEpochDay = today,
            minAxisValue = FULL_DAY_MINUTES,
            goalValue = goalHours * 60,
        )
    }
    StatRowsCard(
        rows = listOf(
            StatRow(stringResource(R.string.progress_fasting_average), averages.averageMinutes?.let(::formatDuration) ?: stringResource(R.string.progress_none)),
            StatRow(stringResource(R.string.progress_fasting_longest), averages.longestMinutes?.let(::formatDuration) ?: stringResource(R.string.progress_none)),
            StatRow(stringResource(R.string.progress_fasting_goals_hit), stringResource(R.string.progress_of, averages.goalsHit, averages.count)),
        ),
    )
}

private fun sessionsPreview(today: Long): List<FastSession> {
    val dayMillis = 86_400_000L
    return listOf(16, 14, 18, 15).mapIndexed { index, hours ->
        val end = (today - 3 + index) * dayMillis
        FastSession(
            id = index.toLong(),
            startMillis = end - hours * 3_600_000L,
            endMillis = end,
            goalHours = 16,
        )
    }
}

@PreviewLightDark
@Composable
private fun FastingScreenPreview() {
    AppTheme {
        FastingContent(
            sessions = sessionsPreview(todayEpochDay()),
            goalHours = 16,
            onSwitchSubject = {},
            onOpenRecap = {},
            onExitFlow = {},
        )
    }
}

/** Nothing completed yet — the page is still a real page, with the way on to its siblings. */
@PreviewLightDark
@Composable
private fun FastingScreenEmptyPreview() {
    AppTheme {
        FastingContent(
            sessions = emptyList(),
            goalHours = 16,
            onSwitchSubject = {},
            onOpenRecap = {},
            onExitFlow = {},
        )
    }
}
