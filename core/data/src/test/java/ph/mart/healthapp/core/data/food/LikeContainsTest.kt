package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The half of the diary history search that isn't Room. `FoodEntryDao.searchByName` ends in
 * `ESCAPE '\'`, and this is what feeds it — get the two out of step and a food named "100% oats"
 * or "chicken_breast" turns its own name into a wildcard, which is a search that quietly returns
 * the whole table.
 */
class LikeContainsTest {

    @Test
    fun `a plain term is wrapped and otherwise untouched`() {
        assertEquals("%yogurt%", likeContains("yogurt"))
    }

    @Test
    fun `an empty term matches everything, which is the screen's opening state`() {
        assertEquals("%%", likeContains(""))
    }

    @Test
    fun `the three LIKE syntax characters are escaped, backslash first`() {
        assertEquals("%100\\% oats%", likeContains("100% oats"))
        assertEquals("%chicken\\_breast%", likeContains("chicken_breast"))
        // Backslash is replaced before the wildcards are, or the backslash this very escaping
        // adds would itself be escaped a second time.
        assertEquals("%a\\\\b%", likeContains("a\\b"))
        assertEquals("%\\\\\\%%", likeContains("\\%"))
    }
}
