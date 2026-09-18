package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.note.NOTE_MAX_CHARS
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R

/**
 * Writes the day's note. [dateLabel] names the day being written about, because the diary can be
 * showing any past day and a note is stamped with the day on screen rather than today.
 *
 * **Saving a blank field is the delete.** There is no second button for it: an emptied note is the
 * day saying nothing, which is the reading the table already gives a blank row, and a Remove beside
 * Save would be two controls for one outcome.
 *
 * The field wraps to four lines — the one other field in the app that holds a sentence rather than
 * a value is talk-to-log's, and this is the same parameter rather than a second component.
 */
@Composable
internal fun DayNoteSheet(
    dateLabel: String,
    draft: String,
    onDraftChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AppBottomSheet(title = stringResource(R.string.food_note_sheet_title), onDismiss = onDismiss) {
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTextField(
                // Capped here as well as in the repository: a field that stops accepting keystrokes
                // is how the user learns the limit, and the repository's cap is what makes it true.
                value = draft,
                onValueChange = { onDraftChange(it.take(NOTE_MAX_CHARS)) },
                placeholder = stringResource(R.string.food_note_placeholder),
                maxLines = 4,
            )
            PrimaryButton(
                label = stringResource(R.string.food_note_save),
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun DayNoteSheetPreview() {
    AppTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim)) {
            DayNoteSheet(
                dateLabel = "Tuesday, 16 September",
                draft = "Long day on site. Ate whatever was going.",
                onDraftChange = {},
                onDismiss = {},
                onSave = {},
            )
        }
    }
}
