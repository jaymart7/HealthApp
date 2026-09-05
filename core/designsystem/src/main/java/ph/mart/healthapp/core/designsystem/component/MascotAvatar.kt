package ph.mart.healthapp.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import kotlin.math.PI
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
 * [Bibo] is the app's original mascot and the default; its silhouette renders exactly as it always
 * has.
 *
 * Colour is [MascotPalette], a second choice the user makes for *every* buddy at once.
 *
 * [topInset]/[sideInset] are fractions of the avatar box. They carve the headroom an accent needs
 * to sit above the head — or, for [Pip], the room its own taper rises into — and they are what make
 * [Sprig] a tall bean rather than a wide one. Bibo's are zero, so its body still fills the box
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
    Bibo("Bibo", MascotBody.RoundedSquare, EyeStyle.Dot, MascotAccent.None),
    Pip("Pip", MascotBody.Teardrop, EyeStyle.Ring, MascotAccent.Blush, topInset = 0.06f, sideInset = 0.14f),
    Zed("Zed", MascotBody.Hexagon, EyeStyle.Visor, MascotAccent.Antenna, topInset = 0.22f, sideInset = 0.02f),
    Momo("Momo", MascotBody.Dome, EyeStyle.Oval, MascotAccent.Ears, topInset = 0.16f, sideInset = 0.04f),
    Sprig("Sprig", MascotBody.Capsule, EyeStyle.Dot, MascotAccent.Sprout, topInset = 0.24f, sideInset = 0.17f),
}

internal data class MascotColors(val body: Color, val feature: Color)

/**
 * The colour the user picks in Profile → Appearance, applied to whichever buddy is picked. Every
 * pair here was one character's fill before the colour became a choice of its own, so each is
 * already proven against light, dark and all three contrast schemes — none of them is a new colour.
 *
 * [Soft] is the default because [MascotCharacter.Bibo] is, so an untouched install renders exactly
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
 * which is what made Zed read as a machine before any buddy could wear it.
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

/** Resolves the name stored on the profile. Anything null or unrecognised is [MascotCharacter.Bibo]
 * — a name from a newer build, or from an export written before the picker existed, degrades to the
 * default rather than failing. */
fun mascotCharacterOf(name: String?): MascotCharacter =
    MascotCharacter.entries.firstOrNull { it.name == name } ?: MascotCharacter.Bibo

/** Provided once by `AppTheme`, off the profile. Every [MascotAvatar] in the app reads it, which is
 * why not one of its ~16 call sites passes a character — only the picker does. `static` because it
 * changes at most once a session. */
val LocalMascot = staticCompositionLocalOf { MascotCharacter.Bibo }

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
 */
private const val BLINK_CYCLE_MS = 3600
private const val BOB_CYCLE_MS = 2600

/** The slice of the cycle the eyes are shut — ~140ms of 3.6s. It ends short of `1f` on purpose. */
private const val BLINK_START = 0.94f
private const val BLINK_END = 0.98f

/** How far the mascot drifts, as a fraction of its own height: ~1.3dp at the default 64dp. */
private const val BOB_FRACTION = 0.02f

internal fun isBlinking(phase: Float): Boolean = phase > BLINK_START && phase < BLINK_END

/** -1..1, and exactly 0 at both ends of the cycle — see [BOB_CYCLE_MS]. */
internal fun bobOffset(phase: Float): Float = sin(phase * 2f * PI.toFloat())

/**
 * The app's geometric mascot: a filled body in the [character]'s silhouette carrying its eyes, one
 * accent and the shared mouth curve. Everything is drawn on one canvas so an accent can sit above
 * the head, and nothing is clipped — the Celebrating sparkles overhang whatever the body's corners
 * do.
 *
 * It blinks every [BLINK_CYCLE_MS] and breathes on [BOB_CYCLE_MS], both driven from one
 * `rememberInfiniteTransition` inside the component — no call site passes anything for it, and no
 * call site can forget to. A blink reuses the closed eyes [MascotState.Sleepy] already draws, so
 * every silhouette shuts them the same way; Sleepy itself never blinks, but it does breathe.
 *
 * [character] and [palette] both default to the user's picks and should be left alone everywhere
 * except the picker that sets them.
 */
