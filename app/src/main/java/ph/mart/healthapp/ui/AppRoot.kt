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
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotCharacter
import ph.mart.healthapp.core.designsystem.component.MascotPalette
import ph.mart.healthapp.core.designsystem.component.mascotCharacterOf
import ph.mart.healthapp.core.designsystem.component.mascotPaletteOf
import ph.mart.healthapp.core.navigation.route.TopLevelDestination
import ph.mart.healthapp.feature.onboarding.ui.onboarding.OnboardingScreen

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

    /** Read by [MainActivity] above the theme, for the same reason [darkThemeOn] is, and resolved
     * here rather than at the ~16 [MascotAvatar] call sites — the theme provides it as a
     * CompositionLocal. Its "not loaded yet" value is the default character, so onboarding (which
     * runs with no profile) shows Bibo. */
    val mascot: StateFlow<MascotCharacter> = profileRepository.observeProfile()
        .map { profile -> mascotCharacterOf(profile?.mascotName) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MascotCharacter.Bibo)

    /** The other half of the mascot's appearance, resolved exactly like [mascot]. Separate because
     * the character and the colour are separate picks: changing one leaves the other alone. */
    val mascotPalette: StateFlow<MascotPalette> = profileRepository.observeProfile()
        .map { profile -> mascotPaletteOf(profile?.mascotPaletteName) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MascotPalette.Soft)
}

/**
 * The app-level gate: shows onboarding until a profile exists in Room, then the real app.
 * Reactive, not a finish-callback — [OnboardingScreen] just writes to Room, and this flips on
 * its own once that Flow re-emits. An explicit [AppRootState.Loading] avoids flashing onboarding
 * for an already-onboarded user before the first Room emission lands.
 */
@Composable
fun AppRoot(
    tabRequest: TopLevelDestination? = null,
    onTabRequestHandled: () -> Unit = {},
    viewModel: AppRootViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    when (state) {
        AppRootState.Loading -> Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {}
        AppRootState.Onboarding -> OnboardingScreen()
        AppRootState.Ready -> AppScaffold(tabRequest = tabRequest, onTabRequestHandled = onTabRequestHandled)
    }
}
