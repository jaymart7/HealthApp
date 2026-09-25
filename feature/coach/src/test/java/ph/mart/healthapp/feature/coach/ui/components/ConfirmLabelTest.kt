package ph.mart.healthapp.feature.coach.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ph.mart.healthapp.core.data.coach.CoachAction
import ph.mart.healthapp.core.data.coach.CoachScreen
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.feature.coach.R

/**
 * What the draft card's confirm says, and what it counts.
 *
 * The verb half is the cheap half — it is a `when` over three kinds. The **count** is the one worth
 * a test: the card's whole promise is that what it shows is what gets written, and it recounts the
 * title, the total and the macro legend on every `✕`. A button still reading "Log 4 items" over
 * two surviving rows is the single place on that card where a stale figure costs the user a row
 * they thought they had removed.
 */
class ConfirmLabelTest {

    private fun food(name: String) = CoachAction.LogFood(
        name = name,
        mealType = MealType.Breakfast,
        calories = 100,
        proteinG = 1,
        carbsG = 1,
        fatG = 1,
        portionAmount = 1.0,
        portionUnit = "serving",
    )

    @Test
    fun `logging is the default verb`() {
        assertEquals(R.string.coach_proposal_confirm, confirmLabelFor(food("Eggs")))
        assertEquals(R.string.coach_proposal_confirm, confirmLabelFor(CoachAction.LogWater(glasses = 1)))
        assertEquals(
            R.string.coach_proposal_confirm,
            confirmLabelFor(
                CoachAction.LogExercise(type = ExerciseType.Run, name = "Run", minutes = 30, burnedKcal = 300),
            ),
        )
    }

    /** The two that do not log. A routine opens a form and writes nothing; a fast flips a timer. */
    @Test
    fun `a routine starts and a fast says which end it is`() {
        assertEquals(
            R.string.coach_proposal_start,
            confirmLabelFor(CoachAction.StartRoutine(name = "Push day", routineId = 1, lifts = emptyList())),
        )
        assertEquals(
            R.string.coach_proposal_start,
            confirmLabelFor(CoachAction.SetFast(ending = false, goalHours = 16)),
        )
        assertEquals(
            R.string.coach_proposal_end,
            confirmLabelFor(CoachAction.SetFast(ending = true, goalHours = 16, elapsedMinutes = 900)),
        )
    }

    /** A multi-row draft has no single action, so the verb falls back to logging — which is right:
     * several rows are always foods. */
    @Test
    fun `a multi-row draft logs`() {
        assertEquals(R.string.coach_proposal_confirm, confirmLabelFor(null))
    }

    /** A change says what it does to a row already there; a library item is saved, not logged. */
    @Test
    fun `a change updates or removes, and a library item saves`() {
        assertEquals(R.string.coach_proposal_update, confirmLabelFor(CoachAction.SetWater(glasses = 5)))
        assertEquals(R.string.coach_proposal_remove_confirm, confirmLabelFor(CoachAction.DeleteFood(entryId = 1)))
        assertEquals(R.string.coach_proposal_save, confirmLabelFor(CoachAction.SaveMeal("Lunch", emptyList())))
        assertEquals(R.string.coach_proposal_open, confirmLabelFor(CoachAction.OpenScreen(CoachScreen.Sleep)))
    }

    @Test
    fun `a draft of one never counts`() {
        assertNull(confirmCountFor(kept = 1, drafted = 1))
    }

    @Test
    fun `a draft of several counts what is left`() {
        assertEquals(4, confirmCountFor(kept = 4, drafted = 4))
        assertEquals(2, confirmCountFor(kept = 2, drafted = 4))
    }

    /** Down to one from four still counts: the user took rows out, and the number is what
     * acknowledges it. */
    @Test
    fun `a draft worn down to one still counts`() {
        assertEquals(1, confirmCountFor(kept = 1, drafted = 4))
    }

    /** Nothing left is not a count of zero — the button is disabled and says its own thing. */
    @Test
    fun `an emptied draft has no count`() {
        assertNull(confirmCountFor(kept = 0, drafted = 4))
    }
}
