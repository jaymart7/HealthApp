package ph.mart.healthapp.feature.profile.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * The one switch that changes what a number on Home means. Your calorie target already multiplies
 * BMR by your activity level, so crediting a logged workout on top of it can count the same
 * training twice — the sublabel says exactly that, because a user who can't tell why the two
 * numbers disagree will assume one of them is broken.
 */
@Composable
internal fun ProfileExerciseSection(
    addToBudget: Boolean,
    onSetAddToBudget: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSection(label = "Exercise", modifier = modifier) {
        AppCard {
            SettingsRow(
                label = "Add exercise calories",
                sublabel = "Logged workouts raise the day's budget. Turn off if your activity " +
                    "level already covers them.",
                trailing = { Switch(checked = addToBudget, onCheckedChange = onSetAddToBudget) },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ProfileExerciseSectionPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ProfileExerciseSection(
                addToBudget = true,
                onSetAddToBudget = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
