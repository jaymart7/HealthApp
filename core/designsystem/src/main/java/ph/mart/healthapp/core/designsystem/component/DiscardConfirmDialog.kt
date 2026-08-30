package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * "You're about to lose unsaved work" — the guard behind a back gesture out of an edited form.
 * Shared because two flows need it: the photo flow's confirmation step and the recipe builder.
 *
 * The two actions use Material 3's own [TextButton] rather than the design system's, because
 * [AlertDialog] styles its button slots itself.
 */
@Composable
fun DiscardConfirmDialog(
    title: String,
    body: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "Discard",
    dismissLabel: String = "Keep editing",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } },
    )
}

/** A dialog renders invisible in isolation, so the preview supplies its own scrim. */
@PreviewLightDark
@Composable
private fun DiscardConfirmDialogPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.32f))
                .padding(24.dp),
        ) {
            DiscardConfirmDialog(
                title = "Discard this meal?",
                body = "You've made edits that haven't been logged yet.",
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}
