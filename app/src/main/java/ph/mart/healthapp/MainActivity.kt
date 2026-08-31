package ph.mart.healthapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.androidx.compose.koinViewModel
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.navigation.route.TopLevelDestination
import ph.mart.healthapp.reminder.EXTRA_TAB
import ph.mart.healthapp.ui.AppRoot
import ph.mart.healthapp.ui.AppRootViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // BottomNavBar paints its own surfaceContainer all the way to the screen edge; without
        // this the system lays a translucent scrim over it in 3-button nav.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        // Cold start only: a reminder tapped while the app is already foregrounded lands on
        // whatever tab was showing. Add onNewIntent handling if that's ever reported.
        val startTab = intent?.getStringExtra(EXTRA_TAB)
            ?.let { name -> TopLevelDestination.entries.firstOrNull { it.name == name } }
            ?: TopLevelDestination.Home

        setContent {
            // Hoisted so the theme can read the profile that AppRoot already gates on — same
            // ViewModelStoreOwner and key, so this is the instance AppRoot uses, not a second one.
            val viewModel: AppRootViewModel = koinViewModel()
            val darkThemeOn by viewModel.darkThemeOn.collectAsState()
            val mascot by viewModel.mascot.collectAsState()
            val mascotPalette by viewModel.mascotPalette.collectAsState()
            AppTheme(
                darkTheme = darkThemeOn ?: isSystemInDarkTheme(),
                mascot = mascot,
                mascotPalette = mascotPalette,
            ) {
                AppRoot(startTab = startTab, viewModel = viewModel)
            }
        }
    }
}
