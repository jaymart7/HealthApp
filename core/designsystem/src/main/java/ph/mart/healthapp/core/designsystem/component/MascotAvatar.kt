package ph.mart.healthapp.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/** The 5 states of the app's geometric mascot. Expression, not identity — every character wears
 * all five, and the mouth geometry below is shared so a state reads the same whichever is picked. */
enum class MascotState { Idle, Happy, Celebrating, Sleepy, Thinking }

internal enum class MascotBody { RoundedSquare, Teardrop, Hexagon, Dome, Capsule }

internal enum class EyeStyle { Dot, Ring, Visor, Oval }

internal enum class MascotAccent { None, Blush, Antenna, Ears, Sprout }

/**
 * The mascots the user can pick between in Profile → Appearance. Each varies on three axes —
 * silhouette, eyes and one accent — because two characters that differ only in outline read as the
 * same character badly drawn. Two of the three separate any pair of them, which is what
 * `MascotCharacterTest` asserts; colour cannot help, because colour is no longer per character.
 *
 * [Rui] is the app's original mascot and the default; its silhouette renders exactly as it always
 * has.
 *
 * Colour is [MascotPalette], a second choice the user makes for *every* buddy at once.
 *
 * [topInset]/[sideInset] are fractions of the avatar box. They carve the headroom an accent needs
 * to sit above the head — or, for [Gel], the room its own taper rises into — and they are what make
 * [Lala] a tall bean rather than a wide one. Rui's are zero, so its body still fills the box
 * exactly as before.
 */
enum class MascotCharacter(
    /** Stays in Kotlin: these are proper names, not copy — and [name] is what `Profile` stores. */
    val label: String,
    internal val body: MascotBody,
    internal val eyes: EyeStyle,
    internal val accent: MascotAccent,
    internal val topInset: Float = 0f,
    internal val sideInset: Float = 0f,
) {
    Rui("Rui", MascotBody.RoundedSquare, EyeStyle.Dot, MascotAccent.None),
    Gel("Gel", MascotBody.Teardrop, EyeStyle.Ring, MascotAccent.Blush, topInset = 0.06f, sideInset = 0.14f),
    Mart("Mart", MascotBody.Hexagon, EyeStyle.Visor, MascotAccent.Antenna, topInset = 0.22f, sideInset = 0.02f),
    Alo("Alo", MascotBody.Dome, EyeStyle.Oval, MascotAccent.Ears, topInset = 0.16f, sideInset = 0.04f),
    Lala("Lala", MascotBody.Capsule, EyeStyle.Dot, MascotAccent.Sprout, topInset = 0.24f, sideInset = 0.17f),
}

internal data class MascotColors(val body: Color, val feature: Color)

/**
 * The colour the user picks in Profile → Appearance, applied to whichever buddy is picked. Every
 * pair here was one character's fill before the colour became a choice of its own, so each is
 * already proven against light, dark and all three contrast schemes — none of them is a new colour.
 *
 * [Soft] is the default because [MascotCharacter.Rui] is, so an untouched install renders exactly
 * as it did.
 *
 * Three roles are deliberately absent and the list stops at five because of them: **tertiary** and
 * **tertiaryContainer** are the AI accent and the carbs colour, **error** means genuinely
 * off-track, and **secondaryContainer** is what both picker rows fill their selected cell with — a
 * mascot that vanished the moment it was chosen is the one thing a picker must not do.
 *
 * [Contrast] is the one pair that *inverts* with the theme: `inverseSurface` is dark on a light
 * scheme and light on a dark one, so it swaps ground for figure when the theme does. [Neutral] is
 * the one whose *features* carry the accent rather than its fill — a grey chassis with a lit face,
 * which is what made Mart read as a machine before any buddy could wear it.
 */
enum class MascotPalette { Soft, Bold, Muted, Contrast, Neutral }

