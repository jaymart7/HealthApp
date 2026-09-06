package ph.mart.healthapp.feature.profile.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.ui.settings.ReminderGroup
import ph.mart.healthapp.feature.profile.ui.settings.ReminderKind
import ph.mart.healthapp.feature.profile.ui.shared.components.AppListRow
import ph.mart.healthapp.feature.profile.ui.shared.components.GroupHeader

/** One of the three groups: its heading, then that group's switches in a single card.
 *
 * The switches persist to the profile row; `:app`'s `ph.mart.healthapp.reminder` package watches
 * that row and reconciles the WorkManager schedule behind them — nothing here calls a scheduler.
 * The group's membership comes off [ReminderKind.group] rather than a list written out here, so a
 * ninth reminder lands in a group by declaring one. */
@Composable
internal fun RemindersGroupCard(
    group: ReminderGroup,
    enabled: (ReminderKind) -> Boolean,
    onToggle: (ReminderKind, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        GroupHeader(label = stringResource(group.label))
        AppCard(contentPadding = PaddingValues(horizontal = 16.dp)) {
            ReminderKind.entries.filter { it.group == group }.forEachIndexed { index, kind ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AppListRow(
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
    }
}

@PreviewLightDark
@Composable
private fun RemindersGroupCardPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                RemindersGroupCard(
                    group = ReminderGroup.Logging,
                    enabled = { it != ReminderKind.Photo },
                    onToggle = { _, _ -> },
                )
                RemindersGroupCard(
                    group = ReminderGroup.Summary,
                    enabled = { false },
                    onToggle = { _, _ -> },
                )
            }
        }
    }
}
