package ph.mart.healthapp.feature.profile.ui.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R

/**
 * Renames one saved meal, recipe or workout routine. Same shape as the sheet that named it in the
 * first place — seeded with the current name, so the fast path is a small edit.
 *
 * The draft lives here rather than in the screen: it is discarded on dismiss, and there is nothing
 * on the other side of Save that needs to have seen it.
 *
 * [onDelete] is the row's delete, which lives here now that the overflow menu is gone — see
 * [SheetDeleteAction]. It does not delete anything itself: the screen dismisses this sheet and
 * raises [DeleteConfirmDialog], which is the one place that asks.
 */
@Composable
internal fun RenameSheet(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember(currentName) { mutableStateOf(currentName) }
    AppBottomSheet(title = stringResource(R.string.profile_rename), onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTextField(value = name, onValueChange = { name = it }, placeholder = stringResource(R.string.profile_name))
            PrimaryButton(
                label = stringResource(R.string.profile_save),
                onClick = { onRename(name) },
                // Same guard as SaveMealSheet: a nameless saved meal is unidentifiable, and
                // unlike a diary entry it has no calorie figure to stand in for one.
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            SheetDeleteAction(onDelete = onDelete)
        }
    }
}

@PreviewLightDark
@Composable
private fun RenameSheetPreview() {
    AppTheme {
        RenameSheet(currentName = "Usual breakfast", onDismiss = {}, onRename = {}, onDelete = {})
    }
}