@Composable
internal fun mascotColors(palette: MascotPalette): MascotColors {
    val scheme = MaterialTheme.colorScheme
    return when (palette) {
        MascotPalette.Soft -> MascotColors(scheme.primaryContainer, scheme.onPrimaryContainer)
        MascotPalette.Bold -> MascotColors(scheme.primary, scheme.onPrimary)
        MascotPalette.Muted -> MascotColors(scheme.secondary, scheme.onSecondary)
        MascotPalette.Contrast -> MascotColors(scheme.inverseSurface, scheme.inverseOnSurface)
        MascotPalette.Neutral -> MascotColors(scheme.surfaceContainerHighest, scheme.primary)
    }
}

/** Resolves the name stored on the profile. Anything null or unrecognised is [MascotCharacter.Rui]
 * — a name from a newer build, or from an export written before the picker existed, degrades to the
 * default rather than failing. */
fun mascotCharacterOf(name: String?): MascotCharacter =
    MascotCharacter.entries.firstOrNull { it.name == name } ?: MascotCharacter.Rui

/** Provided once by `AppTheme`, off the profile. Every [MascotAvatar] in the app reads it, which is
 * why not one of its ~16 call sites passes a character — only the picker does. `static` because it
 * changes at most once a session. */
val LocalMascot = staticCompositionLocalOf { MascotCharacter.Rui }

/** The palette's fill on its own, for the swatch the Profile picker draws. Public where
 * [mascotColors] is internal because a plain circle needs the body colour and nothing else — the
 * feature colour has no meaning without a face to put it on. */
@Composable
fun mascotSwatchColor(palette: MascotPalette): Color = mascotColors(palette).body

/** [mascotCharacterOf] for the colour, and it degrades the same way and for the same reasons. */
fun mascotPaletteOf(name: String?): MascotPalette =
    MascotPalette.entries.firstOrNull { it.name == name } ?: MascotPalette.Soft

/** Provided by `AppTheme` beside [LocalMascot], off the same profile row. The colour is an
 * appearance choice like the buddy and the scheme, so it is resolved where those are. */
val LocalMascotPalette = staticCompositionLocalOf { MascotPalette.Soft }

/**
 * The idle loop. Two linear phases rather than one because a blink and a breath share no period,
 * and both are shaped to rest at their neutral pose at phase `1f`: Compose pins an infinite
 * transition to its **end** value when the user turns on *Remove animations*
 * (`InfiniteTransition` calls `skipToEnd()` and suspends), so a `RepeatMode.Reverse` cycle would
 * park the mascot mid-bob with its eyes shut for exactly the people who asked for stillness.
 *
 * Every channel added since — the hop, the tilt, the breath, the twinkle, the thinking dots and the
 * sleepy "z" — rides the same [BOB_CYCLE_MS] phase and obeys the same rule, which is what
 * [pulse] and [mascotMotion] exist to make unbreakable.
 */
private const val BLINK_CYCLE_MS = 3600
private const val BOB_CYCLE_MS = 2600

/** The slice of the cycle the eyes are shut — ~140ms of 3.6s. It ends short of `1f` on purpose. */
private const val BLINK_START = 0.94f
private const val BLINK_END = 0.98f

/** How far the mascot drifts, as a fraction of its own height: ~1.3dp at the default 64dp. Idle's
 * amplitude is deliberately untouched — Rui idling is what the app has always drawn. */
private const val BOB_FRACTION = 0.02f

/** The expressive states, all fractions of the avatar's own height or degrees of rotation, so a
 * 24dp chat avatar performs the same routine as a 112dp one at a quarter of the size. */
private const val HAPPY_BOB = 0.04f
private const val HAPPY_SWAY_DEG = 4f
private const val HOP_FRACTION = 0.10f
private const val HOP_STRETCH = 0.05f
private const val THINK_BOB = 0.015f
private const val THINK_TILT_DEG = 5f
private const val BREATH = 0.03f

/** How far a sparkle shrinks at the far end of its swing, and how far a thinking dot dims. Both
 * are subtractive from the rest value for the reason everything here is: rest must be full. */
