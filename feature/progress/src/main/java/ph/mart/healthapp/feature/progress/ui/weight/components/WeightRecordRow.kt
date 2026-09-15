package ph.mart.healthapp.feature.progress.ui.weight.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.NOTE_GOOGLE_HEALTH
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.progress.R

/**
 * One weigh-in as it was recorded — the date, whatever note came with it, and the figure in the
 * profile's own unit. `BloodPressureRow`'s shape without the delete button: the whole row opens the
 * log sheet on that date, and the sheet is where a record is changed or removed.
 */
@Composable
internal fun WeightRecordRow(
    entry: WeightEntry,
    unit: UnitSystem,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onTap,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatEpochDay(entry.dateEpochDay),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (entry.note.isNotBlank()) {
                    Text(
                        text = entry.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = stringResource(
                    R.string.progress_weight_value,
                    formatKg(entry.weightKg.kgToDisplayUnit(unit)),
                    unit.weightUnitLabel(),
                ),
                style = MaterialTheme.typography.titleMedium.tabularNums,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun WeightRecordRowPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                WeightRecordRow(
                    entry = WeightEntry(dateEpochDay = 20_700L, weightKg = 76.5),
                    unit = UnitSystem.Metric,
                    onTap = {},
                )
            }
        }
    }
}

/** With a note — the imported rows carry one, and so does anyone who types one. */
@PreviewLightDark
@Composable
private fun WeightRecordRowNotedPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                WeightRecordRow(
                    entry = WeightEntry(dateEpochDay = 20_699L, weightKg = 77.2, note = NOTE_GOOGLE_HEALTH),
                    unit = UnitSystem.Metric,
                    onTap = {},
                )
            }
        }
    }
}
