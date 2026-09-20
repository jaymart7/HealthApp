package ph.mart.healthapp.feature.progress.ui.preview

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressRepository

class AddPhotoPreviewViewModel(
    private val progressRepository: ProgressRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel(), OrbitContainerHost<AddPhotoPreviewUiState, AddPhotoPreviewUiState, AddPhotoPreviewSideEffect> {

    override val container = orbitContainer<AddPhotoPreviewUiState, AddPhotoPreviewSideEffect>(AddPhotoPreviewUiState()) {
        observePhotos(progressRepository, profileRepository)
    }

    fun handleEvent(event: AddPhotoPreviewEvent) {
        when (event) {
            is AddPhotoPreviewEvent.OnSave -> onSave(event.bitmap, event.form)
        }
    }

    private fun observePhotos(progressRepository: ProgressRepository, profileRepository: ProfileRepository) = intent {
        combine(
            progressRepository.observePhotos(),
            progressRepository.observeWeightEntries(),
            profileRepository.observeProfile(),
        ) { photos, weights, profile ->
            AddPhotoPreviewUiState(
                photos = photos,
                preferredUnit = profile?.preferredUnit ?: UnitSystem.Metric,
                // The latest weigh-in ahead of the profile: logging a weight does not write back to
                // the profile, so the entries are the fresher of the two.
                currentWeightKg = weights.maxByOrNull { it.dateEpochDay }?.weightKg
                    ?: profile?.weightKg
                    ?: FALLBACK_WEIGHT_KG,
            )
        }.collect { newState -> reduce { newState } }
    }

    private fun onSave(bitmap: Bitmap, form: AddPhotoPreviewForm) = intent {
        progressRepository.addPhoto(bitmap, form.dateEpochDay, form.weightKg, form.minuteOfDay)
        postSideEffect(AddPhotoPreviewSideEffect.Saved)
    }
}
