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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureReading
import ph.mart.healthapp.core.data.bloodpressure.category
import ph.mart.healthapp.core.data.bloodpressure.formatBloodPressure
import ph.mart.healthapp.core.data.epochDayOf
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.home.R

/**
 * The **latest** reading, whenever it was taken — not today's.
 *
 * That is the deliberate departure from [HeartCard], [SleepCard] and [StepsCard]: those have a
 * watch filling them in every night, so a card that only ever shows today is always current.
 * Nobody takes their blood pressure daily, and a card that vanished on the six days between
 * readings would be a card nobody ever saw.
 *
 * Read-only, and it does not navigate. Logging needs the sheet, the sheet lives in
 * `:feature:progress`, and features never import each other — [WeightMetricCard] is the same shape.
 * The caller hides it entirely until the first reading exists, like [SupplementsCard] and for that
 * card's reason: nothing to import, nothing authored yet.
 */
@Composable
fun BloodPressureCard(reading: BloodPressureReading, todayEpochDay: Long, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.home_bp_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatBloodPressure(reading.systolic, reading.diastolic),
                    style = MaterialTheme.typography.headlineSmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(reading.category.label),
                    style = MaterialTheme.typography.bodySmall,
                    // The trend-arrow rule: `error` for genuinely off-track, never as one step of
                    // a five-colour scale.
                    color = if (reading.category.severe) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Text(
                text = takenLabel(todayEpochDay - reading.dateEpochDay),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
    }
}

/** Same phrasing the photo reminder uses, so the two cards don't count days differently. */
@Composable
private fun takenLabel(daysAgo: Long): String = when {
    daysAgo <= 0L -> stringResource(R.string.home_taken_today)
    daysAgo == 1L -> stringResource(R.string.home_taken_yesterday)
    else -> pluralStringResource(R.plurals.home_days_ago, daysAgo.toInt(), daysAgo)
}

@PreviewLightDark
@Composable
private fun BloodPressureCardPreview() {
    val taken = BloodPressureReading(takenAtMillis = 0, systolic = 128, diastolic = 82, pulseBpm = 71)
    AppTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                BloodPressureCard(reading = taken, todayEpochDay = epochDayOf(0))
                // A reading from last week, and a crisis: the two cases the card has to get right.
                BloodPressureCard(
                    reading = taken.copy(systolic = 185, diastolic = 70, pulseBpm = 0),
                    todayEpochDay = epochDayOf(0) + 6,
                )
            }
        }
    }
}
