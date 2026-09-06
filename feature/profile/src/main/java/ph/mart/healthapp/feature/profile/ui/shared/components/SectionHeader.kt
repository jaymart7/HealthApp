package ph.mart.healthapp.feature.profile.ui.shared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * A heading over a group of cards — `titleMedium`, 12dp of air above it, inset 4dp from the cards
 * it names.
 *
 * Successor to `SettingsSection`, and the difference is the point of the redesign: that one drew an
 * uppercase `labelMedium` caption over every one of fourteen sections, which made a stepper touched
 * once a year look exactly like a switch touched weekly. This one is a sentence-case title, there
 * are four of them on Profile rather than fourteen, and it is a *header* rather than a wrapper —
 * it takes no content slot, so a caller decides what sits beneath it and how far apart.
 */
@Composable
internal fun SectionHeader(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = 12.dp, start = 4.dp),
    )
}

/** The uppercase variant, used only inside Reminders to name its three groups — there the heading
 * sits *within* one screen's single subject rather than naming a section of it, and the scale
 * change is what keeps the two from reading as the same level. */
@Composable
internal fun GroupHeader(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 4.dp, start = 4.dp),
    )
}

@PreviewLightDark
@Composable
private fun SectionHeaderPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeader(label = "Targets")
                GroupHeader(label = "Logging")
            }
        }
    }
}
