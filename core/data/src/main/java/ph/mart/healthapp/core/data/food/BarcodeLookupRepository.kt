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
)

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
