package ph.mart.healthapp.feature.progress.ui.mood.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.mood.MOOD_SCALE
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.mood.inRange
import ph.mart.healthapp.core.data.mood.moodAverages
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreenState
import ph.mart.healthapp.feature.progress.ui.progress.ProgressUiState
import ph.mart.healthapp.feature.progress.ui.progress.Subject
import ph.mart.healthapp.feature.progress.ui.progress.components.ChartCard
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChip
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChipRow
import ph.mart.healthapp.feature.progress.ui.progress.components.HeroValue
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRow
import ph.mart.healthapp.feature.progress.ui.progress.components.StatRowsCard

/**
 * Two series, sparse, placed by date — the window is handed to the chart rather than the list,
 * because the x-position of a bar is its day and a month with four entries has to show the gaps.
 *
 * A zero in either column means "not tapped", never a score of zero, so the averages keep separate
 * denominators: a mood-only week reports a mood average and a blank energy one. The chart draws its
 * own two-colour legend, which is why the card carries none.
 */
@Composable
internal fun ColumnScope.MoodDetailBody(uiState: ProgressUiState, state: ProgressScreenState) {
    val range = state.rangeFor(Subject.Mood)
    val today = todayEpochDay()
    val from = today - (range.days ?: ChartRange.OneYear.days!!)
    val days = uiState.moodDays.inRange(range, today)
    val averages = days.moodAverages()

    HeroValue(
        value = averages.mood?.let { "%.1f".format(it) } ?: stringResource(R.string.progress_none),
        caption = stringResource(R.string.progress_mood_hero, MOOD_SCALE.last),
    )
    FactChipRow(chips = listOf(FactChip(stringResource(R.string.progress_mood_days, averages.daysLogged))))
    ChartCard(
        title = stringResource(R.string.progress_mood_title),
        range = range,
        onRangeChange = { state.setRange(Subject.Mood, it) },
        legend = emptyList(),
    ) {
        MoodTrendChart(days = days, fromEpochDay = from, toEpochDay = today)
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
private fun MoodDetailPreview() {
    val today = todayEpochDay()
    AppTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MoodDetailBody(
                    uiState = ProgressUiState(
                        moodDays = listOf(4 to 3, 5 to 4, 3 to 2, 4 to 4).mapIndexed { index, (mood, energy) ->
                            MoodDay(today - 3 + index, mood, energy)
                        },
                    ),
                    state = ProgressScreenState(),
                )
            }
        }
    }
}
