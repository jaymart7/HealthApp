package ph.mart.healthapp.feature.progress.ui.photo

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressRepository

/**
 * The Photos page's container — the third of the triplet `ComparisonViewModel` and
 * `TimelapseViewModel` already form, reading the same two flows, because the page and the two
 * screens it launches draw the same shots with the same unit. It writes nothing; it exists so the
 * page owns the set it draws rather than being handed a slice of `ProgressUiState`, which is what
 * lets it be a route instead of a body inside the Progress tab. See `DECISIONS.md` →
 * **Progress photos & timelapse**.
 */
class PhotosViewModel(
    progressRepository: ProgressRepository,
    profileRepository: ProfileRepository,
) : ViewModel(), OrbitContainerHost<PhotosUiState, PhotosUiState, Nothing> {

    override val container = orbitContainer<PhotosUiState, Nothing>(PhotosUiState()) {
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
            PhotosUiState(photos = photos, unit = profile?.preferredUnit ?: UnitSystem.Metric)
        }.collect { newState -> reduce { newState } }
    }
}
