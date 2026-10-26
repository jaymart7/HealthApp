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
 * The strength screen's note and burn, folded to one line. On that screen they are the figures
 * nobody came to type — the sets are — and the burn is the sets' own, so the summary is enough
 * until the note needs writing. Collapsed by default. [minutes] is null on a counted workout, which
 * is every new one; only a correction of a row logged without sets is timed, and then the summary
 * keeps its duration in sight so a form that will not save never hides why.
 *
 * `DoseNutrientFields`' disclosure, one module over: a header row that is the whole tap target, a
 * chevron that turns, and the content beneath it. [initiallyExpanded] is for the preview.
 */
@Composable
internal fun WorkoutDetailsDisclosure(
    minutes: Int?,
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
                text = if (minutes == null) {
                    stringResource(R.string.training_strength_kcal, burnedKcal)
                } else {
                    stringResource(R.string.training_strength_details_summary, minutes, burnedKcal)
                },
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

/** The fields the strength screen folds in here, at the summary's own figures — [timed] only for a
 * correction of a row logged without sets. */
@Composable
private fun PreviewFields(timed: Boolean) {
    ExerciseFormFields(
        form = LogExerciseForm(type = ExerciseType.Strength, minutes = 45, burnedKcal = 260, burnedEdited = true),
        weightKg = 74.0,
        onFormChange = {},
        showTypeChips = false,
        timed = timed,
    )
}

/** A counted workout, the usual case: the burn is the sets', and there is no duration. */
@PreviewLightDark
@Composable
private fun WorkoutDetailsDisclosurePreview() {
    AppTheme {
        Surface {
            WorkoutDetailsDisclosure(minutes = null, burnedKcal = 14, modifier = Modifier.padding(16.dp)) {
                PreviewFields(timed = false)
            }
        }
    }
}

/** A correction of a strength row logged by duration alone, opened: the steppers it still has. */
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
                PreviewFields(timed = true)
            }
        }
    }
}
