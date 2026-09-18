package ph.mart.healthapp.feature.profile.ui.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R

/** Wide enough for "Tap a day to put this in your week"-length labels without wrapping, narrow
 * enough not to cover the row it belongs to at phone width. */
private val MenuWidth = 224.dp

/**
 * A saved thing's only control. It replaces the two 44dp icon buttons every one of these rows
 * carried, and the point is not the tap count: **delete sat one thumb-width from rename**, in the
 * same tint, at the same weight. Here it exists only inside the menu, below a rule, in `error` —
 * and the ~96dp of row width the pair took goes back to the figures.
 *
 * [primaryLabel] is supplied per screen because the two words are not synonyms. The food library
 * says **Rename**: a saved food's numbers are corrected by re-saving from the add-entry sheet, and
 * a menu that said Edit would promise a form that does not exist. Supplements say **Edit**,
 * because there is one.
 *
 * **No swipe-to-delete.** That gesture already means "delete a diary *entry*"; reusing it where
 * the same swipe destroys a reusable definition is the wrong muscle memory.
 *
 * While the menu is open the anchor gains a `surfaceContainerHighest` circle — the only state in
 * which this button has a container — so the row the menu belongs to stays obvious once the menu
 * has covered its neighbours.
 */
@Composable
internal fun RowOverflowMenu(
    name: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onDelete: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { open = true },
            modifier = Modifier
                .size(48.dp)
                .then(
                    if (open) {
                        Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    } else {
                        Modifier
                    },
                ),
        ) {
            Icon(
                imageVector = AppIcons.MoreVert,
                contentDescription = stringResource(R.string.profile_row_more, name),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.width(MenuWidth),
        ) {
            DropdownMenuItem(
                text = { Text(text = primaryLabel, style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(imageVector = AppIcons.Edit, contentDescription = null, modifier = Modifier.size(20.dp)) },
                onClick = {
                    open = false
                    onPrimary()
                },
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.onSurface,
                    leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.heightIn(min = 48.dp),
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            // `error` twice — the glyph as well as the ink, so the one destructive item is not
            // told apart by colour alone.
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.profile_delete),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                leadingIcon = { Icon(imageVector = AppIcons.Delete, contentDescription = null, modifier = Modifier.size(20.dp)) },
                onClick = {
                    open = false
                    onDelete()
                },
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun RowOverflowMenuPreview() {
    AppTheme {
        Surface {
            Box(modifier = Modifier.padding(16.dp)) {
                RowOverflowMenu(name = "Chili", primaryLabel = "Rename", onPrimary = {}, onDelete = {})
            }
        }
    }
}
