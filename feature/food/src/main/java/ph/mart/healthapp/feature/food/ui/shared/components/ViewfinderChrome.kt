package ph.mart.healthapp.feature.food.ui.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
 * The two escape hatches every viewfinder in this feature carries — pick an image the flow can read
 * instead of aiming at it, or skip the camera and type the entry in. Shared by the food-photo and
 * barcode viewfinders, which is why it sits in `ui/shared/` rather than either flow's `components/`.
 *
 * Always black/white regardless of app theme, for the reason
 * [CaptureScreen][ph.mart.healthapp.feature.food.ui.photo.components.CaptureScreen]'s KDoc gives:
 * chrome over a live feed of arbitrary brightness can't follow the app's colors.
 */
@Composable
internal fun ViewfinderActions(
    onPickPhoto: () -> Unit,
    onEnterManually: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        // Icon-only, the way every camera app shows its gallery door: two labelled pills side by
        // side need 312dp, which overflows the 296dp a 360dp phone leaves between the viewfinder's
        // margins. The label rides the contentDescription instead.
        IconButton(
            onClick = onPickPhoto,
            modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape),
        ) {
            Icon(imageVector = AppIcons.Gallery, contentDescription = "Upload a photo", tint = Color.White)
        }
        ViewfinderPill(label = "Enter manually", onClick = onEnterManually)
    }
}

@Composable
private fun ViewfinderPill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        color = Color.Black.copy(alpha = 0.5f),
        contentColor = Color.White,
        shape = RoundedCornerShape(999.dp),
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun ViewfinderActionsPreview() {
    AppTheme {
        Row(modifier = Modifier.background(Color.Black).padding(24.dp)) {
            ViewfinderActions(onPickPhoto = {}, onEnterManually = {})
        }
    }
}
