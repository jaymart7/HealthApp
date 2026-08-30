package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodSearchTest {

    @Test
    fun `maps every usable hit`() {
        val body = """
            {"totalHits":2,"foods":[
              {"description":"Yogurt, Greek, plain, nonfat","foodNutrients":[
                {"nutrientId":1008,"value":59},{"nutrientId":1003,"value":10.3},
                {"nutrientId":1005,"value":3.6},{"nutrientId":1004,"value":0.4}]},
              {"description":"GREEK YOGURT","brandName":"OCEAN SPRAY","foodNutrients":[
                {"nutrientId":1008,"value":467},{"nutrientId":1003,"value":3.33},
                {"nutrientId":1005,"value":70.0},{"nutrientId":1004,"value":20.0}]}
            ]}
        """.trimIndent()

        val hits = (parseFdcSearch(body) as FoodSearchResult.Hits).products

        assertEquals(
            listOf(
                ScannedProduct("Yogurt, Greek, plain, nonfat", 100.0, "g", 59, 10, 4, 0),
                ScannedProduct("Ocean Spray · Greek Yogurt", 100.0, "g", 467, 3, 70, 20),
            ),
            hits,
        )
    }

    @Test
    fun `shouted branded rows are recased, prose rows are left alone`() {
        val body = """
            {"foods":[
              {"description":"SPICY SWEET CHILI FLAVORED TORTILLA CHIPS","brandName":"DORITOS",
               "brandOwner":"Frito-Lay","foodNutrients":[{"nutrientId":1008,"value":536}]},
              {"description":"Broccoli, raw","foodNutrients":[{"nutrientId":1008,"value":31}]}
            ]}
        """.trimIndent()

        val hits = (parseFdcSearch(body) as FoodSearchResult.Hits).products

        assertEquals(
            listOf("Doritos · Spicy Sweet Chili Flavored Tortilla Chips", "Broccoli, raw"),
            hits.map { it.name },
        )
    }

    @Test
    fun `a brand already in the description is not repeated`() {
        val body = """
            {"foods":[{"description":"DORITOS NACHO CHEESE","brandName":"DORITOS",
             "foodNutrients":[{"nutrientId":1008,"value":536}]}]}
        """.trimIndent()

        val hits = (parseFdcSearch(body) as FoodSearchResult.Hits).products

        assertEquals("Doritos Nacho Cheese", hits.single().name)
    }

    @Test
    fun `falls back to brandOwner when there is no brandName`() {
        val body = """
            {"foods":[{"description":"GREEK YOGURT","brandOwner":"Ocean Spray Cranberries, Inc.",
             "foodNutrients":[{"nutrientId":1008,"value":97}]}]}
        """.trimIndent()

        val hits = (parseFdcSearch(body) as FoodSearchResult.Hits).products

        assertEquals("Ocean Spray Cranberries, Inc. · Greek Yogurt", hits.single().name)
    }

    @Test
    fun `drops nameless hits rather than showing blank rows`() {
        val body = """
            {"foods":[
              {"description":"  ","foodNutrients":[{"nutrientId":1008,"value":200}]},
              {"description":"Rice, white, cooked","foodNutrients":[{"nutrientId":1008,"value":"130"}]}
            ]}
        """.trimIndent()

        val hits = (parseFdcSearch(body) as FoodSearchResult.Hits).products

        assertEquals(listOf(ScannedProduct("Rice, white, cooked", 100.0, "g", 130, 0, 0, 0)), hits)
    }

    @Test
    fun `a kilojoule-only hit still reports kcal`() {
        val body = """{"foods":[{"description":"Oat bar","foodNutrients":[{"nutrientId":1062,"value":1600}]}]}"""

        val hits = (parseFdcSearch(body) as FoodSearchResult.Hits).products

        // 1600 kJ / 4.184
        assertEquals(382, hits.single().calories)
    }

    @Test
    fun `no matches is empty, not a failure`() {
        assertEquals(FoodSearchResult.Empty, parseFdcSearch("""{"totalHits":0,"foods":[]}"""))
    }

    @Test
    fun `only-nameless matches count as empty`() {
        val body = """{"foods":[{"description":"","foodNutrients":[{"nutrientId":1008,"value":200}]}]}"""

        assertEquals(FoodSearchResult.Empty, parseFdcSearch(body))
    }

    @Test
    fun `an unparseable body fails rather than throwing`() {
        assertEquals(FoodSearchResult.Failed, parseFdcSearch("<html>502</html>"))
        assertEquals(FoodSearchResult.Failed, parseFdcSearch("""{"error":{"code":"API_KEY_INVALID"}}"""))
    }
}
