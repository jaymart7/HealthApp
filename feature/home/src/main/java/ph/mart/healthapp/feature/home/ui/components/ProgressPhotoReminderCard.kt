package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.home.R

/**
 * [daysSinceLastPhoto] null means no photo has ever been taken — the card says so in words rather
 * than printing a nonsense day count, which is why the value here is not always a number. "Take
 * one" opens the same Add photo sheet the FAB uses; the sheet is hosted by AppScaffold, so this is
 * a callback, not a cross-feature import.
 *
 * No status mark: there is no photo *target*, so there is nothing to be on or off track against.
 */
@Composable
fun ProgressPhotoReminderCard(
    daysSinceLastPhoto: Long?,
    onTakePhoto: () -> Unit,
    wide: Boolean,
    modifier: Modifier = Modifier,
) {
    MetricCard(
        label = stringResource(R.string.home_photo_title),
        value = when (daysSinceLastPhoto) {
            null -> stringResource(R.string.home_photo_none_value)
            0L -> stringResource(R.string.home_photo_today)
            else -> "$daysSinceLastPhoto"
        },
        unit = daysSinceLastPhoto?.takeIf { it > 0L }?.let {
            pluralStringResource(R.plurals.home_photo_days_unit, it.toInt())
        },
        wide = wide,
        modifier = modifier,
    ) {
        MetaButton(label = stringResource(R.string.home_photo_cta), onClick = onTakePhoto)
    }
}

@PreviewLightDark
@Composable
private fun ProgressPhotoReminderCardPreview() {
    AppTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                ProgressPhotoReminderCard(daysSinceLastPhoto = 12, onTakePhoto = {}, wide = true)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ProgressPhotoReminderCard(
                        daysSinceLastPhoto = 3,
                        onTakePhoto = {},
                        wide = false,
                        modifier = Modifier.weight(1f),
                    )
                    ProgressPhotoReminderCard(
                        daysSinceLastPhoto = null,
                        onTakePhoto = {},
                        wide = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
