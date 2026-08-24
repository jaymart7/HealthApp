package ph.mart.healthapp.feature.progress.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressRepository

class AddPhotoViewModel(
    private val progressRepository: ProgressRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel(), OrbitContainerHost<AddPhotoUiState, AddPhotoUiState, AddPhotoSideEffect> {

    override val container = orbitContainer<AddPhotoUiState, AddPhotoSideEffect>(AddPhotoUiState()) {
        observePhotos(progressRepository, profileRepository)
    }

    fun handleEvent(event: AddPhotoEvent) {
        when (event) {
            is AddPhotoEvent.OnSave -> onSave(event.bitmap, event.form)
        }
    }

    private fun observePhotos(progressRepository: ProgressRepository, profileRepository: ProfileRepository) = intent {
        combine(progressRepository.observePhotos(), profileRepository.observeProfile()) { photos, profile ->
            AddPhotoUiState(photos = photos, preferredUnit = profile?.preferredUnit ?: UnitSystem.Metric)
        }.collect { newState -> reduce { newState } }
    }

    private fun onSave(bitmap: Bitmap, form: AddPhotoForm) = intent {
        progressRepository.addPhoto(bitmap, form.dateEpochDay, form.weightKg)
        postSideEffect(AddPhotoSideEffect.Saved)
    }
}
