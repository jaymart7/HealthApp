package ph.mart.healthapp.core.data.food

/**
 * A packaged product resolved from a scanned barcode. Deliberately *not* [RecognizedFood] — that
 * carries a [RecognitionConfidence], which is an AI-estimate concept; a barcode lookup either
 * matched a database row or it didn't, and reusing the AI type would drag the AI accent treatment
 * onto a deterministic result.
 *
 * Values are always per 100 g, the unit FoodData Central reports search nutrients in.
 */
data class ScannedProduct(
    val name: String,
    val portionAmount: Double,
    val portionUnit: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val nutrients: Nutrients = Nutrients(),
    /** The package's own serving, exactly as the source declared it — "30 g", "1 bar (25 g)",
     * "2 cookies (30g)". Null for a food with no natural serving, which is most of them.
     *
     * Deliberately the raw string and not a parsed pair: it is the label's own words, it is the
     * chip the add-entry sheet draws, and [servingGrams] is what turns it into the amount that chip
     * sets. Not app copy — it is third-party product data, untranslatable and never authored here,
     * which is why it stays a String rather than a resource.
     *
     * [COMMON_FOODS] leaves it null on purpose: a serving label there *would* be app copy. */
    val servingSize: String? = null,
)

/**
 * The grams in a declared serving, or null when there aren't any to find.
 *
 * Sources write a serving every way a label does — "30 g", "1 bar (25 g)", "2 cookies (30g)",
 * "1 cup (240 ml)". The rule is the **last** gram figure in the string, because the parenthesised
 * one is the weight and the leading one is the count: "1 bar (25 g)" is 25 g, not 1. A serving
 * declared only in millilitres or only as a count has no grams and gets no chip — guessing that a
 * cup is 240 g is the kind of invented number this app's nullable figures exist to avoid.
 */
fun servingGrams(raw: String?): Double? {
    if (raw == null) return null
    return GRAM_FIGURE.findAll(raw)
        .mapNotNull { it.groupValues[1].replace(',', '.').toDoubleOrNull() }
        .lastOrNull()
        ?.takeIf { it > 0 }
}

/** A number followed by a gram symbol, with or without a space, and not part of a longer unit —
 * "25g" and "25 g" match, "25 mg" and "25 gallon" do not. */
private val GRAM_FIGURE = Regex("(?<![a-zA-Z])(\\d+(?:[.,]\\d+)?)\\s*g(?![a-zA-Z])")

sealed interface BarcodeLookupResult {
    data class Found(val product: ScannedProduct) : BarcodeLookupResult

    /** The barcode is well-formed but no usable product exists for it — the user adds it manually. */
    data object NotFound : BarcodeLookupResult

    /** Network or server problem; retrying the same barcode may well work. */
    data object Failed : BarcodeLookupResult
}

interface BarcodeLookupRepository {
    suspend fun lookup(barcode: String): BarcodeLookupResult
}
