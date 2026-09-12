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

/** The divider opens down the middle, so both shots start equally visible. */
private const val DEFAULT_DIVIDER_FRACTION = 0.5f

/** UI-only: where the divider sits and whether the share sheet is up, neither of which means
 * anything outside the overlay holding it. Both ride the saver — a divider dragged to one side
 * and then lost to a rotation is a comparison the user has to set up again. */
internal class ComparisonState(
    dividerFraction: Float = DEFAULT_DIVIDER_FRACTION,
    sharing: Boolean = false,
) {
    /** Read inside draw and offset lambdas only — see `ComparisonSlider`, where a drag repaints
     * without recomposing either `Image`. */
    var dividerFraction: Float by mutableFloatStateOf(dividerFraction)
    var sharing: Boolean by mutableStateOf(sharing)

    companion object {
        fun Saver(): Saver<ComparisonState, Any> = listSaver(
            save = { listOf(it.dividerFraction, it.sharing) },
            restore = { saved -> ComparisonState(saved[0] as Float, saved[1] as Boolean) },
        )
    }
}
