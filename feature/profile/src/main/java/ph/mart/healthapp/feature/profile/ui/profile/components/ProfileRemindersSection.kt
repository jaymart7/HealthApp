package ph.mart.healthapp.feature.profile.ui.profile.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.profile.ReminderKind

/** The switches persist to the profile row; `:app`'s `ph.mart.healthapp.reminder` package watches
 * that row and reconciles the WorkManager schedule behind them. [message] carries the
 * notification-permission refusal, since a denied switch has to explain itself. */
@Composable
internal fun ProfileRemindersSection(
    enabled: (ReminderKind) -> Boolean,
    onToggle: (ReminderKind, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    messageIsError: Boolean = false,
) {
    SettingsSection(label = stringResource(R.string.profile_section_reminders), modifier = modifier) {
        AppCard {
            ReminderKind.entries.forEachIndexed { index, kind ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
                SettingsRow(
                    label = stringResource(kind.label),
                    sublabel = stringResource(kind.sublabel),
                    trailing = {
                        Switch(
                            checked = enabled(kind),
                            onCheckedChange = { checked -> onToggle(kind, checked) },
                        )
                    },
                )
            }
        }
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = if (messageIsError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
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

@PreviewLightDark
@Composable
private fun ProfileRemindersSectionDeniedPreview() {
    AppTheme {
        Surface {
            ProfileRemindersSection(
                enabled = { false },
                onToggle = { _, _ -> },
                message = "Allow notifications for FitPulse in your system settings to use reminders.",
                messageIsError = true,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
