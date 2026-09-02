package ph.mart.healthapp.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ph.mart.healthapp.wear.ui.WearTodayScreen
import ph.mart.healthapp.wear.ui.theme.FitPulseWearTheme

/**
 * One screen, so no navigation graph — the watch app is today and nothing else. Anything deeper
 * (the diary, a chart, the coach) is a phone screen, and saying so by omission is better than a
 * wrist-sized version of it.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitPulseWearTheme { WearTodayScreen() }
        }
    }
}
