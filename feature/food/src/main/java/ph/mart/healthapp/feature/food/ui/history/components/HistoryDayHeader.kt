package ph.mart.healthapp.feature.food.ui.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.formatDayMonth
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.history.relativeAgeLabel

/**
 * One day's heading: the date, how long ago it was, and what the day came to.
 *
 * All three, because each answers a question the others don't. The date is what the list is
 * scanned by and is deliberately absolute — a list spanning months is read by date, and two
 * relative labels among forty absolute ones are the two that have to be decoded. The chip is
 * the age at a glance, which is the thing an absolute date is worst at. The total is the day,
 * not the matches: see [FoodHistoryUiState.dayTotals][ph.mart.healthapp.feature.food.ui.history.FoodHistoryUiState.dayTotals].
 *
 * [totalKcal] is null while a search is in flight, and draws nothing rather than "0 kcal".
 *
 * This sticks to the top of the list as it scrolls, which is what [pinned] is for: on `surface`
 * it would leave the rows visible through it.
 */
@Composable
internal fun HistoryDayHeader(
    dateEpochDay: Long,
    today: Long,
    totalKcal: Int?,
    pinned: Boolean,
    modifier: Modifier = Modifier,
) {
    // Pinned draws one step up the tone ladder: on `surface` the rows would scroll visibly
    // through it.
    val background =
        if (pinned) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surface
    Column(modifier = modifier.fillMaxWidth().background(background)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 16.dp),
        ) {
            Text(
                text = formatDayMonth(dateEpochDay),
                style = MaterialTheme.typography.titleSmall.tabularNums.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            AgeChip(label = relativeAgeLabel(dateEpochDay, today))
            if (totalKcal != null) {
                Text(
                    text = stringResource(R.string.food_history_day_total, totalKcal),
                    style = MaterialTheme.typography.bodySmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun AgeChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun HistoryDayHeaderPreview() {
    AppTheme {
        Surface {
            Column {
                HistoryDayHeader(dateEpochDay = 20_000L, today = 20_001L, totalKcal = 1_806, pinned = false)
                HistoryDayHeader(dateEpochDay = 19_988L, today = 20_001L, totalKcal = 2_104, pinned = true)
            }
        }
    }
}
