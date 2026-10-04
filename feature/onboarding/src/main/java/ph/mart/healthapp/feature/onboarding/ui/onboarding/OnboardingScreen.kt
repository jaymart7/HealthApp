package ph.mart.healthapp.feature.onboarding.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.feature.onboarding.R
import ph.mart.healthapp.feature.onboarding.ui.health.HealthConnectScreen
import ph.mart.healthapp.feature.onboarding.ui.onboarding.components.ActivityScreen
import ph.mart.healthapp.feature.onboarding.ui.onboarding.components.BasicsScreen
import ph.mart.healthapp.feature.onboarding.ui.onboarding.components.ConfirmTargetsScreen
import ph.mart.healthapp.feature.onboarding.ui.onboarding.components.DietaryScreen
import ph.mart.healthapp.feature.onboarding.ui.onboarding.components.GoalScreen
import ph.mart.healthapp.feature.onboarding.ui.onboarding.components.WelcomeScreen
import ph.mart.healthapp.feature.onboarding.ui.shared.components.ONBOARDING_STEPS

/** Long enough to see the check land and the icon circle flip, short enough not to read as lag.
 * It is what stops a single tap that both answers and navigates from feeling like a mis-tap. */
const val SELECTION_HOLD_MS = 400L

/** One step's worth of slide, emphasised-decelerate. */
private const val TRANSITION_MS = 300

/** How long an announcement stays in the live region: long enough for TalkBack to have read it,
 * short enough that the same words can be announced again, and not left behind as a swipe stop. */
private const val ANNOUNCEMENT_CLEAR_MS = 1000L

/**
 * Hosts the whole 7-step wizard. No Nav3 here — back is a plain `step - 1` with no branching, so a
 * saved `step: Int` + `when` dispatch reproduces it exactly (including Skip -> Confirm, then back
 * -> Dietary) without a second, competing back-handler on top of NavDisplay's own.
 *
 * Steps 1 and 3 have no Next button: choosing is what advances them, after [SELECTION_HOLD_MS].
 * The pending advance is deliberately *not* in [OnboardingState] — a process death mid-hold should
 * restore the step the user was looking at, not complete a navigation they never saw.
 */
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = koinViewModel()) {
    val uiState by viewModel.collectAsState()
    val state = rememberOnboardingState()
    val context = LocalContext.current
    var pending by remember { mutableStateOf<Int?>(null) }
    var announcement by remember { mutableStateOf("") }
    // Which way the next transition slides. Read during the step change, so it is set first.
    var direction by remember { mutableIntStateOf(1) }

    // Every way off a step abandons a pending advance rather than racing it: the header's back
    // arrow, like the system back, would otherwise land and then be overtaken by the hold.
    fun goTo(step: Int) {
        pending = null
        direction = if (step >= state.step) 1 else -1
        state.step = step
    }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            // AppRoot's reactive ProfileRepository.observeProfile() flow is what actually
            // switches to AppScaffold once the write lands — nothing to do here.
            OnboardingSideEffect.Finished -> Unit
        }
    }

    LaunchedEffect(pending) {
        val next = pending ?: return@LaunchedEffect
        delay(SELECTION_HOLD_MS)
        goTo(next)
    }

    LaunchedEffect(announcement) {
        if (announcement.isEmpty()) return@LaunchedEffect
        delay(ANNOUNCEMENT_CLEAR_MS)
        announcement = ""
    }

    val backHandlerState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = backHandlerState,
        isBackEnabled = state.step > 0,
        // Back during the hold goes back from the step you were on, with the selection intact.
        onBackCompleted = { goTo(state.step - 1) },
    )

    // A string built in a click callback, so it reads through `LocalContext` rather than reaching
    // a ViewModel for a Context. Nothing else confirms the choice before the screen moves.
    fun announce(@StringRes label: Int, step: Int) {
        announcement =
            context.getString(R.string.onboarding_selected_moving, context.getString(label), step, ONBOARDING_STEPS)
    }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                val enter = slideInHorizontally(tween(TRANSITION_MS)) { width -> direction * width } +
                    fadeIn(tween(TRANSITION_MS))
                val exit = slideOutHorizontally(tween(TRANSITION_MS)) { width -> -direction * width } +
                    fadeOut(tween(TRANSITION_MS))
                enter togetherWith exit
            },
            label = "step",
        ) { step ->
            when (step) {
                0 -> WelcomeScreen(onGetStarted = { goTo(1) })

                1 -> GoalScreen(
                    options = uiState.goalOptions,
                    selected = state.form.goal,
                    onSelect = { option ->
                        state.form = state.form.copy(goal = option.goal).clearOverrides()
                        announce(option.title, 2)
                        pending = 2
                    },
                    onBack = { goTo(0) },
                )

                2 -> BasicsScreen(
                    form = state.form,
                    onFormChange = { state.form = it },
                    onNext = { goTo(3) },
                    onBack = { goTo(1) },
                )

                3 -> ActivityScreen(
                    options = uiState.activityOptions,
                    selected = state.form.activityLevel,
                    onSelect = { option ->
                        state.form = state.form.copy(activityLevel = option.level).clearOverrides()
                        announce(option.title, 4)
                        pending = 4
                    },
                    onBack = { goTo(2) },
                )

                4 -> DietaryScreen(
                    options = uiState.dietOptions,
                    selected = state.form.dietaryPreference,
                    onSelect = { option ->
                        val cleared = state.form.dietaryPreference == option.preference
                        state.form = state.form.copy(
                            dietaryPreference = if (cleared) null else option.preference,
                        )
                        if (cleared) announcement = context.getString(R.string.onboarding_not_selected)
                    },
                    onSkip = {
                        state.form = state.form.copy(dietaryPreference = null)
                        goTo(5)
                    },
                    onNext = { goTo(5) },
                    onBack = { goTo(3) },
                )

                5 -> HealthConnectScreen(onNext = { goTo(6) }, onBack = { goTo(4) })

                else -> ConfirmTargetsScreen(
                    form = state.form,
                    isCelebrating = uiState.isCelebrating,
                    onFormChange = { state.form = it },
                    onFinish = { viewModel.handleEvent(OnboardingEvent.OnFinish(state.form)) },
                    onBack = { goTo(5) },
                )
            }
        }

        // The screen's announcer, outside AnimatedContent so it outlives the step it speaks for.
        // A live region only speaks when its text changes, from a node that was already there.
        // Compose prunes a zero-size node, and one drawn under the full-screen step, from the
        // accessibility tree, so it is 1dp and declared last.
        Box(
            Modifier.requiredSize(1.dp).semantics {
                liveRegion = LiveRegionMode.Polite
                if (announcement.isNotEmpty()) contentDescription = announcement
            },
        )
    }
}
