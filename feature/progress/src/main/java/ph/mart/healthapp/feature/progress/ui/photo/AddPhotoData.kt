package ph.mart.healthapp.feature.progress.ui.photo

import android.graphics.Bitmap
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.designsystem.component.todayEpochDay

enum class AddPhotoStep { Pick, Capture, Preview }

data class AddPhotoUiState(val photos: List<ProgressPhoto> = emptyList(), val preferredUnit: UnitSystem = UnitSystem.Metric)

data class AddPhotoForm(val dateEpochDay: Long = todayEpochDay(), val weightKg: Double? = null)

sealed interface AddPhotoEvent {
    data class OnSave(val bitmap: Bitmap, val form: AddPhotoForm) : AddPhotoEvent
}

sealed interface AddPhotoSideEffect {
    data object Saved : AddPhotoSideEffect
}
