package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Test

class BarcodeLookupTest {

    @Test
    fun `parses a found product with kcal and macros`() {
        val body = """
            {"code":"3017620422003","status":1,"product":{"product_name":"Nutella",
            "nutriments":{"energy-kcal_100g":539,"proteins_100g":6.3,"carbohydrates_100g":57.5,"fat_100g":30.9}}}
        """.trimIndent()

        val result = parseOpenFoodFactsProduct(body)

        assertEquals(
            BarcodeLookupResult.Found(
                ScannedProduct(
                    name = "Nutella",
                    portionAmount = 100.0,
                    portionUnit = "g",
                    calories = 539,
                    proteinG = 6,
                    carbsG = 58,
                    fatG = 31,
                ),
            ),
            result,
        )
    }

    @Test
    fun `falls back to kilojoules when kcal is missing`() {
        val body = """{"status":1,"product":{"product_name":"Oat bar","nutriments":{"energy_100g":1600}}}"""

        val product = (parseOpenFoodFactsProduct(body) as BarcodeLookupResult.Found).product

        // 1600 kJ / 4.184
        assertEquals(382, product.calories)
        assertEquals(0, product.proteinG)
    }

    @Test
    fun `reads nutriments that come back quoted`() {
        val body = """{"status":1,"product":{"product_name":"Rice","nutriments":{"energy-kcal_100g":"130","proteins_100g":"2.7"}}}"""

        val product = (parseOpenFoodFactsProduct(body) as BarcodeLookupResult.Found).product

        assertEquals(130, product.calories)
        assertEquals(3, product.proteinG)
    }

    @Test
    fun `a miss is not found`() {
        val body = """{"code":"0000000000000","status":0,"status_verbose":"product not found"}"""

        assertEquals(BarcodeLookupResult.NotFound, parseOpenFoodFactsProduct(body))
    }

    @Test
    fun `a nameless product is not found`() {
        val body = """{"status":1,"product":{"product_name":"  ","nutriments":{"energy-kcal_100g":200}}}"""

        assertEquals(BarcodeLookupResult.NotFound, parseOpenFoodFactsProduct(body))
    }

    @Test
    fun `an unparseable body fails rather than throwing`() {
        assertEquals(BarcodeLookupResult.Failed, parseOpenFoodFactsProduct("<html>502</html>"))
    }
}
