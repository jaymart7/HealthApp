package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Test
import ph.mart.healthapp.core.data.food.local.SavedMealEntity
import ph.mart.healthapp.core.data.food.local.SavedMealItemEntity

class SavedMealTest {

    private fun meal(id: Long, name: String) = SavedMealEntity(id = id, name = name, createdAt = 0)

    private fun item(id: Long, mealId: Long, name: String, calories: Int = 100) = SavedMealItemEntity(
        id = id,
        mealId = mealId,
        name = name,
        portionAmount = 100.0,
        portionUnit = "g",
        calories = calories,
        proteinG = 10,
        carbsG = 10,
        fatG = 10,
    )

    @Test
    fun `items attach to their own meal, in order`() {
        val grouped = groupSavedMeals(
            meals = listOf(meal(2, "Usual lunch"), meal(1, "Usual breakfast")),
            items = listOf(item(1, 1, "Oats"), item(2, 2, "Rice"), item(3, 1, "Eggs")),
        )
        assertEquals(listOf("Usual lunch", "Usual breakfast"), grouped.map { it.name })
        assertEquals(listOf("Rice"), grouped[0].items.map { it.name })
        assertEquals(listOf("Oats", "Eggs"), grouped[1].items.map { it.name })
    }

    @Test
    fun `items of a meal that is gone are dropped`() {
        val grouped = groupSavedMeals(
            meals = listOf(meal(1, "Usual breakfast")),
            items = listOf(item(1, 1, "Oats"), item(2, 99, "Orphan")),
        )
        assertEquals(1, grouped.size)
        assertEquals(listOf("Oats"), grouped.single().items.map { it.name })
    }

    @Test
    fun `a meal with no items still appears`() {
        val grouped = groupSavedMeals(meals = listOf(meal(1, "Empty")), items = emptyList())
        assertEquals(emptyList<SavedMealItem>(), grouped.single().items)
        assertEquals(0, grouped.single().totalKcal())
    }

    @Test
    fun `total kcal sums the items`() {
        val grouped = groupSavedMeals(
            meals = listOf(meal(1, "Usual breakfast")),
            items = listOf(item(1, 1, "Oats", calories = 150), item(2, 1, "Eggs", calories = 180)),
        )
        assertEquals(330, grouped.single().totalKcal())
    }
}
