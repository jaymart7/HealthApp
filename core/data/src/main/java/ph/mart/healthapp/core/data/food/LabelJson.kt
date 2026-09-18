package ph.mart.healthapp.core.data.food

import com.google.firebase.ai.type.Schema
import org.json.JSONObject

/**
 * The wire shape of a [LabelReading], and the one place that reads it back.
 *
 * Flat, with the units in the field names — the convention [Nutrients] sets and
 * [RECOGNIZED_FOOD_SCHEMA] follows. **Every property is optional**, which is the whole difference
 * between this schema and that one: the recognition schema asks a model to estimate and a missing
 * field there is a short answer, while here a missing field is the panel not printing that line.
 * Required properties would make the model invent one.
 *
 * Three fields exist only because labels are not consistent about which unit they print in — a
 * pack declares salt *or* sodium, and vitamin D in micrograms *or* international units. Both forms
 * are carried raw and resolved in Kotlin by [sodiumMgFrom] and [vitaminDUgFrom], where the
 * arithmetic is testable and the model is not asked to do any.
 *
 * Here rather than in [LabelReading.kt][readable] because that file is pure and JVM-tested;
 * [org.json] is stubbed on the JVM, so the parse below cannot be tested there and the judgement it
 * feeds ([readable]) deliberately can. The split [RECOGNIZED_FOOD_SCHEMA] documents.
 */
internal val LABEL_SCHEMA = Schema.obj(
    mapOf(
        "name" to Schema.string(description = "the product name, only if it is printed and visible"),
        "servingSize" to Schema.string(
            description = "the serving in the label's own words, e.g. \"30 g\", \"1 bar (25 g)\"",
        ),
        "basis" to Schema.enumeration(
            listOf(BASIS_PER_100G, BASIS_PER_SERVING),
            description = "which column the figures below were read from",
        ),
        "calories" to Schema.integer(description = "kcal, not kJ"),
        "proteinG" to Schema.integer(),
        "carbsG" to Schema.integer(),
        "fatG" to Schema.integer(),
        "fiberG" to Schema.integer(),
        "sugarG" to Schema.integer(),
        "sodiumMg" to Schema.integer(description = "milligrams, only if sodium itself is printed"),
        "saltG" to Schema.double(description = "grams, only if the label prints salt instead of sodium"),
        "vitaminDUg" to Schema.double(description = "micrograms, only if printed in µg"),
        "vitaminDIu" to Schema.integer(description = "international units, only if printed in IU"),
        "calciumMg" to Schema.integer(),
        "ironMg" to Schema.double(description = "milligrams, as the panel prints it"),
        "potassiumMg" to Schema.integer(),
    ),
    optionalProperties = listOf(
        "name", "servingSize", "basis", "calories", "proteinG", "carbsG", "fatG", "fiberG",
        "sugarG", "sodiumMg", "saltG", "vitaminDUg", "vitaminDIu", "calciumMg", "ironMg",
        "potassiumMg",
    ),
)

/** Stays in Kotlin: compared wire values, not copy. The schema above enumerates these two exact
 * strings and [parseLabelReading] switches on them. */
internal const val BASIS_PER_100G = "per_100g"

internal const val BASIS_PER_SERVING = "per_serving"

/**
 * The object in, a reading out.
 *
 * Every figure is read through a null-returning accessor rather than `optInt`'s zero default, which
 * is the one thing this parser does differently from [parseRecognizedFoods]: there, a missing
 * calorie count is a short answer from a model that was asked to estimate; here it is a line the
 * panel does not print, and `0` would state a figure the packet never did.
 *
 * [Nutrients] is the exception and keeps its own convention — `0` is unknown-or-none, the app
 * reports coverage alongside, and nothing downstream of this has ever been able to tell them apart.
 */
internal fun parseLabelReading(json: String?): LabelReading {
    if (json.isNullOrBlank()) return LabelReading()
    val body = JSONObject(json)
    return LabelReading(
        name = body.optString("name").trim().ifBlank { null },
        servingSize = body.optString("servingSize").trim().ifBlank { null },
        basis = if (body.optString("basis") == BASIS_PER_SERVING) {
            LabelBasis.PerServing
        } else {
            LabelBasis.Per100g
        },
        calories = body.optIntOrNull("calories"),
        proteinG = body.optIntOrNull("proteinG"),
        carbsG = body.optIntOrNull("carbsG"),
        fatG = body.optIntOrNull("fatG"),
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
    )
}

/** Absent is null, and so is a negative: a panel cannot print −3 g of fat, so a model that emitted
 * one has not read anything there either. */
private fun JSONObject.optIntOrNull(key: String): Int? =
    if (has(key) && !isNull(key)) optInt(key, 0).takeIf { it > 0 } else null

private fun JSONObject.optDoubleOrNull(key: String): Double? =
    if (has(key) && !isNull(key)) optDouble(key, 0.0).takeIf { it > 0.0 } else null
