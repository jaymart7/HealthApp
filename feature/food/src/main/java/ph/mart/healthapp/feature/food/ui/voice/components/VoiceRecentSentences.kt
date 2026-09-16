package ph.mart.healthapp.feature.food.ui.voice.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R

/**
 * The sentences that have already become meals, offered under an empty field.
 *
 * Only under an empty one, which is `HistoryRecentQueries`' rule and its reason: once there is a
 * sentence in the field, these are three ways to throw away what has been typed. A screen that has
 * never logged one draws nothing at all rather than an empty row explaining itself.
 *
 * Rows rather than that screen's pills, and the one place the two deliberately differ: a pill is
 * sized for "chicken", and "two scrambled eggs, a slice of toast and a black coffee" needs the
 * width of the field it is going into. Outlined for the same reason the pills are — they are
 * *offers* beside the field, not the selected meal chips a few dp below them.
 */
@Composable
internal fun VoiceRecentSentences(
    sentences: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.food_voice_recent).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.9.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        sentences.forEach { sentence ->
            Surface(
                onClick = { onSelect(sentence) },
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(
                    text = sentence,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun VoiceRecentSentencesPreview() {
    AppTheme {
        Surface {
            VoiceRecentSentences(
                sentences = listOf(
                    "two scrambled eggs, a slice of toast and a black coffee",
                    "chicken breast, rice and steamed broccoli",
                    "a banana",
                ),
                onSelect = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
