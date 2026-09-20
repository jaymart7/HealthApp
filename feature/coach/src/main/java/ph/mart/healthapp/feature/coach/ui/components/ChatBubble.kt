package ph.mart.healthapp.feature.coach.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.shareText
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.coach.R

/** The avatar's width plus the gap after it — what anything belonging to a coach answer rather than
 * to the list is indented by, so it lines up with that answer's own text edge. */
internal val AnswerIndent = 40.dp

/**
 * One turn. [CoachBubble] on the coach's side beside the mascot, [UserBubble] on the user's,
 * mirrored and with no avatar — the tail points at whoever is speaking, and there is no second face
 * on this screen.
 *
 * [announce] marks this bubble a polite live region, and the screen sets it on the newest coach
 * message only: a finished answer arriving is the one thing on this screen a screen reader user
 * would otherwise have to go looking for. Deliberately *not* on [StreamingBubble] — a live region
 * over text that grows per chunk makes TalkBack restart the whole answer on every chunk.
 *
 * **Long-pressing the coach's side opens Copy / Share / Ask again**, the last only when
 * [onAskAgain] is non-null. **Long-pressing the user's own side opens Edit**, when [onEdit] is —
 * one item, because rephrasing is the only thing worth doing to a question that the answer's menu
 * cannot already do. Copy and Share stay off it: the question is the user's own words and they
 * have them. Edit puts the text back in the composer and sends nothing, so it is offered on every
 * question rather than under `askAgainQuestion()`'s newest-only rule — that rule guards against
 * burying the answer being read, and nothing that does not send can bury anything.
 *
 * [receipt] is what a confirmed draft wrote, drawn inside the answer under a rule rather than
 * appended to its prose. See [Receipt].
 */
@Composable
internal fun ChatBubble(
    text: String,
    fromUser: Boolean,
    modifier: Modifier = Modifier,
    announce: Boolean = false,
    receipt: String? = null,
    onAskAgain: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
) {
    val announced = if (announce) {
        modifier.semantics { liveRegion = LiveRegionMode.Polite }
    } else {
        modifier
    }
    if (fromUser) {
        Row(modifier = announced.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (onEdit == null) {
                UserBubble(text = text)
            } else {
                BubbleActions(
                    menu = { dismiss ->
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.coach_bubble_edit)) },
                            leadingIcon = { Icon(AppIcons.Edit, contentDescription = null) },
                            onClick = {
                                onEdit()
                                dismiss()
                            },
                        )
                    },
                ) { open, raised ->
                    UserBubble(
                        text = text,
                        modifier = raised.combinedClickable(
                            onClick = {},
                            onLongClick = open,
                            onLongClickLabel = stringResource(R.string.coach_question_actions),
                        ),
                    )
                }
            }
        }
    } else {
        Row(
            modifier = announced.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            MascotAvatar(state = MascotState.Idle, size = 32.dp)
            BubbleActions(
                menu = { dismiss ->
                    AnswerMenu(
                        text = text,
                        receipt = receipt,
                        onAskAgain = onAskAgain,
                        dismiss = dismiss,
                    )
                },
            ) { open, raised ->
                CoachBubble(
                    text = text,
                    receipt = receipt,
                    modifier = raised.combinedClickable(
                        // A plain tap does nothing: the bubble is text, not a control, and the
                        // ripple is what says the long press is there at all.
                        onClick = {},
                        onLongClick = open,
                        onLongClickLabel = stringResource(R.string.coach_bubble_actions),
                    ),
                )
            }
        }
    }
}

/**
 * The shell behind a long press on any bubble — the menu's *behaviour*, with [menu] supplying the
 * rows. Both sides share it so a question's menu and an answer's can never disagree about how they
 * open, and neither side owns a second copy of the raise.
 *
 * The pressed bubble is **raised** while its menu is up — the one elevation on this screen, and the
 * only thing that says which of several bubbles the menu belongs to. The menu itself is anchored to
 * the bubble's own edge rather than to the finger, for the same reason.
 */
@Composable
private fun BubbleActions(
    menu: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
    bubble: @Composable (open: () -> Unit, raised: Modifier) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        val raised = if (open) {
            Modifier.shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp))
        } else {
            Modifier
        }
        bubble({ open = true }, raised)
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            offset = DpOffset(x = 0.dp, y = 0.dp),
        ) {
            menu { open = false }
        }
    }
}

