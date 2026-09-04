package ph.mart.healthapp.feature.progress.ui

import androidx.compose.foundation.ScrollState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ph.mart.healthapp.core.navigation.route.ProgressRoute
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreen

/** [openRecap] is the weekly recap notification's request, and [onOpenRecapHandled] is how the
 * screen says it has acted on it — the shape `homeEntries(onOpenCoach = …)` already has, inverted:
 * this one is an input rather than a callback, because the recap is an overlay inside this tab and
 * not a route `AppScaffold` could push itself.
 *
 * [scrollState] is hoisted for the usual reason: the FAB's scroll-collapse lives in AppScaffold,
 * which can't see a ScrollState created inside the screen. */
fun EntryProviderScope<NavKey>.progressEntries(
    scrollState: ScrollState,
    openRecap: Boolean = false,
    onOpenRecapHandled: () -> Unit = {},
) {
    entry<ProgressRoute> {
        ProgressScreen(
            scrollState = scrollState,
            openRecap = openRecap,
            onOpenRecapHandled = onOpenRecapHandled,
        )
    }
}
