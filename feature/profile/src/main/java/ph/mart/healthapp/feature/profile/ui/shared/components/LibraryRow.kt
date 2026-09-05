package ph.mart.healthapp.feature.profile.ui.shared.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R

/**
 * One saved meal, recipe or workout routine, as its library lists it: rename and delete, and **no
 * way to use it**. Logging a meal needs a meal slot and a day, and starting a routine needs a
 * workout in progress; Profile has none of those — the add-entry sheet's panels and the strength
 * screen's chips stay the places those things are used.
 *
 * [contents] names the parts rather than only counting them. A row reading "4 items · 540 kcal" is
 * a row you delete blind, and they are already loaded, so saying which four costs nothing.
 *
 * [trailing] is drawn full-width under the row, and is how the routine library hangs its weekday
 * picker on a row the food library shares — seven chips do not fit beside two icon buttons. Null by
 * default, so the two food libraries render exactly as they did.
 */
@Composable
internal fun LibraryRow(
    name: String,
    summary: String,
    contents: String,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (contents.isNotEmpty()) {
                        Text(
                            text = contents,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(onClick = onRename, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = AppIcons.Edit,
                        contentDescription = stringResource(R.string.profile_rename_item, name),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = AppIcons.Delete,
                        contentDescription = stringResource(R.string.profile_delete_item, name),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            trailing?.let { content ->
                Box(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) { content() }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun LibraryRowPreview() {
    AppTheme {
        Surface {
            LibraryRow(
                name = "Usual breakfast",
                summary = "3 items · 540 kcal",
                contents = "Greek yogurt, Oats, Black coffee",
                onRename = {},
                onDelete = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** A meal saved from an empty section: the third line has nothing to say and is left off entirely
 * rather than rendered blank. */
@PreviewLightDark
@Composable
private fun LibraryRowNoContentsPreview() {
    AppTheme {
        Surface {
            LibraryRow(
                name = "Post-gym shake",
                summary = "0 items · 0 kcal",
                contents = "",
                onRename = {},
                onDelete = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
