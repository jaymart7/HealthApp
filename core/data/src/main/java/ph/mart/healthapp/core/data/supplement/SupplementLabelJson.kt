package ph.mart.healthapp.core.data.supplement

import com.google.firebase.ai.type.Schema
import org.json.JSONObject
import ph.mart.healthapp.core.data.food.Nutrients
import ph.mart.healthapp.core.data.food.ironUgFrom
import ph.mart.healthapp.core.data.food.sodiumMgFrom
import ph.mart.healthapp.core.data.food.vitaminDUgFrom

/**
 * The wire shape of a [SupplementLabelReading], and the one place that reads it back.
 *
 * Flat, units in the field names, **every property optional** — the conventions
 * [ph.mart.healthapp.core.data.food.LABEL_SCHEMA] sets and for its reason: a missing field is a
 * line the panel does not print, and a required one would make the model invent it. The salt/sodium
 * and µg/IU hedges are that schema's too, resolved below by the same three Kotlin functions, where
 * the arithmetic is testable and the model is asked to do none.
 *
 * [OTHER_NUTRIENTS] is the one thing this schema has that the food one does not, and it is why the
 * scan is worth having on a multivitamin at all: [Nutrients] holds seven figures and a bottle
 * declares twenty, so the rest come back as printed text. They are shown and never graded — there
 * is no field to put vitamin B12 in and no target on the profile to price it against.
 *
 * Here rather than in [SupplementLabel.kt][readable] because that file is pure and JVM-tested;
 * [org.json] is stubbed on the JVM, so the parse below cannot be tested there and the judgement it
 * feeds ([readable]) deliberately can. The split [ph.mart.healthapp.core.data.food.RECOGNIZED_FOOD_SCHEMA]
 * documents.
 */
internal val SUPPLEMENT_LABEL_SCHEMA = Schema.obj(
    mapOf(
        "name" to Schema.string(description = "the product name, only if it is printed and visible"),
        "servingSize" to Schema.string(
            description = "the serving in the label's own words, e.g. \"2 capsules\", \"1 scoop\"",
        ),
        "timesPerDay" to Schema.integer(
            description = "how many times a day the directions say to take it, only if they say",
        ),
        "fiberG" to Schema.integer(),
        "sugarG" to Schema.integer(),
        "sodiumMg" to Schema.integer(description = "milligrams, only if sodium itself is printed"),
        "saltG" to Schema.double(description = "grams, only if the label prints salt instead of sodium"),
        "vitaminDUg" to Schema.double(description = "micrograms, only if printed in µg"),
        "vitaminDIu" to Schema.integer(description = "international units, only if printed in IU"),
        "calciumMg" to Schema.integer(),
        "ironMg" to Schema.double(description = "milligrams, as the panel prints it"),
        "potassiumMg" to Schema.integer(),
        OTHER_NUTRIENTS to Schema.array(
            Schema.obj(
                mapOf(
                    "name" to Schema.string(description = "the nutrient as the panel names it"),
                    "amount" to Schema.string(description = "the amount with its unit, as printed"),
                ),
                optionalProperties = listOf("amount"),
            ),
            description = "every other line the panel declares, in the order it prints them",
        ),
    ),
    optionalProperties = listOf(
        "name", "servingSize", "timesPerDay", "fiberG", "sugarG", "sodiumMg", "saltG",
        "vitaminDUg", "vitaminDIu", "calciumMg", "ironMg", "potassiumMg", OTHER_NUTRIENTS,
    ),
)

/** Stays in Kotlin: a wire key, named once because the schema above declares it twice. */
private const val OTHER_NUTRIENTS = "otherNutrients"

/**
 * The object in, a reading out.
 *
 * Every figure is read through a null-returning accessor rather than `optInt`'s zero default, the
 * one thing [ph.mart.healthapp.core.data.food.parseLabelReading] does differently from
 * [ph.mart.healthapp.core.data.food.parseRecognizedFoods] and for its reason: a missing line is one
 * the panel does not print, and `0` would state a figure the bottle never did. [Nutrients] is the
 * exception and keeps its own convention — `0` is unknown-or-none there.
 *
 * [SupplementLabelReading.panel] is assembled from **both** halves — the seven that map and the
 * rest — so what the user checks against the bottle is the whole panel rather than the quarter of
 * it this app happens to grade. The mapped lines are written back as the model read them, not as
 * `Nutrients` stores them, because a panel printing "Vitamin D3 2000 IU" should still read that way
 * under a supplement whose figure is 50 µg.
 */
