package ph.mart.healthapp.feature.food.ui.quicklog

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.exercise.estimateBurnedKcal
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.MAX_PARSE_CHARS
import ph.mart.healthapp.core.data.food.QuickLogRepository
import ph.mart.healthapp.core.data.food.QuickLogResult
import ph.mart.healthapp.core.data.food.QuickLogTurn
import ph.mart.healthapp.core.data.food.offlineQuickLog
import ph.mart.healthapp.core.data.network.NetworkMonitor
import ph.mart.healthapp.core.data.nowMinuteOfDay
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.displayUnitToKg
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.data.water.WaterRepository

/**
 * The FAB sheet's container. Its own flow package because it is a second ViewModel in this
 * feature — `VoiceLogViewModel`'s shape with an exercise half bolted on.
 *
 * The read side is `LogExerciseViewModel`'s combine exactly: the latest weigh-in (falling back to
 * the onboarding weight) prices a parsed activity, and the profile's switch decides whether a save
 * is a credit worth congratulating.
 *
 * The sheet is hosted by `AppScaffold`, outside any nav entry, so this outlives it — which is why
 * dismissing sends [QuickLogEvent.OnCancel], or a late answer would land on the next blank sheet.
 */
/** The strip's length — `VoiceLogViewModel`'s three, since it is the same strip. */
private const val RECENT_SENTENCES = 3

