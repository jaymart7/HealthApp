package ph.mart.healthapp.feature.profile.ui.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R

/**
 * Deletes the thing the sheet is editing, from inside it.
 *
 * **Where the row's overflow menu used to keep it.** That menu held two items — an Edit that
 * opened the sheet the row already opens, and this. Removing the duplicate left one item, which is
 * not a menu; so Delete moved one level in, under the fields it would destroy, and the row went
 * back to having nothing on its right at all.
 *
 * It keeps the rule it had in the menu, and for the same reason: **below a divider, in `error`,
 * with an `error` glyph** — the one destructive control on the screen, told apart by shape as well
 * as by colour. `TextButton`'s own KDoc names that pairing as its destructive form, which the
 * photo flow's Remove already draws.
 *
 * It does not confirm. The caller dismisses the sheet and raises [DeleteConfirmDialog], so one
 * scrim is up at a time and the "a saved thing asks before it goes" rule is answered in one place
 * rather than in each sheet.
 */
@Composable
internal fun SheetDeleteAction(onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        TextButton(
            label = stringResource(R.string.profile_delete),
            onClick = onDelete,
            icon = AppIcons.Delete,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@PreviewLightDark
@Composable
private fun SheetDeleteActionPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            SheetDeleteAction(onDelete = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
