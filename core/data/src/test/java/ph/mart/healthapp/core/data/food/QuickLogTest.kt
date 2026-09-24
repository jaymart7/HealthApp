package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.ParsedExercise

class QuickLogTest {

    private fun food(name: String, calories: Int) = RecognizedFood(
        name = name,
        portionAmount = 1.0,
        portionUnit = "serving",
        calories = calories,
        proteinG = 0,
        carbsG = 0,
        fatG = 0,
        confidence = RecognitionConfidence.High,
    )

    private val run = ParsedExercise(type = ExerciseType.Run, name = "", minutes = 30)

    @Test
    fun `a question is asked while the model may still ask`() {
        val result = quickLogResult("How much rice?", emptyList(), emptyList(), mayAsk = true)

        assertEquals(QuickLogResult.Question("How much rice?"), result)
    }

    /** Past the cap the model was told to estimate; one that asks anyway gets its lists read. */
    @Test
    fun `a question past the cap falls through to the parse`() {
        val result = quickLogResult("How much rice?", listOf(food("Rice", 200)), emptyList(), mayAsk = false)

        assertEquals(QuickLogResult.Parsed(listOf(food("Rice", 200)), emptyList()), result)
    }

    @Test
    fun `a blank question is no question`() {
        val result = quickLogResult("  ", emptyList(), listOf(run), mayAsk = true)

        assertEquals(QuickLogResult.Parsed(emptyList(), listOf(run)), result)
    }

    @Test
    fun `markdown is stripped out of a question and it is capped`() {
        val asked = quickLogResult("**How long?** " + "x".repeat(200), emptyList(), emptyList(), mayAsk = true)

        asked as QuickLogResult.Question
        assertTrue(asked.text.startsWith("How long?"))
        assertTrue(asked.text.length <= MAX_QUESTION_CHARS)
    }

    @Test
    fun `nothing edible and nothing done is nothing found`() {
        val result = quickLogResult(null, listOf(food("Black coffee", 0)), listOf(null), mayAsk = true)

        assertEquals(QuickLogResult.NothingFound, result)
    }

    @Test
    fun `unloggable foods and rejected activities are dropped`() {
        val result = quickLogResult(null, listOf(food("  ", 90), food("Toast", 80)), listOf(null, run), mayAsk = true)

        assertEquals(QuickLogResult.Parsed(listOf(food("Toast", 80)), listOf(run)), result)
    }

    @Test
    fun `the model may ask twice and no more`() {
        val sentence = QuickLogTurn(fromUser = true, text = "rice")
        val asked = QuickLogTurn(fromUser = false, text = "How much?")
        val answer = QuickLogTurn(fromUser = true, text = "some")

        assertTrue(listOf(sentence).mayAsk())
        assertTrue(listOf(sentence, asked, answer).mayAsk())
        assertFalse(listOf(sentence, asked, answer, asked, answer).mayAsk())
    }
}
