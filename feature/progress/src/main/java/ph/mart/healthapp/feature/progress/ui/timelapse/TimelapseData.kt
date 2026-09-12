package ph.mart.healthapp.feature.progress.ui.timelapse

import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressPhoto

/** The three playback speeds, in frames per second. */
internal val TIMELAPSE_FPS = listOf(2, 4, 8)

/** Two photos is the least that plays as a sequence, and it is the floor the Photos page offers
 * the player at — one control appearing without the comparison slider would read as a bug. */
internal const val MIN_TIMELAPSE_PHOTOS = 2

/**
 * The whole set in date order, and the unit its weights are read in. Nothing derived: a timelapse
 * is a way of looking at the grid, not a thing to store, so there is no schema behind this and
 * nothing here that the repositories don't already return.
 */
data class TimelapseUiState(
    val photos: List<ProgressPhoto> = emptyList(),
    val unit: UnitSystem = UnitSystem.Metric,
) {
    val playable: Boolean get() = photos.size >= MIN_TIMELAPSE_PHOTOS
}
