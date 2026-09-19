package ph.mart.healthapp.core.data.supplement

import android.graphics.Bitmap
import ph.mart.healthapp.core.data.food.Nutrients
import ph.mart.healthapp.core.data.food.isEmpty

/**
 * A Supplement Facts panel, transcribed.
 *
 * Deliberately not a [ph.mart.healthapp.core.data.food.LabelReading], though it is that file's
 * twin: a nutrition panel declares figures *per amount of food* and a supplement panel declares
 * them *per dose*, so a `LabelBasis` has nothing to say here and [ph.mart.healthapp.core.data.food.LabelReading]'s
 * per-100 g column simply does not exist on a bottle. The two carry different questions and a
 * shared type would have to answer both.
 *
 * **Nothing here is normalised.** [nutrients] is what one serving declares, which is what a tick
 * records, and [panel] is what was printed. Converting between the two would be arithmetic nobody
 * performed on a figure the user is about to check against the bottle in their hand.
 */
data class SupplementLabelReading(
    /** The product name where it is printed and in frame. A panel photographed on its own has
     * none, which is ordinary — the edit sheet's name field is then empty and typable, and a
     * supplement with no name is the one thing that sheet already refuses to save. */
    val name: String? = null,
    /** The serving in the label's own words — "2 capsules", "1 scoop" — which becomes the dose,
     * the free text nothing parses. Third-party product text, never authored here. */
    val dose: String? = null,
    /** From the directions ("twice daily"), and null when they state no frequency. A bottle that
     * says only "take with food" has not said how often, and guessing 1 would be the model
     * answering a question the label didn't. */
    val timesPerDay: Int? = null,
    /** Of the seven this app grades, per dose. */
    val nutrients: Nutrients = Nutrients(),
    /** Every declaration as printed, one per line — including the ones [nutrients] has no field
     * for, which on a multivitamin is most of them. */
    val panel: String = "",
)

/**
 * Whether there is anything here worth putting in front of the user — [ph.mart.healthapp.core.data.food.readable]'s
 * job and the same judgement.
 *
 * **[panel] counts and the name does not.** A model handed the front of a bottle reads the brand
 * off it and nothing else, and a sheet holding a name and no figures is a worse answer than "point
 * it at the panel". But a panel whose every line is a vitamin this app has no field for — a B-complex,
 * say — is a panel that *was* read, and refusing it would tell the user their bottle is unreadable
 * when the app is simply not able to grade it.
 */
fun SupplementLabelReading.readable(): Boolean = !nutrients.isEmpty || panel.isNotBlank()

/**
 * [NoLabelFound] is its own answer rather than an empty [Found], the call
 * [ph.mart.healthapp.core.data.food.LabelScanResult] makes: "there is no panel in that photo" and
 * "the call didn't work" are different things and the flow shows a different screen for each.
 */
sealed interface SupplementScanResult {
    data class Found(val reading: SupplementLabelReading) : SupplementScanResult
    data object NoLabelFound : SupplementScanResult
    data object Failed : SupplementScanResult
}

interface SupplementScanRepository {
    suspend fun read(photo: Bitmap): SupplementScanResult
}
