package ph.mart.healthapp.core.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductNameTest {

    @Test
    fun `a short all-caps brand is an initialism and keeps its capitals`() {
        assertEquals("USDA · Bananas, raw", brandedName(brand = "USDA", description = "Bananas, raw"))
        assertEquals("KFC · Original Recipe Chicken", brandedName(brand = "KFC", description = "ORIGINAL RECIPE CHICKEN"))
    }

    @Test
    fun `a long all-caps brand is recased like the description`() {
        assertEquals(
            "Doritos · Spicy Sweet Chili",
            brandedName(brand = "DORITOS", description = "SPICY SWEET CHILI"),
        )
    }

    @Test
    fun `short all-caps words in the description are still recased`() {
        assertEquals("Nestle · Milk Bar", brandedName(brand = "NESTLE", description = "MILK BAR"))
    }

    @Test
    fun `the brand is dropped when the name already opens with it`() {
        assertEquals("Nutella Hazelnut Spread", brandedName(brand = "Nutella", description = "Nutella Hazelnut Spread"))
    }

    @Test
    fun `no name means no product`() {
        assertNull(brandedName(brand = "USDA", description = "  "))
    }
}
