package ph.mart.healthapp.feature.profile.ui.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.R as DesignSystemR
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/** 48dp of touch around a 40dp visible circle — the same split
 * [ph.mart.healthapp.core.designsystem.component.NumericStepperField] already uses, so a stepper
 * button feels identical wherever it is drawn. */
private val ButtonTouchSize = 48.dp
private val ButtonVisualSize = 40.dp

/**
 * The compact row form of a stepper: an optional leading tile with the label beneath it, then the
 * value and its unit with the derived sublabel beneath *them* — the sublabel qualifies the figure
 * ("8 glasses" → "1.6 L a day"), so it reads under the figure — then the two nudge buttons. 64dp is
 * the row's minimum, not its height; a row carrying a tile is taller.
 *
 * Deliberately *not*
 * [ph.mart.healthapp.core.designsystem.component.NumericStepperField], which stays exactly where it
 * is. That one is a form field — a label stacked above its own filled input — and it is right in
 * onboarding and the entry sheets, where a value is being *entered*. This is a row in a list of
 * rows, where four targets have to line up their values and their buttons down one edge. Two
 * shapes, two jobs; forking the field to take a leading slot and go horizontal would have left one
 * component that is neither.
 *
 * No [ph.mart.healthapp.core.designsystem.component.NumericStepperField]-style typable value
 * either: every caller here nudges a figure that is already about right (a water goal, an age, a
 * target weight), and each one clamps its write at the edges, which re-seeds a typed field
 * mid-keystroke.
 */
@Composable
internal fun StepperRow(
    label: String,
    value: String,
    unit: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
    sublabel: String? = null,
    leading: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            leading?.invoke()
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        // The label column sizes to its own content and this one takes the rest, so the sublabel
        // wraps inside what is left over rather than widening the column and squeezing the label.
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (sublabel != null) {
                Text(
                    text = sublabel,
                    style = MaterialTheme.typography.bodySmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
            }
        }
        StepperButton(symbol = "−", label = stringResource(DesignSystemR.string.ds_decrease, label), onClick = onDecrement)
        StepperButton(symbol = "+", label = stringResource(DesignSystemR.string.ds_increase, label), onClick = onIncrement)
    }
}

/**
 * One nudge button. Public to this feature because the Calories card draws the same pair beside a
 * 32sp hero figure rather than inside a row, and two circles that differ by a dp would be visible
 * on the one screen that shows both.
 *
 * The symbol is a minus sign and a plus sign, not copy — nothing to translate, and it carries no
 * meaning a screen reader can use, which is what [label] is for.
 */
@Composable
internal fun StepperButton(symbol: String, label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(ButtonTouchSize),
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .size(ButtonVisualSize)
                .semantics { contentDescription = label },
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Text(text = symbol, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun StepperRowPreview() {
    AppTheme {
        Surface {
            AppCard(modifier = Modifier.padding(16.dp)) {
                StepperRow(
                    label = "Water",
                    sublabel = "1.6 L a day",
                    value = "8",
                    unit = "glasses",
                    onIncrement = {},
                    onDecrement = {},
                    leading = { IconTile(icon = AppIcons.Water, contentDescription = null, accent = false) },
                )
                StepperRow(
                    label = "Age",
                    value = "26",
                    unit = "years",
                    onIncrement = {},
                    onDecrement = {},
                )
            }
        }
    }
}