private const val TWINKLE_DEPTH = 0.5f
private const val DOT_DIM_DEPTH = 0.55f

/** The lower-right shading, in [MascotColors.feature] over the fill. Low enough that `Neutral`'s
 * primary-tinted feature colour reads as shade rather than as a second colour. */
private const val SHEEN_ALPHA = 0.1f

/** Where the pop starts. It always animates *to* the neutral pose, never away from it — the same
 * reason the loop rests at phase `1f`: an `Animatable` snaps to its target under *Remove
 * animations*, and its target has to be the pose the mascot should be left in. */
private const val POP_FROM = 0.86f
private const val POKE_FROM = 0.72f

internal fun isBlinking(phase: Float): Boolean = phase > BLINK_START && phase < BLINK_END

/**
 * The one wave every channel is built from: `-1..1`, and exactly `0` at both ends of the cycle for
 * every [harmonic].
 *
 * That last property is the whole point. Staggering three sparkles or three thinking dots by a
 * *phase offset* would put each of them somewhere arbitrary at rest — `sin(2π·(1 + offset)) ≠ 0` —
 * so they are staggered by harmonic instead: k = 1, 2, 3 are visibly out of step frame to frame and
 * all exactly neutral at phase `1f`.
 */
internal fun pulse(phase: Float, harmonic: Int): Float = sin(phase * harmonic * 2f * PI.toFloat())

/** -1..1, and exactly 0 at both ends of the cycle — see [BOB_CYCLE_MS]. */
internal fun bobOffset(phase: Float): Float = pulse(phase, 1)

/** The sleepy "z"'s opacity: a single arch over the cycle, `0` at both ends, so at rest there is
 * simply no "z" rather than one frozen mid-drift. */
internal fun zAlpha(phase: Float): Float = sin(phase * PI.toFloat())

/**
 * How the whole avatar is posed this frame. Separated from the drawing because it is the one part
 * of the motion a JVM test can reach — [MascotAvatarTest] sweeps every state at phase `1f` and
 * asserts the identity pose, so a state added later cannot quietly skip the rule.
 */
internal data class MascotMotion(
    /** Of the avatar's height. Negative is up. */
    val translateFraction: Float,
    val rotationDeg: Float,
    val scaleX: Float,
    val scaleY: Float,
)

/**
 * One `when`, one vocabulary. Every branch is built from [pulse], so every branch is the identity
 * pose at phase `1f`.
 *
 * [MascotState.Idle] is the only one that is unchanged from before there was a vocabulary: it is
 * what ~20 call sites draw and what the Home header has always shown, and a greeting that started
 * hopping would be a change nobody asked for.
 */
internal fun mascotMotion(state: MascotState, phase: Float): MascotMotion {
    val wave = pulse(phase, 1)
    return when (state) {
        MascotState.Idle -> MascotMotion(wave * BOB_FRACTION, 0f, 1f, 1f)

        MascotState.Happy -> MascotMotion(wave * HAPPY_BOB, wave * HAPPY_SWAY_DEG, 1f, 1f)

        // Two hops a cycle, and it stretches in the air rather than squashing on the ground — the
        // ground is where phase `1f` leaves it, and the ground has to be the neutral pose.
        MascotState.Celebrating -> {
            val lift = abs(wave)
            MascotMotion(-lift * HOP_FRACTION, 0f, 1f - lift * HOP_STRETCH, 1f + lift * HOP_STRETCH)
        }

        // The tilt runs at twice the bob's rate so the head is never simply bobbing in a tilt.
        MascotState.Thinking ->
            MascotMotion(wave * THINK_BOB, pulse(phase, 2) * THINK_TILT_DEG, 1f, 1f)

        // A breath, not a drift: it swells in place, which is the one thing a sleeping shape does.
        MascotState.Sleepy -> MascotMotion(0f, 0f, 1f + wave * BREATH, 1f + wave * BREATH)
    }
}

