package ph.mart.healthapp.feature.food.ui.quicklog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.QuickLogTurn
import ph.mart.healthapp.core.data.food.RecognitionConfidence
import ph.mart.healthapp.core.data.food.RecognizedFood
import ph.mart.healthapp.feature.food.R

/** What "Ask coach" carries — and when there is nothing to carry, that the door is not there — and
 * which part of the conversation is drawn as a thread, which as the "You said" line. */
class QuickLogStateTest {

    private fun state(text: String = "") = QuickLogState(text = text, mealType = MealType.Lunch)

    @Test
    fun `a blank start offers no door, even with words typed`() {
        assertNull(state(text = "two eggs").coachQuestion)
    }

    @Test
    fun `mid-conversation it carries every user turn and the field, never the model's question`() {
        val state = state(text = "and a coffee").apply {
            turns = listOf(
                QuickLogTurn(fromUser = true, text = "went for a run"),
                QuickLogTurn(fromUser = false, text = "How long did it last?"),
                QuickLogTurn(fromUser = true, text = "30 minutes"),
            )
        }
        assertEquals("went for a run, 30 minutes, and a coffee", state.coachQuestion)
    }

    @Test
    fun `a photo sent alone has no words to carry`() {
        val state = state().apply { turns = listOf(QuickLogTurn(fromUser = true, text = "")) }
        assertNull(state.coachQuestion)
    }

    @Test
    fun `a first send that came to nothing still offers the words it handed back`() {
        val state = state(text = "what should I eat tonight")
        state.send()
        state.restoreLast(R.string.food_quick_nothing)
        assertEquals("what should I eat tonight", state.coachQuestion)
    }

    private val toast = RecognizedFood("Toast", 1.0, "slice", 80, 4, 14, 1, confidence = RecognitionConfidence.High)

    /** Asked once, answered, then parsed: the state every review test starts from. */
    private fun reviewed() = state(text = "rice and adobo").apply {
        send()
        applyQuestion("One cup or two?")
        text = "two cups"
        send()
        applyParsed(foods = listOf(toast), exercises = emptyList(), mealType = null)
    }

    @Test
    fun `while asking, the thread is the whole conversation and nothing is collapsed`() {
        val state = state(text = "rice and adobo").apply {
            send()
            applyQuestion("One cup or two?")
        }
        assertEquals(listOf("rice and adobo", "One cup or two?"), state.thread.map { it.text })
        assertNull(state.said)
    }

    @Test
    fun `rows collapse the thread into one said line`() {
        val state = reviewed()
        assertTrue(state.thread.isEmpty())
        assertEquals("rice and adobo · two cups", state.said)
    }

    @Test
    fun `a correction in flight is the only thread, under an unchanged said line`() {
        val state = reviewed().apply {
            text = "make it one cup"
            send()
        }
        assertEquals(listOf("make it one cup"), state.thread.map { it.text })
        assertEquals("rice and adobo · two cups", state.said)
    }

    @Test
    fun `a correction that came to nothing leaves no thread and the rows' said line`() {
        val state = reviewed().apply {
            text = "make it one cup"
            send()
            restoreLast(R.string.food_quick_failed)
        }
        assertTrue(state.thread.isEmpty())
        assertEquals("rice and adobo · two cups", state.said)
        assertEquals("make it one cup", state.text)
    }

    @Test
    fun `a question after a correction brings the whole thread back`() {
        val state = reviewed().apply {
            text = "and some soup"
            send()
            applyQuestion("What kind of soup?")
        }
        assertEquals(5, state.thread.size)
        assertNull(state.said)
    }

    @Test
    fun `removing every row keeps the said line and says the review is empty`() {
        val state = reviewed().apply { removeFood(0) }
        assertEquals("rice and adobo · two cups", state.said)
        assertTrue(state.showRemoved)
        state.message = R.string.food_quick_failed
        assertFalse(state.showRemoved)
    }
}
