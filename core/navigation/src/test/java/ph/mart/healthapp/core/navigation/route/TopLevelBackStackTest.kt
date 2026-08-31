package ph.mart.healthapp.core.navigation.route

import androidx.compose.runtime.mutableStateListOf
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Test

/** Stands in for a real sub-route (`RecipeBuilderRoute`, `BarcodeScanRoute`, …). Those live in
 * `:feature:*`, which this leaf module can't see — and all this class cares about is that a key
 * is *not* one of the four tab roots. */
@Serializable
private data class SubRoute(val id: Int) : NavKey

/**
 * The grouping is derived from the flat list rather than saved beside it, so a restore has to
 * reconstruct it exactly. That derivation is the whole of what this guards — get it wrong and a
 * rotation silently reshuffles which tab owns which screen.
 */
class TopLevelBackStackTest {

    private fun stackOf(vararg keys: NavKey) = TopLevelBackStack(mutableStateListOf(*keys))

    @Test
    fun `a flat stack regroups into the same flat stack`() {
        val restored = listOf(
            TopLevelDestination.Home.route,
            TopLevelDestination.Profile.route,
            SubRoute(1),
            SubRoute(2),
        )
        val stack = TopLevelBackStack(mutableStateListOf(*restored.toTypedArray()))

        assertEquals(restored, stack.backStack.toList())
        assertEquals(TopLevelDestination.Profile.route, stack.topLevelKey)
    }

    @Test
    fun `back from a restored sub-route returns to its own tab, not the start tab`() {
        val stack = stackOf(
            TopLevelDestination.Home.route,
            TopLevelDestination.Profile.route,
            SubRoute(1),
        )

        stack.removeLast()

        assertEquals(TopLevelDestination.Profile.route, stack.topLevelKey)
        assertEquals(
            listOf(TopLevelDestination.Home.route, TopLevelDestination.Profile.route),
            stack.backStack.toList(),
        )
    }

    @Test
    fun `switching tabs keeps each tab's own history`() {
        val stack = stackOf(TopLevelDestination.Home.route)

        stack.addTopLevel(TopLevelDestination.Food.route)
        stack.add(SubRoute(7))
        stack.addTopLevel(TopLevelDestination.Progress.route)
        stack.addTopLevel(TopLevelDestination.Food.route)

        // Food is current again, still holding its sub-route, and Progress kept its place.
        assertEquals(TopLevelDestination.Food.route, stack.topLevelKey)
        assertEquals(
            listOf(
                TopLevelDestination.Home.route,
                TopLevelDestination.Progress.route,
                TopLevelDestination.Food.route,
                SubRoute(7),
            ),
            stack.backStack.toList(),
        )
    }

    /** Only reachable from a mangled `Bundle`. Starting over on Home beats throwing on the empty
     * map `topLevelKey` would otherwise read. */
    @Test
    fun `a restored stack with no tab root falls back to Home`() {
        val stack = stackOf(SubRoute(1), SubRoute(2))

        assertEquals(TopLevelDestination.Home.route, stack.topLevelKey)
        assertEquals(listOf(TopLevelDestination.Home.route), stack.backStack.toList())
    }
}
