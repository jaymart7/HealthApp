package ph.mart.healthapp.feature.progress.ui.supplement

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
import kotlin.math.roundToInt
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.supplement.SUPPLEMENT_BACKFILL_DAYS
import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.core.data.supplement.SupplementDay
import ph.mart.healthapp.core.data.supplement.adherenceByDay
import ph.mart.healthapp.core.data.supplement.averageAdherence
import ph.mart.healthapp.core.data.supplement.inRange
import ph.mart.healthapp.core.data.supplement.supplementsOn
import ph.mart.healthapp.core.data.todayEpochDay
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
import ph.mart.healthapp.feature.progress.ui.supplement.components.SupplementAdherenceChart
import ph.mart.healthapp.feature.progress.ui.supplement.components.SupplementCatchUpCard

/**
 * Adherence over the picked window — a route of its own, `SleepScreen`'s shape. The authoring is
 * still Profile's list and today is still Home's card; what this page adds is the one thing
 * neither can reach, a day that has already gone by and was never ticked.
 */
@Composable
internal fun SupplementsScreen(
    onOpenRecap: () -> Unit,
    onAskCoach: (question: String, source: String) -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SupplementsViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    SupplementsContent(
        days = uiState.days,
        supplements = uiState.supplements,
        onSetTaken = viewModel::setTaken,
        onOpenRecap = onOpenRecap,
        onAskCoach = onAskCoach,
        onExitFlow = onExitFlow,
        modifier = modifier,
    )
}

@Composable
private fun SupplementsContent(
    days: List<SupplementDay>,
    supplements: List<Supplement>,
    onSetTaken: (date: Long, supplementId: Long, taken: Int) -> Unit,
    onOpenRecap: () -> Unit,
    onAskCoach: (question: String, source: String) -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    state: SupplementsState = rememberSupplementsState(),
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Zero insets: this bar sits in the scaffold's content, which its `innerPadding` has
            // already cleared of the status bar. See `SleepScreen`.
            AppTopBar(
                title = stringResource(Subject.Supplements.label),
                onBack = onExitFlow,
                windowInsets = WindowInsets(0),
                actions = {
                    AskCoachAction(subject = Subject.Supplements, onAskCoach = onAskCoach)
                    IconButton(onClick = onOpenRecap) {
                        Icon(
                            imageVector = AppIcons.Share,
                            contentDescription = stringResource(R.string.progress_recap),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
            // The empty state needs *both* halves empty now. Someone who added their list on
            // Monday and forgot to tick all week has no days at all, and that is exactly the
            // person the catch-up card is for — hiding it behind "nothing ticked yet" would put
            // the fix behind the problem.
            if (days.isEmpty() && supplements.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    FullScreenState(
                        icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
                        heading = stringResource(R.string.progress_empty_supplements_heading),
                        body = stringResource(R.string.progress_empty_supplements_body),
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
                    SupplementsBody(
                        days = days,
                        supplements = supplements,
                        onSetTaken = onSetTaken,
                        state = state,
                    )
                }
            }
        }
    }
}

/**
 * Adherence, priced off each day's own snapshotted target — dropping a supplement from twice daily
 * to once next month must not turn a past day that read "2 of 2" into "2 of 1".
 *
 * A day with rows and nothing ticked is a slot with no height; a day with no rows at all draws
 * nothing. Seen-and-missed and never-tracked are different facts, and the chart says which.
 *
 * The trend comes first and the catch-up sits under it: this page's subject is the run of days,
 * and correcting one of them is what you came back for, not what you came for. With no days at
 * all there is no trend to draw — a hero reading "—" over an empty canvas is noise — so the
 * checklist is the whole page until the first tick lands.
 */
@Composable
private fun ColumnScope.SupplementsBody(
    days: List<SupplementDay>,
    supplements: List<Supplement>,
    onSetTaken: (date: Long, supplementId: Long, taken: Int) -> Unit,
    state: SupplementsState,
) {
    val range = state.range
    val today = todayEpochDay()
    val from = today - range.days
    val inWindow = days.inRange(range, today)
    val byDay = inWindow.adherenceByDay()
    val average = inWindow.averageAdherence()

    if (days.isNotEmpty()) {
        HeroValue(value = average?.let { "${(it * 100).roundToInt()}" } ?: stringResource(R.string.progress_none), caption = stringResource(R.string.progress_supplements_hero))
        FactChipRow(chips = listOf(FactChip(stringResource(R.string.progress_supplements_days, byDay.size))))
        ChartCard(
            title = stringResource(R.string.progress_supplements_adherence),
            range = range,
            onRangeChange = { state.range = it },
            legend = listOf(LegendEntry(stringResource(R.string.progress_supplements_legend), MaterialTheme.colorScheme.primary)),
        ) {
            SupplementAdherenceChart(days = inWindow, fromEpochDay = from, toEpochDay = today)
        }
    }
    // Clamped on read as well as on write: the state survives a process death, and a saved day
    // restored after midnight would otherwise sit one step into the future.
    val earliest = today - SUPPLEMENT_BACKFILL_DAYS
    val selected = state.selectedDate.coerceIn(earliest, today)
    SupplementCatchUpCard(
        selectedDate = selected,
        today = today,
        earliestDate = earliest,
        rows = supplementsOn(date = selected, supplements = supplements, days = days),
        onSelectDate = { state.selectedDate = it.coerceIn(earliest, today) },
        onSetTaken = { id, taken -> onSetTaken(selected, id, taken) },
    )
    if (days.isNotEmpty()) {
        StatRowsCard(
            rows = listOf(
                StatRow(stringResource(R.string.progress_supplements_average), average?.let { "${(it * 100).roundToInt()}%" } ?: "—"),
                StatRow(stringResource(R.string.progress_supplements_full_days), "${byDay.count { it.second >= 1f }}"),
                StatRow(stringResource(R.string.progress_supplements_days_logged), "${byDay.size}"),
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun SupplementsScreenPreview() {
    val today = todayEpochDay()
    AppTheme {
        SupplementsContent(
            days = listOf(2 to 2, 1 to 2, 2 to 2, 0 to 2, 2 to 2).mapIndexed { index, (taken, due) ->
                SupplementDay(today - 4 + index, supplementId = 1, taken = taken, dueTimes = due)
            },
            supplements = listOf(Supplement(id = 1, name = "Creatine", dose = "5 g", timesPerDay = 2)),
            onSetTaken = { _, _, _ -> },
            onOpenRecap = {},
            onAskCoach = { _, _ -> },
            onExitFlow = {},
        )
    }
}

/** A list with nothing ticked against it yet — no trend to draw, and the catch-up card is the
 * whole page. */
@PreviewLightDark
@Composable
private fun SupplementsScreenCatchUpOnlyPreview() {
    AppTheme {
        SupplementsContent(
            days = emptyList(),
            supplements = listOf(
                Supplement(id = 1, name = "Vitamin D", dose = "2000 IU"),
                Supplement(id = 2, name = "Magnesium"),
            ),
            onSetTaken = { _, _, _ -> },
            onOpenRecap = {},
            onAskCoach = { _, _ -> },
            onExitFlow = {},
        )
    }
}

/** Nothing due yet — the page is still a real page, with the way on to its siblings. */
@PreviewLightDark
@Composable
private fun SupplementsScreenEmptyPreview() {
    AppTheme {
        SupplementsContent(
            days = emptyList(),
            supplements = emptyList(),
            onSetTaken = { _, _, _ -> },
            onOpenRecap = {},
            onAskCoach = { _, _ -> },
            onExitFlow = {},
        )
    }
}
