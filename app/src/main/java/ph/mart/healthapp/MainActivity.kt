package ph.mart.healthapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.ui.AppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
<<<<<<< HEAD
=======
        // BottomNavBar paints its own surfaceContainer all the way to the screen edge; without
        // this the system lays a translucent scrim over it in 3-button nav.
>>>>>>> refs/heads/debug-seed-data
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            AppTheme {
                AppRoot()
            }
        }
    }
}
