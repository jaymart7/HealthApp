package ph.mart.healthapp.core.data.supplement

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import ph.mart.healthapp.core.data.AI_MODEL_NAME
import ph.mart.healthapp.core.data.AI_THINKING
import ph.mart.healthapp.core.data.logAiFailure

/**
 * A Supplement Facts panel in, its figures out.
 *
 * `LabelScanRepositoryImpl`'s twin, and it keeps that file's one rule: **this is a transcription
 * and the prompt exists to keep it one.** A bottle has a printed answer, so the instruction runs
 * the opposite way to the photo flow's — read what is printed, omit what is not, never complete a
 * panel from what you know about the product. A model that recalls a well-known multivitamin's
 * formula instead of reading the one in frame has produced a figure that then counts toward the
 * user's day.
 *
 * The one thing it asks for that no other call in this app does is the *rest* of the panel — the
 * vitamins [ph.mart.healthapp.core.data.food.Nutrients] has no field for, carried back as text so a
 * multivitamin reads as the twenty lines it declares rather than the four this app can grade.
 */
private val PROMPT = """
You are reading a supplement bottle's Supplement Facts panel for a health-tracking app.

Report the figures exactly as they are printed. Read only what is visible. Do not estimate, do not
infer, and do not fill in anything from what you know about this product or products like it — if a
line is not printed on the panel, leave that field out entirely. Never write 0 for a line that is
absent.

The panel declares its figures against one serving. Report them per serving, unchanged, and set
servingSize to that serving in the label's own words ("2 capsules", "1 scoop"). Do not convert
anything to a per-capsule or per-day amount.

Fill the named fields for the nutrients that have one. Put every other line the panel declares —
vitamins, minerals, herbs, a proprietary blend — in otherNutrients, with its amount as printed,
in the order the panel prints them. A line with no amount still belongs there.

Units: if the panel declares salt rather than sodium, fill saltG and leave sodiumMg out. If
vitamin D is printed in international units, fill vitaminDIu and leave vitaminDUg out. Never fill
both forms of either.

Set timesPerDay only if the directions state how many times a day to take it. Set name only if the
product's name is printed and visible in the photo. If there is no supplement panel in the photo at
all, return an object with no fields set.
""".trimIndent()

/**
 * The same figures asked for from the other end, and the opposite instruction in one respect only.
 *
 * [PROMPT] above says *read what is in frame and nothing else*; there is no frame here, so the
 * guard has to be the product's identity instead: answer for a product the model **recognises**, or
 * answer with nothing. The failure this is written against is the plausible one — asked about a
 * multivitamin it has never seen, a model will happily return a typical multivitamin, and the user
 * then ticks a formula nobody published into their day's nutrient panel. An empty object is a dead
 * end the screen has words for; an invented panel is a figure that looks read.
 *
 * *Recognises*, not *knows exactly*. The first version asked for "this exact product, as its
 * manufacturer prints it", and a lite model at the minimal thinking level never claims that much
 * about any panel — every field is optional, so `{}` always complied, and the sheet said "we don't
 * know that one" to nearly every name. An approximate panel for a product it does recognise is
 * what the sheet is built to receive: `PanelReadout` labels it an AI estimate and tells the user to
 * check it against the bottle, and nothing is written until Save. A bare category ("magnesium")
 * still gets nothing, because answering it *is* the typical-formula failure above.
 *
 * Everything else is deliberately [PROMPT]'s: per serving, unchanged, the named fields for what
 * maps and otherNutrients for the rest, the same two unit hedges. The reading is the same type and
 * the same schema parses it, so a difference here would be a difference the rest of the flow could
 * not see.
 */
