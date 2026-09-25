package ph.mart.healthapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import ph.mart.healthapp.core.data.coach.CoachScreen
import ph.mart.healthapp.core.data.recap.ReportSection
import ph.mart.healthapp.core.navigation.route.TopLevelDestination
import ph.mart.healthapp.feature.progress.ui.progress.Subject
import ph.mart.healthapp.feature.progress.ui.route

/** Every name the coach may hand out has somewhere to go — a name that maps to nothing is a card
 * whose "Open it" does nothing. */
class CoachScreenRouteTest {

    @Test
    fun `every coach screen and report section maps to a route`() {
        (CoachScreen.entries.map { it.name } + ReportSection.entries.map { it.name }).forEach {
            assertNotNull(it, coachScreenRoute(it))
        }
    }

    @Test
    fun `the names that differ from their destination`() {
        assertEquals(TopLevelDestination.Food.route, coachScreenRoute(CoachScreen.Diary.name))
        assertEquals(Subject.Activity.route(), coachScreenRoute(ReportSection.Steps.name))
        assertEquals(Subject.Strength.route(), coachScreenRoute(ReportSection.Training.name))
    }
}
