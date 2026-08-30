package ph.mart.healthapp.feature.onboarding.ui.onboarding

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.feature.onboarding.ui.health.HealthConnectScreen
import ph.mart.healthapp.feature.onboarding.ui.onboarding.components.ActivityScreen
import ph.mart.healthapp.feature.onboarding.ui.onboarding.components.BasicsScreen
import ph.mart.healthapp.feature.onboarding.ui.onboarding.components.ConfirmTargetsScreen
import ph.mart.healthapp.feature.onboarding.ui.onboarding.components.DietaryScreen
import ph.mart.healthapp.feature.onboarding.ui.onboarding.components.GoalScreen
import ph.mart.healthapp.feature.onboarding.ui.onboarding.components.WelcomeScreen

/**
 * Hosts the whole 7-step wizard. No Nav3 here — the prototype's back is a plain `step - 1` with
 * no branching, so a saved `step: Int` + `when` dispatch reproduces it exactly (including
 * Skip -> Confirm, then back -> Dietary) without a second, competing back-handler on top of
 * NavDisplay's own.
 */
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = koinViewModel()) {
    val uiState by viewModel.collectAsState()
    val state = rememberOnboardingState()

    viewModel.collectSideEffect { effect ->
        when (effect) {
            // AppRoot's reactive ProfileRepository.observeProfile() flow is what actually
            // switches to AppScaffold once the write lands — nothing to do here.
            OnboardingSideEffect.Finished -> Unit
        }
    }

    val backHandlerState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = backHandlerState,
        isBackEnabled = state.step > 0,
        onBackCompleted = { state.step -= 1 },
    )

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        when (state.step) {
            0 -> WelcomeScreen(onGetStarted = { state.step = 1 })

            1 -> GoalScreen(
                options = uiState.goalOptions,
                selected = state.form.goal,
                onSelect = { goal -> state.form = state.form.copy(goal = goal).clearOverrides() },
                onNext = { state.step = 2 },
                onBack = { state.step = 0 },
            )

            2 -> BasicsScreen(
                form = state.form,
                onFormChange = { state.form = it },
                onNext = { state.step = 3 },
                onBack = { state.step = 1 },
            )

            3 -> ActivityScreen(
                options = uiState.activityOptions,
                selected = state.form.activityLevel,
                onSelect = { level -> state.form = state.form.copy(activityLevel = level).clearOverrides() },
                onNext = { state.step = 4 },
                onBack = { state.step = 2 },
            )

            4 -> DietaryScreen(
                options = uiState.dietOptions,
                selected = state.form.dietaryPreference,
                onSelect = { preference ->
                    state.form = state.form.copy(
                        dietaryPreference = if (state.form.dietaryPreference == preference) null else preference,
                    )
                },
                onNext = { state.step = 5 },
                onBack = { state.step = 3 },
            )

            5 -> HealthConnectScreen(onNext = { state.step = 6 }, onBack = { state.step = 4 })

            else -> ConfirmTargetsScreen(
                form = state.form,
                isCelebrating = uiState.isCelebrating,
                onFormChange = { state.form = it },
                onFinish = { viewModel.handleEvent(OnboardingEvent.OnFinish(state.form)) },
                onBack = { state.step = 5 },
            )
        }
    }
}
