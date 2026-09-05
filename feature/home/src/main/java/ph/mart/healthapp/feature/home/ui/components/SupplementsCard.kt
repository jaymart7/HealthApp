package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.core.data.supplement.SupplementToday
import ph.mart.healthapp.core.data.supplement.completedCount
import ph.mart.healthapp.core.data.supplement.nextTaken
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.home.R

/** Tighter than the card's own 20dp: a row inside a card, not a card inside a card. */
private val ROW_RADIUS = 12.dp

/**
 * Today's supplements, as a checklist. Renders nothing when the list is empty — the same choice
 * the sleep, steps and heart cards make, and for the same reason: an empty card is not an
 * invitation, and Profile → Supplements is where the list is authored.
 *
 * One tap advances a row by a dose and wraps back to zero at its target, so a once-daily row
 * behaves as a checkbox and a twice-daily one steps 0–1–2–0 with the identical gesture — and a
 * mis-tap is corrected by the same gesture that made it, like [MoodCard]'s rows.
 */
@Composable
fun SupplementsCard(
    supplements: List<SupplementToday>,
    onSetTaken: (Long, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (supplements.isEmpty()) return
    AppCard(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
        ) {
            Text(
                text = stringResource(R.string.home_supplements_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.home_supplements_count, supplements.completedCount, supplements.size),
                style = MaterialTheme.typography.titleSmall.tabularNums,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        supplements.forEach { item ->
            SupplementRow(
                item = item,
                onTap = {
                    onSetTaken(item.supplement.id, nextTaken(item.taken, item.supplement.timesPerDay))
                },
            )
        }
    }
}

@Composable
private fun SupplementRow(item: SupplementToday, onTap: () -> Unit) {
    val tint = if (item.isComplete) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    // Resolved outside the semantics lambda, which is not a composable scope.
    val description = describe(item)
    // The whole row is the target, not just the icon: a checklist read left-to-right should be
    // tappable where the eye already is.
    Surface(
        onClick = onTap,
        shape = RoundedCornerShape(ROW_RADIUS),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TapTargetMin)
            .clearAndSetSemantics {
                contentDescription = description
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Icon(
                imageVector = if (item.isComplete) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = item.supplement.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (item.supplement.dose.isNotBlank()) {
                    Text(
                        text = item.supplement.dose,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // The count is only worth printing when a row can hold more than one dose — "1 / 1"
            // says nothing the tick hasn't already said.
            if (item.supplement.timesPerDay > 1) {
                Text(
                    text = stringResource(R.string.home_supplements_taken_of, item.taken, item.supplement.timesPerDay),
                    style = MaterialTheme.typography.labelLarge.tabularNums,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun describe(item: SupplementToday): String = with(item.supplement) {
    // The name and its dose are the user's own text, so they are interpolated into the sentence
    // rather than being three separate slots a translator would have to reassemble.
    val named = if (dose.isBlank()) name else "$name, $dose"
    if (timesPerDay > 1) {
        stringResource(R.string.home_supplements_desc_partial, named, item.taken, timesPerDay)
    } else if (item.isComplete) {
        stringResource(R.string.home_supplements_desc_done, named)
    } else {
        stringResource(R.string.home_supplements_desc_todo, named)
    }
}

@PreviewLightDark
@Composable
private fun SupplementsCardPreview() {
    AppTheme {
        Surface {
            SupplementsCard(
                supplements = listOf(
                    SupplementToday(Supplement(id = 1, name = "Vitamin D", dose = "2000 IU"), taken = 1),
                    SupplementToday(
                        Supplement(id = 2, name = "Creatine", dose = "5 g", timesPerDay = 2),
                        taken = 1,
                    ),
                    SupplementToday(Supplement(id = 3, name = "Magnesium"), taken = 0),
                ),
                onSetTaken = { _, _ -> },
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