internal fun parseSupplementLabel(json: String?): SupplementLabelReading {
    if (json.isNullOrBlank()) return SupplementLabelReading()
    val body = JSONObject(json)
    return SupplementLabelReading(
        name = body.optString("name").trim().ifBlank { null },
        dose = body.optString("servingSize").trim().ifBlank { null },
        timesPerDay = body.optIntOrNull("timesPerDay")?.coerceIn(SUPPLEMENT_TIMES_PER_DAY),
        nutrients = Nutrients(
            fiberG = body.optIntOrNull("fiberG") ?: 0,
            sugarG = body.optIntOrNull("sugarG") ?: 0,
            sodiumMg = sodiumMgFrom(body.optIntOrNull("sodiumMg"), body.optDoubleOrNull("saltG")),
            vitaminDUg = vitaminDUgFrom(
                body.optDoubleOrNull("vitaminDUg"),
                body.optIntOrNull("vitaminDIu"),
            ),
            calciumMg = body.optIntOrNull("calciumMg") ?: 0,
            ironUg = ironUgFrom(body.optDoubleOrNull("ironMg")),
            potassiumMg = body.optIntOrNull("potassiumMg") ?: 0,
        ),
        panel = body.panelLines(),
    )
}

/** The panel as printed: the lines this app has fields for, then the ones it does not. A line
 * with no amount is still a line — a "proprietary blend" declares no figure, and leaving it out
 * would make the readout disagree with the bottle. */
private fun JSONObject.panelLines(): String {
    val mapped = PRINTED_FIGURES.mapNotNull { (key, printed) ->
        val (label, unit) = printed
        val value = optDoubleOrNull(key) ?: return@mapNotNull null
        "$label ${trimZero(value)} $unit"
    }
    val others = optJSONArray(OTHER_NUTRIENTS)?.let { array ->
        (0 until array.length()).mapNotNull { index ->
            val row = array.optJSONObject(index) ?: return@mapNotNull null
            val name = row.optString("name").trim().ifBlank { null } ?: return@mapNotNull null
            val amount = row.optString("amount").trim()
            if (amount.isBlank()) name else "$name $amount"
        }
    }.orEmpty()
    return (mapped + others).joinToString("\n")
}

/**
 * The nine wire keys that carry a figure, and how a panel prints each one.
 *
 * Stays in Kotlin, and the unit symbols are the app's existing rule rather than an exception: g,
 * mg, µg and IU are not copy. The *labels* stay too, and deliberately are not
 * [ph.mart.healthapp.core.data.food.Nutrient]'s resource ones — this is a transcription of a
 * printed panel, not the app grading one, and a localized label here would translate half of a
 * readout whose other half is whatever the bottle happens to say.
 *
 * Both vitamin D keys appear because a label prints one or the other and this echoes back whichever
 * it was; the parse above resolves them to a single stored figure.
 */
private val PRINTED_FIGURES = listOf(
    "fiberG" to ("Fiber" to "g"),
    "sugarG" to ("Sugars" to "g"),
    "sodiumMg" to ("Sodium" to "mg"),
    "saltG" to ("Salt" to "g"),
    "vitaminDUg" to ("Vitamin D" to "µg"),
    "vitaminDIu" to ("Vitamin D" to "IU"),
    "calciumMg" to ("Calcium" to "mg"),
    "ironMg" to ("Iron" to "mg"),
    "potassiumMg" to ("Potassium" to "mg"),
)

/** "2.0 µg" reads wrong under a panel that printed "2". */
private fun trimZero(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

/** Absent is null, and so is a negative: a panel cannot print −3 mg of iron, so a model that
 * emitted one has not read anything there either. */
private fun JSONObject.optIntOrNull(key: String): Int? =
    if (has(key) && !isNull(key)) optInt(key, 0).takeIf { it > 0 } else null

private fun JSONObject.optDoubleOrNull(key: String): Double? =
    if (has(key) && !isNull(key)) optDouble(key, 0.0).takeIf { it > 0.0 } else null
