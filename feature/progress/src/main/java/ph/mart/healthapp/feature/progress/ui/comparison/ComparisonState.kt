package ph.mart.healthapp.feature.progress.ui.comparison

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
internal fun rememberComparisonState(): ComparisonState =
    rememberSaveable(saver = ComparisonState.Saver()) { ComparisonState() }

/** Just past the middle, so the divider reads as something that moved rather than as a seam
 * baked into the picture. */
private const val DEFAULT_DIVIDER_FRACTION = 0.52f

/** The divider stops short of either edge: dragged flush, the handle has nothing under it and the
 * frame looks like a single photo with a line on it. */
internal val DIVIDER_RANGE = 0.04f..0.96f

/** Two framings of one pair. The slider answers "what changed here"; side by side answers "what
 * shape am I" — a posture or an outline is not a thing a wipe can show. */
internal enum class CompareMode { Slider, SideBySide }

/** UI-only: where the divider sits and whether the share sheet is up, neither of which means
 * anything outside the overlay holding it. Both ride the saver — a divider dragged to one side
 * and then lost to a rotation is a comparison the user has to set up again. */
internal class ComparisonState(
    dividerFraction: Float = DEFAULT_DIVIDER_FRACTION,
    sharing: Boolean = false,
    mode: CompareMode = CompareMode.Slider,
) {
    /** Read inside draw and offset lambdas only — see `ComparisonSlider`, where a drag repaints
     * without recomposing either `Image`. */
    var dividerFraction: Float by mutableFloatStateOf(dividerFraction)
    var sharing: Boolean by mutableStateOf(sharing)
    var mode: CompareMode by mutableStateOf(mode)

    /** Clamped here rather than at each of the three call sites that move it — a drag, a
     * pointer-down jump and a screen reader's `setProgress`. */
    fun moveDivider(fraction: Float) {
        dividerFraction = fraction.coerceIn(DIVIDER_RANGE)
    }

    companion object {
        fun Saver(): Saver<ComparisonState, Any> = listSaver(
            // Appended, never renumbered — the same rule `ProgressScreenState`'s saver keeps.
            save = { listOf(it.dividerFraction, it.sharing, it.mode.name) },
            restore = { saved ->
                ComparisonState(
                    saved[0] as Float,
                    saved[1] as Boolean,
                    CompareMode.valueOf(saved[2] as String),
                )
            },
        )
    }
}
