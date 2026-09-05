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
import androidx.compose.foundation.lazy.items
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
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.coach.R
import ph.mart.healthapp.feature.coach.ui.components.ChatBubble
import ph.mart.healthapp.feature.coach.ui.components.ChatInputBar
import ph.mart.healthapp.feature.coach.ui.components.CoachEmptyState
import ph.mart.healthapp.feature.coach.ui.components.FailureBubble

@Composable
fun CoachScreen(viewModel: CoachViewModel = koinViewModel()) {
    val uiState by viewModel.collectAsState()
    CoachContent(
        uiState = uiState,
        state = rememberCoachScreenState(),
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
    // user's own question — scrolls to it.
    val lastIndex = uiState.messages.size + if (uiState.failure != null || uiState.sending) 1 else 0
    LaunchedEffect(lastIndex) {
        if (lastIndex > 0) listState.animateScrollToItem(lastIndex - 1)
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
                items(uiState.messages, key = { it.id }) { message ->
                    ChatBubble(text = message.text, fromUser = message.fromUser)
                }
                if (uiState.sending) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MascotAvatar(state = MascotState.Thinking, size = 32.dp)
                        }
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
                sending = uiState.sending,
                onDraftChange = { state.draft = it },
                onSend = {
                    onEvent(CoachEvent.OnSend(state.draft))
                    state.draft = ""
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
