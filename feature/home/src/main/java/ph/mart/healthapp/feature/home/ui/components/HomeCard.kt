package ph.mart.healthapp.feature.home.ui.components

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

/** The shared chrome for every Home card — `surfaceContainerLow`, 20dp corners, 16dp padding.
 * Screen-specific by design: it exists only to stop six files repeating the same Surface, not as
 * a design-system card (see CLAUDE.md's shared-vs-screen-specific rule). */
@Composable
internal fun HomeCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(shape = RoundedCornerShape(20.dp), color = color, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@PreviewLightDark
@Composable
private fun HomeCardPreview() {
    AppTheme {
        Surface {
            HomeCard(modifier = Modifier.padding(16.dp)) {
                Text(text = "Card content", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
