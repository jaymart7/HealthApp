package ph.mart.healthapp.feature.profile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** An uppercase section label over one or more cards. Every Profile section uses it, which is why
 * the cards are a slot rather than baked in — Data holds two, the rest hold one. */
@Composable
internal fun SettingsSection(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
        )
        content()
    }
}

@PreviewLightDark
@Composable
private fun SettingsSectionPreview() {
    AppTheme {
        Surface {
            SettingsSection(label = "Units", modifier = Modifier.padding(16.dp)) {
                AppCard {
                    Text(text = "Card content", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
