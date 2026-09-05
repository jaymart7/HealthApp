package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.health.HeartDay
import ph.mart.healthapp.core.data.health.formatBpm
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.home.R

/**
 * Today's heart rate, from Google Health. Same rule as [SleepCard] and [StepsCard]: one source, so
 * the caller hides the card entirely rather than rendering a zero for a user who never connected.
 *
 * The sub-line says "Lowest", not "Resting". FitPulse aggregates whatever samples the watch
 * happened to take, and a minimum is a minimum — calling it a resting heart rate would claim a
 * measurement nobody made. No status mark either: the app has no target heart rate, so it has no
 * verdict to give.
 */
@Composable
fun HeartCard(heart: HeartDay, wide: Boolean, modifier: Modifier = Modifier) {
    MetricCard(
        label = stringResource(R.string.home_heart_title),
        value = "${heart.averageBpm}",
        unit = " " + stringResource(R.string.home_heart_bpm),
        wide = wide,
        modifier = modifier,
    ) {
        MetaText(
            text = stringResource(R.string.home_heart_average),
            sub = if (heart.minBpm > 0) {
                stringResource(R.string.home_heart_lowest, formatBpm(heart.minBpm))
            } else {
                null
            },
        )
    }
}

@PreviewLightDark
@Composable
private fun HeartCardPreview() {
    val day = HeartDay(dateEpochDay = 20_000, averageBpm = 68, minBpm = 52)
    AppTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                HeartCard(heart = day, wide = true)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    HeartCard(heart = day, wide = false, modifier = Modifier.weight(1f))
                    // A day the watch sampled without ever recording a low.
                    HeartCard(heart = day.copy(minBpm = 0), wide = false, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
