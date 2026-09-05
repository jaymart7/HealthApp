package ph.mart.healthapp.feature.food.ui.voice

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.data.food.MealParseResult
import ph.mart.healthapp.core.designsystem.component.DiscardConfirmDialog
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.components.ThinkingState
import ph.mart.healthapp.feature.food.ui.shared.toFoodEntry
import ph.mart.healthapp.feature.food.ui.voice.components.NoFoodHeardScreen
import ph.mart.healthapp.feature.food.ui.voice.components.VoiceFailedScreen
import ph.mart.healthapp.feature.food.ui.voice.components.VoiceInputScreen
import ph.mart.healthapp.feature.food.ui.voice.components.VoiceOfflineScreen
import ph.mart.healthapp.feature.food.ui.voice.components.VoiceReviewScreen

/**
 * Log a meal by saying or typing a sentence — the third fast path beside the camera and the
 * barcode, and the one that covers a plate with three things on it.
 *
 * Hosted the way [PhotoCaptureScreen][ph.mart.healthapp.feature.food.ui.photo.PhotoCaptureScreen]
 * is: one always-mounted [NavigationBackHandler] dispatching on the current [VoiceFlow] rather
 * than one behavior for every state. Input exits the route, Parsing cancels the call and returns to
 * the sentence, Review steps back to the sentence — asking first once the rows have been touched.
 *
 * [dateEpochDay] is the diary's day, carried by the route the way `BarcodeScanRoute` carries it;
 * `0` means today, which the repository stamps.
 */
@Composable
fun VoiceLogScreen(
    dateEpochDay: Long,
    onExit: () -> Unit,
    viewModel: VoiceLogViewModel = koinViewModel(),
) {
    val state = rememberVoiceLogScreen()

    // Set when a retry finds the network still down, so the screen says so instead of appearing to
    // ignore the tap — PhotoOfflineScreen's flag, for its reason.
    var retriedWhileOffline by remember { mutableStateOf(false) }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is VoiceLogSideEffect.ParseFinished -> when (val result = effect.result) {
                is MealParseResult.Success -> state.applyParsed(result.foods)
                MealParseResult.NoFoodFound -> state.flow = VoiceFlow.NothingHeard
                MealParseResult.Failed -> state.flow = VoiceFlow.Failed
            }

            VoiceLogSideEffect.MealLogged -> onExit()
        }
    }

    val backHandlerState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = backHandlerState,
        onBackCompleted = {
            when (state.flow) {
                VoiceFlow.Input -> onExit()
                VoiceFlow.Parsing -> {
                    viewModel.handleEvent(VoiceLogEvent.OnCancelParse)
                    state.flow = VoiceFlow.Input
                }

                VoiceFlow.Review -> if (state.isDirty) {
                    state.pendingDiscard = state::backToInput
                } else {
                    state.backToInput()
                }

                VoiceFlow.NothingHeard, VoiceFlow.Offline, VoiceFlow.Failed ->
                    state.flow = VoiceFlow.Input
            }
        },
    )

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (state.flow) {
                VoiceFlow.Input -> VoiceInputScreen(
                    text = state.text,
                    mealType = state.mealType,
                    onTextChange = { state.text = it },
                    onMealTypeSelect = state::selectMealType,
                    onEstimate = { startParse(viewModel, state) },
                )

                VoiceFlow.Parsing -> ThinkingState(line = stringResource(R.string.food_voice_parsing))

                VoiceFlow.Review -> VoiceReviewScreen(
                    items = state.items,
                    mealType = state.mealType,
                    expandedIndex = state.expandedIndex,
                    lowConfidence = state.anyLowConfidence,
                    onMealTypeSelect = state::selectMealType,
                    onItemChange = state::updateItem,
                    onRemoveItem = state::removeItem,
                    onToggleExpanded = state::toggleExpanded,
                    onLog = {
                        viewModel.handleEvent(
                            VoiceLogEvent.OnLogMeal(state.items.map { it.toFoodEntry(dateEpochDay) }),
                        )
                    },
                    // Back already asks before throwing away edits; the button that means the same
                    // thing asked nothing at all.
                    onDiscard = {
                        if (state.isDirty) state.pendingDiscard = { onExit() } else onExit()
                    },
                )

                VoiceFlow.NothingHeard -> NoFoodHeardScreen(onEdit = { state.flow = VoiceFlow.Input })

                VoiceFlow.Failed -> VoiceFailedScreen(onRetry = { startParse(viewModel, state) })

                VoiceFlow.Offline -> VoiceOfflineScreen(
                    retried = retriedWhileOffline,
                    onRetry = {
                        if (viewModel.isOnline()) {
                            retriedWhileOffline = false
                            startParse(viewModel, state)
                        } else {
                            retriedWhileOffline = true
                        }
                    },
                )
            }

            state.pendingDiscard?.let { discard ->
                DiscardConfirmDialog(
                    title = stringResource(R.string.food_discard_meal_title),
                    body = stringResource(R.string.food_unsaved_edits),
                    onConfirm = {
                        state.pendingDiscard = null
                        discard()
                    },
                    onDismiss = { state.pendingDiscard = null },
                )
            }
        }
    }
}

/**
 * The one path into [VoiceFlow.Parsing], whether the sentence is being estimated for the first time
 * or retried. The offline check runs before the call rather than being inferred from its failure:
 * the screen says something different for "you're offline" than for "that didn't work", and a
 * `FirebaseAIException` cannot tell the two apart.
 */
private fun startParse(viewModel: VoiceLogViewModel, state: VoiceLogScreenState) {
    if (!viewModel.isOnline()) {
        state.flow = VoiceFlow.Offline
        return
    }
    state.flow = VoiceFlow.Parsing
    viewModel.handleEvent(VoiceLogEvent.OnParse(state.text))
}