/**
 * What a long press on an answer offers. Ask again only when [onAskAgain] is non-null — the newest
 * answer, and never mid-turn.
 *
 * The clipboard is the platform's, not Compose's: `LocalClipboardManager` is deprecated and its
 * replacement is a suspending API with a moving shape, while two lines of `ClipData` have been
 * stable for a decade. Android 13 and up show their own "copied" confirmation, which is why nothing
 * here raises a snackbar.
 *
 * Both take the bubble as it is *read*, receipt included: the one turn with no prose at all is the
 * one a confirmed draft wrote, and copying it used to hand over an empty string.
 */
@Composable
private fun AnswerMenu(
    text: String,
    receipt: String?,
    onAskAgain: (() -> Unit)?,
    dismiss: () -> Unit,
) {
    val context = LocalContext.current
    val payload = listOfNotNull(text.takeIf(String::isNotBlank), receipt).joinToString("\n\n")
    DropdownMenuItem(
        text = { Text(stringResource(R.string.coach_bubble_copy)) },
        leadingIcon = { Icon(AppIcons.Copy, contentDescription = null) },
        onClick = {
            context.getSystemService(ClipboardManager::class.java)
                ?.setPrimaryClip(ClipData.newPlainText(null, payload))
            dismiss()
        },
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.coach_bubble_share)) },
        leadingIcon = { Icon(AppIcons.Share, contentDescription = null) },
        onClick = {
            shareText(context, payload)
            dismiss()
        },
    )
    if (onAskAgain != null) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.coach_bubble_ask_again)) },
            leadingIcon = { Icon(AppIcons.Refresh, contentDescription = null) },
            onClick = {
                onAskAgain()
                dismiss()
            },
        )
    }
}

/**
 * The answer as it arrives, and the wait before it: [text] is null until the first chunk lands.
 *
 * **The wait gets a bubble from the first frame.** It used to be the mascot alone on an empty row,
 * with the bubble appearing under it when the first chunk arrived — which meant the one moment the
 * user is most attentive was the one moment the layout jumped. Now a real bubble is there from the
 * tap, holding a status line over three placeholder lines, and the first chunk **overwrites them in
 * place**: no bubble swap, no reflow, and an honest answer to "is it doing anything?".
 *
 * One `Row` across both states, deliberately — the avatar stays a single node for the whole turn,
 * so `MascotAvatar`'s state spring plays the `Thinking → Idle` move when the answer starts instead
 * of remounting as a cut.
 */
@Composable
internal fun StreamingBubble(text: String?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        MascotAvatar(
            state = if (text == null) MascotState.Thinking else MascotState.Idle,
            size = 32.dp,
        )
        if (text != null) CoachBubble(text = text) else PendingBubble()
    }
}

/**
 * What the bubble holds before the first chunk: what the coach is doing, and the shape of the
 * answer that is coming.
 *
 * Three lines at 100% / 88% / 54% rather than a spinner, because the widths are the message — they
 * say "a short paragraph is on its way", which a spinner cannot. They are `surfaceContainerHighest`
 * on `surfaceContainerHigh`: visible, and quiet enough not to read as content.
 */
@Composable
private fun PendingBubble(modifier: Modifier = Modifier) {
    // A semantics lambda cannot read a resource, so it is resolved a line above what it labels.
    val spoken = stringResource(R.string.coach_pending_spoken)
    CoachBubbleShell(modifier = modifier.semantics { contentDescription = spoken }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.coach_pending_status),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PlaceholderLine(fraction = 1f)
            PlaceholderLine(fraction = 0.88f)
            PlaceholderLine(fraction = 0.54f)
        }
    }
}

@Composable
private fun PlaceholderLine(fraction: Float) {
    Surface(
        shape = RoundedCornerShape(5.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth(fraction).height(10.dp),
    ) {}
}

@PreviewLightDark
@Composable
private fun ChatBubblePreview() {
    AppTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ChatBubble(text = "Am I getting enough protein?", fromUser = true)
                StreamingBubble(text = null)
                StreamingBubble(text = "You're at 62 g of 150 g, so there's plenty of")
                ChatBubble(
                    text = "You're at 62 g of 150 g, so there's plenty of room. A high-protein " +
                        "dinner would close most of that gap.",
                    fromUser = false,
                )
            }
        }
    }
}

/** The turn after a confirmed draft: the card is gone and what it wrote is part of the answer. */
@PreviewLightDark
@Composable
private fun ChatBubbleReceiptPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                ChatBubble(
                    text = "Done — two scrambled eggs for breakfast.",
                    fromUser = false,
                    receipt = "Logged: Scrambled eggs, 220 kcal.",
                )
            }
        }
    }
}
