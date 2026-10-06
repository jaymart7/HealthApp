package ph.mart.healthapp.feature.training.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.exercise.StrengthSet
import ph.mart.healthapp.core.data.exercise.loadLabel
import ph.mart.healthapp.core.data.exercise.volumeKg
import ph.mart.healthapp.core.data.exercise.volumeLabel
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.resolve
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.training.R

/**
 * The sets logged so far, grouped under the lift they belong to.
 *
 * The grouping is a `groupBy` over the flat list rather than a level in the data: each set carries
 * its own exercise name, which is what keeps the schema at two tables instead of three. Insertion
 * order is preserved by `groupBy`, so the lifts read in the order they were done.
 *
 * Tapping a set hands its index in the flat list — the same list the form holds — to [onSelect],
 * which loads it into the editor to update or remove. A row from the middle of a group can't hit
 * its neighbour, and there is no bin on every row: one stray tap used to delete a set with no way
 * back, and fixing a typo cost a delete and a re-add at the end of the list. [selected] is the set
 * the editor is holding, drawn so the user can see which one they are changing.
 */
@Composable
internal fun StrengthSetList(
    sets: List<StrengthSet>,
    unit: UnitSystem,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selected: Int = -1,
) {
    if (sets.isEmpty()) {
        Text(
            text = stringResource(R.string.training_strength_no_sets),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }
    val editSet = stringResource(R.string.training_strength_edit_set)
    // Indices ride along so a row knows which entry of the flat list it is.
    val grouped = sets.withIndex().groupBy { it.value.exerciseName }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier) {
        grouped.forEach { (name, indexed) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = name.ifBlank { stringResource(R.string.training_strength_unnamed) },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    // A bodyweight lift moves no load, and "0 kg" beside it read as a mistake.
                    val volume = indexed.map { it.value }.volumeKg()
                    if (volume > 0) {
                        Text(
                            text = volumeLabel(volume, unit),
                            style = MaterialTheme.typography.labelMedium.tabularNums,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                indexed.forEachIndexed { position, (index, set) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                if (index == selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                            )
                            .clickable(onClickLabel = editSet) { onSelect(index) }
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            // Numbered within the lift, not within the workout: "set 3 of bench"
                            // is what a lifter counts, and the flat index is bookkeeping.
                            text = "${position + 1}",
                            style = MaterialTheme.typography.bodySmall.tabularNums,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                        Text(
                            text = set.loadLabel(unit).resolve(LocalResources.current),
                            style = MaterialTheme.typography.bodyMedium.tabularNums,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = AppIcons.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun StrengthSetListPreview() {
    AppTheme {
        Surface {
            StrengthSetList(
                sets = listOf(
                    StrengthSet("Bench press", 8, 60.0),
                    StrengthSet("Bench press", 8, 62.5),
                    StrengthSet("Pull-up", 10, 0.0),
                ),
                unit = UnitSystem.Metric,
                onSelect = {},
                selected = 1,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** Nothing logged yet — an invitation, not an empty container. */
@PreviewLightDark
@Composable
private fun StrengthSetListEmptyPreview() {
    AppTheme {
        Surface {
            StrengthSetList(
                sets = emptyList(),
                unit = UnitSystem.Metric,
                onSelect = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
