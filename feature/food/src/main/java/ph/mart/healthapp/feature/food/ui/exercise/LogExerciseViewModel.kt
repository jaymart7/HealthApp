package ph.mart.healthapp.feature.food.ui.exercise

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.exercise.recentLiftNames
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressRepository

/**
 * Shared by the log-exercise sheet and the strength workout screen — one form, two presentations,
 * so there is no second ViewModel and therefore no second flow package (see CLAUDE.md's rule).
 *
 * The always-on read side is two numbers: the weight the MET estimate multiplies by, and the unit
 * the strength screen prints loads in. The weight is the latest weigh-in rather than
 * `Profile.weightKg`, which is the onboarding weight and is never updated — same fallback rule
 * `trendVsSevenDaysAgo(fallbackKg)` uses on Home.
 *
 * Everything the strength screen needs is loaded on demand instead, by
 * [LogExerciseEvent.OnOpenStrength]: the sheet shares this container, and it shows none of it.
 */
class LogExerciseViewModel(
    private val exerciseRepository: ExerciseRepository,
    profileRepository: ProfileRepository,
    progressRepository: ProgressRepository,
) : ViewModel(), OrbitContainerHost<LogExerciseUiState, LogExerciseUiState, LogExerciseSideEffect> {

    override val container = orbitContainer<LogExerciseUiState, LogExerciseSideEffect>(LogExerciseUiState()) {
        observeWeight(profileRepository, progressRepository)
    }

    fun handleEvent(event: LogExerciseEvent) {
        when (event) {
            is LogExerciseEvent.OnSave -> onSave(event.form, event.dateEpochDay, event.editingId)
            is LogExerciseEvent.OnOpenStrength -> onOpenStrength(event.editingId)
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
            // Copied onto the state rather than replacing it: the strength load below can land
            // first, and a weigh-in landing after it must not erase the workout being edited.
            reduce {
                state.copy(
                    weightKg = latestKg ?: profile?.weightKg ?: LogExerciseUiState().weightKg,
                    preferredUnit = profile?.preferredUnit ?: UnitSystem.Metric,
                )
            }
        }
    }

    /** One read for all three: the row being corrected, the session to repeat, and the chips.
     * [strengthLoaded] is what the screen waits on before it composes a form. */
    private fun onOpenStrength(editingId: Long) = intent {
        val recent = exerciseRepository.recentStrengthEntries()
        val editing = editingId.takeIf { it > 0 }?.let { exerciseRepository.entry(it) }
        reduce {
            state.copy(
                editing = editing,
                // Never the row being corrected: "repeat" would then offer the workout already
                // on screen, which is the one session it can't usefully seed.
                lastWorkout = recent.firstOrNull { it.id != editingId },
                recentLifts = recent.recentLiftNames(),
                strengthLoaded = true,
            )
        }
    }

    private fun onSave(form: LogExerciseForm, dateEpochDay: Long, editingId: Long?) = intent {
        val entry = form.toExerciseEntry(dateEpochDay)
        if (editingId == null) {
            exerciseRepository.addEntry(entry)
        } else {
            exerciseRepository.updateEntry(entry.copy(id = editingId))
        }
        postSideEffect(LogExerciseSideEffect.Saved)
    }
}
