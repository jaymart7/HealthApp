package ph.mart.healthapp.core.data.food

import android.graphics.Bitmap
import kotlin.math.roundToInt

/**
 * Which column of the panel the figures were read from.
 *
 * Not a detail: a European pack declares per 100 g, a US Nutrition Facts panel declares per
 * serving, and the same seven numbers mean different things under each. Nothing here converts
 * between them — see [LabelReading] — so the basis travels with the figures and the review screen
 * says which one it is looking at.
 */
enum class LabelBasis { Per100g, PerServing }

/**
 * A nutrition panel, transcribed.
 *
 * Deliberately *not* a [RecognizedFood], for the reason [ScannedProduct] is not one either: a
 * [RecognitionConfidence] is an estimate concept, and this is a reading. The model is not asked
 * what is in the food — it is asked what the packet says is in the food, which is a question with a
 * printed answer sitting in the frame.
 *
 * **Every figure is nullable and null is "not printed".** A panel that omits sugar is not a panel
 * declaring zero sugar, and `AddEntryForm` already has the vocabulary for the difference — its own
 * four figures are nullable for exactly this reason. [nutrients] is the one exception and keeps the
 * app-wide convention [Nutrients] documents: `0` there means unknown-or-none, and the whole app
 * already reports coverage rather than pretending otherwise.
 *
 * **Nothing here is normalised.** The figures are whatever the panel printed, for whatever amount
 * it printed them against; `LabelReading.toAddEntryForm` turns [basis] and [servingSize] into the
 * portion they belong to, and `withPortionAmount` reprices from there. Converting a per-serving
 * panel to per 100 g here would be arithmetic nobody performed on a number the user is about to
 * check against the packet in their hand.
 */
data class LabelReading(
    /** The product name where it is printed and in frame — a panel photographed on its own has
     * none, which is ordinary rather than a failure. The review screen's title field is empty and
     * editable, and a nameless entry with calories is still a valid quick add. */
    val name: String? = null,
    /** The serving as the label's own words — "30 g", "1 bar (25 g)". Feeds [servingGrams] and the
     * portion control's third preset chip, exactly as [ScannedProduct.servingSize] does. Third-party
     * product text, never authored here, which is why it is a String and not a resource. */
    val servingSize: String? = null,
    val basis: LabelBasis = LabelBasis.Per100g,
    val calories: Int? = null,
    val proteinG: Int? = null,
    val carbsG: Int? = null,
    val fatG: Int? = null,
    val nutrients: Nutrients = Nutrients(),
)

/**
 * Whether there is anything here worth putting in front of the user — [loggable]'s job for the
 * meal parse, and the same judgement: a response the model filled with nothing is not a panel it
 * read, and the flow has a screen that says so.
 *
 * The name alone does not count. A model handed a photo of the *front* of a pack can read the
 * brand off it and nothing else, and a review screen holding a name and seven dashes is a worse
 * answer than "point it at the panel".
 */
fun LabelReading.readable(): Boolean =
    (calories ?: 0) > 0 || (proteinG ?: 0) > 0 || (carbsG ?: 0) > 0 || (fatG ?: 0) > 0 ||
        !nutrients.isEmpty

/**
 * Sodium from whichever of the two a label declares.
 *
 * Plenty of packs print salt and no sodium at all, and the conversion is the same one
 * [OpenFoodFacts.kt][SALT_TO_SODIUM] already carries. It is done here rather than in the prompt for
 * the reason every conversion below is: a model doing arithmetic on an optional field produces a
 * number nobody can check against the packet.
 */
fun sodiumMgFrom(sodiumMg: Int?, saltG: Double?): Int = when {
    sodiumMg != null && sodiumMg > 0 -> sodiumMg
    saltG != null && saltG > 0 -> (saltG / SALT_TO_SODIUM * MG_PER_G).roundToInt()
    else -> 0
}

/** Vitamin D from micrograms, or from the international units a US panel prints instead.
 * 40 IU is 1 µg — the published equivalence, not a rounding. */
fun vitaminDUgFrom(vitaminDUg: Double?, vitaminDIu: Int?): Int = when {
    vitaminDUg != null && vitaminDUg > 0 -> vitaminDUg.roundToInt()
    vitaminDIu != null && vitaminDIu > 0 -> (vitaminDIu / IU_PER_UG_VITAMIN_D).roundToInt()
    else -> 0
}

/** Labels print iron in milligrams and [Nutrients.ironUg] stores micrograms, for the reason that
 * type gives: an `Int` milligram would round a 0.4 mg food to nothing. */
fun ironUgFrom(ironMg: Double?): Int =
    if (ironMg != null && ironMg > 0) (ironMg * UG_PER_MG).roundToInt() else 0

private const val IU_PER_UG_VITAMIN_D = 40.0

private const val UG_PER_MG = 1000

/**
 * [NoLabelFound] is its own answer rather than an empty [Found], the call [RecognitionResult]
 * makes: "there is no nutrition panel in that photo" and "the call didn't work" are different
 * things, and the flow shows a different screen for each.
 */
sealed interface LabelScanResult {
    data class Found(val reading: LabelReading) : LabelScanResult
    data object NoLabelFound : LabelScanResult
    data object Failed : LabelScanResult
}

interface LabelScanRepository {
    suspend fun read(photo: Bitmap): LabelScanResult
}
