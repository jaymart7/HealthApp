package ph.mart.healthapp.core.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** Four sanitizers lean on this one function, so every rule it has is checked here rather than
 * four times over — `CoachTest`, `InsightTest`, `MealIdeaTest` and `MealParseTest` each check only
 * that they are wired to it. */
class MarkdownTest {

    @Test
    fun `bold and italics lose their markers and keep their words`() {
        assertEquals("62 g of 150 g", stripMarkdown("**62 g** of *150 g*"))
        assertEquals("plenty", stripMarkdown("***plenty***"))
        assertEquals("plenty of room", stripMarkdown("__plenty__ of _room_"))
        assertEquals("gone", stripMarkdown("~~gone~~"))
    }

    @Test
    fun `inline code loses its backticks`() {
        assertEquals("1,900 kcal", stripMarkdown("`1,900 kcal`"))
    }

    @Test
    fun `a heading keeps its words`() {
        assertEquals("Your day", stripMarkdown("## Your day"))
    }

    @Test
    fun `a blockquote loses its marker`() {
        assertEquals("Keep it steady.", stripMarkdown("> Keep it steady."))
    }

    /** The one format the coach's prompt asks for, so every other bullet becomes it. */
    @Test
    fun `every bullet marker becomes a dash`() {
        assertEquals(
            "- Protein\n- Carbs\n- Fat",
            stripMarkdown("* Protein\n+ Carbs\n• Fat"),
        )
        assertEquals("- Water", stripMarkdown("- Water"))
    }

    /** A person writes `1.` too, so it is left exactly as it is. */
    @Test
    fun `a numbered list is left alone`() {
        assertEquals("1. Eggs\n2. Toast", stripMarkdown("1. Eggs\n2. Toast"))
    }

    @Test
    fun `fences and rules drop out, their content stays`() {
        assertEquals("eggs, 220 kcal", stripMarkdown("```\neggs, 220 kcal\n```"))
        assertEquals("Above\nBelow", stripMarkdown("Above\n---\nBelow"))
    }

    @Test
    fun `a link keeps its text`() {
        assertEquals("the diary", stripMarkdown("[the diary](fitpulse://diary)"))
    }

    /** The three things a stripper this blunt could break, and the reason each marker needs a
     * closing partner hugging a non-space character. */
    @Test
    fun `arithmetic, snake case and an unclosed marker survive`() {
        assertEquals("2 * 3 * 4", stripMarkdown("2 * 3 * 4"))
        assertEquals("chicken_breast_100g", stripMarkdown("chicken_breast_100g"))
        assertEquals("**Prot", stripMarkdown("**Prot"))
    }

    @Test
    fun `plain prose passes through untouched`() {
        val answer = "You're 320 kcal under target, so dinner has room."
        assertEquals(answer, stripMarkdown(answer))
    }
}
