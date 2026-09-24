package ph.mart.healthapp.feature.food.ui.quicklog

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.exercise.estimateBurnedKcal
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.QuickLogRepository
import ph.mart.healthapp.core.data.food.QuickLogResult
import ph.mart.healthapp.core.data.food.QuickLogTurn
import ph.mart.healthapp.core.data.network.NetworkMonitor
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.progress.ProgressRepository

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
class QuickLogViewModel(
    private val quickLogRepository: QuickLogRepository,
    private val foodRepository: FoodRepository,
    private val exerciseRepository: ExerciseRepository,
    private val networkMonitor: NetworkMonitor,
    profileRepository: ProfileRepository,
    progressRepository: ProgressRepository,
) : ViewModel(), OrbitContainerHost<QuickLogUiState, QuickLogUiState, QuickLogSideEffect> {

    override val container = orbitContainer<QuickLogUiState, QuickLogSideEffect>(QuickLogUiState()) {
        observeWeight(profileRepository, progressRepository)
    }

    /** Cancelled by back, by dismissing the sheet, and by the next send. */
    private var sendJob: Job? = null

    /** Asked at the moment of the tap, not observed — `LogExerciseViewModel.isOnline`'s reason. */
    fun isOnline(): Boolean = networkMonitor.isOnline()

    fun handleEvent(event: QuickLogEvent) {
        when (event) {
            is QuickLogEvent.OnSend -> onSend(event.turns)
            QuickLogEvent.OnCancel -> sendJob?.cancel()
            is QuickLogEvent.OnLog -> onLog(event.foods, event.exercises)
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
                )
            }
        }
    }

    private fun onSend(turns: List<QuickLogTurn>) {
        sendJob?.cancel()
        sendJob = intent {
            val effect = when (val result = quickLogRepository.parse(turns)) {
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
                )
                QuickLogResult.NothingFound -> QuickLogSideEffect.NothingFound
                QuickLogResult.Failed -> QuickLogSideEffect.Failed
            }
            postSideEffect(effect)
        }
    }

    /** Day 0 on both — the FAB is today-only, and both repositories stamp it. */
    private fun onLog(foods: List<FoodEntry>, exercises: List<ExerciseEntry>) = intent {
        if (foods.isNotEmpty()) foodRepository.addEntries(foods)
        exercises.forEach { exerciseRepository.addEntry(it) }
        val credited = if (state.addExerciseToBudget) exercises.sumOf { it.burnedKcal } else 0
        postSideEffect(QuickLogSideEffect.Logged(credited))
    }
}
