package ph.mart.healthapp.feature.progress.ui.strength.components

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
import ph.mart.healthapp.core.data.exercise.LiftRecord
import ph.mart.healthapp.core.data.exercise.StrengthSet
import ph.mart.healthapp.core.data.exercise.formatLoad
import ph.mart.healthapp.core.data.exercise.loadLabel
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/**
 * One lift's best set, and the estimated one-rep max that ranked it.
 *
 * The estimate is shown beside the set rather than instead of it: it is what makes two sets
 * comparable, but the set is what was actually done, and a screen that printed only the estimate
 * would be reporting a lift nobody performed. A bodyweight record has no estimate, and prints none.
 */
@Composable
internal fun LiftRecordRow(record: LiftRecord, unit: UnitSystem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.exerciseName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${record.sets} ${if (record.sets == 1) "set" else "sets"} · " +
                    formatEpochDay(record.dateEpochDay),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = StrengthSet(record.exerciseName, record.bestReps, record.bestWeightKg).loadLabel(unit),
                style = MaterialTheme.typography.bodyMedium.tabularNums,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (record.bestOneRepMaxKg > 0) {
                Text(
                    text = "~${formatLoad(record.bestOneRepMaxKg.kgToDisplayUnit(unit))} " +
                        "${unit.weightUnitLabel()} 1RM",
                    style = MaterialTheme.typography.bodySmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun LiftRecordRowPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                LiftRecordRow(
                    record = LiftRecord("Squat", 102.5, 5, 119.6, 20_000, sets = 12),
                    unit = UnitSystem.Metric,
                )
                // Bodyweight: a real record with no estimate to print.
                LiftRecordRow(
                    record = LiftRecord("Pull-up", 0.0, 12, 0.0, 20_000, sets = 6),
                    unit = UnitSystem.Metric,
                )
            }
        }
    }
}
