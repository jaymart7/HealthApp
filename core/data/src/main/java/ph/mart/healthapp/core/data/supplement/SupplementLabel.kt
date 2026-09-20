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

/**
 * A bottle's figures, from either end.
 *
 * One interface because it is one question — *what does one dose of this carry?* — and one wire
 * shape, [SUPPLEMENT_LABEL_SCHEMA], answering it. What differs is **trust**, and it is the whole
 * distance between the two calls: [read] transcribes a panel that is in frame, [lookUp] recalls a
 * product from its name. A transcription can be checked against the bottle the user is holding; a
 * recollection cannot, which is why the lookup's prompt is told to answer with nothing rather than
 * from a similar product, and why the sheet it seeds says out loud that the figures are an
 * estimate. Neither call writes anything: Save does, on fields the user has read.
 */
interface SupplementScanRepository {
    suspend fun read(photo: Bitmap): SupplementScanResult

    /**
     * [name] as the user typed it — a product ("Centrum Adults"), or just a nutrient and a strength
     * ("vitamin D3 2000 IU"). Bounded by [SUPPLEMENT_NAME_MAX] at the field it is typed in, so
     * there is no cap here.
     *
     * [SupplementScanResult.NoLabelFound] is "I don't know that product" on this path. The same
     * case rather than a fourth one: both mean *the model had nothing to say about what you showed
     * it*, and the two callers already word their own dead ends.
     */
    suspend fun lookUp(name: String): SupplementScanResult
}

/**
 * The reading applied to a supplement — one mapping from a panel to a row, and both flows use it.
 *
 * [existing] is what it is layered over: `Supplement(name = "")` from the scan, whose confirmation
 * only ever adds, and the sheet's own draft from the lookup, which may be a row Room already has.
 * So the figures are overwritten and everything identifying the row — `id`, `createdAt`, `days`,
 * `deleted` — is left alone. Filling in the figures of a supplement typed in last month must not
 * renumber it or rewrite its schedule.
 *
 * **A field the reading doesn't carry leaves the existing one standing**, which is the same
 * sentence from both ends. A panel photographed on its own prints no product name, and there
 * `existing.name` is empty and typable — the sheet already refuses to save a nameless supplement.
 * A lookup that read the strength off "vitamin D3 2000 IU" without naming a product leaves the
 * words the user typed. Same for the dose, and for [SupplementLabelReading.timesPerDay]: a label
 * that states no frequency has not answered the question, so whatever was already set stands —
 * which on an add is the app's default of once.
 */
fun SupplementLabelReading.appliedTo(existing: Supplement): Supplement = existing.copy(
    name = name ?: existing.name,
    dose = dose ?: existing.dose,
    timesPerDay = timesPerDay ?: existing.timesPerDay,
    nutrients = nutrients,
    panel = panel,
)
