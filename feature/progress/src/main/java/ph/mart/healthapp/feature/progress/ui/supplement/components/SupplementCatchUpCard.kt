package ph.mart.healthapp.feature.progress.ui.supplement.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.core.data.supplement.SupplementOnDay
import ph.mart.healthapp.core.data.supplement.nextTaken
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.progress.R

/** Tighter than the card's own 20dp: a row inside a card, not a card inside a card — the figure
 * `SupplementsCard` uses for the same rows on Home. */
private val ROW_RADIUS = 12.dp

/** The floor, not the height: at a large font scale a row's name needs more than 48dp and
 * clipping it would be worse than a taller row. */
private val TapTargetMin = 48.dp

/** "Today" / "Yesterday" / "Aug 27, 2026". Pure, so the two relative cases are testable, and
 * `SupplementCatchUpTest` asserts both words — the reading `diaryDateLabel` and
 * `goalProjectionLine()` got, and what keeps these two in Kotlin. The diary's copy is
 * `:feature:food`-internal and a feature never imports another feature's types. */
internal fun catchUpDateLabel(epochDay: Long, today: Long): String = when (epochDay) {
    today -> "Today"
    today - 1 -> "Yesterday"
    else -> formatEpochDay(epochDay)
}

/**
 * The day you forgot, and the checklist for it.
 *
 * The one writing control on a Progress subject page, and it earns the exception: today belongs
 * to Home's card and the list itself to Profile, so a Tuesday nobody ticked has no other surface
 * that can reach it. One tap advances a row by a dose and wraps back to zero at **that day's** own
 * target, so the gesture is the card's on Home and the mis-tap is corrected by the gesture that
 * made it.
 *
 * Both chevrons stay **present and disabled** at their edge rather than disappearing —
 * `DiaryDateHeader`'s rule: a control that vanishes at the edge of its range teaches nothing about
 * where the edge is.
 */
@Composable
internal fun SupplementCatchUpCard(
    selectedDate: Long,
    today: Long,
    earliestDate: Long,
    rows: List<SupplementOnDay>,
    onSelectDate: (Long) -> Unit,
    onSetTaken: (supplementId: Long, taken: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.progress_supplements_catch_up),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DateStepper(
            selectedDate = selectedDate,
            today = today,
            earliestDate = earliestDate,
            onSelectDate = onSelectDate,
        )
        if (rows.isEmpty()) {
            Text(
                text = stringResource(R.string.progress_supplements_nothing_due),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            rows.forEach { row ->
                CatchUpRow(
                    row = row,
                    onTap = { onSetTaken(row.supplement.id, nextTaken(row.taken, row.dueTimes)) },
                )
            }
        }
    }
}

@Composable
private fun DateStepper(
    selectedDate: Long,
    today: Long,
    earliestDate: Long,
    onSelectDate: (Long) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = TapTargetMin),
    ) {
        IconButton(
            onClick = { onSelectDate(selectedDate - 1) },
            enabled = selectedDate > earliestDate,
            modifier = Modifier.size(TapTargetMin),
        ) {
            Icon(
                imageVector = AppIcons.ChevronLeft,
                contentDescription = stringResource(R.string.progress_supplements_previous_day),
            )
        }
        Text(
            text = catchUpDateLabel(selectedDate, today),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = { onSelectDate(selectedDate + 1) },
            enabled = selectedDate < today,
            modifier = Modifier.size(TapTargetMin),
        ) {
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = stringResource(R.string.progress_supplements_next_day),
            )
        }
    }
}

@Composable
private fun CatchUpRow(row: SupplementOnDay, onTap: () -> Unit) {
    val tint = if (row.isComplete) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    // Resolved outside the semantics lambda, which is not a composable scope.
    val description = describe(row)
    // The whole row is the target, not just the icon: a checklist read left-to-right should be
    // tappable where the eye already is.
    Surface(
        onClick = onTap,
        shape = RoundedCornerShape(ROW_RADIUS),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TapTargetMin)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Icon(
                imageVector = if (row.isComplete) AppIcons.CheckCircle else AppIcons.Circle,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = row.supplement.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (row.supplement.dose.isNotBlank()) {
                    Text(
                        text = row.supplement.dose,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Only worth printing when the day could hold more than one dose — "1 / 1" says
            // nothing the tick hasn't already said. The figure is the day's own, never today's.
            if (row.dueTimes > 1) {
                Text(
                    text = stringResource(R.string.progress_supplements_taken_of, row.taken, row.dueTimes),
                    style = MaterialTheme.typography.labelLarge.tabularNums,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun describe(row: SupplementOnDay): String = with(row.supplement) {
    // The name and its dose are the user's own text, so they are interpolated into the sentence
    // rather than being three separate slots a translator would have to reassemble.
    val named = if (dose.isBlank()) name else "$name, $dose"
    if (row.dueTimes > 1) {
        stringResource(R.string.progress_supplements_desc_partial, named, row.taken, row.dueTimes)
    } else if (row.isComplete) {
        stringResource(R.string.progress_supplements_desc_done, named)
    } else {
        stringResource(R.string.progress_supplements_desc_todo, named)
    }
}

@PreviewLightDark
@Composable
private fun SupplementCatchUpCardPreview() {
    val today = todayEpochDay()
    AppTheme {
        Surface {
            SupplementCatchUpCard(
                selectedDate = today - 1,
                today = today,
                earliestDate = today - 30,
                rows = listOf(
                    SupplementOnDay(Supplement(id = 1, name = "Vitamin D", dose = "2000 IU"), taken = 1, dueTimes = 1),
                    SupplementOnDay(Supplement(id = 2, name = "Creatine", dose = "5 g"), taken = 1, dueTimes = 2),
                    SupplementOnDay(Supplement(id = 3, name = "Magnesium"), taken = 0, dueTimes = 1),
                ),
                onSelectDate = {},
                onSetTaken = { _, _ -> },
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** A weekday nothing is scheduled on — the card is still the card, so the stepper stays where the
 * thumb left it. */
@PreviewLightDark
@Composable
private fun SupplementCatchUpCardNothingDuePreview() {
    val today = todayEpochDay()
    AppTheme {
        Surface {
            SupplementCatchUpCard(
                selectedDate = today - 3,
                today = today,
                earliestDate = today - 30,
                rows = emptyList(),
                onSelectDate = {},
                onSetTaken = { _, _ -> },
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
