package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** The 5 states of the app's geometric mascot, "Bibo". */
enum class MascotState { Idle, Happy, Celebrating, Sleepy, Thinking }

/**
 * The app's geometric mascot: a rounded-square [MaterialTheme.colorScheme.primaryContainer]
 * body with dot eyes + a mouth curve in [MaterialTheme.colorScheme.onPrimaryContainer]. No other
 * detail is added at any size, per the prototype's "Bibo" spec.
 */
@Composable
fun MascotAvatar(state: MascotState, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val featureColor = MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3))
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size * 0.62f)) {
            drawMascotFace(state, featureColor)
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

private fun DrawScope.drawMascotFace(state: MascotState, color: Color) {
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
    } else {
        drawCircle(color, radius = eyeRadius, center = Offset(centerX - eyeGap, eyeY))
        drawCircle(color, radius = eyeRadius, center = Offset(centerX + eyeGap, eyeY))
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
            Row(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                MascotState.entries.forEach { state ->
                    MascotAvatar(state = state, modifier = Modifier.size(72.dp))
                }
            }
        }
    }
}
