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
}

/**
 * The app-level gate: shows onboarding until a profile exists in Room, then the real app.
 * Reactive, not a finish-callback — [OnboardingScreen] just writes to Room, and this flips on
 * its own once that Flow re-emits. An explicit [AppRootState.Loading] avoids flashing onboarding
 * for an already-onboarded user before the first Room emission lands.
 */
@Composable
fun AppRoot(viewModel: AppRootViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    when (state) {
        AppRootState.Loading -> Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {}
        AppRootState.Onboarding -> OnboardingScreen()
        AppRootState.Ready -> AppScaffold()
    }
}
