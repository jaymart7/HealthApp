package ph.mart.healthapp.feature.food.ui.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * The bar the screen's one commitment sits in, pinned below the scrolling content.
 *
 * Talk-to-log's two long screens both ended in a button that was simply the last item in a
 * `Column`: with the keyboard up, a wrapped sentence, a recents strip and a chip row above it,
 * Estimate was reliably below the fold on a short phone, and the scroll that fixed that only made
 * it reachable rather than present. Docked, the action is where the thumb already is at every
 * height, and [imePadding] lifts it over the keyboard rather than letting the keyboard bury it.
 *
 * A 1dp `outlineVariant` rule and a `surface` fill, no elevation — the app separates surfaces with
 * rules everywhere else, and a shadow here would be the only one in the flow.
 */
@Composable
internal fun DockedActionBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                content = content,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun DockedActionBarPreview() {
    AppTheme {
        Surface {
            DockedActionBar {
                PrimaryButton(label = "Estimate", onClick = {}, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** The review screen's pair: the destructive one demoted to a word at the start, the commitment
 * filled and taking the rest. */
@PreviewLightDark
@Composable
private fun DockedActionBarPairPreview() {
    AppTheme {
        Surface {
            DockedActionBar {
                TextButton(
                    label = "Discard",
                    onClick = {},
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrimaryButton(
                    label = "Log 3 items · 274 kcal",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
