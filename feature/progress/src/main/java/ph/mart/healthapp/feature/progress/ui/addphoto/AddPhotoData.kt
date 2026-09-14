package ph.mart.healthapp.feature.progress.ui.addphoto

import android.graphics.Bitmap
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.todayEpochDay

/** The route opens on [Capture]; [PermissionDenied] is where it lands instead when the camera is
 * refused, and the only step with no photo behind it. */
enum class AddPhotoStep { Capture, Preview, PermissionDenied }

data class AddPhotoUiState(val photos: List<ProgressPhoto> = emptyList(), val preferredUnit: UnitSystem = UnitSystem.Metric)

data class AddPhotoForm(val dateEpochDay: Long = todayEpochDay(), val weightKg: Double? = null)

sealed interface AddPhotoEvent {
    data class OnSave(val bitmap: Bitmap, val form: AddPhotoForm) : AddPhotoEvent
}

sealed interface AddPhotoSideEffect {
    data object Saved : AddPhotoSideEffect
}
