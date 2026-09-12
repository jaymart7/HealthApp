package ph.mart.healthapp.feature.food.ui.photo.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R

/** Four times is as far as an 768px-long-edge plate is worth magnifying; past that it is pixels. */
private const val MAX_SCALE = 4f

/**
 * The captured plate, full-bleed on black, pinch-zoomable and pannable — the second level inside
 * [ph.mart.healthapp.feature.food.ui.photo.CaptureFlow.Confirmation], drawn over the review form so
 * the photo the numbers were read off can actually be looked at before they are committed.
 *
 * Full-bleed on purpose: the rest of the flow's non-camera states are inset by the caller's
 * `safeDrawingPadding`, but a viewer that letterboxes itself inside the system bars is showing
 * less of the picture than the camera did. The close button carries the inset instead.
 *
 * Back closes it — the flow's one always-mounted handler does that, not a second handler here.
 */
@Composable
internal fun PhotoViewerOverlay(photo: Bitmap, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var scale by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var frame by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { frame = it },
    ) {
        Image(
            bitmap = photo.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = pan.x
                    translationY = pan.y
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, panChange, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, MAX_SCALE)
                        // Zoomed all the way back out there is nothing left to pan to, and a photo
                        // left sitting off-centre at 1x is the state you can't get out of.
                        pan = if (scale == 1f) {
                            Offset.Zero
                        } else {
                            // The layer's translation sits outside its scale, so the drag tracks
                            // the finger 1:1 and only the clamp has to know about the zoom.
                            val next = pan + panChange
                            val limitX = maxPan(scale, frame.width.toFloat())
                            val limitY = maxPan(scale, frame.height.toFloat())
                            Offset(next.x.coerceIn(-limitX, limitX), next.y.coerceIn(-limitY, limitY))
                        }
                    }
                },
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .safeDrawingPadding()
                .padding(16.dp)
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape),
        ) {
            Icon(
                imageVector = AppIcons.Close,
                contentDescription = stringResource(R.string.food_photo_close),
                tint = Color.White,
            )
        }
    }
}

/**
 * How far the image may be dragged along one axis before its own edge would enter the frame: half
 * the extent the zoom added, and nothing at all at 1x.
 *
 * ponytail: clamped to the container, not to the drawn image. `ContentScale.Fit` letterboxes a
 * photo whose aspect ratio isn't the screen's, so on its short axis it can be dragged a little past
 * its own edge into the black. Clamp against the fitted rect if that ever reads as a bug.
 */
internal fun maxPan(scale: Float, extent: Float): Float = (extent * (scale - 1f) / 2f).coerceAtLeast(0f)

@PreviewLightDark
@Composable
private fun PhotoViewerOverlayPreview() {
    AppTheme {
        PhotoViewerOverlay(photo = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888), onClose = {})
    }
}
