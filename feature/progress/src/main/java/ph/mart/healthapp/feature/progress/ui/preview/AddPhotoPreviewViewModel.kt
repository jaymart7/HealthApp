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
        combine(progressRepository.observePhotos(), profileRepository.observeProfile()) { photos, profile ->
            AddPhotoPreviewUiState(photos = photos, preferredUnit = profile?.preferredUnit ?: UnitSystem.Metric)
        }.collect { newState -> reduce { newState } }
    }

    private fun onSave(bitmap: Bitmap, form: AddPhotoPreviewForm) = intent {
        progressRepository.addPhoto(bitmap, form.dateEpochDay, form.weightKg, form.minuteOfDay)
        postSideEffect(AddPhotoPreviewSideEffect.Saved)
    }
}
