package ph.mart.healthapp.feature.food.ui.quicklog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.QuickLogTurn
import ph.mart.healthapp.feature.food.R

/** What "Ask coach" carries — and when there is nothing to carry, that the door is not there. */
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
}
