package ph.mart.healthapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.water.WaterRepository
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.navigation.route.TopLevelDestination
import ph.mart.healthapp.reminder.EXTRA_TAB
import ph.mart.healthapp.today.addGlass
import ph.mart.healthapp.ui.AppRoot
import ph.mart.healthapp.ui.AppRootViewModel

/** Intent extra naming what a launcher shortcut asked for — written by `@xml/shortcuts`, read
 * here. Its own constant rather than a sibling of [EXTRA_TAB] in `reminder/`: a shortcut is not a
 * reminder, and this Activity is the only reader either way. */
const val EXTRA_ACTION = "ph.mart.healthapp.shortcut.ACTION"

/**
 * Every request that reaches the app as an *intent* rather than as a tap, and the whole vocabulary
 * [EXTRA_ACTION] can carry.
 *
 * The first four are the launcher shortcuts, and they are the FAB's quick-action rows rather than a
 * list of their own — a shortcut is [ph.mart.healthapp.ui.QuickActionSheet] with the tap pre-made,
 * so each resolves to the same route or the same sheet that sheet's row does. [AddWater] is the one
 * exception and is handled here rather than in `AppScaffold`: water is an inline glass row on a
 * Home card, not a sheet or a route, so there is nothing to navigate to.
 *
 * [HealthSync] is not a shortcut and arrives differently — it is Health Connect's "why does this
 * app want my data?" tap, which carries an intent *action* and no extra (see [healthRationale]).
 * It rides this enum anyway because what it needs is exactly what a shortcut needs: a route
 * request, delivered by an intent, that must re-point an app already running. A fourth nullable
 * state beside [tabRequest] and [shortcutRequest] would be a third copy of one mechanism.
 *
 * [OpenRecap] rides it for that same argument, one surface over: it is the weekly recap
 * notification's tap, which carries [EXTRA_TAB] to reach the Progress tab and this to open the
 * recap overlay once it is there. Its notification is the only one whose tab is not the whole
 * answer — "a recap the user still has to find" is the thing it exists to fix.
 */
enum class ShortcutAction { SpeakFood, LogFood, AddWater, LogWeight, HealthSync, OpenRecap }

/** Pure over the extra's string so a JVM test can reach it, and an unknown name degrades to null —
 * the reading `mascotCharacterOf` already gives `Profile.mascotName`, and what lets a shortcut
 * pinned by an older build fail quietly rather than crash. */
fun shortcutActionOf(name: String?): ShortcutAction? =
    ShortcutAction.entries.firstOrNull { it.name == name }

class MainActivity : ComponentActivity() {

    /** The tab a reminder asked for, and null once the app has gone there. Mutable state so
     * [onNewIntent] can re-point an app that is already running; see there. */
    private var tabRequest by mutableStateOf<TopLevelDestination?>(null)

    /** What a launcher shortcut asked for, in [tabRequest]'s exact shape and for its exact reason:
     * a shortcut tapped on an already-running app must re-point it the way a second notification
     * does. Only ever the navigational actions — see [handle]. */
    private var shortcutRequest by mutableStateOf<ShortcutAction?>(null)

    // Reached through Koin's global context, the trick every surface with no ViewModel uses
    // (ReminderWorker, AddGlassAction, PhoneWearListenerService). Only the water shortcut writes.
    private val waterRepository: WaterRepository by inject()
    private val profileRepository: ProfileRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // BottomNavBar paints its own surfaceContainer all the way to the screen edge; without
        // this the system lays a translucent scrim over it in 3-button nav.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        // A reminder can arrive cold *or* while the app is already foregrounded, and the second
        // case is the common one — the nudge fires while the user is mid-scroll. So the tab is
        // state rather than a value read once, and [onNewIntent] re-points it.
        //
        // Only on a genuinely fresh start, though: after a process-death restore the saved back
        // stack is where the user actually was, and `intent` is still whatever launched the task,
        // which may be a notification they tapped days ago.
        if (savedInstanceState == null) {
            tabRequest = intent.tabExtra()
            intent.shortcutAction()?.let(::handle)
        }

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
                AppRoot(
                    tabRequest = tabRequest,
                    onTabRequestHandled = { tabRequest = null },
                    shortcutRequest = shortcutRequest,
                    onShortcutRequestHandled = { shortcutRequest = null },
                    viewModel = viewModel,
                )
            }
        }
    }

    /**
     * The notification's `FLAG_ACTIVITY_CLEAR_TOP` delivers here rather than recreating the
     * Activity whenever the task is already up, so without this the tab extra was simply dropped
     * and the reminder landed on whatever screen happened to be showing. A shortcut lands here for
     * a different reason: `@xml/shortcuts` cannot set intent flags, so `singleTop` in the manifest
     * is what stops a second MainActivity stacking on the first.
     *
     * Writing to [tabRequest] is what moves the app: `AppScaffold` watches it, switches tabs,
     * and clears it — so a second notification for a tab the user has since navigated away from
     * still lands, which a plain non-null value could not do.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.tabExtra()?.let { tabRequest = it }
        intent.shortcutAction()?.let(::handle)
    }

    /**
     * Everything but water is a destination and goes to `AppScaffold`; water is a *write*,
     * and runs the shared [addGlass] the widget's button and the watch's already run — count and
     * goal re-read there, so a launcher tap cannot add a glass against a stale count or past the
     * goal. Home follows it so the water card shows the new count.
     */
    private fun handle(action: ShortcutAction) {
        if (action == ShortcutAction.AddWater) {
            lifecycleScope.launch { addGlass(waterRepository, profileRepository) }
            tabRequest = TopLevelDestination.Home
        } else {
            shortcutRequest = action
        }
    }
}

/** Null when the intent carries no tab — an ordinary launcher tap, which must not re-point a
 * running app. */
private fun Intent?.tabExtra(): TopLevelDestination? = this?.getStringExtra(EXTRA_TAB)
    ?.let { name -> TopLevelDestination.entries.firstOrNull { it.name == name } }

/** [tabExtra]'s twin for a shortcut. The resolution is [shortcutActionOf] rather than inline so a
 * JVM test can drive it — `Intent` is not on a unit test's classpath in any usable form. */
private fun Intent?.shortcutAction(): ShortcutAction? =
    shortcutActionOf(this?.getStringExtra(EXTRA_ACTION)) ?: this?.healthRationale()

/**
 * Health Connect's rationale tap, on both paths: the `SHOW_PERMISSIONS_RATIONALE` action on
 * Android 13 and below, and the permission-usage action the `activity-alias` catches on 14+. It is
 * an action rather than an extra because the system composes the intent, not `@xml/shortcuts` —
 * which is the whole reason it can't simply be another [EXTRA_ACTION] value.
 */
private fun Intent.healthRationale(): ShortcutAction? = when (action) {
    "androidx.health.connect.action.SHOW_PERMISSIONS_RATIONALE",
    "android.intent.action.VIEW_PERMISSION_USAGE",
    -> ShortcutAction.HealthSync

    else -> null
}
