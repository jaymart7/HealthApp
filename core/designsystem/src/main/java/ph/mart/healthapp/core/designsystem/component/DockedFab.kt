package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * How far a [DockedFab] reaches up from the bottom of a screen's content area: `FabPosition.End`
 * parks it 16dp above the bottom bar and the FAB itself is 56dp tall. Every scrollable screen that
 * sits under the FAB adds this to the *bottom of its scrolling content* — after `verticalScroll`,
 * not before — so the content scrolls under the FAB and only comes to rest clear of it, rather
 * than the viewport stopping short and leaving a dead band.
 */
val DockedFabContentPadding = 72.dp

/**
 * Extended FAB, [MaterialTheme.colorScheme.primaryContainer] fill, Level-3 shadow — the only real
 * drop shadow in the app. Positioning (docked, overlapping the nav bar) is the caller's job.
 */
@Composable
fun DockedFab(onClick: () -> Unit, modifier: Modifier = Modifier, label: String = "Log") {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 6.dp,
        modifier = modifier.height(56.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = AppIcons.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

@PreviewLightDark
@Composable
private fun DockedFabPreview() {
    AppTheme {
        Surface {
            DockedFab(onClick = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
