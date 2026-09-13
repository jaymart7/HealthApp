package ph.mart.healthapp.feature.coach.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.coach.ChatMessage
import ph.mart.healthapp.core.designsystem.component.DiscardConfirmDialog
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.coach.R
import ph.mart.healthapp.feature.coach.ui.components.ChatBubble
import ph.mart.healthapp.feature.coach.ui.components.ChatInputBar
import ph.mart.healthapp.feature.coach.ui.components.CoachEmptyState
import ph.mart.healthapp.feature.coach.ui.components.FailureBubble
import ph.mart.healthapp.feature.coach.ui.components.FollowUpRow
import ph.mart.healthapp.feature.coach.ui.components.ProposalCard
import ph.mart.healthapp.feature.coach.ui.components.StreamingBubble

@Composable
fun CoachScreen(question: String? = null, viewModel: CoachViewModel = koinViewModel()) {
    val uiState by viewModel.collectAsState()
    val state = rememberCoachScreenState()
    // Fills the field and stops — the mic's rule, and for its reason: a send is a model call and a
    // persisted pair of rows, and a question arrived at by tapping an icon is a starting point.
    // `prefilled` is saved, so a rotation cannot re-fill a field the user has since cleared.
    LaunchedEffect(question) {
        if (question != null && !state.prefilled) {
            state.draft = question
            state.prefilled = true
        }
    }
    CoachContent(
        uiState = uiState,
        state = state,
        onEvent = viewModel::handleEvent,
    )
}

/**
 * The conversation, then the input bar. No `NavigationEventHandler`: this is one level with no
 * sub-views, so NavDisplay's own back is already the right answer — the clear-history dialog is
 * the only overlay, and `DiscardConfirmDialog` dismisses itself.
 *
 * `imePadding()` sits on the outer column so the whole screen lifts with the keyboard; the
 * `AppScaffold` above already cleared the system bars.
 */
@Composable
private fun CoachContent(
    uiState: CoachUiState,
    state: CoachScreenState,
    onEvent: (CoachEvent) -> Unit,
) {
    val listState = rememberLazyListState()
    // The newest turn is the one worth reading, so every arrival — a reply, a failure, or the
    // user's own question — scrolls to it. A pending turn is two items: the question and the
    // answer filling in under it.
    val itemCount = uiState.messages.size +
        (if (uiState.pending != null) 2 else 0) +
        (if (uiState.proposal.isNotEmpty()) 1 else 0) +
        (if (uiState.failure != null) 1 else 0) +
        // The follow-up row is an item too, and it is the last one — scrolling to the answer above
        // it would leave the chips off screen, which is the whole of what they are for.
        (if (uiState.messages.isNotEmpty() && uiState.pending == null && uiState.failure == null) 1 else 0)
    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }
    // Chunks arrive faster than an animation settles, so the growing bubble is followed without one.
    // ponytail: a scroll per chunk, and this pins the bubble's top rather than its bottom — both
    // only show on a reply taller than the viewport, which MAX_REPLY_CHARS all but rules out.
    LaunchedEffect(uiState.streaming) {
        if (uiState.streaming != null && itemCount > 0) listState.scrollToItem(itemCount - 1)
    }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (uiState.loaded && uiState.messages.isEmpty() && uiState.failure == null) {
                    item {
                        CoachEmptyState(onStarter = { onEvent(CoachEvent.OnSend(it)) })
                    }
                }
                itemsIndexed(uiState.messages, key = { _, message -> message.id }) { index, message ->
                    // The question this answer came from, when re-asking it makes sense — the
                    // newest answer only, and never mid-turn. `askAgainQuestion` is the rule.
                    val question = uiState.askAgainQuestion(index)
                    ChatBubble(
                        text = message.text,
                        fromUser = message.fromUser,
                        // The newest answer, and only it: the live region is what makes a finished
                        // reply reach a screen reader at all, and marking every bubble would
                        // re-announce the whole conversation.
                        announce = !message.fromUser && message.id == uiState.messages.last().id,
                        onAskAgain = question?.let { { onEvent(CoachEvent.OnSend(it)) } },
                    )
                }
                // The turn in flight, neither half of it in Room yet: the question is on screen
                // from the tap, and the answer grows under it in place.
                uiState.pending?.let { pending ->
                    item(key = "pending-question") { ChatBubble(text = pending, fromUser = true) }
                    item(key = "pending-answer") { StreamingBubble(text = uiState.streaming) }
                }
                // Under the answer that introduced it, and inside the list rather than over it:
                // a proposal is part of the conversation, so it scrolls with the conversation and
                // ignoring it is as valid an answer as tapping it.
                if (uiState.proposal.isNotEmpty()) {
                    item(key = "proposal") {
                        ProposalCard(
                            actions = uiState.proposal,
                            onConfirm = { kept, line ->
                                onEvent(CoachEvent.OnConfirmProposal(kept, line))
                            },
                            onDismiss = { onEvent(CoachEvent.OnDismissProposal) },
                        )
                    }
                }
                // Under the newest answer, and only when nothing is in flight: a row of questions
                // beside a half-written one asks the user to abandon the answer they are reading.
                if (uiState.loaded && uiState.messages.isNotEmpty() &&
                    uiState.pending == null && uiState.failure == null
                ) {
                    item(key = "follow-ups") {
                        FollowUpRow(
                            followUps = followUpsFor(uiState.request),
                            onAsk = { onEvent(CoachEvent.OnSend(it)) },
                        )
                    }
                }
                uiState.failure?.let { failure ->
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FailureBubble(reason = failure.reason, insight = failure.insight)
                            TextButton(label = stringResource(R.string.coach_retry), onClick = { onEvent(CoachEvent.OnRetry) })
                        }
                    }
                }
            }

            if (uiState.messages.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(label = stringResource(R.string.coach_clear), onClick = { state.confirmingClear = true })
                }
            }

            ChatInputBar(
                draft = state.draft,
                // A proposal leaves `pending` set and the bar locked, which is deliberate: the
                // turn has not ended, and starting a second one would race the first one's write
                // — `withMessages` retires the bubbles on a list-size change, so the new question
                // would vanish the moment the old pair landed. The card carries both ways out.
                sending = uiState.pending != null,
                onDraftChange = { state.draft = it },
                onSend = {
                    onEvent(CoachEvent.OnSend(state.draft))
                    state.draft = ""
                },
                // The abandoned question goes back in the field — the reading `CoachFailure`
                // already gives one that failed to send: stopping must not cost the user their
                // typing. Only into an empty field, since the field stays editable while a turn
                // runs and whatever is in it is newer.
                onStop = {
                    if (state.draft.isBlank()) state.draft = uiState.pending.orEmpty()
                    onEvent(CoachEvent.OnStop)
                },
            )
        }
    }

    // A conversation is something the user authored, so it asks before going — the rule saved
    // meals and recipes follow, rather than the diary's swipe-and-undo.
    if (state.confirmingClear) {
        DiscardConfirmDialog(
            title = stringResource(R.string.coach_clear_title),
            body = stringResource(R.string.coach_clear_body),
            confirmLabel = stringResource(R.string.coach_clear_confirm),
            dismissLabel = stringResource(R.string.coach_clear_dismiss),
            onConfirm = {
                onEvent(CoachEvent.OnClear)
                state.confirmingClear = false
            },
            onDismiss = { state.confirmingClear = false },
        )
    }
}

