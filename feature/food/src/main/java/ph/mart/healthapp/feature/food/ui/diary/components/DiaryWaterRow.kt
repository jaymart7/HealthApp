package ph.mart.healthapp.feature.food.ui.diary.components

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
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.water.waterVolumeLabel
import ph.mart.healthapp.core.designsystem.component.WaterGlassRow
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/** The diary's own view of today's water — same count and the same shared row Home shows, so
 * logging a glass in either place lands in the other immediately. Deliberately card-less: it sits
 * in the diary's flow above the meal sections, not among Home's cards. */
@Composable
internal fun DiaryWaterRow(
    glasses: Int,
    goalGlasses: Int,
    unit: UnitSystem,
    onSetGlasses: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        ) {
            Text(
                text = "Water",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "$glasses / $goalGlasses · ${waterVolumeLabel(glasses, unit)}",
                style = MaterialTheme.typography.labelMedium.tabularNums,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        WaterGlassRow(glasses = glasses, goal = goalGlasses, onSetGlasses = onSetGlasses)
    }
}

@PreviewLightDark
@Composable
private fun DiaryWaterRowPreview() {
    AppTheme {
        Surface {
            DiaryWaterRow(
                glasses = 3,
                goalGlasses = 8,
                unit = UnitSystem.Metric,
                onSetGlasses = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