private val LOOKUP_PROMPT = """
You are identifying a supplement for a health-tracking app from the name the user typed.

If you recognise the product — a brand and product line you know — report its Supplement Facts panel
as best you know it. The app shows your answer as an estimate and asks the user to check it against
the bottle, so a panel you know approximately is still worth reporting. If the name does not
identify a product you recognise, return an object with no fields set: do not answer with a
different product, and do not answer a bare category ("magnesium", "fish oil") with what such a
supplement typically contains. Never write 0 for a line you are unsure of — leave the field out.

If the name is a nutrient and a strength rather than a product ("vitamin D3 2000 IU"), report that
much and leave the rest out.

The panel declares its figures against one serving. Report them per serving, unchanged, and set
servingSize to that serving in the label's own words ("2 capsules", "1 scoop"). Do not convert
anything to a per-capsule or per-day amount.

Fill the named fields for the nutrients that have one. Put every other line the panel declares —
vitamins, minerals, herbs, a proprietary blend — in otherNutrients, with its amount as printed, in
the order the panel prints them.

Units: if the panel declares salt rather than sodium, fill saltG and leave sodiumMg out. If vitamin
D is printed in international units, fill vitaminDIu and leave vitaminDUg out. Never fill both forms
of either.

Set timesPerDay only if the product's directions state how many times a day to take it. Set name to
the product's full name as it is printed. Give no medical advice, no diagnosis, and no dosage or
supplement recommendations.

The name the user typed:
""".trimIndent()

/**
 * Higher than the nutrition label's 500: that panel is seven lines and a multivitamin is thirty,
 * each one an object of two short strings. Still well inside the recognition call's headroom.
 */
private const val MAX_OUTPUT_TOKENS = 900

/** [org.json.JSONObject] parses the response — see [parseSupplementLabel]. */
internal class SupplementScanRepositoryImpl : SupplementScanRepository {

    private val model = Firebase.ai(
        backend = GenerativeBackend.googleAI(),
        useLimitedUseAppCheckTokens = true,
    ).generativeModel(
        modelName = AI_MODEL_NAME,
        generationConfig = generationConfig {
            // `AI_THINKING`, the floor, for the label scan's reason: copying printed numbers out
            // of an image is the clearest case in the app of a task that does not reason.
            thinkingConfig = AI_THINKING
            maxOutputTokens = MAX_OUTPUT_TOKENS
            responseMimeType = "application/json"
            responseSchema = SUPPLEMENT_LABEL_SCHEMA
        },
    )

    override suspend fun read(photo: Bitmap): SupplementScanResult = try {
        val response = model.generateContent(content { image(photo); text(PROMPT) })
        val reading = parseSupplementLabel(response.text)
        // A name and nothing else is the front of the bottle, not the panel. `readable()` is what
        // decides; this only picks the screen.
        if (reading.readable()) SupplementScanResult.Found(reading) else SupplementScanResult.NoLabelFound
    } catch (e: CancellationException) {
        // Backing out of the screen cancels the scope, and that is not an AI failure — the rule
        // every other call site here already follows.
        throw e
    } catch (e: Exception) {
        logAiFailure("supplement scan", e)
        SupplementScanResult.Failed
    }

    /**
     * The same model instance, because the configuration is the same one: same schema, same
     * thinking floor, same ceiling. Only the prompt differs, and a second `generativeModel` built
     * from identical settings would be a second thing to keep in step.
     *
     * [SupplementLabelReading.readable] is the gate here for the reason it is above, with the
     * halves swapped: there, a name and nothing else is the front of the bottle rather than the
     * panel; here it is the model repeating the user's own words back with no figures behind them.
     * Either way it is not an answer worth seeding a sheet with.
     */
    override suspend fun lookUp(name: String): SupplementScanResult = try {
        val response = model.generateContent(content { text("$LOOKUP_PROMPT\n$name") })
        val reading = parseSupplementLabel(response.text)
        if (reading.readable()) SupplementScanResult.Found(reading) else SupplementScanResult.NoLabelFound
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logAiFailure("supplement lookup", e)
        SupplementScanResult.Failed
    }
}
