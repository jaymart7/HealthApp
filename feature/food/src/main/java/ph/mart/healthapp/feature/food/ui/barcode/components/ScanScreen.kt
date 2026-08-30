package ph.mart.healthapp.feature.food.ui.barcode.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import ph.mart.healthapp.feature.food.ui.photo.components.CaptureScreen

/**
 * The barcode viewfinder — [CaptureScreen] without a shutter button, since the decoder fires by
 * itself as soon as it reads a code. Same hardcoded black/white overlay for the same reason
 * documented there: chrome over a live feed of arbitrary brightness can't follow the app theme.
 *
 * The guide is a wide, short rectangle rather than the food flow's square: it's the shape of the
 * thing being framed, and it's what tells the user to hold the phone across the barcode.
 */
@Composable
internal fun ScanScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    cameraPreview: @Composable () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        cameraPreview()

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(280.dp)
                .height(140.dp)
                .border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
        )

        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape),
            ) {
                Icon(imageVector = AppIcons.Close, contentDescription = "Close", tint = Color.White)
            }

            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 88.dp),
            ) {
                Text(
                    text = "Line up the barcode",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ScanScreenPreview() {
    AppTheme {
        ScanScreen(onClose = {})
    }
}
