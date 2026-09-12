package ph.mart.healthapp.feature.progress.ui.timelapse

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressRepository

/**
 * The timelapse flow's container, and `ComparisonViewModel`'s twin — the same two flows, because
 * the two photo overlays draw the same shots with the same unit. It writes nothing; it exists so
 * the player owns the set it plays rather than being handed a slice of `ProgressUiState`. See
 * `DECISIONS.md` → **Progress photos & timelapse**.
 */
class TimelapseViewModel(
    progressRepository: ProgressRepository,
    profileRepository: ProfileRepository,
) : ViewModel(), OrbitContainerHost<TimelapseUiState, TimelapseUiState, Nothing> {

    override val container = orbitContainer<TimelapseUiState, Nothing>(TimelapseUiState()) {
        observePhotos(progressRepository, profileRepository)
    }

    private fun observePhotos(
        progressRepository: ProgressRepository,
        profileRepository: ProfileRepository,
    ) = intent {
        combine(
            progressRepository.observePhotos(),
            profileRepository.observeProfile(),
        ) { photos, profile ->
            TimelapseUiState(photos = photos, unit = profile?.preferredUnit ?: UnitSystem.Metric)
        }.collect { newState -> reduce { newState } }
    }
}