class QuickLogViewModel(
    private val quickLogRepository: QuickLogRepository,
    private val foodRepository: FoodRepository,
    private val exerciseRepository: ExerciseRepository,
    private val networkMonitor: NetworkMonitor,
    private val waterRepository: WaterRepository,
    private val progressRepository: ProgressRepository,
    profileRepository: ProfileRepository,
) : ViewModel(), OrbitContainerHost<QuickLogUiState, QuickLogUiState, QuickLogSideEffect> {

    override val container = orbitContainer<QuickLogUiState, QuickLogSideEffect>(QuickLogUiState()) {
        observeWeight(profileRepository, progressRepository)
        observeRecentSentences()
    }

    /** Cancelled by back, by dismissing the sheet, and by the next send. */
    private var sendJob: Job? = null

    fun handleEvent(event: QuickLogEvent) {
        when (event) {
            is QuickLogEvent.OnSend -> onSend(event.turns)
            QuickLogEvent.OnCancel -> sendJob?.cancel()
            is QuickLogEvent.OnLog -> onLog(event)
            is QuickLogEvent.OnUndo -> onUndo(event.batch)
        }
    }

    private fun observeWeight(
        profileRepository: ProfileRepository,
        progressRepository: ProgressRepository,
    ) = intent {
        combine(
            profileRepository.observeProfile(),
            progressRepository.observeWeightEntries(),
        ) { profile, entries ->
            entries.maxByOrNull { it.dateEpochDay }?.weightKg to profile
        }.collect { (latestKg, profile) ->
            reduce {
                state.copy(
                    weightKg = latestKg ?: profile?.weightKg ?: QuickLogUiState().weightKg,
                    addExerciseToBudget = profile?.addExerciseToBudget != false,
                    unit = profile?.preferredUnit ?: UnitSystem.Metric,
                )
            }
        }
    }

    /** Talk-to-log's list, read here too — one store of "sentences that became meals". */
    private fun observeRecentSentences() = intent {
        foodRepository.observeRecentSentences(RECENT_SENTENCES).collect { sentences ->
            reduce { state.copy(recentSentences = sentences) }
        }
    }

    /**
     * Online, the model. Offline — asked at the moment of the send, the rule every AI call site
     * follows — the phone's own word match over everything the user said, against their own foods
     * first: the offline-first rule, applied to the one AI surface that can still do something
     * without the model. It asks nothing, and every row it finds is a tagged guess.
     */
    private fun onSend(turns: List<QuickLogTurn>) {
        sendJob?.cancel()
        sendJob = intent {
            val online = networkMonitor.isOnline()
            val result = if (online) {
                quickLogRepository.parse(turns)
            } else {
                val said = turns.filter { it.fromUser }.joinToString(" ") { it.text }
                offlineQuickLog(said, foodRepository.observeMyFoods().first())
            }
            val effect = when (result) {
                is QuickLogResult.Question -> QuickLogSideEffect.Asked(result.text)
                is QuickLogResult.Parsed -> QuickLogSideEffect.Parsed(
                    foods = result.foods,
                    exercises = result.activities.map { activity ->
                        ExerciseEntry(
                            type = activity.type,
                            name = activity.name,
                            minutes = activity.minutes,
                            burnedKcal = estimateBurnedKcal(activity.type, activity.minutes, state.weightKg),
                        )
                    },
                    mealType = result.mealType,
                    waterGlasses = result.waterGlasses,
                    // The number the user said, in their own unit — `CoachRepositoryImpl`'s one
                    // conversion, made here for the same reason: the model never picks the unit.
                    weightKg = result.weight?.displayUnitToKg(state.unit),
                    offline = !online,
                )
                QuickLogResult.NothingFound -> QuickLogSideEffect.NothingFound(offline = !online)
                QuickLogResult.Failed -> QuickLogSideEffect.Failed
            }
            postSideEffect(effect)
        }
    }

    /**
     * Day 0 on both — the FAB is today-only, and both repositories stamp it.
     *
     * The sentence is remembered only for a meal and nothing else: talk-to-log offers the same list
     * back under a *food* field, and "30 min run" there is a sentence that can only fail. After the
     * write, `VoiceLogViewModel.logMeal`'s order — a log that never happened proves nothing.
     */
    private fun onLog(event: QuickLogEvent.OnLog) = intent {
        val foods = event.foods
        val exercises = event.exercises
        val foodIds = if (foods.isNotEmpty()) foodRepository.addEntries(foods) else emptyList()
        val exerciseIds = exercises.map { exerciseRepository.addEntry(it) }
        // Added to the day, never assigned — the coach's water rule — and the count before is kept
        // so Undo can put it back exactly.
        val waterBefore = event.waterGlasses?.let { glasses ->
            waterRepository.observeToday().first().also { waterRepository.setToday(it + glasses) }
        }
        // Keyed on the day, so it replaces today's weigh-in the way the weigh-in sheet does; the
        // one it replaces is what Undo restores.
        val today = todayEpochDay()
        val weightBefore = event.weightKg?.let { kg ->
            progressRepository.observeWeightEntries().first().firstOrNull { it.dateEpochDay == today }.also {
                progressRepository.upsertWeightEntry(
                    WeightEntry(dateEpochDay = today, weightKg = kg, minuteOfDay = nowMinuteOfDay()),
                )
            }
        }
        if (foods.isNotEmpty() && exercises.isEmpty() && event.sentence.isNotBlank()) {
            foodRepository.recordSentence(event.sentence.take(MAX_PARSE_CHARS))
        }
        val credited = if (state.addExerciseToBudget) exercises.sumOf { it.burnedKcal } else 0
        val batch = LoggedBatch(
            foodIds = foodIds,
            exerciseIds = exerciseIds,
            waterBefore = waterBefore,
            weightDay = today.takeIf { event.weightKg != null },
            weightBefore = weightBefore,
        )
        postSideEffect(QuickLogSideEffect.Logged(credited, batch))
    }

    /**
     * Every write of one Log, reversed. Food and activities soft-delete, the module rule; water goes
     * back to its count; a weigh-in goes back to the one it replaced, or away if it replaced none —
     * the weigh-in sheet's own delete, and weigh-ins are the one domain keyed by day, not by row.
     */
    private fun onUndo(batch: LoggedBatch) = intent {
        batch.foodIds.forEach { foodRepository.deleteEntry(it) }
        batch.exerciseIds.forEach { exerciseRepository.deleteEntry(it) }
        batch.waterBefore?.let { waterRepository.setToday(it) }
        batch.weightDay?.let { day ->
            batch.weightBefore?.let { progressRepository.upsertWeightEntry(it) }
                ?: progressRepository.deleteWeightEntry(day)
        }
    }
}
