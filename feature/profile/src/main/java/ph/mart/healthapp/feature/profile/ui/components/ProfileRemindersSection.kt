package ph.mart.healthapp.feature.profile.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.ui.ReminderKind

/** The switches persist to the profile row and nothing else — no notifications are scheduled.
 * Real scheduling is deliberately out of Phase 8's scope (see COMPONENTS.md). */
@Composable
internal fun ProfileRemindersSection(
    enabled: (ReminderKind) -> Boolean,
    onToggle: (ReminderKind, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSection(label = "Reminders", modifier = modifier) {
        AppCard {
            ReminderKind.entries.forEachIndexed { index, kind ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
                SettingsRow(
                    label = kind.label,
                    sublabel = kind.sublabel,
                    trailing = {
                        Switch(
                            checked = enabled(kind),
                            onCheckedChange = { checked -> onToggle(kind, checked) },
                        )
                    },
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ProfileRemindersSectionPreview() {
    AppTheme {
        Surface {
            ProfileRemindersSection(
                enabled = { it != ReminderKind.Photo },
                onToggle = { _, _ -> },
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
