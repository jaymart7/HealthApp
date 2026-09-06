package ph.mart.healthapp.feature.profile.ui.settings.components

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.transfer.LocalBackup
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.shared.components.AppListRow
import ph.mart.healthapp.feature.profile.ui.shared.components.NavChevron

/** Export and import are the only destructive-ish controls in Settings, so each says exactly what
 * it does in its sublabel. [message] carries an import failure, or a confirmation.
 *
 * [backups] is what the weekly job has on disk. The rows are absent rather than empty when there
 * is nothing yet — the job has simply not run, which is not a state worth a line of copy — and
 * they only ever *point* the existing import at a file: restoring is manual, because a job that
 * restored on its own could wipe a good device from a stale file.
 *
 * One card with dividers rather than a card per row, unlike the version this replaces: these three
 * are one subject (what leaves and enters this phone) and three separate cards read as three
 * unrelated decisions. */
@Composable
internal fun SettingsDataSection(
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
    backups: List<LocalBackup> = emptyList(),
    onRestore: (LocalBackup) -> Unit = {},
    message: String? = null,
    messageIsError: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        AppCard(contentPadding = PaddingValues(horizontal = 16.dp)) {
            AppListRow(
                label = stringResource(R.string.profile_export),
                sublabel = stringResource(R.string.profile_export_sub),
                trailing = { NavChevron() },
                modifier = Modifier.clickable(onClick = onExport),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            AppListRow(
                label = stringResource(R.string.profile_import),
                sublabel = stringResource(R.string.profile_import_sub),
                trailing = { NavChevron() },
                modifier = Modifier.clickable(onClick = onImport),
            )
            backups.forEach { backup ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AppListRow(
                    label = stringResource(R.string.profile_backup_restore),
                    // The platform's own relative phrasing, so the date needs no format of its own
                    // and follows the device locale for free.
                    sublabel = stringResource(
                        R.string.profile_backup_restore_sub,
                        DateUtils.getRelativeTimeSpanString(backup.savedAtMillis),
                    ),
                    trailing = { NavChevron() },
                    modifier = Modifier.clickable { onRestore(backup) },
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
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SettingsDataSectionPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            SettingsDataSection(
                onExport = {},
                onImport = {},
                backups = listOf(
                    LocalBackup("fitpulse-1.json", System.currentTimeMillis() - 2 * 86_400_000L),
                    LocalBackup("fitpulse-2.json", System.currentTimeMillis() - 9 * 86_400_000L),
                ),
                message = "That file couldn't be read.",
                messageIsError = true,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
