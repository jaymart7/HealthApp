package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** Rounded speech bubble with a left tail, used for mascot dialogue. */
@Composable
fun MascotSpeechBubble(text: String, modifier: Modifier = Modifier) {
    val bubbleColor = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(modifier = modifier.widthIn(max = 280.dp)) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 20.dp, y = (-6).dp)
                .size(12.dp)
                .rotate(45f)
                .background(bubbleColor),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(bubbleColor)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun MascotSpeechBubblePreview() {
    AppTheme {
        Surface {
            Box(modifier = Modifier.padding(24.dp)) {
                MascotSpeechBubble(text = "Track your body and nutrition, with Bibo by your side.")
            }
        }
    }
}
