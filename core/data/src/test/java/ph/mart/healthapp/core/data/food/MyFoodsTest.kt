package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MyFoodsTest {

    private val bar = ScannedProduct("Protein bar", 1.0, "bar", 200, 20, 22, 7)
    private val adobo = ScannedProduct("Mom's adobo", 1.0, "bowl", 450, 30, 12, 28)

    private fun estimate(name: String, amount: Double, unit: String) = RecognizedFood(
        name = name,
        portionAmount = amount,
        portionUnit = unit,
        calories = 999,
        proteinG = 1,
        carbsG = 1,
        fatG = 1,
        confidence = RecognitionConfidence.High,
    )

    /** The offline path's own word rule — plural and word-start — so both paths offer the same foods. */
    @Test
    fun `the saved foods a sentence names are the candidates`() {
        assertEquals(listOf(bar), listOf(bar, adobo).namedIn("had two protein bars after the gym"))
        assertEquals(emptyList<ScannedProduct>(), listOf(bar, adobo).namedIn("a banana"))
    }

    @Test
    fun `no candidates means no extra prompt line`() {
        assertNull(myFoodsLine(emptyList()))
        assertTrue(myFoodsLine(listOf(bar))!!.contains("\"Protein bar\" (bar)"))
    }

    /** The model's estimate loses to the label: its portion is kept, its figures are not. */
    @Test
    fun `a saved food in its own unit is repriced to the portion`() {
        val food = listOf(estimate("protein bar", 2.0, "bar")).preferMyFoods(listOf(bar)).single()

        assertEquals("Protein bar", food.name)
        assertEquals(2.0, food.portionAmount, 0.0)
        assertEquals(400, food.calories)
        assertEquals(40, food.proteinG)
        assertEquals(RecognitionConfidence.High, food.confidence)
    }

    /** No honest conversion from grams to bars, so the saved serving stands and the review tags it. */
    @Test
    fun `a saved food in another unit keeps its own serving and goes low`() {
        val food = listOf(estimate("Protein bar", 60.0, "g")).preferMyFoods(listOf(bar)).single()

        assertEquals(1.0, food.portionAmount, 0.0)
        assertEquals("bar", food.portionUnit)
        assertEquals(200, food.calories)
        assertEquals(RecognitionConfidence.Low, food.confidence)
    }

    @Test
    fun `a food that is not saved passes untouched`() {
        val rice = estimate("White rice", 1.0, "cup")

        assertEquals(listOf(rice), listOf(rice).preferMyFoods(listOf(bar, adobo)))
    }
}
