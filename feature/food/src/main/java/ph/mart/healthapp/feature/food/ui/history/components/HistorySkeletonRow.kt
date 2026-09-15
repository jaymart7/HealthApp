package ph.mart.healthapp.feature.food.ui.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * A row that is on its way — drawn at the *tail* of the list, never over it.
 *
 * The rule the whole in-flight state follows: the previous results stay fully painted and these
 * appear below them, because a list that blanks on every keystroke is a list nobody can read while
 * typing. Two of them, because the point is to say "more is coming", not to guess how much.
 *
 * Static, with no shimmer: the indeterminate line under the field is already the thing that moves,
 * and a second animation for the same fact is noise. Announced as nothing at all — there is no row
 * here yet to describe.
 */
@Composable
internal fun HistorySkeletonRow(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clearAndSetSemantics {},
    ) {
        Block(width = 40.dp, height = 40.dp, radius = 8.dp)
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Block(widthFraction = 0.58f, height = 15.dp, radius = 8.dp)
            Block(widthFraction = 0.40f, height = 11.dp, radius = 6.dp, dim = true)
        }
        Spacer(modifier = Modifier.size(12.dp))
        Block(width = 52.dp, height = 20.dp, radius = 8.dp)
    }
}

@Composable
private fun Block(
    height: androidx.compose.ui.unit.Dp,
    radius: androidx.compose.ui.unit.Dp,
    width: androidx.compose.ui.unit.Dp? = null,
    widthFraction: Float? = null,
    dim: Boolean = false,
) {
    val color =
        if (dim) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    Spacer(
        modifier = Modifier
            .then(
                when {
                    width != null -> Modifier.width(width)
                    widthFraction != null -> Modifier.fillMaxWidth(widthFraction)
                    else -> Modifier.fillMaxWidth()
                },
            )
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(color),
    )
}

@PreviewLightDark
@Composable
private fun HistorySkeletonRowPreview() {
    AppTheme {
        Surface {
            Column {
                HistorySkeletonRow()
                HistorySkeletonRow()
            }
        }
    }
}
