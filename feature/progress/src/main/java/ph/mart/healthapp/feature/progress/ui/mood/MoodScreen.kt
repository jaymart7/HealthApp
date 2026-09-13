package ph.mart.healthapp.feature.progress.ui.mood

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
import ph.mart.healthapp.core.data.mood.MOOD_SCALE
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.mood.inRange
import ph.mart.healthapp.core.data.mood.moodAverages
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.AppTopBar
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.mood.components.MoodTrendChart
import ph.mart.healthapp.feature.progress.ui.progress.Subject
import ph.mart.healthapp.feature.progress.ui.progress.components.AskCoachAction
import ph.mart.healthapp.feature.progress.ui.progress.components.ChartCard
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChip
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChipRow
import ph.mart.healthapp.feature.progress.ui.progress.components.HeroValue
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRow
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRowsCard
import ph.mart.healthapp.feature.progress.ui.progress.components.SubjectSwitcher

/**
 * Mood and energy over the picked window — a route of its own, `SleepScreen`'s shape.
 *
 * The page reads: the two-tap reflection that fills this series is Home's `MoodCard`, so there is
 * no sheet here and no call to action on the empty state.
 */
@Composable
internal fun MoodScreen(
    onSwitchSubject: (Subject) -> Unit,
    onOpenRecap: () -> Unit,
    onAskCoach: (String) -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoodViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    MoodContent(
        days = uiState.days,
        cycleTrackingOn = uiState.cycleTrackingOn,
        onSwitchSubject = onSwitchSubject,
        onOpenRecap = onOpenRecap,
        onAskCoach = onAskCoach,
        onExitFlow = onExitFlow,
        modifier = modifier,
    )
}

@Composable
private fun MoodContent(
    days: List<MoodDay>,
    cycleTrackingOn: Boolean,
    onSwitchSubject: (Subject) -> Unit,
    onOpenRecap: () -> Unit,
    onAskCoach: (String) -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    state: MoodState = rememberMoodState(),
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Zero insets: this bar sits in the scaffold's content, which its `innerPadding` has
            // already cleared of the status bar. See `SleepScreen`.
            AppTopBar(
                title = stringResource(Subject.Mood.label),
                onBack = onExitFlow,
                windowInsets = WindowInsets(0),
                actions = {
                    AskCoachAction(subject = Subject.Mood, onAskCoach = onAskCoach)
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
                            icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
                            heading = stringResource(R.string.progress_empty_mood_heading),
                            body = stringResource(R.string.progress_empty_mood_body),
                        )
                    }
                    SubjectSwitcher(
                        subject = Subject.Mood,
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
                    MoodBody(days = days, state = state)
                    SubjectSwitcher(
                        subject = Subject.Mood,
                        cycleTracking = cycleTrackingOn,
                        onSelect = onSwitchSubject,
                    )
                }
            }
        }
    }
}

/**
 * Two series, sparse, placed by date — the window is handed to the chart rather than the list,
 * because the x-position of a bar is its day and a month with four entries has to show the gaps.
 *
 * A zero in either column means "not tapped", never a score of zero, so the averages keep separate
 * denominators: a mood-only week reports a mood average and a blank energy one. The chart draws its
 * own two-colour legend, which is why the card carries none.
 */
@Composable
private fun ColumnScope.MoodBody(days: List<MoodDay>, state: MoodState) {
    val range = state.range
    val today = todayEpochDay()
    val from = today - (range.days ?: ChartRange.OneYear.days!!)
    val inWindow = days.inRange(range, today)
    val averages = inWindow.moodAverages()

    HeroValue(
        value = averages.mood?.let { "%.1f".format(it) } ?: stringResource(R.string.progress_none),
        caption = stringResource(R.string.progress_mood_hero, MOOD_SCALE.last),
    )
    FactChipRow(chips = listOf(FactChip(stringResource(R.string.progress_mood_days, averages.daysLogged))))
    ChartCard(
        title = stringResource(R.string.progress_mood_title),
        range = range,
        onRangeChange = { state.range = it },
        legend = emptyList(),
    ) {
        MoodTrendChart(days = inWindow, fromEpochDay = from, toEpochDay = today)
    }
    StatRowsCard(
        rows = listOf(
            StatRow(stringResource(R.string.progress_mood_average), averages.mood?.let { "%.1f / ${MOOD_SCALE.last}".format(it) } ?: stringResource(R.string.progress_none)),
            StatRow(stringResource(R.string.progress_mood_energy_average), averages.energy?.let { "%.1f / ${MOOD_SCALE.last}".format(it) } ?: stringResource(R.string.progress_none)),
            StatRow(stringResource(R.string.progress_mood_days_logged), "${averages.daysLogged}"),
        ),
    )
}

@PreviewLightDark
@Composable
private fun MoodScreenPreview() {
    val today = todayEpochDay()
    AppTheme {
        MoodContent(
            days = listOf(4 to 3, 5 to 4, 3 to 2, 4 to 4).mapIndexed { index, (mood, energy) ->
                MoodDay(today - 3 + index, mood, energy)
            },
            cycleTrackingOn = true,
            onSwitchSubject = {},
            onOpenRecap = {},
            onAskCoach = {},
            onExitFlow = {},
        )
    }
}

/** Nothing tapped yet — the page is still a real page, with the way on to its siblings. */
@PreviewLightDark
@Composable
private fun MoodScreenEmptyPreview() {
    AppTheme {
        MoodContent(
            days = emptyList(),
            cycleTrackingOn = true,
            onSwitchSubject = {},
            onOpenRecap = {},
            onAskCoach = {},
            onExitFlow = {},
        )
    }
}
