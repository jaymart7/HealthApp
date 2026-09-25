package ph.mart.healthapp.feature.food.ui.quicklog.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import ph.mart.healthapp.core.designsystem.theme.Motion

/**
 * The quick log handoff's timings, in one place.
 *
 * Not [Motion]'s scale, and that is the argued exception (`DECISIONS.md` → the quick log
 * redesign): the handoff times one sheet's choreography as a whole — a 100ms fade under a 300ms
 * flight, a 150ms tail on a 250ms expand — and rounding each figure to the nearest rung of a ladder
 * built for single changes would break the overlaps it was timed around. The curves are still
 * [Motion]'s, and everything is spent through Compose's animation APIs, so Remove animations still
 * collapses all of it.
 */
internal object QuickLogMotion {
    /** The sent words leaving the field; the ring leaving the send circle. */
    const val Fade = 100

    /** Colour, placeholder and label crossfades; the tail of every expand. */
    const val Swap = 150

    /** Log leaving on a correction send — exits are quicker than their entrance. */
    const val Exit = 200

    /** Cards rising, the thread collapsing, the portion opening, Log arriving. */
    const val Enter = 250

    /** The sent words travelling into their bubble; the thinking bubble becoming the question. */
    const val Travel = 300

    /** The food card and then the other card. */
    const val CardStagger = 50

    /** One pulse of the thinking dots, and the offset between each. */
    const val DotsLoop = 1200
    const val DotStagger = 150
}

/**
 * The sheet's [SharedTransitionScope] — null wherever the sheet is not, which is every preview and
 * every component drawn on its own. [quickLogShared] reads it, so nothing below has to be handed a
 * scope to be previewed.
 */
internal val LocalQuickLogShared = staticCompositionLocalOf<SharedTransitionScope?> { null }

/**
 * Tags a node as one end of the send flight — the field on one side, the user's bubble on the
 * other. A no-op outside the sheet. Remeasured rather than scaled, so the words reflow as the
 * bubble grows out of the field instead of stretching.
 */
@Composable
internal fun Modifier.quickLogShared(key: Any, visibility: AnimatedVisibilityScope): Modifier {
    val scope = LocalQuickLogShared.current ?: return this
    return with(scope) {
        this@quickLogShared.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = visibility,
            boundsTransform = { _, _ -> tween(QuickLogMotion.Travel, easing = Motion.EmphasizedDecelerate) },
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
        )
    }
}

/** The key both ends of one user turn's flight share. */
internal fun turnKey(index: Int): String = "quicklog-turn-$index"

/**
 * [value] while there is one, and the last one there was once it goes null — what a block leaving
 * the layout draws while it leaves. Plain storage rather than snapshot state: it only has to outlive
 * recomposition, and writing state from composition would recompose for nothing.
 */
@Composable
internal fun <T : Any> retainLast(value: T?): T? {
    val last = remember { arrayOfNulls<Any>(1) }
    if (value != null) last[0] = value
    @Suppress("UNCHECKED_CAST")
    return value ?: last[0] as T?
}
