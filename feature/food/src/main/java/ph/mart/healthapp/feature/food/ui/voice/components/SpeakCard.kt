package ph.mart.healthapp.feature.food.ui.voice.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R

/**
 * The mic, at the size the screen's fastest path deserves.
 *
 * It used to be a 48dp grey glyph in a right-aligned row under the field, carrying the same weight
 * as Clear beside it — two identical icon buttons doing opposite jobs, one of which is the whole
 * reason the screen exists. Filled `primary` at 88dp it is the first thing on the screen and the
 * obvious first move, and typing is still one tap away in the field below it.
 *
 * Two label pairs, because tapping it means something different once there are words in the box:
 * with an empty field it explains what to say, and with a full one it warns what it will do to
 * what is already there. [onPrimary] at full opacity throughout — a subtitle dimmed with alpha on
 * a filled container is the first thing to fail a contrast check in the high-contrast scheme.
 *
 * Not composed at all where no recognizer is installed. That is the same call Home's supplements
 * card makes — a control that cannot answer shouldn't be there — and the one thing the manifest's
 * `<queries>` entry exists for.
 */
@Composable
internal fun SpeakCard(
    hasText: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .heightIn(min = 88.dp)
                .padding(16.dp),
        ) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = AppIcons.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(
                        if (hasText) R.string.food_voice_speak_again else R.string.food_voice_speak_title,
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    text = stringResource(
                        if (hasText) R.string.food_voice_speak_again_body else R.string.food_voice_speak_body,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SpeakCardPreview() {
    AppTheme {
        Surface {
            SpeakCard(hasText = false, onClick = {}, modifier = Modifier.padding(16.dp))
        }
    }
}

/** The warning half: there is a sentence in the box, and this replaces it. */
@PreviewLightDark
@Composable
private fun SpeakCardWithTextPreview() {
    AppTheme {
        Surface {
            SpeakCard(hasText = true, onClick = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
