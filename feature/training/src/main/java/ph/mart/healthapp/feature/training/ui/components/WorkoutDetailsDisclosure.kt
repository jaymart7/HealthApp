package ph.mart.healthapp.feature.training.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.training.R
import ph.mart.healthapp.feature.training.ui.LogExerciseForm

/**
 * The strength screen's note, duration and burn, folded to one line. On that screen they are the
 * figures nobody came to type — the sets are — and the burn re-estimates itself off the duration
 * anyway, so the summary is enough until something in it is wrong. Collapsed by default, and the
 * summary keeps both figures in sight so a form that will not save never hides why.
 *
 * `DoseNutrientFields`' disclosure, one module over: a header row that is the whole tap target, a
 * chevron that turns, and the content beneath it. [initiallyExpanded] is for the preview.
 */
@Composable
internal fun WorkoutDetailsDisclosure(
    minutes: Int,
    burnedKcal: Int,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button) { expanded = !expanded }
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.training_strength_details),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.training_strength_details_summary, minutes, burnedKcal),
                style = MaterialTheme.typography.bodySmall.tabularNums,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
            Icon(
                imageVector = AppIcons.ChevronDown,
                contentDescription = stringResource(
                    if (expanded) R.string.training_strength_details_hide else R.string.training_strength_details_show,
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(rotation),
            )
        }
        if (expanded) content()
    }
}

/** The fields the strength screen folds in here, at the summary's own figures. */
@Composable
private fun PreviewFields() {
    ExerciseFormFields(
        form = LogExerciseForm(type = ExerciseType.Strength, minutes = 45, burnedKcal = 260, burnedEdited = true),
        weightKg = 74.0,
        onFormChange = {},
        showTypeChips = false,
    )
}

@PreviewLightDark
@Composable
private fun WorkoutDetailsDisclosurePreview() {
    AppTheme {
        Surface {
            WorkoutDetailsDisclosure(minutes = 45, burnedKcal = 260, modifier = Modifier.padding(16.dp)) {
                PreviewFields()
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun WorkoutDetailsDisclosureExpandedPreview() {
    AppTheme {
        Surface {
            WorkoutDetailsDisclosure(
                minutes = 45,
                burnedKcal = 260,
                initiallyExpanded = true,
                modifier = Modifier.padding(16.dp),
            ) {
                PreviewFields()
            }
        }
    }
}
