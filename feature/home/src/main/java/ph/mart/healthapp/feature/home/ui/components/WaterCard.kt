package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.water.waterVolumeLabel
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.WaterGlassRow
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/** Today's hydration. [goalGlasses] comes from the profile row, same as every other target on
 * this screen — never a constant read here. */
@Composable
fun WaterCard(
    glasses: Int,
    goalGlasses: Int,
    unit: UnitSystem,
    onSetGlasses: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        ) {
            Text(
                text = "Water",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "$glasses / $goalGlasses · ${waterVolumeLabel(glasses, unit)}",
                style = MaterialTheme.typography.titleSmall.tabularNums,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        WaterGlassRow(glasses = glasses, goal = goalGlasses, onSetGlasses = onSetGlasses)
    }
}

@PreviewLightDark
@Composable
private fun WaterCardPreview() {
    AppTheme {
        Surface {
            WaterCard(
                glasses = 5,
                goalGlasses = 8,
                unit = UnitSystem.Metric,
                onSetGlasses = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
