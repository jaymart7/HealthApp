package ph.mart.healthapp.feature.food.ui.shared.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R

/**
 * Full-bleed camera chrome for the two flows that take a still and send it to a model — the food
 * photo and the nutrition label. Here rather than in either flow's `components/` for that reason;
 * the barcode viewfinder is [ScanScreen][ph.mart.healthapp.feature.food.ui.barcode.components.ScanScreen]
 * next door, which is this screen without a shutter, because its decoder fires by itself.
 *
 * Always black/white regardless of app theme — a camera viewfinder's overlay controls need to stay
 * legible over a live feed of arbitrary brightness, the same reason the prototype hardcodes
 * `rgba(0,0,0,…)`/`#fff` here instead of theme tokens even though every other screen in the app
 * reads colors from [MaterialTheme.colorScheme].
 *
 * [guideSize] and [hint] are the only two things the two flows disagree about, and they disagree
 * about them for the same reason: **the guide is the shape of the thing being framed.** A plate is
 * square and a nutrition panel is a column, and a square drawn over a panel tells the user to stand
 * too far back to read it.
 */
@Composable
internal fun CaptureScreen(
    onClose: () -> Unit,
    onCapture: () -> Unit,
    onPickPhoto: () -> Unit,
    onEnterManually: () -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    guideSize: DpSize = DpSize(220.dp, 220.dp),
    cameraPreview: @Composable () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        cameraPreview()

        // Framing guide is centred on the preview, so it stays outside the safe-area box below.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(guideSize)
                .border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(24.dp)),
        )

        // Everything tappable or readable sits inside the safe area; the preview stays full-bleed.
        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape),
            ) {
                Icon(imageVector = AppIcons.Close, contentDescription = stringResource(R.string.food_photo_close), tint = Color.White)
            }

            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 88.dp),
            ) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 32.dp),
            ) {
                // The two doors out of the camera: a picture already taken, or no picture at all.
                // The flash icon that used to flank the shutter is still gone rather than labelled —
                // it was prototype chrome with no handler behind it, and a control that looks
                // tappable and does nothing is worse than an absent one.
                ViewfinderActions(onPickPhoto = onPickPhoto, onEnterManually = onEnterManually)

                // An empty Surface is invisible to a screen reader, and this one is the whole
                // point of the screen. Resolved above the lambda, which cannot read a resource.
                val takePhoto = stringResource(R.string.food_photo_take)
                Surface(
                    onClick = onCapture,
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(4.dp, Color.White.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .size(72.dp)
                        .semantics {
                            contentDescription = takePhoto
                            role = Role.Button
                        },
                ) {}
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun CaptureScreenPreview() {
    AppTheme {
        CaptureScreen(
            onClose = {},
            onCapture = {},
            onPickPhoto = {},
            onEnterManually = {},
            hint = stringResource(R.string.food_photo_center),
        )
    }
}

/** The label flow's shape: a column, not a plate. */
@PreviewLightDark
@Composable
private fun CaptureScreenLabelPreview() {
    AppTheme {
        CaptureScreen(
            onClose = {},
            onCapture = {},
            onPickPhoto = {},
            onEnterManually = {},
            hint = stringResource(R.string.food_label_frame),
            guideSize = LabelGuideSize,
        )
    }
}

/** The nutrition panel's own proportions — tall and narrow, because that is how one is printed.
 * Here rather than in the label flow so the preview above and its caller cannot drift. */
internal val LabelGuideSize = DpSize(240.dp, 320.dp)
