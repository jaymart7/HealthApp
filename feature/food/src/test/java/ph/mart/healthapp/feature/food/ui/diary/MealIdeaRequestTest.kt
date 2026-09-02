package ph.mart.healthapp.feature.food.ui.diary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.Goal

/** What the meal-ideas screen is allowed to ask, and when the button that opens it is there at
 * all. The budget below is the summary bar's own arithmetic — if the two ever disagree, the screen
 * offers calories the bar above it says are spent. */
class MealIdeaRequestTest {

    private val targets = DailyTargets(calories = 2000, proteinG = 150, carbsG = 200, fatG = 65, floor = 1500)

    private fun state(
        entries: List<FoodEntry> = emptyList(),
        exercise: List<ExerciseEntry> = emptyList(),
        addExerciseToBudget: Boolean = true,
        goal: Goal? = Goal.Lose,
        targets: DailyTargets? = this.targets,
    ) = FoodUiState(
        entries = entries,
        exercise = exercise,
        addExerciseToBudget = addExerciseToBudget,
        targets = targets,
        goal = goal,
    )

    private fun entry(calories: Int, proteinG: Int = 0) = FoodEntry(
        name = "Something",
        mealType = MealType.Lunch,
        portionAmount = 1.0,
        portionUnit = "serving",
        calories = calories,
        proteinG = proteinG,
        carbsG = 0,
        fatG = 0,
    )

    private fun workout(burnedKcal: Int) = ExerciseEntry(
        type = ExerciseType.Run,
        minutes = 30,
        burnedKcal = burnedKcal,
    )

    @Test
    fun `the gap is what is left of the day, macros included`() {
        val request = state(entries = listOf(entry(calories = 1400, proteinG = 90)))
            .mealIdeaRequest(MealType.Dinner)

        assertNotNull(request)
        assertEquals(600, request!!.remainingKcal)
        assertEquals(60, request.remainingProteinG)
        assertEquals(MealType.Dinner, request.mealType)
    }

    /** The same credit `budgetKcal()` gives the summary bar, and the same switch turns it off. */
    @Test
    fun `a workout raises what there is to spend, unless the user opted out`() {
        val entries = listOf(entry(calories = 1400))

        assertEquals(
            900,
            state(entries = entries, exercise = listOf(workout(300))).mealIdeaRequest(MealType.Dinner)?.remainingKcal,
        )
        assertEquals(
            600,
            state(entries = entries, exercise = listOf(workout(300)), addExerciseToBudget = false)
                .mealIdeaRequest(MealType.Dinner)?.remainingKcal,
        )
    }

    @Test
    fun `no profile means no gap to describe`() {
        assertNull(state(targets = null).mealIdeaRequest(MealType.Lunch))
        assertNull(state(goal = null).mealIdeaRequest(MealType.Lunch))
    }

    /** A day with 40 kcal left has no meal in it, and one already over target has less than that. */
    @Test
    fun `a spent day offers nothing`() {
        assertNull(state(entries = listOf(entry(calories = 1960))).mealIdeaRequest(MealType.Snacks))
        assertNull(state(entries = listOf(entry(calories = 2400))).mealIdeaRequest(MealType.Snacks))
    }
}
