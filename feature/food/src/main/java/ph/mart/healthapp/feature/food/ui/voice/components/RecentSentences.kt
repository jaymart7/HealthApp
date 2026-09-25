package ph.mart.healthapp.feature.food.ui.voice.components

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
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
 * width of the field it is going into.
 *
 * Filled `surfaceContainerLow` with a leading clock, where they used to be outlined and bare. Both
 * changes are about the chip row a few dp below them: three outlined rounded rectangles above four
 * outlined rounded rectangles read as one control with seven options, and one of the two groups had
 * to stop looking like the other. The fill and the glyph say "something you did before" where the
 * chips say "pick one".
 *
 * Two lines rather than one ellipsised. A sentence cut off at "two scrambled eggs, a slice of…" is
 * exactly the sentence you cannot tell apart from the other one that starts the same way, which is
 * the whole job of the row.
 */
@Composable
internal fun RecentSentences(
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
            RecentSentenceRow(sentence = sentence, onSelect = { onSelect(sentence) })
        }
    }
}

@Composable
private fun RecentSentenceRow(sentence: String, onSelect: () -> Unit) {
    Surface(
        onClick = onSelect,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Icon(
                imageVector = AppIcons.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = sentence,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun RecentSentencesPreview() {
    AppTheme {
        Surface {
            RecentSentences(
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

/** The length the two-line clamp exists for: a dictated meal that a single line would cut at the
 * word that tells it apart from the one above it. */
@PreviewLightDark
@Composable
private fun RecentSentencesLongPreview() {
    AppTheme {
        Surface {
            RecentSentences(
                sentences = listOf(
                    "two scrambled eggs, a slice of wholemeal toast with butter and a black coffee",
                    "two scrambled eggs, a slice of wholemeal toast and a flat white",
                ),
                onSelect = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
