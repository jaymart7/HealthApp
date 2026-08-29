package ph.mart.healthapp.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember

/**
 * The app's motion scale, in the same spirit as the spacing and radius ladders: a fixed, small set
 * of named values so no call site invents its own timing. A duration that wants to be 180 or 340
 * is a duration that hasn't picked a side.
 *
 * Every value here is spent through Compose's animation APIs rather than a hand-rolled
 * `LaunchedEffect` + `delay` loop. That is deliberate: Android's recomposer installs a
 * `MotionDurationScale` sourced from `Settings.Global.ANIMATOR_DURATION_SCALE`, so every animation
 * built on these collapses to an instant cut when the user turns on **Remove animations**. A
 * `delay` loop would ignore that setting.
 */
object Motion {
    /** Tap acknowledgment — the smallest change that makes cause and result unmistakable. */
    const val Feedback = 120

    /** A routine state change, and every exit. Exits are never slower than their entrance. */
    const val State = 220

    /** Something appearing or leaving the layout. */
    const val Enter = 300

    /** The one authored entrance in the app: the calorie ring finding its share of the day. */
    const val Settle = 450

    /** Milliseconds between sibling cards in Home's entrance. */
    const val StaggerStep = 40

    /** Cards past this index all share the last delay, so the curtain can't grow with the screen. */
    const val StaggerCap = 5

    /** How much larger an active step of a meter sits than an inactive one. A second channel
     * beside colour and the filled/outlined glyph, which is what makes the state readable
     * without relying on hue alone. */
    const val ActiveStepScale = 1.08f

    // Material 3's published curves, written out rather than pulled from Material3 1.5's
    // MotionScheme — that API is still expressive-experimental, and these need no opt-in.
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
}

private const val FILL_STAGGER_STEP = 25
private const val FILL_STAGGER_MAX = 200

/**
 * Which way a meter's count just moved. Filling and clearing animate in opposite directions, so
 * the motion matches the gesture that caused it.
 *
 * The previous value is held in a plain array rather than snapshot state on purpose: it only needs
 * to survive recomposition, and observing it would invalidate the very composition that just read
 * the direction out of it.
 */
@Composable
fun rememberFillDirection(level: Int): Boolean {
    val previous = remember { intArrayOf(level) }
    return remember(level) {
        val up = level >= previous[0]
        previous[0] = level
        up
    }
}

/**
 * The 0..1 driver for one step of a meter — a water glass, an energy bolt, a mood face. Drive both
 * the tint and the scale off this single value so the row reads as one idea rather than two
 * effects.
 *
 * Filling runs left-to-right, clearing runs right-to-left, and the total delay is clamped so even
 * a 14-glass goal finishes inside [FILL_STAGGER_MAX]. Pass [stagger] `false` for a single-choice
 * row, where there is no direction to express.
 */
@Composable
fun stepFillProgress(
    active: Boolean,
    index: Int,
    count: Int,
    filling: Boolean,
    stagger: Boolean = true,
): State<Float> = animateFloatAsState(
    targetValue = if (active) 1f else 0f,
    animationSpec = tween(
        durationMillis = Motion.State,
        delayMillis = if (stagger) fillStaggerDelay(index, count, filling) else 0,
        easing = Motion.Standard,
    ),
    label = "stepFill",
)

internal fun fillStaggerDelay(index: Int, count: Int, filling: Boolean): Int {
    val position = if (filling) index else (count - 1 - index).coerceAtLeast(0)
    return (position * FILL_STAGGER_STEP).coerceAtMost(FILL_STAGGER_MAX)
}
