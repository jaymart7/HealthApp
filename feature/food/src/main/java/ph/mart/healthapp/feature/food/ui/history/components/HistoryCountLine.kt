package ph.mart.healthapp.feature.food.ui.history.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.food.R

/**
 * What the search found, and what a tap on it will do.
 *
 * Two facts on one 32dp row because they answer the two questions a first visit asks, and neither
 * is worth a row of its own on a screen where the keyboard already takes a third of the height.
 * The left says the search worked; the right says a tap *reviews* rather than logs — which is the
 * one thing this screen does differently from every other list of foods in the app, and the thing
 * a row that looks like a diary row will otherwise be assumed to do.
 */
@Composable
internal fun HistoryCountLine(
    matches: Int,
    days: Int,
    query: String,
    searching: Boolean,
    modifier: Modifier = Modifier,
) {
    val left = when {
        searching -> stringResource(R.string.food_history_searching)
        query.isBlank() -> stringResource(R.string.food_history_count_newest)
        else -> stringResource(
            R.string.food_history_count,
            pluralStringResource(R.plurals.food_history_count_matches, matches, matches),
            pluralStringResource(R.plurals.food_history_count_days, days, days),
        )
    }
    // The long form where the left side is a sentence rather than a count, so the row isn't two
    // clauses fighting for the same width.
    val hint = if (query.isBlank()) {
        stringResource(R.string.food_history_hint_long)
    } else {
        stringResource(R.string.food_history_hint)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().height(32.dp).padding(horizontal = 16.dp),
    ) {
        Text(
            text = left,
            style = MaterialTheme.typography.bodySmall.tabularNums,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            // One announcement, not a sentence followed by a bare glyph.
            modifier = Modifier.padding(start = 8.dp).clearAndSetSemantics {},
        ) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun HistoryCountLinePreview() {
    AppTheme {
        Surface {
            Column {
                HistoryCountLine(matches = 9, days = 4, query = "chick", searching = false)
                HistoryCountLine(matches = 1, days = 1, query = "oats", searching = false)
                HistoryCountLine(matches = 0, days = 0, query = "", searching = false)
                HistoryCountLine(matches = 9, days = 4, query = "chick", searching = true)
            }
        }
    }
}
