package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.home.R

/** [daysSinceLastPhoto] null means no photo has ever been taken — the card prompts for a first
 * one rather than printing a nonsense day count. "Take one" opens the same Add photo sheet the
 * FAB uses; the sheet is hosted by AppScaffold, so this is a callback, not a cross-feature import. */
@Composable
fun ProgressPhotoReminderCard(
    daysSinceLastPhoto: Long?,
    onTakePhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_photo_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = when (daysSinceLastPhoto) {
                        null -> stringResource(R.string.home_photo_none)
                        0L -> stringResource(R.string.home_photo_today)
                        else -> pluralStringResource(
                            R.plurals.home_photo_days_ago,
                            daysSinceLastPhoto.toInt(),
                            daysSinceLastPhoto,
                        )
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onTakePhoto,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                Text(text = stringResource(R.string.home_photo_cta), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ProgressPhotoReminderCardPreview() {
    AppTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                ProgressPhotoReminderCard(daysSinceLastPhoto = 12, onTakePhoto = {})
                ProgressPhotoReminderCard(daysSinceLastPhoto = null, onTakePhoto = {})
            }
        }
    }
}
