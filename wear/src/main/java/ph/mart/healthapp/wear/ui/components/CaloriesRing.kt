package ph.mart.healthapp.wear.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.Text
import ph.mart.healthapp.core.today.TodaySnapshot
import ph.mart.healthapp.core.today.progress
import ph.mart.healthapp.core.today.remainingKcal
import ph.mart.healthapp.wear.R

private val RING_SIZE = 132.dp

/**
 * Home's calorie ring, wrist-sized — the one place the watch draws an arc rather than the bar the
 * widget settles for, because Glance cannot draw arcs and Wear can.
 *
 * The figure inside is what's *left*, not what's eaten: a glance answers "can I have this", and
 * the consumed total is the smaller line under it. Over budget is stated plainly and in
 * `onSurfaceVariant`, never `error` — a day over target is not a failure state, the rule the
 * widget and Home both follow.
 */
@Composable
internal fun CaloriesRing(snapshot: TodaySnapshot, modifier: Modifier = Modifier) {
    val remaining = snapshot.remainingKcal
    Box(modifier = modifier.size(RING_SIZE), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { snapshot.progress },
            modifier = Modifier.size(RING_SIZE),
            colors = ProgressIndicatorDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${if (remaining >= 0) remaining else -remaining}",
                style = MaterialTheme.typography.numeralMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(if (remaining >= 0) R.string.wear_kcal_left else R.string.wear_kcal_over),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "${snapshot.consumedKcal} / ${snapshot.budgetKcal}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
