package ph.mart.healthapp.feature.progress.ui.timelapse

import kotlin.math.abs
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressPhoto

/** The three playback speeds, in frames per second. */
internal val TIMELAPSE_FPS = listOf(2, 4, 8)

/**
 * The crossfade into each new frame, per speed. Every one is comfortably shorter than the interval
 * it sits inside — a fade still running when the next frame is due never finishes, and the player
 * turns into a permanent double exposure.
 */
internal val TIMELAPSE_FADE_MILLIS = listOf(220, 150, 80)

/** Two photos is the least that plays as a sequence, and it is the floor the Photos page offers
 * the player at — one control appearing without the comparison slider would read as a bug. */
internal const val MIN_TIMELAPSE_PHOTOS = 2

/** How long one frame is held, at [speed]. */
internal fun frameIntervalMillis(speed: Int): Long = 1000L / TIMELAPSE_FPS[speed.coerceIn(TIMELAPSE_FPS.indices)]

/** The fade into the next frame, at [speed]. */
internal fun fadeMillis(speed: Int): Int = TIMELAPSE_FADE_MILLIS[speed.coerceIn(TIMELAPSE_FADE_MILLIS.indices)]

/**
 * Where each photo sits along the run's **date** range, 0f at the first shot and 1f at the last.
 *
 * This is what makes the scrubber a timeline rather than an index: three shots in one week and
 * then nothing for a month should look like three shots in one week and then nothing for a month.
 * A slider stepping one notch per photo says the opposite — that the run was evenly paced — which
 * is the single thing a progress timelapse is most often read for.
 *
 * Falls back to even spacing when every shot shares a date, since there is then no range to place
 * them in and stacking them all at 0f would draw one tick for the whole set.
 *
 * [photos] is expected in date order, as the repository returns it.
 */
internal fun tickFractions(photos: List<ProgressPhoto>): List<Float> {
    if (photos.isEmpty()) return emptyList()
    if (photos.size == 1) return listOf(0f)
    val first = photos.first().dateEpochDay
    val span = photos.last().dateEpochDay - first
    val last = photos.size - 1
    if (span <= 0L) return photos.indices.map { it.toFloat() / last }
    return photos.map { (it.dateEpochDay - first).toFloat() / span }
}

/**
 * The frame a scrub to [fraction] lands on — nearest by date position, not by index, so dragging
 * to the middle of a three-week gap snaps to whichever end of the gap is actually closer.
 */
internal fun nearestFrame(fraction: Float, photos: List<ProgressPhoto>): Int {
    val fractions = tickFractions(photos)
    if (fractions.isEmpty()) return 0
    val target = fraction.coerceIn(0f, 1f)
    return fractions.indices.minBy { abs(fractions[it] - target) }
}

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
