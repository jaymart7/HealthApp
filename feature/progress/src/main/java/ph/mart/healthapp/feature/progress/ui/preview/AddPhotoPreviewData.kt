package ph.mart.healthapp.feature.progress.ui.preview

import android.graphics.Bitmap
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.nowMinuteOfDay
import ph.mart.healthapp.core.data.todayEpochDay

/** What a blank weight stepper opens on before there is a weigh-in or a profile to read. */
internal const val FALLBACK_WEIGHT_KG = 70.0

/**
 * [photos] is only ever read for the calendar's marked dates — the set is the Photos page's job.
 *
 * [currentWeightKg] is what the untouched weight stepper steps off: the latest weigh-in, or the
 * profile's own figure until there is one.
 */
data class AddPhotoPreviewUiState(
    val photos: List<ProgressPhoto> = emptyList(),
    val preferredUnit: UnitSystem = UnitSystem.Metric,
    val currentWeightKg: Double = FALLBACK_WEIGHT_KG,
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