@PreviewLightDark
@Composable
private fun CoachScreenPreview() {
    AppTheme {
        CoachContent(
            uiState = CoachUiState(
                loaded = true,
                messages = listOf(
                    ChatMessage(id = 1, fromUser = true, text = "Am I getting enough protein?", sentAtMillis = 1),
                    ChatMessage(
                        id = 2,
                        fromUser = false,
                        text = "You're at 62 g of 150 g today, so there's plenty of room — a " +
                            "high-protein dinner would close most of that gap.",
                        sentAtMillis = 2,
                    ),
                ),
            ),
            state = CoachScreenState(),
            onEvent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun CoachScreenEmptyPreview() {
    AppTheme {
        CoachContent(uiState = CoachUiState(loaded = true), state = CoachScreenState(), onEvent = {})
    }
}

@PreviewLightDark
@Composable
private fun CoachScreenStreamingPreview() {
    AppTheme {
        CoachContent(
            uiState = CoachUiState(
                loaded = true,
                messages = listOf(
                    ChatMessage(id = 1, fromUser = true, text = "How am I doing today?", sentAtMillis = 1),
                    ChatMessage(id = 2, fromUser = false, text = "Nicely — you're on target.", sentAtMillis = 2),
                ),
                pending = "What should I eat tonight?",
                streaming = "You have 600 kcal left and most of your protein still to",
            ),
            state = CoachScreenState(),
            onEvent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun CoachScreenOfflinePreview() {
    AppTheme {
        CoachContent(
            uiState = CoachUiState(
                loaded = true,
                messages = listOf(
                    ChatMessage(id = 1, fromUser = true, text = "How am I doing today?", sentAtMillis = 1),
                ),
                failure = CoachFailure(
                    reason = OFFLINE_REASON,
                    insight = "You're 88g short on protein today.",
                    question = "How am I doing today?",
                ),
            ),
            state = CoachScreenState(),
            onEvent = {},
        )
    }
}
