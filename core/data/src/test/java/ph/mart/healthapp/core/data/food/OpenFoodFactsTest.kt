package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Bodies here are trimmed captures of **real** Open Food Facts responses, not invented ones — the
 * units in particular ([NUTELLA]'s `sodium_100g: 0.0428`) are the whole reason this file exists.
 */
class OpenFoodFactsTest {

    @Test
    fun `parses a product with kcal, macros and the label's own nutrients`() {
        val product = (parseOffProduct(NUTELLA) as BarcodeLookupResult.Found).product

        assertEquals("Nutella", product.name)
        assertEquals(100.0, product.portionAmount, 0.0)
        assertEquals("g", product.portionUnit)
        assertEquals(539, product.calories)
        assertEquals(6, product.proteinG)
        assertEquals(58, product.carbsG)
        assertEquals(31, product.fatG)
        assertEquals(56, product.nutrients.sugarG)
        assertEquals(0, product.nutrients.fiberG)
    }

    /** `sodium_100g` is grams: 0.0428 g is 43 mg, not 0 and not 42,800. */
    @Test
    fun `sodium is read in grams and stored in milligrams`() {
        val product = (parseOffProduct(NUTELLA) as BarcodeLookupResult.Found).product

        assertEquals(43, product.nutrients.sodiumMg)
    }

    /**
     * The guard on OFF's ingredient-derived estimates. [NUTELLA] declares calcium, iron, potassium
     * and vitamin D *only* under `nutriments_estimated` — read those and every scanned product
     * would report micronutrients a model worked out from an ingredient list.
     */
    @Test
    fun `nutriments_estimated is never read`() {
        val product = (parseOffProduct(NUTELLA) as BarcodeLookupResult.Found).product

        assertEquals(0, product.nutrients.calciumMg)
        assertEquals(0, product.nutrients.ironUg)
        assertEquals(0, product.nutrients.potassiumMg)
        assertEquals(0, product.nutrients.vitaminDUg)
    }

    @Test
    fun `declared micronutrients convert from grams to the units Nutrients stores`() {
        val body = """
            {"product":{"product_name":"Fortified crackers","nutriments":{"energy-kcal_100g":500,
            "calcium_100g":0.0253623188405797,"iron_100g":0.000942028985507246,
            "potassium_100g":0.5063166,"vitamin-d_100g":0.0000051}}}
        """.trimIndent()

        val product = (parseOffProduct(body) as BarcodeLookupResult.Found).product

        assertEquals(25, product.nutrients.calciumMg)
        assertEquals(942, product.nutrients.ironUg)
        assertEquals(506, product.nutrients.potassiumMg)
        assertEquals(5, product.nutrients.vitaminDUg)
    }

    @Test
    fun `sodium falls back to salt when only salt is declared`() {
        val body = """
            {"product":{"product_name":"Cracker","nutriments":{"energy-kcal_100g":500,"salt_100g":1.33}}}
        """.trimIndent()

        val product = (parseOffProduct(body) as BarcodeLookupResult.Found).product

        // 1.33 g salt / 2.5 = 0.532 g sodium
        assertEquals(532, product.nutrients.sodiumMg)
    }

    @Test
    fun `falls back to kilojoules when kcal is missing`() {
        val body = """
            {"product":{"product_name":"Oat bar","nutriments":{"energy_100g":1600,"proteins_100g":8}}}
        """.trimIndent()

        val product = (parseOffProduct(body) as BarcodeLookupResult.Found).product

        // 1600 kJ / 4.184
        assertEquals(382, product.calories)
        assertEquals(8, product.proteinG)
    }

    @Test
    fun `reads nutrient values that come back quoted`() {
        val body = """
            {"product":{"product_name":"Rice","nutriments":{"energy-kcal_100g":"130","proteins_100g":"2.7"}}}
        """.trimIndent()

        val product = (parseOffProduct(body) as BarcodeLookupResult.Found).product

        assertEquals(130, product.calories)
        assertEquals(3, product.proteinG)
    }

    /** What a code OFF does not stock really returns: 200, no `product` key. */
    @Test
    fun `a code with no product object is not found`() {
        val body = """{"code":"00000000","status":0,"status_verbose":"no code or invalid code"}"""

        assertEquals(BarcodeLookupResult.NotFound, parseOffProduct(body))
    }

    @Test
    fun `a nameless product is not found`() {
        val body = """{"product":{"product_name":"  ","nutriments":{"energy-kcal_100g":200}}}"""

        assertEquals(BarcodeLookupResult.NotFound, parseOffProduct(body))
    }

    /** OFF is full of products someone photographed and never entered a label for. */
    @Test
    fun `a product with no nutriments at all is not found`() {
        val body = """{"product":{"product_name":"Sky Flakes Crackers"}}"""

        assertEquals(BarcodeLookupResult.NotFound, parseOffProduct(body))
    }

    @Test
    fun `a product declaring neither energy nor a macro is not found`() {
        val body = """{"product":{"product_name":"Mystery bar","nutriments":{"nova-group_100g":4}}}"""

        assertEquals(BarcodeLookupResult.NotFound, parseOffProduct(body))
    }

    @Test
    fun `an unparseable body fails rather than throwing`() {
        assertEquals(BarcodeLookupResult.Failed, parseOffProduct("<html>502</html>"))
    }

    @Test
    fun `the search drops hits that carry no nutriments and keeps the rest`() {
        val products = parseOffSearch(SKY_FLAKES)!!

        assertEquals(1, products.size)
        assertEquals("Sky Flakes · Cracker Sandwich Chocolate", products.single().name)
        assertEquals(500, products.single().calories)
    }

    @Test
    fun `an unparseable search body is not an empty search`() {
        assertNull(parseOffSearch("<html>503</html>"))
        assertTrue(parseOffSearch("""{"hits":[]}""")!!.isEmpty())
    }

    @Test
    fun `brands read the same as an array and as a comma-separated string`() {
        val nutriments = """"nutriments":{"energy-kcal_100g":383}"""
        val asArray = """{"product":{"product_name":"Biscuits","brands":["Kelloggs","Special K"],$nutriments}}"""
        val asString = """{"product":{"product_name":"Biscuits","brands":"Kelloggs, Special K",$nutriments}}"""

        val fromArray = (parseOffProduct(asArray) as BarcodeLookupResult.Found).product
        val fromString = (parseOffProduct(asString) as BarcodeLookupResult.Found).product

        assertEquals("Kelloggs · Biscuits", fromArray.name)
        assertEquals(fromArray.name, fromString.name)
    }

    /** "Nutella" branded "Nutella, Ferrero, Yum yum" must not become "Nutella · Nutella". */
    @Test
    fun `a name already opening with its brand is not prefixed twice`() {
        assertEquals("Nutella", (parseOffProduct(NUTELLA) as BarcodeLookupResult.Found).product.name)
    }

    @Test
    fun `falls back to the english name when the local one is absent`() {
        val body = """{"product":{"product_name_en":"Chocolate spread","nutriments":{"energy-kcal_100g":539}}}"""

        val product = (parseOffProduct(body) as BarcodeLookupResult.Found).product

        assertEquals("Chocolate spread", product.name)
    }

    @Test
    fun `the search url encodes a multi-word query`() {
        assertTrue(offSearchUrl("sky flakes").contains("q=sky+flakes"))
    }

    /** OFF normalises zero-padding server-side, so the stripped key goes straight into the path. */
    @Test
    fun `the product url takes the barcode key as it is`() {
        assertTrue(offProductUrl("28400642255").contains("/product/28400642255.json"))
    }
}

/**
 * `world.openfoodfacts.org/api/v2/product/3017620422003.json`, trimmed to the requested fields.
 * Note where calcium and iron actually live.
 */
private val NUTELLA = """
    {"code":"3017620422003","product":{"brands":"Nutella, Ferrero, Yum yum",
    "nutriments":{"carbohydrates_100g":57.5,"energy-kcal_100g":539,"energy-kj_100g":2252,
    "energy_100g":2252,"fat_100g":30.9,"fiber_100g":0,"proteins_100g":6.3,"salt_100g":0.107,
    "saturated-fat_100g":10.6,"sodium_100g":0.0428,"sugars_100g":56.3},
    "nutriments_estimated":{"calcium_100g":0.107269704,"iron_100g":0.0039284262,
    "potassium_100g":0.5063166,"vitamin-d_100g":2.13628e-07},
    "product_name":"Nutella","product_name_en":"Nutella"},"status":1,"status_verbose":"product found"}
""".trimIndent()

/** `search.openfoodfacts.org/search?q=sky+flakes`, trimmed. The second hit has no `nutriments`. */
private val SKY_FLAKES = """
    {"hits":[{"code":"0750515531536","brands":["Sky Flakes"],
    "nutriments":{"carbohydrates_100g":70,"energy-kcal_100g":500,"fat_100g":20,"proteins_100g":6.67,
    "salt_100g":1.33,"saturated-fat_100g":15,"sodium_100g":0.533,"sugars_100g":26.7},
    "product_name":"Cracker Sandwich Chocolate"},
    {"code":"0750515018303","product_name":"Sky Flakes Crackers"}],"page":1,"page_size":2}
""".trimIndent()
