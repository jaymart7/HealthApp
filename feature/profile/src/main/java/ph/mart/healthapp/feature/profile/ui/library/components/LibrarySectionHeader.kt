package ph.mart.healthapp.feature.profile.ui.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/**
 * One of the library's three sticky headings, with the count of what is under it.
 *
 * A **segmented filter was the alternative and lost**: at two hundred rows the user's question is
 * "where is the thing I saved", and a segment answers it by hiding two thirds of the library, so
 * a wrong guess costs a second guess and a second scroll. The count does the other half of a
 * filter's job — it says how far a section runs before you commit to scrolling it — and sticking
 * the header says which section you are in once you have.
 *
 * The fill is **opaque `surface`**, never a translucent scrim: a row ghosting through a heading
 * while the list moves under it is worse than no heading at all.
 *
 * Not `shared/components/SectionHeader.kt`. That one is Profile's and Settings' heading — no
 * fill, no count, and padding written for a heading that scrolls away.
 */
@Composable
internal fun LibrarySectionHeader(label: String, count: Int, first: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 4.dp, end = 4.dp, top = if (first) 4.dp else 16.dp, bottom = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodySmall.tabularNums,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@PreviewLightDark
@Composable
private fun LibrarySectionHeaderPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                LibrarySectionHeader(label = "My foods", count = 128, first = true)
                LibrarySectionHeader(label = "Saved meals", count = 61, first = false)
            }
        }
    }
}
