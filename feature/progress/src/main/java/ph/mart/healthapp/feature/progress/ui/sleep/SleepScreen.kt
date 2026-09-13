package ph.mart.healthapp.feature.progress.ui.sleep

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
import ph.mart.healthapp.core.data.health.SleepNight
import ph.mart.healthapp.core.data.health.formatDuration
import ph.mart.healthapp.core.data.health.inRange
import ph.mart.healthapp.core.data.health.sleepAverages
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

/** The axis floor: a night is read against a full one, not against the best night in the window. */
private const val FULL_NIGHT_MINUTES = 480

/**
 * One subject's page, and the first of the thirteen to become a **route** rather than one of
 * `SubjectDetail`'s swap-ins — so it draws the full window with no bottom bar and no FAB over its
 * chart, Nav3 owns its back, and its container dies with its entry. See `DECISIONS.md` ->
 * **Progress, recap & the energy check-in**.
 *
 * Import-only: FitPulse cannot measure sleep, so every figure here came off a watch and the page
 * offers no way to type one in. That is also why it has no log sheet and no call to action on the
 * empty state — Progress reads.
 */
@Composable
internal fun SleepScreen(
    onSwitchSubject: (Subject) -> Unit,
    onOpenRecap: () -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SleepViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    SleepContent(
        nights = uiState.nights,
        cycleTrackingOn = uiState.cycleTrackingOn,
        onSwitchSubject = onSwitchSubject,
        onOpenRecap = onOpenRecap,
        onExitFlow = onExitFlow,
        modifier = modifier,
    )
}

@Composable
private fun SleepContent(
    nights: List<SleepNight>,
    cycleTrackingOn: Boolean,
    onSwitchSubject: (Subject) -> Unit,
    onOpenRecap: () -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    state: SleepState = rememberSleepState(),
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Its own bar rather than `AppScaffold`'s, the way the Photos page draws one — and with
            // **zero insets**, because this bar sits in the scaffold's *content*, which its
            // `innerPadding` has already cleared of the status bar. The default would apply that
            // inset a second time and leave a bar-height gap above the toolbar.
            AppTopBar(
                title = stringResource(Subject.Sleep.label),
                onBack = onExitFlow,
                windowInsets = WindowInsets(0),
                actions = {
                    // Always offered, unlike the header this page replaced, whose share was gated
                    // on there being a week worth reporting — a fold over every subject at once,
                    // which a page holding one series cannot see. `RecapScreen` folds its own and
                    // says so when there is nothing.
                    IconButton(onClick = onOpenRecap) {
                        Icon(
                            imageVector = AppIcons.Share,
                            contentDescription = stringResource(R.string.progress_recap),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
            if (nights.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        FullScreenState(
                            icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
                            heading = stringResource(R.string.progress_empty_sleep_heading),
                            body = stringResource(R.string.progress_empty_sleep_body),
                        )
                    }
                    SubjectSwitcher(
                        subject = Subject.Sleep,
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
                    SleepBody(nights = nights, state = state)
                    SubjectSwitcher(
                        subject = Subject.Sleep,
                        cycleTracking = cycleTrackingOn,
                        onSelect = onSwitchSubject,
                    )
                }
            }
        }
    }
}

/**
 * Windowed by date rather than sliced off the end, for the same reason the mood series is: nights
 * are sparse, so the chart needs the window's bounds to know where the gaps are.
 */
@Composable
private fun ColumnScope.SleepBody(nights: List<SleepNight>, state: SleepState) {
    val range = state.range
    val today = todayEpochDay()
    val from = today - (range.days ?: ChartRange.OneYear.days!!)
    val inWindow = nights.inRange(range, today)
    val averages = inWindow.sleepAverages()

    HeroValue(value = averages.averageMinutes?.let(::formatDuration) ?: stringResource(R.string.progress_none), caption = stringResource(R.string.progress_sleep_hero))
    FactChipRow(chips = listOf(FactChip(stringResource(R.string.progress_from_watch))))
    ChartCard(
        title = stringResource(R.string.progress_sleep_nights),
        range = range,
        onRangeChange = { state.range = it },
        legend = listOf(LegendEntry(stringResource(R.string.progress_sleep_asleep), MaterialTheme.colorScheme.primary)),
    ) {
        DayBarChart(
            bars = inWindow.map { DayBar(it.dateEpochDay, it.minutesAsleep) },
            fromEpochDay = from,
            toEpochDay = today,
            minAxisValue = FULL_NIGHT_MINUTES,
        )
    }
    StatRowsCard(
        rows = listOf(
            StatRow(stringResource(R.string.progress_sleep_average), averages.averageMinutes?.let(::formatDuration) ?: stringResource(R.string.progress_none)),
            StatRow(stringResource(R.string.progress_sleep_longest), averages.longestMinutes?.let(::formatDuration) ?: stringResource(R.string.progress_none)),
            StatRow(stringResource(R.string.progress_sleep_recorded), "${averages.nights}"),
        ),
    )
}

@PreviewLightDark
@Composable
private fun SleepScreenPreview() {
    val today = todayEpochDay()
    AppTheme {
        SleepContent(
            nights = listOf(432, 401, 512, 388, 447).mapIndexed { index, minutes ->
                SleepNight(today - 4 + index, minutes)
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
private fun SleepScreenEmptyPreview() {
    AppTheme {
        SleepContent(
            nights = emptyList(),
            cycleTrackingOn = true,
            onSwitchSubject = {},
            onOpenRecap = {},
            onExitFlow = {},
        )
    }
}
