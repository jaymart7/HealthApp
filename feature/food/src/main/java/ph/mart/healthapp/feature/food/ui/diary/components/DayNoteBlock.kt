package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.components.SectionRule

/**
 * What the user wrote about the day, at the foot of the diary under its own rule — the shape
 * [ExerciseSection] already has, and for the same reason: a note is not a fifth meal, and the break
 * plus the label is what stops it reading as one.
 *
 * **Drawn only when there is something to read.** A day nobody wrote about shows nothing here; the
 * way in is the "Add a note" link in the footer row below, which costs the scroll one word rather
 * than an empty card on every day of the year. Tapping the card reopens the sheet on the text.
 */
@Composable
internal fun DayNoteBlock(
    note: String,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (note.isBlank()) return
    Column(modifier = modifier) {
        SectionRule(
            label = stringResource(R.string.food_note_rule),
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
        )
        AppCard(onClick = onEdit) {
            Text(text = note, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@PreviewLightDark
@Composable
private fun DayNoteBlockPreview() {
    AppTheme {
        Surface {
            DayNoteBlock(
                note = "Slept badly and skipped the gym. Lunch was out for someone's birthday, so " +
                    "the afternoon is a guess.",
                onEdit = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