@Composable
fun MascotAvatar(
    state: MascotState,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
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
    Box(
        modifier = modifier
            .size(size)
            // Read inside the lambda, so the bob settles in the Draw phase and recomposes nothing.
            // On the Box rather than the Canvas, so the Celebrating sparkles ride along with it.
            .graphicsLayer {
                translationY = bobOffset(bobPhase.value) * this.size.height * BOB_FRACTION
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val body = Rect(
                left = this.size.width * character.sideInset,
                top = this.size.height * character.topInset,
                right = this.size.width * (1f - character.sideInset),
                bottom = this.size.height,
            )
            drawAccent(character, body, colors.feature)
            drawBody(character, body, colors.body)
            // Square, centred on the body — for Bibo (which has no insets) that is the 62%-of-box
            // canvas the face has always been drawn into.
            val faceHalf = minOf(body.width, body.height) * 0.62f / 2f
            drawMascotFace(state, colors.feature, character, Rect(body.center, faceHalf), blinking)
        }
        if (state == MascotState.Celebrating) {
            Text(
                text = "✦",
                color = colors.feature,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = -size * 0.12f, y = size * 0.08f),
            )
            Text(
                text = "✦",
                color = colors.feature,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = size * 0.1f, y = size * 0.28f),
            )
        }
    }
}

private fun DrawScope.drawBody(character: MascotCharacter, body: Rect, color: Color) {
    when (character.body) {
        MascotBody.RoundedSquare -> drawRoundRect(
            color = color,
            topLeft = body.topLeft,
            size = body.size,
            cornerRadius = CornerRadius(body.width / 3f),
        )

        // A round base tapering to a soft point — the one silhouette that spends its own headroom
        // instead of an accent, which is why Pip is the only character with nothing above its head.
        MascotBody.Teardrop -> drawPath(
            path = Path().apply {
                val radius = body.width / 2f
                val base = Offset(body.center.x, body.bottom - radius)
                moveTo(base.x, body.top)
                quadraticTo(base.x + radius * 0.62f, base.y - radius * 0.72f, base.x + radius, base.y)
                arcTo(Rect(base, radius), 0f, 180f, false)
                quadraticTo(base.x - radius * 0.62f, base.y - radius * 0.72f, base.x, body.top)
                close()
            },
            color = color,
        )

        MascotBody.Hexagon -> drawPath(
            // Flat top and bottom, points at left and right mid-height — the flat edges leave room
            // for the same eyes and mouth every other character draws.
            path = Path().apply {
                moveTo(body.left + body.width * 0.25f, body.top)
                lineTo(body.left + body.width * 0.75f, body.top)
                lineTo(body.right, body.center.y)
                lineTo(body.left + body.width * 0.75f, body.bottom)
                lineTo(body.left + body.width * 0.25f, body.bottom)
                lineTo(body.left, body.center.y)
                close()
            },
            color = color,
        )

        MascotBody.Dome -> drawPath(
            path = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = body,
                        topLeft = CornerRadius(body.width * 0.48f),
                        topRight = CornerRadius(body.width * 0.48f),
                        bottomLeft = CornerRadius(body.width * 0.16f),
                        bottomRight = CornerRadius(body.width * 0.16f),
                    ),
                )
            },
            color = color,
        )

        MascotBody.Capsule -> drawRoundRect(
            color = color,
            topLeft = body.topLeft,
            size = body.size,
            cornerRadius = CornerRadius(body.width / 2f),
        )
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

        MascotState.Thinking -> drawCircle(color, radius = strokeWidth * 1.3f, center = Offset(centerX, mouthY))
    }
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
