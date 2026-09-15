package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * One photographed plate is several diary rows, and only the first of them carries the picture.
 * The prune is why — it counts rows against `MAX_MEAL_PHOTOS` and deletes their files, so a path
 * repeated across a batch is a file deleted out from under the rows that still point at it.
 */
class MealPhotoAttachTest {

    private fun entry(name: String) = FoodEntry(
        name = name,
        mealType = MealType.Lunch,
        portionAmount = 1.0,
        portionUnit = "serving",
        calories = 200,
        proteinG = 10,
        carbsG = 20,
        fatG = 5,
    )

    private val plate = listOf(entry("Chicken"), entry("Rice"), entry("Greens"))

    @Test
    fun `the path lands on the first entry only`() {
        val stamped = plate.withPhotoOnFirst("/files/meal_photos/a.jpg")

        assertEquals("/files/meal_photos/a.jpg", stamped[0].photoPath)
        assertNull(stamped[1].photoPath)
        assertNull(stamped[2].photoPath)
    }

    @Test
    fun `nothing else about the entries moves`() {
        val stamped = plate.withPhotoOnFirst("/files/meal_photos/a.jpg")

        assertEquals(plate.map { it.name }, stamped.map { it.name })
        assertEquals(plate.map { it.calories }, stamped.map { it.calories })
    }

    @Test
    fun `a null path is a no-op`() {
        assertSame(plate, plate.withPhotoOnFirst(null))
    }

    @Test
    fun `an empty batch is safe`() {
        assertEquals(emptyList<FoodEntry>(), emptyList<FoodEntry>().withPhotoOnFirst("/a.jpg"))
    }

    @Test
    fun `a single-entry batch still gets it`() {
        val one = listOf(entry("Quick add")).withPhotoOnFirst("/a.jpg")

        assertEquals("/a.jpg", one.single().photoPath)
    }
}
