package ph.mart.healthapp.feature.profile.ui.supplement.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.profile.R

/** Not copy — the multiplication sign after the count, the same one "3×8" uses on a routine. */
private const val TIMES = "×"

/**
 * How many times a day this one is taken, in the row's marker slot. Times-per-day used to be the
 * tail of a grey caption and invisible unless you read it; a supplement taken twice a day is
 * materially different from one taken once, and this is what makes a scan down the list show
 * which ones still owe a second dose.
 *
 * **Not a checkbox and not tappable.** A tick belongs to a day and Profile has none — Home's card
 * is where a dose is logged. The tile is a figure, which is why it prints "2×" rather than
 * carrying a state.
 *
 * `primaryContainer` at two or more, the quiet `surfaceContainerHighest` at one: the common case
 * gets no emphasis, so the emphasis means something when it appears. The figure is printed either
 * way, so nothing here rests on the fill alone.
 */
@Composable
internal fun FrequencyMarker(timesPerDay: Int, modifier: Modifier = Modifier) {
    val repeated = timesPerDay > 1
    // Resolved out here: a semantics lambda cannot read a resource.
    val spoken = stringResource(R.string.profile_supplements_times_a11y, timesPerDay)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (repeated) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
            )
            .clearAndSetSemantics { contentDescription = spoken },
    ) {
        Text(
            text = "$timesPerDay$TIMES",
            style = MaterialTheme.typography.titleMedium.tabularNums.copy(fontWeight = FontWeight.SemiBold),
            color = if (repeated) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@PreviewLightDark
@Composable
private fun FrequencyMarkerPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                FrequencyMarker(timesPerDay = 1)
                FrequencyMarker(timesPerDay = 2)
                FrequencyMarker(timesPerDay = 6)
            }
        }
    }
}
