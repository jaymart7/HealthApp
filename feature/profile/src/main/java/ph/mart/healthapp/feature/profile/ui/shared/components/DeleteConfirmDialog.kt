package ph.mart.healthapp.feature.profile.ui.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R

/**
 * One dialog, four bodies — the confirm every saved thing's delete asks first, because a
 * definition is something the user authored and there is no swipe-and-undo here the way the diary
 * has one.
 *
 * Not [ph.mart.healthapp.core.designsystem.component.DiscardConfirmDialog], which it replaced on
 * all three screens: that one is the guard behind a back gesture out of an edited form, and its
 * *confirm* is the destructive answer. Here **Keep is the confirm slot** — rightmost, `primary`,
 * where the thumb lands by default — and Delete sits left in `error`. Neither is filled: one
 * filled `error` container would out-shout the body copy, which is the thing actually doing the
 * reassuring.
 *
 * The name goes in the title and **never in the body**, so a body stays identical for every row
 * of its type and each one can say the one true thing about what deletion does and does not
 * touch. Two lines of title, then an ellipsis: a forty-character supplement name must not push
 * the reassurance off the dialog.
 */
@Composable
internal fun DeleteConfirmDialog(
    name: String,
    body: String,
    onDelete: () -> Unit,
    onKeep: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onKeep,
        title = {
            Text(
                text = stringResource(R.string.profile_delete_title, name),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onKeep) {
                Text(
                    text = stringResource(R.string.profile_keep),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text(
                    text = stringResource(R.string.profile_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}

/** A dialog renders invisible in isolation, so the preview supplies its own scrim. */
@PreviewLightDark
@Composable
private fun DeleteConfirmDialogPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.32f))
                .padding(24.dp),
        ) {
            DeleteConfirmDialog(
                name = "Usual breakfast",
                body = "This saved meal is removed for good. Anything already logged from it stays in your diary.",
                onDelete = {},
                onKeep = {},
            )
        }
    }
}
