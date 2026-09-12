package ph.mart.healthapp.feature.progress.ui.comparison

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressRepository

/**
 * The comparison flow's container. It writes nothing — the two repositories are read so the
 * overlay owns the data it draws rather than being handed a slice of `ProgressUiState`, which is
 * the price of the slider being a screen with its own package instead of a sub-view of the Photos
 * page. See `DECISIONS.md` → **Progress photos & timelapse**.
 */
class ComparisonViewModel(
    progressRepository: ProgressRepository,
    profileRepository: ProfileRepository,
) : ViewModel(), OrbitContainerHost<ComparisonUiState, ComparisonUiState, Nothing> {

    /** The grid's selection, and the only thing that re-points the pair below. `FoodViewModel`'s
     * `selectedDate` shape: an input the UI changes, combined rather than reduced separately. */
    private val selectedIds = MutableStateFlow(emptyList<Long>())

    override val container = orbitContainer<ComparisonUiState, Nothing>(ComparisonUiState()) {
        observePhotos(progressRepository, profileRepository)
    }

    fun handleEvent(event: ComparisonEvent) {
        when (event) {
            is ComparisonEvent.OnSelect -> selectedIds.value = event.ids
        }
    }

    private fun observePhotos(
        progressRepository: ProgressRepository,
        profileRepository: ProfileRepository,
    ) = intent {
        combine(
            progressRepository.observePhotos(),
            profileRepository.observeProfile(),
            selectedIds,
        ) { photos, profile, ids ->
            ComparisonUiState(
                photos = photos,
                unit = profile?.preferredUnit ?: UnitSystem.Metric,
                selectedIds = ids,
                goal = profile?.goal,
            )
        }.collect { newState -> reduce { newState } }
    }
}
