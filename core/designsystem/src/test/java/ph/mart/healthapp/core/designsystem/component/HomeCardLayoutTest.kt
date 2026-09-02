package ph.mart.healthapp.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeCardLayoutTest {

    @Test
    fun `an unset layout is every card in declaration order`() {
        listOf(null, "", "   ").forEach { stored ->
            assertEquals(HomeCard.entries.map { HomeCardSetting(it) }, homeCardLayout(stored))
        }
    }

    @Test
    fun `a layout round-trips through encode and parse`() {
        val layout = listOf(
            HomeCardSetting(HomeCard.Macros),
            HomeCardSetting(HomeCard.Sleep, visible = false),
        ) + HomeCard.entries.filterNot { it == HomeCard.Macros || it == HomeCard.Sleep }
            .map { HomeCardSetting(it) }
        assertEquals(layout, homeCardLayout(encodeHomeCardLayout(layout)))
    }

    @Test
    fun `a name from another build is dropped`() {
        val parsed = homeCardLayout("Macros,Telepathy,-calories,Water")
        assertEquals(listOf(HomeCard.Macros, HomeCard.Water), parsed.take(2).map { it.card })
        assertEquals(HomeCard.entries.size, parsed.size)
    }

    @Test
    fun `a card the string never mentions is appended visible`() {
        // What a saved layout from an older build looks like once a card is added.
        val parsed = homeCardLayout("Macros,-Water")
        assertEquals(HomeCard.entries.size, parsed.size)
        assertEquals(HomeCard.Macros, parsed.first().card)
        assertEquals(HomeCardSetting(HomeCard.Water, visible = false), parsed[1])
        assertTrue(parsed.drop(2).all { it.visible })
    }

    @Test
    fun `a repeated card keeps its first position`() {
        val parsed = homeCardLayout("Water,-Water")
        assertEquals(HomeCardSetting(HomeCard.Water), parsed.first())
        assertEquals(HomeCard.entries.size, parsed.size)
    }
}
