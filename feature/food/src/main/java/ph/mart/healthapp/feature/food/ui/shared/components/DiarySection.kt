package ph.mart.healthapp.feature.food.ui.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * How far a diary section's contents sit in from the screen edge: 16dp of screen padding, the
 * 16dp chevron the header draws, and the 8dp gap the header's own label uses. Written as its parts
 * rather than as the 38dp that used to be hardcoded into four separate paddings — that number was
 * off the spacing scale *and* two dp short of the label it was meant to line up under.
 */
internal val EntryIndent = 16.dp + 16.dp + 8.dp

/** A section with nothing in it. */
internal const val EMPTY_SECTION_LABEL = "Nothing here yet."

/**
 * A section that *does* have entries, all of them hidden by the diary's filter. Saying "nothing
 * here yet" in this case contradicts the subtotal still showing in the header directly above it,
 * and tells the user a section is empty when their own filter is what emptied it.
 */
internal const val FILTERED_SECTION_LABEL = "No matches here."

/**
 * Swipe an entry away to delete it. One implementation for both the meal sections and the exercise
 * section, which carried a byte-identical copy of this chrome each.
 *
 * Deleting from [confirmValueChange] is deliberate: the row leaves the list because the repository
 * flow re-emits without it, so the dismissal and the delete are the same event rather than an
 * animation waiting on a callback. The screen behind this raises an undo snackbar.
 */
@Composable
internal fun SwipeToDeleteRow(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDelete()
            true
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Delete",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(end = 24.dp),
                )
            }
        },
        content = { content() },
    )
}