/**
 * The app's geometric mascot: a filled body in the [character]'s silhouette carrying its eyes, one
 * accent, the shared mouth curve and the state's own performance. Everything is drawn on one canvas
 * so an accent can sit above the head, and nothing is clipped — the Celebrating sparkles overhang
 * whatever the body's corners do.
 *
 * It blinks every [BLINK_CYCLE_MS] and moves on [BOB_CYCLE_MS] — see [mascotMotion] for what each
 * state does with that phase — both driven from one `rememberInfiniteTransition` inside the
 * component, so no call site passes anything for it and no call site can forget to. A blink reuses
 * the closed eyes [MascotState.Sleepy] already draws, so every silhouette shuts them the same way;
 * Sleepy itself never blinks, but it does breathe.
 *
 * A change of [state] springs through a small squash, so the `Idle → Celebrating` flip a screen
 * makes is a move rather than a cut.
 *
 * [interactive] adds a poke: a tap bounces the mascot. It is **off by default** because a tap
 * handler consumes the gesture, and most call sites sit inside something already clickable — the
 * buddy picker's cell, a Progress card, a chat row. It is a `pointerInput` rather than a
 * `clickable` so no ripple lands on a drawn character and no unlabelled control appears in the
 * accessibility tree for something purely decorative.
 *
 * [character] and [palette] both default to the user's picks and should be left alone everywhere
 * except the picker that sets them.
 */
