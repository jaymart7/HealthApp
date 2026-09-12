package ph.mart.healthapp.feature.progress.ui

import androidx.compose.foundation.ScrollState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import ph.mart.healthapp.core.navigation.route.ProgressRoute
import ph.mart.healthapp.feature.progress.ui.comparison.PhotoComparisonScreen
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreen
import ph.mart.healthapp.feature.progress.ui.recap.RecapScreen
import ph.mart.healthapp.feature.progress.ui.timelapse.TimelapseScreen

/** Two progress photos read against each other. Carries the grid's selection, and the order of the
 * two ids does not matter — `comparisonPair()` sorts by date, so the route names a pair rather than
 * a before and an after. */
@Serializable
data class PhotoComparisonRoute(val firstId: Long, val secondId: Long) : NavKey

/** Every progress photo played in date order — the whole-set answer to [PhotoComparisonRoute]'s
 * two-photo one. Carries nothing: the player reads the set itself. */
@Serializable
data object TimelapseRoute : NavKey

/** The period in one page. Carries nothing — `RecapViewModel` owns the period, which is what lets
 * the weekly notification and the overview's own icon push the same route. */
@Serializable
data object RecapRoute : NavKey

/**
 * [scrollState] is hoisted for the usual reason: the FAB's scroll-collapse lives in AppScaffold,
 * which can't see a ScrollState created inside the screen. [twoPane] comes from there too: that is
 * the one place in the app that reads the window's width, so this tab is told rather than asking —
 * which is also why `:feature:progress` needs no adaptive dependency of its own.
 *
 * The three read-only surfaces are routes rather than overlays drawn inside [ProgressRoute], so
 * none of them wears the bottom bar or the FAB and none wires a back handler of its own. What the
 * tab hands up is the tap: [onCompare] carries the two photos the grid picked, [onOpenTimelapse]
 * and [onOpenRecap] carry nothing.
 */
fun EntryProviderScope<NavKey>.progressEntries(
    scrollState: ScrollState,
    twoPane: Boolean = false,
    onCompare: (Long, Long) -> Unit,
    onOpenTimelapse: () -> Unit,
    onOpenRecap: () -> Unit,
    onExitFlow: () -> Unit,
) {
    entry<ProgressRoute> {
        ProgressScreen(
            scrollState = scrollState,
            twoPane = twoPane,
            onCompare = onCompare,
            onOpenTimelapse = onOpenTimelapse,
            onOpenRecap = onOpenRecap,
        )
    }
    entry<PhotoComparisonRoute> { key ->
        PhotoComparisonScreen(selectedIds = listOf(key.firstId, key.secondId))
    }
    entry<TimelapseRoute> { TimelapseScreen() }
    entry<RecapRoute> { RecapScreen(onExitFlow = onExitFlow) }
}
