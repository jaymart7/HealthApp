package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** Pill button, 48dp min height, [MaterialTheme.colorScheme.primary] fill. Disabled = 40% opacity. */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier
            .graphicsLayer(alpha = if (enabled) 1f else 0.4f)
            .heightIn(min = 48.dp),
    ) {
        Box(modifier = Modifier.padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
            Text(text = label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

/** Pill button, [MaterialTheme.colorScheme.outline] border, transparent fill, primary text. */
@Composable
fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .graphicsLayer(alpha = if (enabled) 1f else 0.4f)
            .heightIn(min = 48.dp),
    ) {
        Box(modifier = Modifier.padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
            Text(text = label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

/** No container, primary text, compact height. */
@Composable
fun TextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .graphicsLayer(alpha = if (enabled) 1f else 0.4f)
            .heightIn(min = 44.dp),
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@PreviewLightDark
@Composable
private fun ButtonsPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                PrimaryButton(label = "Get started", onClick = {}, modifier = Modifier.width(200.dp))
                Box(modifier = Modifier.height(12.dp))
                PrimaryButton(label = "Disabled", onClick = {}, enabled = false, modifier = Modifier.width(200.dp))
                Box(modifier = Modifier.height(12.dp))
                SecondaryButton(label = "Log manually instead", onClick = {}, modifier = Modifier.width(200.dp))
                Box(modifier = Modifier.height(12.dp))
                TextButton(label = "I already have an account", onClick = {})
            }
        }
    }
}
