package ph.mart.healthapp.feature.profile.ui.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * Renames one saved meal or recipe. Same shape as `SaveMealSheet`, which is where the name was
 * typed in the first place — seeded with the current one, so the fast path is a small edit.
 *
 * The draft lives here rather than in the screen: it is discarded on dismiss, and there is nothing
 * on the other side of Save that needs to have seen it.
 */
@Composable
internal fun RenameSheet(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    var name by remember(currentName) { mutableStateOf(currentName) }
    AppBottomSheet(onDismiss = onDismiss) {
        Text(
            text = "Rename",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTextField(value = name, onValueChange = { name = it }, placeholder = "Name")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryButton(label = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                PrimaryButton(
                    label = "Save",
                    onClick = { onRename(name) },
                    // Same guard as SaveMealSheet: a nameless saved meal is unidentifiable, and
                    // unlike a diary entry it has no calorie figure to stand in for one.
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun RenameSheetPreview() {
    AppTheme {
        RenameSheet(currentName = "Usual breakfast", onDismiss = {}, onRename = {})
    }
}
