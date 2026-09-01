package ph.mart.healthapp.feature.food.ui.photo.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * The model looked and couldn't tell. The photo it failed on is shown faded behind the message —
 * "that photo" means more with the photo there, and it is the one thing the user can act on.
 *
 * A sibling of [CaptureScreen] and the rest rather than an inline branch of the flow's `when`,
 * which is what `CLAUDE.md`'s file-breakdown rule asks for: [photo] may be null, since a failure
 * can arrive after the bitmap has gone.
 */
@Composable
internal fun RetryScreen(photo: Bitmap?, onRetry: () -> Unit, onLogManually: () -> Unit) {
    FullScreenState(
        icon = { RetryPhotoIcon(photo) },
        heading = "We couldn't analyze that photo",
        body = "Try taking the photo again with better lighting, or log the meal manually.",
        actions = {
            PrimaryButton(label = "Retry", onClick = onRetry, modifier = Modifier.fillMaxWidth())
            SecondaryButton(
                label = "Log manually instead",
                onClick = onLogManually,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

@Composable
private fun RetryPhotoIcon(photo: Bitmap?) {
    if (photo == null) return
    Image(
        bitmap = photo.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .alpha(0.6f),
    )
}

@PreviewLightDark
@Composable
private fun RetryScreenPreview() {
    AppTheme { RetryScreen(photo = null, onRetry = {}, onLogManually = {}) }
}
