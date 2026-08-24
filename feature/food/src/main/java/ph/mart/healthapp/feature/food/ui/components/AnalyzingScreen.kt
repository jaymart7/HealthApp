package ph.mart.healthapp.feature.food.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.theme.AppTheme

private val STATUS_PHRASES = listOf("Looking at your photo…", "Identifying the food…", "Estimating portion…")

/**
 * The rotating status text is purely cosmetic (loops on a [LaunchedEffect], doesn't gate
 * anything) — the real transition off this screen waits for the actual recognition call, however
 * long it takes, per CLAUDE.md's "reflect actual request latency" requirement.
 */
@Composable
internal fun AnalyzingScreen(photo: Bitmap, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    var phraseIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_200)
            phraseIndex = (phraseIndex + 1) % STATUS_PHRASES.size
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Image(
            bitmap = photo.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            MascotAvatar(state = MascotState.Thinking, size = 88.dp)
            LinearProgressIndicator(modifier = Modifier.size(width = 200.dp, height = 4.dp))
            Text(
                text = STATUS_PHRASES[phraseIndex],
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
        AnalyzingScreen(photo = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888), onCancel = {})
    }
}
