package ph.mart.healthapp.feature.profile.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R

/**
 * Shown only when Android is refusing FitPulse's notifications, and **the one place in this work an
 * `errorContainer` surface is right**: every switch below it is silently inert, which is a genuine
 * failure rather than a caution. The calorie floor is the counter-example — it is `error` *text* on
 * an ordinary card, because being under a target is not a failure.
 *
 * It carries a way out rather than only a complaint: the button opens FitPulse's own notification
 * settings, since the permission cannot be asked for again once it has been refused twice.
 */
@Composable
internal fun RemindersPermissionBanner(
    onOpenSystemSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Icon(
                imageVector = AppIcons.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp).padding(top = 2.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.profile_reminders_permission_blocked),
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(
                    onClick = onOpenSystemSettings,
                    modifier = Modifier.heightIn(min = 40.dp),
                ) {
                    Text(
                        text = stringResource(R.string.profile_reminders_open_settings),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textDecoration = TextDecoration.Underline,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun RemindersPermissionBannerPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            RemindersPermissionBanner(onOpenSystemSettings = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
