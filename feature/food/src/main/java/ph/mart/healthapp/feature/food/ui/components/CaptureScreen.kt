package ph.mart.healthapp.feature.food.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * Full-bleed camera chrome. Always black/white regardless of app theme — a camera viewfinder's
 * overlay controls need to stay legible over a live feed of arbitrary brightness, the same reason
 * the prototype hardcodes `rgba(0,0,0,…)`/`#fff` here instead of theme tokens even though every
 * other screen in the app reads colors from [MaterialTheme.colorScheme]. Flash and the gallery
 * icon are visual-only, matching the prototype (neither has a real handler there either).
 */
@Composable
internal fun CaptureScreen(
    onClose: () -> Unit,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier,
    cameraPreview: @Composable () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        cameraPreview()

        // Framing guide is centred on the preview, so it stays outside the safe-area box below.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(220.dp)
                .border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(24.dp)),
        )

        // Everything tappable or readable sits inside the safe area; the preview stays full-bleed.
        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape),
            ) {
                Icon(imageVector = AppIcons.Close, contentDescription = "Close", tint = Color.White)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = AppIcons.Flash, contentDescription = null, tint = Color.White)
            }

            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 88.dp),
            ) {
                Text(
                    text = "Center one food item",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 32.dp, vertical = 32.dp)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = AppIcons.Gallery, contentDescription = null, tint = Color.White)
                }

                Surface(
                    onClick = onCapture,
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(4.dp, Color.White.copy(alpha = 0.5f)),
                    modifier = Modifier.align(Alignment.Center).size(72.dp),
                ) {}
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun CaptureScreenPreview() {
    AppTheme {
        CaptureScreen(onClose = {}, onCapture = {})
    }
}
