package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.health.HeartDay
import ph.mart.healthapp.core.data.health.formatBpm
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/**
 * Today's heart rate, from Google Health. Same rule as [SleepCard] and [StepsCard]: one source, so
 * the caller hides the card entirely rather than rendering a zero for a user who never connected.
 *
 * The second line says "Lowest", not "Resting". FitPulse aggregates whatever samples the watch
 * happened to take, and a minimum is a minimum — calling it a resting heart rate would claim a
 * measurement nobody made.
 */
@Composable
fun HeartCard(heart: HeartDay, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Heart rate today",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatBpm(heart.averageBpm),
                    style = MaterialTheme.typography.headlineSmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (heart.minBpm > 0) {
                    Text(
                        text = "Lowest ${formatBpm(heart.minBpm)}",
                        style = MaterialTheme.typography.bodySmall.tabularNums,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = "From Google Health",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun HeartCardPreview() {
    AppTheme {
        Surface {
            HeartCard(
                heart = HeartDay(dateEpochDay = 20_000, averageBpm = 68, minBpm = 52),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
