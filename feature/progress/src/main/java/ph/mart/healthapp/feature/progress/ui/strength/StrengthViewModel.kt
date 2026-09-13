package ph.mart.healthapp.feature.progress.ui.strength

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.exercise.withSets
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem

/**
 * The Strength page's container — `SleepViewModel`'s shape, with the profile read for the unit the
 * volume and the records are quoted in rather than for the switcher.
 *
 * `withSets()` is applied here rather than in the screen, so [StrengthUiState.entries] means "the
 * lifting" and the page's empty state can be a plain `isEmpty()`. The writing is
 * `:feature:training`'s workout screen, under its own container.
 */
class StrengthViewModel(
    exerciseRepository: ExerciseRepository,
    profileRepository: ProfileRepository,
) : ViewModel(), OrbitContainerHost<StrengthUiState, StrengthUiState, Nothing> {

    override val container = orbitContainer<StrengthUiState, Nothing>(StrengthUiState()) {
        observeStrength(exerciseRepository, profileRepository)
    }

    private fun observeStrength(
        exerciseRepository: ExerciseRepository,
        profileRepository: ProfileRepository,
    ) = intent {
        combine(
            exerciseRepository.observeRecentEntries(),
            profileRepository.observeProfile(),
        ) { entries, profile ->
            StrengthUiState(
                entries = entries.withSets(),
                unit = profile?.preferredUnit ?: UnitSystem.Metric,
            )
        }.collect { newState -> reduce { newState } }
    }
}
