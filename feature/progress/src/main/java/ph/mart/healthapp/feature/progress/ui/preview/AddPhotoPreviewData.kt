package ph.mart.healthapp.feature.progress.ui.preview

import android.graphics.Bitmap
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.nowMinuteOfDay
import ph.mart.healthapp.core.data.todayEpochDay

/** [photos] is only ever read for the calendar's marked dates — the set is the Photos page's job. */
data class AddPhotoPreviewUiState(
    val photos: List<ProgressPhoto> = emptyList(),
    val preferredUnit: UnitSystem = UnitSystem.Metric,
)

/** [minuteOfDay] opens at now, and the shot is filed under it — when a progress photo was taken
 * is as much of the reading as the day it was taken on. */
data class AddPhotoPreviewForm(
    val dateEpochDay: Long = todayEpochDay(),
    val weightKg: Double? = null,
    val minuteOfDay: Int = nowMinuteOfDay(),
)

sealed interface AddPhotoPreviewEvent {
    /** The decoded staging file, not its path: the repository re-encodes into `filesDir`, and the
     * screen has the bitmap in hand anyway to draw it. */
    data class OnSave(val bitmap: Bitmap, val form: AddPhotoPreviewForm) : AddPhotoPreviewEvent
}

sealed interface AddPhotoPreviewSideEffect {
    data object Saved : AddPhotoPreviewSideEffect
}
