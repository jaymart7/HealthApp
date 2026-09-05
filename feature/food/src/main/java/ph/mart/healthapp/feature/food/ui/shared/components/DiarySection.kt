package ph.mart.healthapp.feature.food.ui.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R

/**
 * How far a diary section's contents sit in from the *card's* edge: the header's 4dp start
 * padding, the 24dp chevron it draws, and the 8dp gap before its label. The 16dp of screen padding
 * that used to be part of this number now belongs to the card the section is drawn on, so it is
 * gone from here — the rows line up under the meal's name, which is the whole job.
 */
internal val EntryIndent = 4.dp + 24.dp + 8.dp

/** Far enough that a thumb resting on a row mid-scroll can't reach it, near enough that a
 * deliberate swipe doesn't have to cross the screen. */
private val SwipeThreshold = 96.dp

/**
 * Swipe an entry away to delete it. One implementation for both the meal sections and the exercise
 * section, which carried a byte-identical copy of this chrome each.
 *
 * Deleting from `confirmValueChange` is deliberate: the row leaves the list because the repository
 * flow re-emits without it, so the dismissal and the delete are the same event rather than an
 * animation waiting on a callback. The screen behind this raises an undo snackbar.
 *
 * The reveal is `errorContainer`/`onErrorContainer` rather than `error`/`onError` — this is the one
 * place on the diary the error role appears at all, and a full-strength `error` field sliding out
 * from under a row reads as an alarm rather than as the thing about to happen. The word does double
 * duty: it labels the reveal, and it is the TalkBack custom action, because a swipe is not a
 * gesture switch access or explore-by-touch can perform.
 */
@Composable
internal fun SwipeToDeleteRow(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val deleteLabel = stringResource(R.string.food_delete)
    val threshold = with(LocalDensity.current) { SwipeThreshold.toPx() }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDelete()
            true
        },
        positionalThreshold = { threshold },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        modifier = Modifier.semantics {
            customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete(); true })
        },
        backgroundContent = { DeleteReveal(label = deleteLabel, modifier = Modifier.fillMaxSize()) },
        content = { content() },
    )
}

/** Its own composable so the preview below draws the thing that actually ships rather than a
 * hand-copied stand-in of it. */
@Composable
private fun DeleteReveal(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.background(MaterialTheme.colorScheme.errorContainer),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = AppIcons.Delete,
            // The row's custom action already names the gesture; announcing the reveal as well
            // would say "Delete" twice for one row.
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(start = 8.dp, end = 24.dp),
        )
    }
}

/** The reveal, which no preview reached before — it is the one surface on the diary allowed to use
 * the error role, so it is the one that most needs looking at in both schemes. */
@PreviewLightDark
@Composable
private fun DeleteRevealPreview() {
    AppTheme {
        DeleteReveal(
            label = stringResource(R.string.food_delete),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        )
    }
}
