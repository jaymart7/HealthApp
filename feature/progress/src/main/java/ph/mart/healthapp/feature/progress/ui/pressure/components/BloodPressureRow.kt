package ph.mart.healthapp.feature.progress.ui.pressure.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureReading
import ph.mart.healthapp.core.data.bloodpressure.category
import ph.mart.healthapp.core.data.bloodpressure.formatBloodPressure
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/**
 * One reading, with its category named beneath it. Only [BloodPressureCategory.severe] is coloured
 * — `error` on a crisis and `onSurfaceVariant` on the other four, which is the trend-arrow rule
 * (`error` for genuinely off-track, never as one step of a scale).
 *
 * The delete asks first rather than raising an undo snackbar: the diary's swipe-and-undo needs a
 * snackbar host Progress doesn't have, and nothing else in this tab is swipeable.
 */
@Composable
internal fun BloodPressureRow(
    reading: BloodPressureReading,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val category = reading.category
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatBloodPressure(reading.systolic, reading.diastolic),
                    style = MaterialTheme.typography.titleMedium.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(category.label),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (category.severe) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatEpochDay(reading.dateEpochDay),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
                Text(
                    text = trailingLine(reading),
                    style = MaterialTheme.typography.bodySmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = AppIcons.Delete,
                    contentDescription = "Delete reading",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The time, plus the pulse when the cuff's figure was actually typed in — a `0` is "not entered",
 * so it prints nothing rather than "0 bpm". */
private fun trailingLine(reading: BloodPressureReading): String {
    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(reading.takenAtMillis))
    return if (reading.pulseBpm > 0) "$time · ${reading.pulseBpm} bpm" else time
}

@PreviewLightDark
@Composable
private fun BloodPressureRowPreview() {
    AppTheme {
        Surface {
            BloodPressureRow(
                reading = BloodPressureReading(
                    takenAtMillis = 1_756_600_000_000,
                    systolic = 128,
                    diastolic = 82,
                    pulseBpm = 71,
                ),
                onDelete = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** A crisis is the one category that colours itself, and the one the row has to get right. */
@PreviewLightDark
@Composable
private fun BloodPressureRowSeverePreview() {
    AppTheme {
        Surface {
            BloodPressureRow(
                reading = BloodPressureReading(
                    takenAtMillis = 1_756_600_000_000,
                    systolic = 185,
                    diastolic = 70,
                ),
                onDelete = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