@Composable
fun MascotAvatar(
    state: MascotState,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    interactive: Boolean = false,
    character: MascotCharacter = LocalMascot.current,
    palette: MascotPalette = LocalMascotPalette.current,
) {
    val colors = mascotColors(palette)
    // Per-instance, so the five buddies in the picker don't blink in lockstep. Frozen in previews
    // so the 5x5 grid renders the rest pose instead of catching a random mid-blink.
    val phaseOffset = if (LocalInspectionMode.current) 0f else remember { Random.nextFloat() }
    val transition = rememberInfiniteTransition(label = "mascot")
    val blinkPhase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(BLINK_CYCLE_MS, easing = LinearEasing),
            initialStartOffset = StartOffset((phaseOffset * BLINK_CYCLE_MS).toInt()),
        ),
        label = "blink",
    )
    val bobPhase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(BOB_CYCLE_MS, easing = LinearEasing),
            initialStartOffset = StartOffset((phaseOffset * BOB_CYCLE_MS).toInt()),
        ),
        label = "bob",
    )
    // The phase moves every frame; the boolean flips twice a cycle. Read straight, the draw below
    // would invalidate on every one of those frames for a value nobody saw change.
    val blinking by remember(state) {
        derivedStateOf { state != MascotState.Sleepy && isBlinking(blinkPhase.value) }
    }
    val pop = remember { Animatable(1f) }
    // The previous state in a plain array, the idiom `rememberFillDirection` already uses: it only
    // needs to survive recomposition, and it is what keeps the *first* composition from popping —
    // three dozen avatars all squashing on screen entry is not an entrance.
    val previous = remember { arrayOf(state) }
    LaunchedEffect(state) {
        if (previous[0] != state) {
            previous[0] = state
            pop.snapTo(POP_FROM)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    // rememberCoroutineScope, not a scope of our own: this one carries the recomposer's
    // MotionDurationScale, so the poke collapses to a cut under *Remove animations* like the rest.
    val scope = rememberCoroutineScope()
    Canvas(
        modifier = modifier
            .size(size)
            // Read inside the lambda, so the pose settles in the Draw phase and recomposes nothing.
            .graphicsLayer {
                val motion = mascotMotion(state, bobPhase.value)
                val squash = pop.value
                translationY = motion.translateFraction * this.size.height
                rotationZ = motion.rotationDeg
                scaleX = motion.scaleX * squash
                scaleY = motion.scaleY * squash
                // Feet planted: a squash presses down into the floor and a tilt swings the head,
                // which is what stops either reading as the whole avatar sliding.
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .then(
                if (!interactive) {
                    Modifier
                } else {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures {
                            scope.launch {
                                pop.snapTo(POKE_FROM)
                                pop.animateTo(
                                    targetValue = 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
                                )
                            }
                        }
                    }
                },
            ),
    ) {
        val phase = bobPhase.value
        val body = Rect(
            left = this.size.width * character.sideInset,
            top = this.size.height * character.topInset,
            right = this.size.width * (1f - character.sideInset),
            bottom = this.size.height,
        )
        drawAccent(character, body, colors.feature)
        val silhouette = bodyPath(character, body)
        drawPath(silhouette, colors.body)
        // Clipped to the silhouette rather than drawn per shape: one circle shades all five.
        clipPath(silhouette) {
            drawCircle(
                color = colors.feature.copy(alpha = SHEEN_ALPHA),
                radius = body.width * 0.62f,
                center = Offset(body.right, body.bottom),
            )
        }
        // Square, centred on the body — for Rui (which has no insets) that is the 62%-of-box
        // canvas the face has always been drawn into.
        val faceHalf = minOf(body.width, body.height) * 0.62f / 2f
        drawMascotFace(state, colors.feature, character, Rect(body.center, faceHalf), blinking, phase)
        when (state) {
            MascotState.Celebrating -> drawSparkles(colors.feature, phase)
            MascotState.Sleepy -> drawSleepZ(colors.feature, phase)
            else -> Unit
        }
    }
}

/**
 * The silhouette as a path rather than five draw calls, so the sheen above can be clipped to it.
 * Every shape is the one it always was — a `RoundRect` added to a path is the same pixels
 * `drawRoundRect` produced.
 */
private fun bodyPath(character: MascotCharacter, body: Rect): Path = Path().apply {
    when (character.body) {
        MascotBody.RoundedSquare ->
            addRoundRect(RoundRect(body, CornerRadius(body.width / 3f)))

        // A round base tapering to a soft point — the one silhouette that spends its own headroom
        // instead of an accent, which is why Gel is the only character with nothing above its head.
        MascotBody.Teardrop -> {
            val radius = body.width / 2f
            val base = Offset(body.center.x, body.bottom - radius)
            moveTo(base.x, body.top)
            quadraticTo(base.x + radius * 0.62f, base.y - radius * 0.72f, base.x + radius, base.y)
            arcTo(Rect(base, radius), 0f, 180f, false)
            quadraticTo(base.x - radius * 0.62f, base.y - radius * 0.72f, base.x, body.top)
            close()
        }

        // Flat top and bottom, points at left and right mid-height — the flat edges leave room
        // for the same eyes and mouth every other character draws.
        MascotBody.Hexagon -> {
            moveTo(body.left + body.width * 0.25f, body.top)
            lineTo(body.left + body.width * 0.75f, body.top)
            lineTo(body.right, body.center.y)
            lineTo(body.left + body.width * 0.75f, body.bottom)
            lineTo(body.left + body.width * 0.25f, body.bottom)
            lineTo(body.left, body.center.y)
            close()
        }

        MascotBody.Dome -> addRoundRect(
            RoundRect(
                rect = body,
                topLeft = CornerRadius(body.width * 0.48f),
                topRight = CornerRadius(body.width * 0.48f),
                bottomLeft = CornerRadius(body.width * 0.16f),
                bottomRight = CornerRadius(body.width * 0.16f),
            ),
        )

        MascotBody.Capsule -> addRoundRect(RoundRect(body, CornerRadius(body.width / 2f)))
    }
}

/** Drawn before the body so a stem or an ear tucks behind it rather than butting against its edge.
 *
 * Every accent is sized off `body.top` — the headroom the character's `topInset` carved — rather
 * than off the body, so none of them can reach past the top of the box the Canvas clips to. That
 * makes the insets a knob for how *big* an accent reads, never for whether it survives. */
private fun DrawScope.drawAccent(character: MascotCharacter, body: Rect, color: Color) {
    val head = body.top
    val centerX = body.center.x
    when (character.accent) {
        // Blush sits on the cheeks, over the body — see drawMascotFace.
        MascotAccent.None, MascotAccent.Blush -> Unit

        MascotAccent.Antenna -> {
            val bulb = head * 0.30f
            val bulbY = head * 0.36f
            drawLine(
                color = color,
                start = Offset(centerX, body.top + body.height * 0.1f),
                end = Offset(centerX, bulbY),
                strokeWidth = bulb * 0.5f,
                cap = StrokeCap.Round,
            )
            drawCircle(color, radius = bulb, center = Offset(centerX, bulbY))
        }

        MascotAccent.Ears -> listOf(0.26f, 0.74f).forEach { x ->
            val tipX = body.left + body.width * x
            drawPath(
                path = Path().apply {
                    moveTo(tipX - body.width * 0.13f, body.top + body.height * 0.09f)
                    lineTo(tipX, head * 0.10f)
                    lineTo(tipX + body.width * 0.13f, body.top + body.height * 0.09f)
                    close()
                },
                color = color,
            )
        }

        MascotAccent.Sprout -> {
            val stemTop = head * 0.45f
            drawLine(
                color = color,
                start = Offset(centerX, body.top + body.height * 0.06f),
                end = Offset(centerX, stemTop),
                strokeWidth = body.width * 0.06f,
                cap = StrokeCap.Round,
            )
            // Rotating the oval about the stem's tip lifts its far corner by width * sin(28°); the
            // cap is what keeps that corner inside the box on a narrow headroom.
            val leafWidth = minOf(body.width * 0.46f, stemTop * 2f)
            val pivot = Offset(centerX, stemTop)
            rotate(degrees = -28f, pivot = pivot) {
                drawOval(color = color, topLeft = pivot, size = Size(leafWidth, leafWidth * 0.52f))
            }
        }
    }
}

private fun DrawScope.drawMascotFace(
    state: MascotState,
    color: Color,
    character: MascotCharacter,
    face: Rect,
    blinking: Boolean,
    phase: Float,
) {
    val eyeRadius = minOf(face.width, face.height) * 0.09f
    val eyeY = face.top + face.height * 0.38f
    val eyeGap = face.width * 0.22f
    val centerX = face.center.x
    val strokeWidth = eyeRadius * 0.6f

    if (state == MascotState.Sleepy || blinking) {
        // Closed eyes are the state, not the character — every silhouette shuts them the same way,
        // which is what lets a blink borrow them rather than draw five more shapes.
        val lineHalf = eyeRadius * 1.2f
        listOf(centerX - eyeGap, centerX + eyeGap).forEach { x ->
            drawLine(
                color = color,
                start = Offset(x - lineHalf, eyeY),
                end = Offset(x + lineHalf, eyeY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    } else when (character.eyes) {
        EyeStyle.Dot -> listOf(centerX - eyeGap, centerX + eyeGap).forEach { x ->
            drawCircle(color, radius = eyeRadius, center = Offset(x, eyeY))
        }

        EyeStyle.Ring -> listOf(centerX - eyeGap, centerX + eyeGap).forEach { x ->
            drawCircle(
                color = color,
                radius = eyeRadius * 1.3f,
                center = Offset(x, eyeY),
                style = Stroke(width = strokeWidth * 1.2f),
            )
        }

        // One slot across both eye positions rather than two — a visor, not a pair of eyes.
        EyeStyle.Visor -> {
            val height = eyeRadius * 1.5f
            val width = eyeGap * 2f + eyeRadius * 2.4f
            drawRoundRect(
                color = color,
                topLeft = Offset(centerX - width / 2f, eyeY - height / 2f),
                size = Size(width, height),
                cornerRadius = CornerRadius(height / 2f),
            )
        }

        EyeStyle.Oval -> listOf(centerX - eyeGap, centerX + eyeGap).forEach { x ->
            val slot = Size(eyeRadius * 1.6f, eyeRadius * 2.7f)
            drawRoundRect(
                color = color,
                topLeft = Offset(x - slot.width / 2f, eyeY - slot.height / 2f),
                size = slot,
                cornerRadius = CornerRadius(slot.width / 2f),
            )
        }
    }

    drawBrows(state, color, Offset(centerX, eyeY - eyeRadius * 2.6f), eyeGap, eyeRadius, strokeWidth)

    if (character.accent == MascotAccent.Blush && state != MascotState.Sleepy) {
        listOf(centerX - eyeGap * 1.9f, centerX + eyeGap * 1.9f).forEach { x ->
            drawCircle(
                color = color.copy(alpha = 0.35f),
                radius = eyeRadius * 1.1f,
                center = Offset(x, eyeY + face.height * 0.16f),
            )
        }
    }

    val mouthY = face.top + face.height * 0.64f
    val mouthWidth = face.width * 0.34f
    when (state) {
        MascotState.Idle -> drawArc(
            color = color,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(centerX - mouthWidth / 2f, mouthY - mouthWidth * 0.25f),
            size = Size(mouthWidth, mouthWidth * 0.5f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )

        MascotState.Happy -> drawArc(
            color = color,
            startAngle = 15f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(centerX - mouthWidth / 2f, mouthY - mouthWidth * 0.35f),
            size = Size(mouthWidth, mouthWidth * 0.6f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )

        MascotState.Celebrating -> drawArc(
            color = color,
            startAngle = 10f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(centerX - mouthWidth * 0.65f, mouthY - mouthWidth * 0.45f),
            size = Size(mouthWidth * 1.3f, mouthWidth * 0.75f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )

        MascotState.Sleepy -> drawLine(
            color = color,
            start = Offset(centerX - mouthWidth * 0.25f, mouthY),
            end = Offset(centerX + mouthWidth * 0.25f, mouthY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )

        // Three dots lit in sequence rather than one: an ellipsis is what "working on it" looks
        // like, and it costs no animation channel — the three harmonics are already running.
        MascotState.Thinking -> listOf(-1, 0, 1).forEachIndexed { index, slot ->
            drawCircle(
                color = color.copy(alpha = 1f - DOT_DIM_DEPTH * abs(pulse(phase, index + 1))),
                radius = strokeWidth * 1.1f,
                center = Offset(centerX + slot * mouthWidth * 0.38f, mouthY),
            )
        }
    }
}

/**
 * Brows, and the same brows on every buddy — the state vocabulary is shared, so a raised brow means
 * the same thing whichever silhouette wears it. They sit above the highest eye style (Mart's visor),
 * which is what keeps them from landing on it.
 *
 * Idle and Sleepy have none on purpose: a resting face with drawn brows reads as an opinion.
 */
private fun DrawScope.drawBrows(
    state: MascotState,
    color: Color,
    center: Offset,
    eyeGap: Float,
    eyeRadius: Float,
    strokeWidth: Float,
) {
    val halfWidth = eyeRadius * 1.3f
    val stroke = Stroke(width = strokeWidth * 0.9f, cap = StrokeCap.Round)
    when (state) {
        // Arched, both sides — the top half of a small circle over each eye.
        MascotState.Happy, MascotState.Celebrating ->
            listOf(center.x - eyeGap, center.x + eyeGap).forEach { x ->
                drawArc(
                    color = color,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(x - halfWidth, center.y - halfWidth * 0.5f),
                    size = Size(halfWidth * 2f, halfWidth),
                    style = stroke,
                )
            }

        // One up, one level: the asymmetry is the whole expression.
        MascotState.Thinking -> {
            drawLine(
                color = color,
                start = Offset(center.x - eyeGap - halfWidth, center.y),
                end = Offset(center.x - eyeGap + halfWidth, center.y - halfWidth * 0.7f),
                strokeWidth = strokeWidth * 0.9f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(center.x + eyeGap - halfWidth, center.y + halfWidth * 0.2f),
                end = Offset(center.x + eyeGap + halfWidth, center.y + halfWidth * 0.2f),
                strokeWidth = strokeWidth * 0.9f,
                cap = StrokeCap.Round,
            )
        }

        MascotState.Idle, MascotState.Sleepy -> Unit
    }
}

/**
 * The Celebrating sparkles: drawn geometry now rather than two `✦` glyphs in a `Text`, which is
 * what lets them twinkle at all. Three of them on three harmonics, so they never pulse together —
 * and all three sit at full size at rest, because [pulse] is `0` there.
 *
 * They overhang the body exactly as the two glyphs did; nothing here is clipped.
 */
private fun DrawScope.drawSparkles(color: Color, phase: Float) {
    val unit = minOf(size.width, size.height)
    listOf(
        Triple(Offset(size.width * 0.84f, size.height * 0.14f), unit * 0.10f, 1),
        Triple(Offset(size.width * 0.14f, size.height * 0.32f), unit * 0.09f, 2),
        Triple(Offset(size.width * 0.90f, size.height * 0.52f), unit * 0.06f, 3),
    ).forEach { (center, radius, harmonic) ->
        val scale = 1f - TWINKLE_DEPTH * abs(pulse(phase, harmonic))
        drawSparkle(center, radius * scale, color)
    }
}

/** A four-point star. The control points sit near the centre, which is what pulls the four sides
 * concave instead of leaving a diamond. */
private fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
    val waist = radius * 0.18f
    drawPath(
        path = Path().apply {
            moveTo(center.x, center.y - radius)
            quadraticTo(center.x + waist, center.y - waist, center.x + radius, center.y)
            quadraticTo(center.x + waist, center.y + waist, center.x, center.y + radius)
            quadraticTo(center.x - waist, center.y + waist, center.x - radius, center.y)
            quadraticTo(center.x - waist, center.y - waist, center.x, center.y - radius)
            close()
        },
        color = color,
    )
}

/**
 * One "z" drifting up off the sleeping mascot. Positioned against the *box* rather than the body's
 * headroom, unlike an accent: Rui and Gel have almost no headroom, and a "z" only Mart and Lala
 * could show would not be the shared state vocabulary the rest of the face is.
 *
 * [zAlpha] is what makes it safe — it fades to nothing at both ends of the cycle, so the rest pose
 * has no "z" in it at all rather than one stuck halfway up.
 */
private fun DrawScope.drawSleepZ(color: Color, phase: Float) {
    val unit = minOf(size.width, size.height)
    val glyph = unit * 0.14f
    val left = size.width * 0.76f
    val top = size.height * (0.28f - 0.20f * phase)
    drawPath(
        path = Path().apply {
            moveTo(left, top)
            lineTo(left + glyph, top)
            lineTo(left, top + glyph)
            lineTo(left + glyph, top + glyph)
        },
        color = color.copy(alpha = zAlpha(phase) * 0.9f),
        style = Stroke(width = glyph * 0.2f, cap = StrokeCap.Round),
    )
}

@PreviewLightDark
@Composable
private fun MascotAvatarPreview() {
    AppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
            ) {
                MascotCharacter.entries.forEach { character ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MascotState.entries.forEach { state ->
                            MascotAvatar(state = state, size = 56.dp, character = character)
                        }
                    }
                }
                // The other axis: one buddy, every colour. Both grids matter in both schemes —
                // Contrast inverts between them and Neutral is nearly the surface it sits on.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MascotPalette.entries.forEach { palette ->
                        MascotAvatar(state = MascotState.Happy, size = 56.dp, palette = palette)
                    }
                }
            }
        }
    }
}

/** The face at the size onboarding and the photo flow draw it, where the brows, the thinking dots
 * and the sheen are actually legible. Previews freeze the loop at its rest pose, which is exactly
 * the pose the whole design is built to guarantee — no sparkle mid-twinkle, no "z". */
@PreviewLightDark
@Composable
private fun MascotAvatarLargePreview() {
    AppTheme {
        Surface {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(8.dp),
            ) {
                MascotState.entries.forEach { state ->
                    MascotAvatar(state = state, size = 112.dp)
                }
            }
        }
    }
}
