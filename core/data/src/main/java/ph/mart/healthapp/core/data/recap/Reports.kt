package ph.mart.healthapp.core.data.recap

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.health.DEFAULT_STEP_GOAL
import ph.mart.healthapp.core.data.health.StepDay
import ph.mart.healthapp.core.data.health.StepsRepository
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.mood.MoodRepository
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.streak.loggedDays
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.data.water.WaterRepository

/**
 * The only two windows a report may cover.
 *
 * A week and a month, which are the two windows `RecapPeriod` already offers the coach a door for
 * and the two `MAX_HISTORY_DAYS` can carry — a year card in a chat is a card whose window the
 * coach cannot then discuss, the confident-answer-over-the-wrong-window failure the subject doors
 * are written against. An arbitrary integer would also leave the set of folds [observeReports]
 * holds unbounded.
 *
 * Here rather than beside the tool that takes it, because three things read it: the tool's schema,
 * the parse that validates a call, and the card's own period chips.
 */
val REPORT_DAYS = listOf(7, 30)

/**
 * One window, folded, with the two dense series its charts draw.
 *
 * [recap] is null when nothing at all was logged in the window — the card says so rather than
 * drawing four zeros, which is what the recap screen already does.
 *
 * [calories] and [steps] are sliced to the window here rather than in the card: a composable that
 * does date arithmetic is a rule no JVM test can reach, and both series arrive long.
 */
data class Report(
    val days: Int,
    val recap: Recap?,
    val calories: List<DayNutrition>,
    val steps: List<StepDay>,
    val stepGoal: Int,
    val unit: UnitSystem,
)

/**
 * Every window at once, keyed by [Report.days] — the shape `observeInsightRequest` has, for its
 * reason: the coach has no folded state of its own, Progress does, and neither should own the
 * knowledge of how a report is assembled.
 *
 * **Both windows, always.** The card's period chips switch between them with no round trip, and a
 * fold over lists already in memory costs nothing next to the seven flows underneath it. It is the
 * subscription that is expensive, and it is the same one either way.
 *
 * Photos are deliberately not read: a report card in a chat transcript draws none, and
 * `observePhotos()` is the one flow here that would be collected for nothing.
 *
 * Two nested combines because the typed overloads stop at five and this needs seven — the same
 * shape `observeInsightRequest` and `RecapViewModel` both use. `todayEpochDay()` is read on every
 * emission rather than once, so a session left open across midnight doesn't freeze the window.
 */
fun observeReports(
    profileRepository: ProfileRepository,
    foodRepository: FoodRepository,
    progressRepository: ProgressRepository,
    waterRepository: WaterRepository,
    exerciseRepository: ExerciseRepository,
    moodRepository: MoodRepository,
    stepsRepository: StepsRepository,
): Flow<Map<Int, Report>> {
    // The four-domain definition, so a report can never disagree with the streak about what a
    // logged day is — `RecapViewModel.observeRecap()`'s call, chained for its reason.
    val activeDays = combine(
        foodRepository.observeDailyNutrition(),
        waterRepository.observeLoggedDays(),
        progressRepository.observeWeightEntries(),
        exerciseRepository.observeLoggedDays(),
        ::loggedDays,
    )

    // Grouped before the outer combine, which is already at the arity the typed overloads stop
    // at. Mood rides with movement for that reason alone.
    val movement = combine(
        exerciseRepository.observeRecentEntries(),
        stepsRepository.observeDays(),
        moodRepository.observeDays(),
        ::Movement,
    )

    return combine(
        profileRepository.observeProfile(),
        foodRepository.observeDailyNutrition(),
        progressRepository.observeWeightEntries(),
        activeDays,
        movement,
    ) { profile, dailyNutrition, weightEntries, days, moves ->
        reports(profile, dailyNutrition, weightEntries, days, moves, todayEpochDay())
    }
}

/** The fold itself, lifted out of the flow so a test can reach it. */
private fun reports(
    profile: Profile?,
    dailyNutrition: List<DayNutrition>,
    weightEntries: List<WeightEntry>,
    activeDays: Set<Long>,
    moves: Movement,
    today: Long,
): Map<Int, Report> {
    val stepGoal = profile?.stepGoal ?: DEFAULT_STEP_GOAL
    return REPORT_DAYS.associateWith { days ->
        val from = today - (days - 1)
        Report(
            days = days,
            recap = recap(
                days = days,
                dailyNutrition = dailyNutrition,
                activeDays = activeDays,
                weightEntries = weightEntries,
                moodDays = moves.moodDays,
                targets = profile?.dailyTargets(),
                todayEpochDay = today,
                exerciseEntries = moves.exercise,
                stepDays = moves.stepDays,
                stepGoal = stepGoal,
            ),
            // The dense series is a plain tail slice — `recap()`'s own reading of it. The sparse
            // one is sliced by date, because its last rows could reach back months.
            calories = dailyNutrition.takeLast(days),
            steps = moves.stepDays.filter { it.dateEpochDay in from..today },
            stepGoal = stepGoal,
            unit = profile?.preferredUnit ?: UnitSystem.Metric,
        )
    }
}

/** Grouped so the outer combine stays inside the typed overloads' five-flow arity. Private and
 * structural — it never leaves this file. */
private data class Movement(
    val exercise: List<ExerciseEntry>,
    val stepDays: List<StepDay>,
    val moodDays: List<MoodDay>,
)
