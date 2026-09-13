package ph.mart.healthapp.feature.progress.ui.nutrition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.progress.ChartRange
import ph.mart.healthapp.feature.progress.ui.progress.DEFAULT_CHART_RANGE

@Composable
internal fun rememberNutritionState(): NutritionState =
    rememberSaveable(saver = NutritionState.Saver()) { NutritionState() }

/**
 * UI-only: the range the calorie chart is showing, and the meal-photo gallery's two levels.
 *
 * [viewedMealPhotoId] is the plate opened full-frame *inside* the gallery, so back closes the frame
 * before the gallery — the gallery wires that handler itself. Both fields moved out of
 * `ProgressScreenState` outright, this page being the gallery's only door, so that saver renumbers
 * in the same commit.
 */
internal class NutritionState(
    range: ChartRange = DEFAULT_CHART_RANGE,
    galleryOpen: Boolean = false,
    viewedMealPhotoId: Long? = null,
) {
    var range: ChartRange by mutableStateOf(range)
    var galleryOpen: Boolean by mutableStateOf(galleryOpen)
    var viewedMealPhotoId: Long? by mutableStateOf(viewedMealPhotoId)

    /** The strip's tiles open the gallery *on* the plate they show, so a tap lands where it was
     * aimed rather than at the top of a grid. */
    fun openGallery(photoId: Long? = null) {
        viewedMealPhotoId = photoId
        galleryOpen = true
    }

    fun closeGallery() {
        galleryOpen = false
        viewedMealPhotoId = null
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun Saver(): Saver<NutritionState, Any> = listSaver(
            // Appended, never renumbered — the rule `ProgressScreenState`'s saver keeps.
            save = { listOf(it.range.name, it.galleryOpen, it.viewedMealPhotoId) },
            restore = { saved ->
                NutritionState(
                    range = ChartRange.valueOf(saved[0] as String),
                    galleryOpen = saved[1] as Boolean,
                    viewedMealPhotoId = saved[2] as Long?,
                )
            },
        )
    }
}
