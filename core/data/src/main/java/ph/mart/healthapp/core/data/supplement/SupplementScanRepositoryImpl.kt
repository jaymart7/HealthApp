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
}
