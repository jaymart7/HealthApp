package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** The shared card chrome — `surfaceContainerLow`, 20dp corners, 16dp padding. Started life as
 * Home's private card; promoted here in Phase 8 once Profile needed the identical chrome, per
 * CLAUDE.md's "used in ≥2 screens → :core:designsystem, never duplicated" rule. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    if (onClick == null) {
        Surface(shape = shape, color = color, modifier = modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    } else {
        // Surface's own onClick overload, so the ripple is clipped to the card's corners.
        Surface(onClick = onClick, shape = shape, color = color, modifier = modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@PreviewLightDark
@Composable
private fun AppCardPreview() {
    AppTheme {
        Surface {
            AppCard(modifier = Modifier.padding(16.dp)) {
                Text(text = "Card content", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
