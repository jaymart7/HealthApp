package ph.mart.healthapp.feature.home.ui

import androidx.compose.foundation.ScrollState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ph.mart.healthapp.core.navigation.route.HomeRoute

/** [onAddPhoto] is how the photo-reminder CTA reaches the Add photo sheet without this module
 * importing `:feature:progress` — AppScaffold already hosts that sheet, so it wires the callback
 * (same shape as `foodEntries(onExitCapture = ...)`).
 *
 * [onOpenCoach] is wired the same way, and for a second reason: `CoachRoute` lives in
 * `:feature:coach`, which this module must not import. [onStartRoutine] is a third of the same
 * shape — the training-plan card starts a workout on `StrengthWorkoutRoute`, which lives in
 * `:feature:food`. [onOpenHomeLayout] is a fourth: the "Rearrange your Home" link at the foot of
 * the card block opens Profile's `HomeLayoutRoute`, and `:feature:home` cannot import
 * `:feature:profile` either.
 *
 * [scrollState] is hoisted for the same reason: the FAB's scroll-collapse and tap-active-tab-to-
 * scroll-to-top both live in AppScaffold, which can't see a ScrollState created inside the screen. */
fun EntryProviderScope<NavKey>.homeEntries(
    scrollState: ScrollState,
    onAddPhoto: () -> Unit,
    onOpenCoach: () -> Unit,
    onStartRoutine: (Long) -> Unit,
    onOpenHomeLayout: () -> Unit,
) {
    entry<HomeRoute> {
        HomeScreen(
            onAddPhoto = onAddPhoto,
            onOpenCoach = onOpenCoach,
            onStartRoutine = onStartRoutine,
            onOpenHomeLayout = onOpenHomeLayout,
            scrollState = scrollState,
        )
    }
}
