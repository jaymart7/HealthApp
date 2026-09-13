package ph.mart.healthapp.feature.progress.ui.cycle.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.cycle.CyclePeriod
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R

/** One period, dated and measured. A period still running says so rather than reporting a length
 * it hasn't reached. */
@Composable
internal fun PeriodRow(period: CyclePeriod, today: Long, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (period.lengthDays == 1) {
                    formatEpochDay(period.startEpochDay)
                } else {
                    stringResource(R.string.progress_cycle_span, formatEpochDay(period.startEpochDay), formatEpochDay(period.endEpochDay))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (today in period) {
                    stringResource(R.string.progress_cycle_ongoing)
                } else {
                    stringResource(R.string.progress_cycle_length, period.lengthDays)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PeriodRowPreview() {
    val today = todayEpochDay()
    AppTheme {
        Surface {
            Row(modifier = Modifier.padding(16.dp)) {
                PeriodRow(period = CyclePeriod(startEpochDay = today - 4, endEpochDay = today - 1), today = today)
            }
        }
    }
}
