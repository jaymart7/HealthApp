package ph.mart.healthapp.feature.food.ui.history.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R

/**
 * The queries that have found something before, offered under an empty field.
 *
 * Only under an empty one: once there is a query, these are three ways to throw away what has been
 * typed, sitting directly above the results it produced. A search that has never been used draws
 * nothing at all rather than an empty row explaining itself.
 *
 * Outlined rather than filled, so they read as *offers* beside the field and not as the selected
 * filter chips they sit a few dp away from.
 */
@Composable
internal fun HistoryRecentQueries(
    queries: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().height(40.dp),
    ) {
        Text(
            text = stringResource(R.string.food_history_recent).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.9.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        queries.forEach { query ->
            Surface(
                onClick = { onSelect(query) },
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.height(40.dp),
            ) {
                Text(
                    text = query,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun HistoryRecentQueriesPreview() {
    AppTheme {
        Surface {
            HistoryRecentQueries(
                queries = listOf("chicken", "oats", "protein bar"),
                onSelect = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
