package ph.mart.healthapp.core.data.food

/**
 * The fourth Gemini-backed feature, and the third payload type that leaves the device — the
 * narrowest of the three.
 *
 * [InsightRequest][ph.mart.healthapp.core.data.insight.InsightRequest] describes the whole day and
 * [MealIdeaRequest] describes the gap that is left; this one carries the user's own sentence and
 * nothing else. No goal, no gaps, no diet, no profile: parsing "two scrambled eggs and toast" needs
 * none of them, so sending them would be widening the payload for nothing.
 *
 * Nothing is cached, for the coach's reason rather than the daily insight's: every sentence is its
 * own answer.
 */
interface MealParseRepository {
    suspend fun parse(text: String): MealParseResult
}

/**
 * [NoFoodFound] is its own state rather than an empty [Success], the call `ScanFlow.NoBarcode`
 * makes: "there was nothing edible in that sentence" and "the call didn't work" are different
 * answers, and the screen says different things about them.
 */
sealed interface MealParseResult {
    data class Success(val foods: List<RecognizedFood>) : MealParseResult
    data object NoFoodFound : MealParseResult
    data object Failed : MealParseResult
}

/**
 * How many foods one sentence may become. Nobody names nine things they ate in one breath, and the
 * cap is what stops a model that has started listing side dishes from filling a diary section.
 */
const val MAX_PARSED_FOODS = 8

/**
 * How much of the sentence is sent. A dictated meal is a sentence; a pasted page is not a meal, and
 * `MAX_REPLY_CHARS`' reasoning applies in the other direction — it is cheaper to cap the input than
 * to pay for a parse of something that was never a meal.
 */
const val MAX_PARSE_CHARS = 300

/**
 * The whole of the trust boundary on the model's parse — everything the review screen renders
 * passes through here.
 *
 * A pure function for [fitting]'s reason: the [org.json.JSONArray] parse around it is stubbed in
 * JVM unit tests, so the judgement is kept on this side of it where a test can reach it.
 *
 * Filtering rather than truncating: an item with no name or no calories is not a shorter item, it
 * is not one. There is deliberately no per-item calorie *ceiling* — unlike a meal idea, which is
 * offered against a budget the header has just quoted, a parse is a claim about what the user
 * already ate, and the review screen shows every figure before anything is written.
 */
fun List<RecognizedFood>.loggable(): List<RecognizedFood> =
    filter { it.name.isNotBlank() && it.calories > 0 }.take(MAX_PARSED_FOODS)
