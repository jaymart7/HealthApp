package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** The 5 states of the app's geometric mascot. Expression, not identity — every character wears
 * all five, and the mouth geometry below is shared so a state reads the same whichever is picked. */
enum class MascotState { Idle, Happy, Celebrating, Sleepy, Thinking }

/** Flat top and bottom with points at left and right mid-height: the flat edges are what leave
 * room for the same eyes and mouth every other character draws. */
private val HexagonShape = GenericShape { size, _ ->
    moveTo(size.width * 0.25f, 0f)
    lineTo(size.width * 0.75f, 0f)
    lineTo(size.width, size.height * 0.5f)
    lineTo(size.width * 0.75f, size.height)
    lineTo(size.width * 0.25f, size.height)
    lineTo(0f, size.height * 0.5f)
    close()
}

internal enum class EyeStyle { Dot, Slot }

/**
 * The mascots the user can pick between in Profile → Appearance. They differ by **silhouette and
 * eye shape only** — every one keeps [MaterialTheme.colorScheme.primaryContainer] for its body and
 * `onPrimaryContainer` for its features, so the mascot never competes with the nav bar's
 * `secondaryContainer` pill and `tertiaryContainer` stays reserved for AI output.
 *
 * [Bibo] is the default and the app's original mascot; it renders exactly as it always has.
 */
enum class MascotCharacter(val label: String, internal val eyes: EyeStyle) {
    Bibo("Bibo", EyeStyle.Dot),
    Pip("Pip", EyeStyle.Dot),
    Zed("Zed", EyeStyle.Slot),
    ;

    fun shape(size: Dp): Shape = when (this) {
        Bibo -> RoundedCornerShape(size / 3)
        Pip -> CircleShape
        Zed -> HexagonShape
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

/**
 * The app's geometric mascot: a [MaterialTheme.colorScheme.primaryContainer] body in the
 * [character]'s silhouette, carrying eyes + a mouth curve in
 * [MaterialTheme.colorScheme.onPrimaryContainer]. No other detail is added at any size.
 *
 * [character] defaults to the user's pick and should be left alone everywhere except the picker.
 */
@Composable
fun MascotAvatar(
    state: MascotState,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    character: MascotCharacter = LocalMascot.current,
) {
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val featureColor = MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        // Shaped background rather than a clip: the Celebrating sparkles sit near the corners, and
        // a clip would slice them off whichever silhouette cuts the most corner (Zed's).
        modifier = modifier
            .size(size)
            .background(containerColor, character.shape(size)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size * 0.62f)) {
            drawMascotFace(state, featureColor, character.eyes)
        }
        if (state == MascotState.Celebrating) {
            Text(
                text = "✦",
                color = featureColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = -size * 0.12f, y = size * 0.08f),
            )
            Text(
                text = "✦",
                color = featureColor,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = size * 0.1f, y = size * 0.28f),
            )
        }
    }
}

private fun DrawScope.drawMascotFace(state: MascotState, color: Color, eyes: EyeStyle) {
    val eyeRadius = size.minDimension * 0.09f
    val eyeY = size.height * 0.38f
    val eyeGap = size.width * 0.22f
    val centerX = size.width / 2f
    val strokeWidth = eyeRadius * 0.6f

    if (state == MascotState.Sleepy) {
        val lineHalf = eyeRadius * 1.2f
        drawLine(
            color = color,
            start = Offset(centerX - eyeGap - lineHalf, eyeY),
            end = Offset(centerX - eyeGap + lineHalf, eyeY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(centerX + eyeGap - lineHalf, eyeY),
            end = Offset(centerX + eyeGap + lineHalf, eyeY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    } else when (eyes) {
        EyeStyle.Dot -> {
            drawCircle(color, radius = eyeRadius, center = Offset(centerX - eyeGap, eyeY))
            drawCircle(color, radius = eyeRadius, center = Offset(centerX + eyeGap, eyeY))
        }

        EyeStyle.Slot -> {
            val slot = Size(eyeRadius * 2.6f, eyeRadius * 1.4f)
            val corner = CornerRadius(slot.height / 2f)
            drawRoundRect(
                color = color,
                topLeft = Offset(centerX - eyeGap - slot.width / 2f, eyeY - slot.height / 2f),
                size = slot,
                cornerRadius = corner,
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(centerX + eyeGap - slot.width / 2f, eyeY - slot.height / 2f),
                size = slot,
                cornerRadius = corner,
            )
        }
    }

    val mouthY = size.height * 0.64f
    val mouthWidth = size.width * 0.34f
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
                            MascotAvatar(
                                state = state,
                                size = 56.dp,
                                character = character,
                            )
                        }
                    }
                }
            }
        }
    }
}
