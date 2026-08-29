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
import ph.mart.healthapp.core.data.health.SleepNight
import ph.mart.healthapp.core.data.health.formatDuration
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/**
 * Last night, from Google Health. FitPulse cannot measure sleep, so this card has exactly one
 * source — which is why the caller hides it entirely when there is nothing rather than rendering
 * a zero, the same rule the weekly recap follows.
 */
@Composable
fun SleepCard(night: SleepNight, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Last night",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = night.formatDuration(),
                    style = MaterialTheme.typography.headlineSmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                )
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
private fun SleepCardPreview() {
    AppTheme {
        Surface {
            SleepCard(
                night = SleepNight(dateEpochDay = 20_000, minutesAsleep = 432),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
