package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodSearchTest {

    @Test
    fun `maps every usable hit`() {
        val body = """
            {"count":2,"products":[
              {"product_name":"Greek yogurt","nutriments":{"energy-kcal_100g":97,"proteins_100g":9,"carbohydrates_100g":3.6,"fat_100g":5}},
              {"product_name":"Greek yogurt, plain","nutriments":{"energy-kcal_100g":59,"proteins_100g":10.3,"carbohydrates_100g":3.6,"fat_100g":0.4}}
            ]}
        """.trimIndent()

        val hits = (parseOpenFoodFactsSearch(body) as FoodSearchResult.Hits).products

        assertEquals(
            listOf(
                ScannedProduct("Greek yogurt", 100.0, "g", 97, 9, 4, 5),
                ScannedProduct("Greek yogurt, plain", 100.0, "g", 59, 10, 4, 0),
            ),
            hits,
        )
    }

    @Test
    fun `drops nameless hits rather than showing blank rows`() {
        val body = """
            {"products":[
              {"product_name":"  ","nutriments":{"energy-kcal_100g":200}},
              {"product_name":"Rice","nutriments":{"energy-kcal_100g":"130"}}
            ]}
        """.trimIndent()

        val hits = (parseOpenFoodFactsSearch(body) as FoodSearchResult.Hits).products

        assertEquals(listOf(ScannedProduct("Rice", 100.0, "g", 130, 0, 0, 0)), hits)
    }

    @Test
    fun `a kilojoule-only hit still reports kcal`() {
        val body = """{"products":[{"product_name":"Oat bar","nutriments":{"energy_100g":1600}}]}"""

        val hits = (parseOpenFoodFactsSearch(body) as FoodSearchResult.Hits).products

        // 1600 kJ / 4.184
        assertEquals(382, hits.single().calories)
    }

    @Test
    fun `no matches is empty, not a failure`() {
        assertEquals(FoodSearchResult.Empty, parseOpenFoodFactsSearch("""{"count":0,"products":[]}"""))
    }

    @Test
    fun `only-nameless matches count as empty`() {
        val body = """{"products":[{"product_name":"","nutriments":{"energy-kcal_100g":200}}]}"""

        assertEquals(FoodSearchResult.Empty, parseOpenFoodFactsSearch(body))
    }

    @Test
    fun `an unparseable body fails rather than throwing`() {
        assertEquals(FoodSearchResult.Failed, parseOpenFoodFactsSearch("<html>502</html>"))
        assertEquals(FoodSearchResult.Failed, parseOpenFoodFactsSearch("""{"error":"nope"}"""))
    }
}
