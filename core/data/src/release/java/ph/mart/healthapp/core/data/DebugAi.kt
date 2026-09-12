package ph.mart.healthapp.core.data

import ph.mart.healthapp.core.data.coach.CoachRepository
import ph.mart.healthapp.core.data.coach.CoachToolbox
import ph.mart.healthapp.core.data.food.FoodRecognitionRepository
import ph.mart.healthapp.core.data.food.MealIdeaRepository
import ph.mart.healthapp.core.data.food.MealParseRepository
import ph.mart.healthapp.core.data.insight.InsightRepository

/**
 * Release counterparts of the debug AI fakes — deliberately all null, which every binding reads as
 * "use the real repository". `seedDebugData`'s shape, and for its reason: a source-set pair means a
 * release build **cannot contain** the fakes, rather than merely never reaching them. A
 * `BuildConfig.DEBUG` branch in `main` would leave five fake repositories sitting beside the real
 * ones and trust R8 to notice.
 *
 * If this file ever drifts out of step with its debug twin, `assembleRelease` fails to compile.
 * That is the whole point of it.
 */
internal fun debugCoach(real: CoachRepository, toolbox: CoachToolbox): CoachRepository? = null

internal fun debugInsight(): InsightRepository? = null

internal fun debugRecognition(): FoodRecognitionRepository? = null

internal fun debugMealIdeas(): MealIdeaRepository? = null

internal fun debugMealParse(): MealParseRepository? = null
