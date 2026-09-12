package ph.mart.healthapp.feature.food.ui.diary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.StrengthSet
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType

/**
 * What "copy a day" actually is, once the sheet and the repositories are taken away: three fields
 * re-stamped. Get any of them wrong and the copy either collides with the row it came from, lands
 * on the day it was copied *from*, or claims a photo and a step count that belong to that day.
 */
class CopyDayTest {

    private fun food(name: String, mealType: MealType, photoPath: String? = null) = FoodEntry(
        id = 7,
        name = name,
        dateEpochDay = SOURCE,
        mealType = mealType,
        portionAmount = 1.0,
        portionUnit = "serving",
        calories = 200,
        proteinG = 10,
        carbsG = 20,
        fatG = 5,
        photoPath = photoPath,
    )

    private val source = CopyDay(
        dateEpochDay = SOURCE,
        entries = listOf(
            food("Oats", MealType.Breakfast),
            food("Adobo", MealType.Lunch, photoPath = "/photos/plate.jpg"),
        ),
        exercise = listOf(
            ExerciseEntry(
                id = 3,
                dateEpochDay = SOURCE,
                type = ExerciseType.Strength,
                minutes = 45,
                burnedKcal = 260,
                steps = 4200,
                sets = listOf(StrengthSet("Bench press", reps = 8, weightKg = 60.0)),
            ),
        ),
        waterGlasses = 6,
    )

    @Test
    fun `copied food lands on the target day as a new row`() {
        val copied = source.foodOnto(TARGET, MealType.entries.toSet())

        assertEquals(listOf("Oats", "Adobo"), copied.map { it.name })
        assertTrue(copied.all { it.id == 0L })
        assertTrue(copied.all { it.dateEpochDay == TARGET })
    }

    /** The plate belongs to the meal it was taken of — the same call the history search's
     * one-row re-log makes. */
    @Test
    fun `a copied row never inherits the source row's photo`() {
        val copied = source.foodOnto(TARGET, setOf(MealType.Lunch))

        assertEquals("Adobo", copied.single().name)
        assertNull(copied.single().photoPath)
    }

    @Test
    fun `an unticked meal contributes nothing`() {
        val copied = source.foodOnto(TARGET, setOf(MealType.Breakfast))

        assertEquals(listOf("Oats"), copied.map { it.name })
    }

    @Test
    fun `nothing ticked copies nothing`() {
        assertTrue(source.foodOnto(TARGET, emptySet()).isEmpty())
    }

    /** Sets ride on the entry, so a copied strength session is still a strength session — and the
     * step count is dropped for `addEntry` to re-estimate, because the watch's own figure was
     * recorded against the day it was walked. */
    @Test
    fun `a copied workout keeps its sets and drops the imported step count`() {
        val copied = source.exerciseOnto(TARGET).single()

        assertEquals(0L, copied.id)
        assertEquals(TARGET, copied.dateEpochDay)
        assertEquals(0, copied.steps)
        assertEquals(260, copied.burnedKcal)
        assertEquals(listOf("Bench press"), copied.sets.map { it.exerciseName })
    }

    @Test
    fun `a day with nothing on it is empty`() {
        assertTrue(CopyDay(dateEpochDay = SOURCE).isEmpty)
        assertTrue(!source.isEmpty)
        // Water alone is still something to bring over.
        assertTrue(!CopyDay(dateEpochDay = SOURCE, waterGlasses = 2).isEmpty)
    }

    private companion object {
        const val SOURCE = 20_000L
        const val TARGET = 20_001L
    }
}
