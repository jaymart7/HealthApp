package ph.mart.healthapp.core.data.food

import android.graphics.Bitmap
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import ph.mart.healthapp.core.data.aiModel
import ph.mart.healthapp.core.data.AI_THINKING
import ph.mart.healthapp.core.data.logAiFailure
import ph.mart.healthapp.core.data.logAiUsage

/**
 * A nutrition panel in, its figures out.
 *
 * **This is a transcription and the prompt exists to keep it one.** Every other AI call in this
 * app is allowed to estimate — `FoodRecognitionRepositoryImpl` is told to estimate rather than
 * decline, because a photographed plate has no printed answer. A packet does. So the instruction
 * runs the other way here: read what is printed, omit what is not, and never complete a panel from
 * what you know about the product. A model that recalls Nutella's sodium instead of reading it has
 * produced the exact number `OpenFoodFacts.kt` refuses `nutriments_estimated` for.
 *
 * That distinction is also why this path may fill all seven nutrients while the photo and voice
 * schemas ask for three: the four Nutrition Facts nutrients are printed on the panel in the frame.
 */
private val PROMPT = """
You are reading a nutrition label for a food-logging app. The photo shows a packaged food's
nutrition information panel.

Report the figures exactly as they are printed. Read only what is visible. Do not estimate, do not
infer, and do not fill in anything from what you know about this product or products like it — if a
line is not printed on the panel, leave that field out entirely. Never write 0 for a line that is
absent.

The panel declares its figures against one amount. Read a single column and say which it was: set
basis to "$BASIS_PER_100G" if you read a per-100 g (or per-100 ml) column, or "$BASIS_PER_SERVING"
if you read a per-serving column. If the panel prints both, read the per-100 g one. Set servingSize
to the serving in the label's own words if it declares one.

Units: calories in kcal — if only kilojoules are printed, leave calories out. If the panel declares
salt rather than sodium, fill saltG and leave sodiumMg out. If vitamin D is printed in
international units, fill vitaminDIu and leave vitaminDUg out. Never fill both forms of either.

Set name only if the product's name is printed and visible in the photo. If there is no nutrition
panel in the photo at all, return an object with no fields set.
"""

/**
 * One flat object of sixteen short fields — a fraction of the recognition call's list of plates,
 * which is why this sits well inside the cap rather than needing that call's headroom.
 */
private const val MAX_OUTPUT_TOKENS = 500

/** [org.json.JSONObject] parses the response — see [parseLabelReading]. */
internal class LabelScanRepositoryImpl : LabelScanRepository {

    private val model = aiModel(
        generationConfig = generationConfig {
            // `AI_THINKING`, not the recognition call's raised level. That one estimates — identify
            // a food, judge how much of it is on the plate, recall its figures and scale them —
            // and `Ai.kt` says the floor is right for everything that does not. Copying a printed
            // number out of an image is the clearest case of that in the app.
            thinkingConfig = AI_THINKING
            maxOutputTokens = MAX_OUTPUT_TOKENS
            responseMimeType = "application/json"
            responseSchema = LABEL_SCHEMA
        },
    )

    override suspend fun read(photo: Bitmap): LabelScanResult = try {
        val response = model.generateContent(content { image(photo); text(PROMPT) })
        logAiUsage("label scan", response.usageMetadata)
        val reading = parseLabelReading(response.text)
        // A name and nothing else is the front of the pack, not the panel — and a review screen
        // holding seven dashes is a worse answer than saying so. `readable()` is what decides;
        // this only picks the screen.
        if (reading.readable()) LabelScanResult.Found(reading) else LabelScanResult.NoLabelFound
    } catch (e: CancellationException) {
        // Backing out of the screen cancels the scope, and that is not an AI failure — the rule
        // `FoodRecognitionRepositoryImpl` and `CoachRepositoryImpl` already follow.
        throw e
    } catch (e: Exception) {
        logAiFailure("label scan", e)
        LabelScanResult.Failed
    }
}
