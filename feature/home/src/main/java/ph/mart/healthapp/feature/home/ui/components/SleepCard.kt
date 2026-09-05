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
import ph.mart.healthapp.core.data.health.SleepNight
import ph.mart.healthapp.core.data.health.formatDuration
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.home.R

/**
 * Last night, from Google Health. FitPulse cannot measure sleep, so this card has exactly one
 * source — which is why the caller hides it entirely when there is nothing rather than rendering a
 * zero, the same rule the weekly recap follows.
 *
 * There is **no sleep-stage bar**, and that is not an omission from the redesign: [SleepNight]
 * carries `minutesAsleep` and nothing else, because neither health leg imports stages. Drawing
 * Deep/REM/Light/Awake would mean a schema change plus new parsing on both legs, and colouring the
 * four segments would collide with the frozen Protein/Carbs/Fat semantics. One number is what the
 * app knows, so one number is what it says.
 */
@Composable
fun SleepCard(night: SleepNight, wide: Boolean, modifier: Modifier = Modifier) {
    MetricCard(
        label = stringResource(R.string.home_sleep_title),
        value = night.formatDuration(),
        wide = wide,
        modifier = modifier,
    ) {
        MetaText(text = stringResource(R.string.home_from_google_health))
    }
}

@PreviewLightDark
@Composable
private fun SleepCardPreview() {
    val night = SleepNight(dateEpochDay = 20_000, minutesAsleep = 432)
    AppTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                SleepCard(night = night, wide = true)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    SleepCard(night = night, wide = false, modifier = Modifier.weight(1f))
                    SleepCard(
                        night = night.copy(minutesAsleep = 318),
                        wide = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
