package ph.mart.healthapp.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.navigation.route.HomeRoute
import ph.mart.healthapp.core.navigation.route.ProfileRoute
import ph.mart.healthapp.core.navigation.route.ProgressRoute
import ph.mart.healthapp.feature.coach.ui.CoachRoute
import ph.mart.healthapp.feature.profile.ui.FoodLibraryRoute
import ph.mart.healthapp.feature.profile.ui.HealthConnectionRoute
import ph.mart.healthapp.feature.profile.ui.SettingsRoute

class TabChromeTest {

    @Test
    fun `a tab always wears the chrome, at either width`() {
        assertTrue(showsTabChrome(current = HomeRoute, beneath = null, twoPane = false))
        assertTrue(showsTabChrome(current = ProgressRoute, beneath = HomeRoute, twoPane = true))
    }

    @Test
    fun `a Profile detail keeps the chrome only once there is room to draw it beside Profile`() {
        assertFalse(showsTabChrome(current = FoodLibraryRoute, beneath = ProfileRoute, twoPane = false))
        assertTrue(showsTabChrome(current = FoodLibraryRoute, beneath = ProfileRoute, twoPane = true))
    }

    /** The three routes the redesign added are panes like the five that predate them — Settings is
     * the one reached from the tab's own gear, so it is the one this pins. */
    @Test
    fun `a route added by the Settings split is a Profile detail like the rest`() {
        assertFalse(showsTabChrome(current = SettingsRoute, beneath = ProfileRoute, twoPane = false))
        assertTrue(showsTabChrome(current = SettingsRoute, beneath = ProfileRoute, twoPane = true))
    }

    /** Health Connect's rationale intent pushes onto whichever tab is showing. With no Profile
     * under it there is no list pane, so the scene never forms and neither does the chrome. */
    @Test
    fun `the same route pushed onto another tab stays single-pane`() {
        assertFalse(showsTabChrome(current = HealthConnectionRoute, beneath = HomeRoute, twoPane = true))
    }

    @Test
    fun `a route that is not a Profile detail never earns a pane`() {
        assertFalse(showsTabChrome(current = CoachRoute, beneath = HomeRoute, twoPane = true))
    }

    @Test
    fun `an empty stack wears nothing rather than throwing`() {
        assertFalse(showsTabChrome(current = null, beneath = null, twoPane = true))
    }
}
