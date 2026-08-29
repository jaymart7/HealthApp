package ph.mart.healthapp.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.androidx.compose.koinViewModel
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.navigation.route.TopLevelDestination
import ph.mart.healthapp.feature.onboarding.ui.OnboardingScreen

sealed interface AppRootState {
    data object Loading : AppRootState
    data object Onboarding : AppRootState
    data object Ready : AppRootState
}

class AppRootViewModel(profileRepository: ProfileRepository) : ViewModel() {
    val state: StateFlow<AppRootState> = profileRepository.observeProfile()
        .map { profile -> if (profile == null) AppRootState.Onboarding else AppRootState.Ready }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppRootState.Loading)

    /** Read by [MainActivity] above the theme. Its own chain rather than a share of [state]'s: the
     * two have different "not loaded yet" values (Loading vs. follow-the-system), and a wrapper to
     * carry both costs more than a second query against a one-row table. */
    val darkThemeOn: StateFlow<Boolean?> = profileRepository.observeProfile()
        .map { profile -> profile?.darkThemeOn }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/**
 * The app-level gate: shows onboarding until a profile exists in Room, then the real app.
 * Reactive, not a finish-callback — [OnboardingScreen] just writes to Room, and this flips on
 * its own once that Flow re-emits. An explicit [AppRootState.Loading] avoids flashing onboarding
 * for an already-onboarded user before the first Room emission lands.
 */
@Composable
fun AppRoot(
    startTab: TopLevelDestination = TopLevelDestination.Home,
    viewModel: AppRootViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    when (state) {
        AppRootState.Loading -> Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {}
        AppRootState.Onboarding -> OnboardingScreen()
        AppRootState.Ready -> AppScaffold(startTab = startTab)
    }
}
