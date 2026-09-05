package ph.mart.healthapp.feature.food.ui.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * A quiet heading that names what comes after it, then rules off to the edge.
 *
 * It exists for one line on the diary — "Adds to today" over the exercise block — and that line is
 * doing real work: four sections above it report calories eaten and the fifth reports calories
 * spent, and until now the only thing saying so was the word "burned" inside a subtotal. A break
 * in the run plus a label is what stops the fifth section reading as a fifth meal.
 *
 * The tracking is set here rather than in `Type.kt`: that file is the app's type scale and this is
 * one label's treatment, not a new step on it.
 *
 * Feature-local for [LabelledActionChip]'s reason — one caller today.
 */
@Composable
internal fun SectionRule(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.08.em),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@PreviewLightDark
@Composable
private fun SectionRulePreview() {
    AppTheme {
        Surface {
            SectionRule(label = "Adds to today", modifier = Modifier.padding(16.dp))
        }
    }
}
