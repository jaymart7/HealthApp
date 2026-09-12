package ph.mart.healthapp.feature.progress.ui.photo

import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressPhoto

/**
 * Every progress photo in date order, and the unit the strip this page shares reads its weights in.
 * The same two fields `TimelapseUiState` carries, for the same reason: the page draws the set, it
 * does not derive anything from it, and there is no schema behind a grid.
 */
data class PhotosUiState(
    val photos: List<ProgressPhoto> = emptyList(),
    val unit: UnitSystem = UnitSystem.Metric,
)
