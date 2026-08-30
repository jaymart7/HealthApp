package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Test

class BarcodeLookupTest {

    @Test
    fun `parses a found product with kcal and macros`() {
        val body = """
            {"totalHits":1,"foods":[{"gtinUpc":"028400642255","dataType":"Branded",
            "description":"SPICY SWEET CHILI FLAVORED TORTILLA CHIPS","brandName":"DORITOS",
            "foodNutrients":[{"nutrientId":1008,"value":536},{"nutrientId":1003,"value":7.14},
            {"nutrientId":1005,"value":64.3},{"nutrientId":1004,"value":25.0}]}]}
        """.trimIndent()

        val result = parseFdcProduct(body, "028400642255")

        assertEquals(
            BarcodeLookupResult.Found(
                ScannedProduct(
                    name = "Doritos · Spicy Sweet Chili Flavored Tortilla Chips",
                    portionAmount = 100.0,
                    portionUnit = "g",
                    calories = 536,
                    proteinG = 7,
                    carbsG = 64,
                    fatG = 25,
                ),
            ),
            result,
        )
    }

    @Test
    fun `a code FDC does not stock is not found, however many hits it fuzzy-matched`() {
        // What a code FDC cannot tokenize really returns: it falls back to relevance and hands back
        // the top of the branded database. Without the gtinUpc check this would log the nuggets.
        val body = """
            {"totalHits":433403,"foods":[
              {"gtinUpc":"0099447210127","description":"CHICKEN NUGGETS",
               "foodNutrients":[{"nutrientId":1008,"value":220}]},
              {"gtinUpc":"0852501006001","description":"FETA CHEESE",
               "foodNutrients":[{"nutrientId":1008,"value":264}]}
            ]}
        """.trimIndent()

        assertEquals(BarcodeLookupResult.NotFound, parseFdcProduct(body, "3017620422003"))
    }

    @Test
    fun `leading zeros do not decide the match`() {
        val body = """
            {"foods":[{"gtinUpc":"0099447210127","description":"Chicken nuggets",
             "foodNutrients":[{"nutrientId":1008,"value":220}]}]}
        """.trimIndent()

        val product = (parseFdcProduct(body, "99447210127") as BarcodeLookupResult.Found).product

        assertEquals("Chicken nuggets", product.name)
    }

    @Test
    fun `falls back to kilojoules when kcal is missing`() {
        val body = """
            {"foods":[{"gtinUpc":"028400642255","description":"Oat bar",
             "foodNutrients":[{"nutrientId":1062,"value":1600}]}]}
        """.trimIndent()

        val product = (parseFdcProduct(body, "028400642255") as BarcodeLookupResult.Found).product

        // 1600 kJ / 4.184
        assertEquals(382, product.calories)
        assertEquals(0, product.proteinG)
    }

    @Test
    fun `reads nutrient values that come back quoted`() {
        val body = """
            {"foods":[{"gtinUpc":"028400642255","description":"Rice",
             "foodNutrients":[{"nutrientId":1008,"value":"130"},{"nutrientId":1003,"value":"2.7"}]}]}
        """.trimIndent()

        val product = (parseFdcProduct(body, "028400642255") as BarcodeLookupResult.Found).product

        assertEquals(130, product.calories)
        assertEquals(3, product.proteinG)
    }

    @Test
    fun `a miss is not found`() {
        assertEquals(
            BarcodeLookupResult.NotFound,
            parseFdcProduct("""{"totalHits":0,"foods":[]}""", "0000000000000"),
        )
    }

    @Test
    fun `a nameless product is not found`() {
        val body = """
            {"foods":[{"gtinUpc":"028400642255","description":"  ",
             "foodNutrients":[{"nutrientId":1008,"value":200}]}]}
        """.trimIndent()

        assertEquals(BarcodeLookupResult.NotFound, parseFdcProduct(body, "028400642255"))
    }

    @Test
    fun `an unparseable body fails rather than throwing`() {
        assertEquals(BarcodeLookupResult.Failed, parseFdcProduct("<html>502</html>", "028400642255"))
    }
}
