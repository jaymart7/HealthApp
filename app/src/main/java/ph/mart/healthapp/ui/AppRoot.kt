package ph.mart.healthapp.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
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
import ph.mart.healthapp.BuildConfig
import ph.mart.healthapp.ShortcutAction
import ph.mart.healthapp.core.navigation.route.TopLevelDestination
import ph.mart.healthapp.feature.onboarding.ui.onboarding.OnboardingScreen
import ph.mart.healthapp.reminder.Reminder
import ph.mart.healthapp.reminder.enabledIn

sealed interface AppRootState {
    data object Loading : AppRootState
    data object Onboarding : AppRootState
    data object Ready : AppRootState
}

/** Debug-only escape hatch: flip to true to see onboarding on every run without clearing app
 * data. Dead in release — [BuildConfig.DEBUG] guards the read. */
private const val FORCE_ONBOARDING = false

class AppRootViewModel(profileRepository: ProfileRepository) : ViewModel() {
    val state: StateFlow<AppRootState> = profileRepository.observeProfile()
        .map { profile ->
            if (profile == null || (BuildConfig.DEBUG && FORCE_ONBOARDING)) AppRootState.Onboarding
            else AppRootState.Ready
        }
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
     * runs with no profile) shows Rui. */
    val mascot: StateFlow<MascotCharacter> = profileRepository.observeProfile()
        .map { profile -> mascotCharacterOf(profile?.mascotName) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MascotCharacter.Rui)

    /** The other half of the mascot's appearance, resolved exactly like [mascot]. Separate because
     * the character and the colour are separate picks: changing one leaves the other alone. */
    val mascotPalette: StateFlow<MascotPalette> = profileRepository.observeProfile()
        .map { profile -> mascotPaletteOf(profile?.mascotPaletteName) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MascotPalette.Soft)

    /** Whether any switch that posts a notification is on — the meal and weigh-in reminders are,
     * from the first launch — which is what earns [AskForNotifications] its dialog. */
    val wantsNotifications: StateFlow<Boolean> = profileRepository.observeProfile()
        .map { profile ->
            profile != null && (Reminder.entries.any { it.enabledIn(profile) } || profile.fastingRemindersOn)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
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
    shortcutRequest: ShortcutAction? = null,
    onShortcutRequestHandled: () -> Unit = {},
    viewModel: AppRootViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    when (state) {
        AppRootState.Loading -> Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {}
        AppRootState.Onboarding -> OnboardingScreen()
        AppRootState.Ready -> {
            val wantsNotifications by viewModel.wantsNotifications.collectAsState()
            AskForNotifications(wanted = wantsNotifications)
            AppScaffold(
                tabRequest = tabRequest,
                onTabRequestHandled = onTabRequestHandled,
                shortcutRequest = shortcutRequest,
                onShortcutRequestHandled = onShortcutRequestHandled,
            )
        }
    }
}

/**
 * Android 13+ posts nothing until `POST_NOTIFICATIONS` is granted, and the meal and weigh-in
 * reminders are on from the first launch — so before this they were switched on and silent on
 * every new install: the only ask lived behind turning a switch *on* in Settings → Reminders, and
 * those two were already on. `ReminderWorker` then read the missing grant as "stay quiet".
 *
 * Asked once per launch, the moment the app is past onboarding, while a reminder is on and the
 * grant is missing. Android stops showing the dialog itself after two refusals, which is the cap
 * on asking — no stored "already asked" flag to keep in step with the system's own. Refused, the
 * switches stay as they are and Reminders' banner explains, as it always has.
 */
@Composable
private fun AskForNotifications(wanted: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(wanted) {
        if (wanted && !asked && !NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
