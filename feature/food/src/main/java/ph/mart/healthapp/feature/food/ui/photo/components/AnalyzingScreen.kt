package ph.mart.healthapp.feature.food.ui.photo.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * The status line is one honest sentence rather than a rotating set of three.
 *
 * The rotation was a `while (true)` loop on a `LaunchedEffect` + `delay`, which is a hand-rolled
 * clock: it breaks DESIGN.md's No Loops Rule (nothing animates at rest) and its Remove-Animations
 * Rule at once, since a hand-rolled timer ignores `MotionDurationScale` and keeps ticking for a
 * user who has turned system animations off. It also promised progress it never had — the
 * transition off this screen waits for the recognition call however long it takes, and the
 * indeterminate bar already says "working" truthfully.
 */
@Composable
internal fun AnalyzingScreen(photo: Bitmap, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Image(
            bitmap = photo.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            // safeDrawingPadding after background: the scrim still covers the bars, the
            // mascot/progress/status stack sits inside them.
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            MascotAvatar(state = MascotState.Thinking, size = 88.dp)
            LinearProgressIndicator(modifier = Modifier.size(width = 200.dp, height = 4.dp))
            Text(
                text = "Looking at your photo…",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            // Same black/white-on-scrim exception as CaptureScreen — SecondaryButton's themed
            // outline/primary colors aren't guaranteed legible over this dark photo scrim.
            Surface(
                onClick = onCancel,
                shape = CircleShape,
                color = Color.Transparent,
                contentColor = Color.White,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun AnalyzingScreenPreview() {
    AppTheme {
        Surface {
            AnalyzingScreen(
                photo = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888),
                onCancel = {})
        }
    }
}
