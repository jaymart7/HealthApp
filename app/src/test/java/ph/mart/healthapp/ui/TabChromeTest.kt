package ph.mart.healthapp.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.navigation.route.FoodRoute
import ph.mart.healthapp.core.navigation.route.HomeRoute
import ph.mart.healthapp.core.navigation.route.ProfileRoute
import ph.mart.healthapp.core.navigation.route.ProgressRoute
import ph.mart.healthapp.feature.coach.ui.CoachRoute
import ph.mart.healthapp.feature.food.ui.BarcodeScanRoute
import ph.mart.healthapp.feature.food.ui.LabelScanRoute
import ph.mart.healthapp.feature.food.ui.MealIdeasRoute
import ph.mart.healthapp.core.data.food.MealIdeaRequest
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.feature.profile.ui.FoodLibraryRoute
import ph.mart.healthapp.feature.profile.ui.HealthConnectionRoute
import ph.mart.healthapp.feature.profile.ui.SettingsRoute
import ph.mart.healthapp.feature.progress.ui.AddPhotoPreviewRoute
import ph.mart.healthapp.feature.progress.ui.AddPhotoRoute
import ph.mart.healthapp.feature.progress.ui.PhotoComparisonRoute
import ph.mart.healthapp.feature.progress.ui.ProgressSubjectRoutes
import ph.mart.healthapp.feature.progress.ui.RecapRoute
import ph.mart.healthapp.feature.progress.ui.TimelapseRoute

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
        assertFalse(showsTabChrome(current = CoachRoute(), beneath = HomeRoute, twoPane = true))
    }

    /** Progress' read-only surfaces stopped being overlays drawn inside the tab and became routes,
     * which is the whole point: a chart or a full-screen viewer wears no bottom bar and no FAB, at
     * either width. Nothing in [showsTabChrome] names them — that is what this pins.
     *
     * It reads [ProgressSubjectRoutes] rather than listing the subject pages, so each conversion
     * commit is covered by the set it already has to edit. The add-photo flow's two routes ride
     * along as the one write flow among them — a viewfinder has as little use for a bottom bar as
     * a viewer does, and neither has the form behind it. */
    @Test
    fun `a Progress viewer route wears no chrome at either width`() {
        val routes = ProgressSubjectRoutes +
            listOf(
                PhotoComparisonRoute(1, 2),
                TimelapseRoute,
                RecapRoute,
                AddPhotoRoute,
                AddPhotoPreviewRoute("/cache/progress_capture_1.jpg"),
            )
        routes.forEach { route ->
            assertFalse(showsTabChrome(current = route, beneath = ProgressRoute, twoPane = false))
            assertFalse(showsTabChrome(current = route, beneath = ProgressRoute, twoPane = true))
        }
    }

    /** The camera flows are full-bleed at every width and dispatch back per capture state, so
     * neither the bar nor the FAB may draw over them. The label scan is pushed from *inside* the
     * barcode flow, which is the one case worth pinning: a route reached from another route above
     * a tab is still not a tab. */
    @Test
    fun `a camera flow wears no chrome, including one pushed from another camera flow`() {
        listOf(BarcodeScanRoute(0), LabelScanRoute(0)).forEach { route ->
            assertFalse(showsTabChrome(current = route, beneath = FoodRoute, twoPane = false))
            assertFalse(showsTabChrome(current = route, beneath = FoodRoute, twoPane = true))
        }
        assertFalse(showsTabChrome(current = LabelScanRoute(0), beneath = BarcodeScanRoute(0), twoPane = true))
    }

    /** Meal ideas stopped being an overlay drawn inside the diary and became a route, which is
     * the whole point of the move: the bar and the FAB it used to be drawn over are gone at either
     * width. Nothing in [showsTabChrome] names it — that is what this pins. */
    @Test
    fun `the meal-ideas route wears no chrome at either width`() {
        val route = MealIdeasRoute(
            MealIdeaRequest(
                goal = Goal.Lose,
                mealType = MealType.Dinner,
                remainingKcal = 640,
                remainingProteinG = 48,
                remainingCarbsG = 70,
                remainingFatG = 20,
                diet = null,
            ),
        )
        assertFalse(showsTabChrome(current = route, beneath = FoodRoute, twoPane = false))
        assertFalse(showsTabChrome(current = route, beneath = FoodRoute, twoPane = true))
    }

    @Test
    fun `an empty stack wears nothing rather than throwing`() {
        assertFalse(showsTabChrome(current = null, beneath = null, twoPane = true))
    }
}
