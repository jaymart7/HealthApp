package ph.mart.healthapp.feature.profile.ui.shared.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.DAYS_IN_WEEK
import ph.mart.healthapp.core.data.hasWeekday
import ph.mart.healthapp.core.data.toggleWeekday
import ph.mart.healthapp.core.data.weekdayInitials
import ph.mart.healthapp.core.data.weekdayNames
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R

/** The visual pill inside each cell. The touch box around it is 48 — the same split
 * `NumericStepperField` and `StepperRow` already ship. */
private val CellHeight = 40.dp
private val CellTouchHeight = 48.dp
private val CellShape = RoundedCornerShape(12.dp)

/**
 * Which weekdays something happens on — a routine's plan, a supplement's schedule. It is in
 * `shared/` because two flows draw it, and it knows about neither: it takes the mask and hands one
 * back.
 *
 * Seven cells sharing the width equally — the
 * `SegmentedToggle` argument, and the one row in the app whose labels genuinely cannot be
 * shortened further.
 *
 * `M T W T F S S` repeats two letters, so the initial is decoration and the **full day name rides
 * a `contentDescription`**: a picker whose cells all read "T" to a screen reader is not a picker.
 *
 * **The selected state never rests on colour or on the letter.** The fill is `secondaryContainer`
 * and the border `primary`, but under both sits a 4dp bottom edge and a 600-weight letter: flatten
 * the whole thing to grey, as a high-contrast scheme nearly does, and the thickened baseline still
 * says which days are chosen.
 *
 * A [days] of 0 draws all seven cells with a dashed border. "Nothing chosen" becomes a different
 * shape rather than only a different sentence, and the plan zone above says what to do about it.
 * That is a routine's state — everything saved before the plan existed is in it. A supplement
 * cannot reach it: something due on no day is not a supplement, so its caller refuses the toggle
 * that would empty the mask and this branch never fires there.
 *
 * Chips rather than switches: the whole point is reading the week at a glance, and seven rows of
 * switches is a screen, not a row.
 */
@Composable
internal fun WeekdayPicker(days: Int, onDaysChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val unscheduled = days == 0
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        val names = weekdayNames()
        val initials = weekdayInitials()
        val dashColor = MaterialTheme.colorScheme.outline
        val edgeColor = MaterialTheme.colorScheme.primary
        (0 until DAYS_IN_WEEK).forEach { index ->
            val selected = days.hasWeekday(index)
            // Resolved outside the semantics lambda, which cannot read a resource.
            val spoken = if (selected) stringResource(R.string.profile_weekday_selected, names[index]) else names[index]
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).height(CellTouchHeight),
            ) {
                Surface(
                    onClick = { onDaysChange(days.toggleWeekday(index)) },
                    shape = CellShape,
                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                    contentColor = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    border = when {
                        selected -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        unscheduled -> null
                        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CellHeight)
                        .semantics { contentDescription = spoken },
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                if (unscheduled) drawDashedOutline(dashColor)
                                if (selected) drawBottomEdge(edgeColor)
                            },
                    ) {
                        Text(
                            text = initials[index],
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                        )
                    }
                }
            }
        }
    }
}

/** The unscheduled cell's border. Drawn rather than a `BorderStroke` because Compose has no
 * dashed stroke on a `Surface`, and dashed is the whole point — an empty week has to be a
 * different *shape*. */
private fun DrawScope.drawDashedOutline(color: Color) {
    val width = 1.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(width / 2, width / 2),
        size = Size(size.width - width, size.height - width),
        cornerRadius = CornerRadius(12.dp.toPx()),
        style = Stroke(
            width = width,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
        ),
    )
}

/** The 4dp baseline under a selected cell — the half of the selected state that survives a
 * contrast swap flattening every fill in sight. */
private fun DrawScope.drawBottomEdge(color: Color) {
    val edge = 4.dp.toPx()
    drawRect(
        color = color,
        topLeft = Offset(0f, size.height - edge),
        size = Size(size.width, edge),
    )
}

@PreviewLightDark
@Composable
private fun WeekdayPickerPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Box(modifier = Modifier.padding(12.dp)) {
                WeekdayPicker(days = 0b0010101, onDaysChange = {})
            }
        }
    }
}

/** Nothing planned — the state every routine saved before the plan existed is in, and the one the
 * dashed border exists for. */
@PreviewLightDark
@Composable
private fun WeekdayPickerEmptyPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Box(modifier = Modifier.padding(12.dp)) {
                WeekdayPicker(days = 0, onDaysChange = {})
            }
        }
    }
}
