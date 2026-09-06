package ph.mart.healthapp.feature.profile.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.cmToDisplayUnit
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.data.profile.displayUnitToCm
import ph.mart.healthapp.core.data.profile.displayUnitToKg
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.lengthUnitLabel
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.component.SelectableCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.shared.components.AppListRow
import ph.mart.healthapp.feature.profile.ui.shared.components.SectionHeader
import ph.mart.healthapp.feature.profile.ui.shared.components.StepperRow
import ph.mart.healthapp.feature.profile.ui.shared.formatBodyValue
import ph.mart.healthapp.feature.profile.ui.shared.label
import ph.mart.healthapp.feature.profile.ui.shared.sublabel

/**
 * The six Mifflin–St Jeor inputs, editable for the first time since onboarding — which is the gap
 * this whole redesign exists to close. The app knew every one of these and showed none of them.
 *
 * It reuses [ProfileViewModel] rather than declaring one of its own: these write the same profile
 * row the identity header reads, through the same setters, so a second host would be a second set
 * of writes to keep in step for nothing. No save button either — every control writes on change and
 * the result card below recomputes in the same frame, because nothing caches the calorie figure.
 *
 * Its goal and activity options are declared here rather than imported from `:feature:onboarding`:
 * a cross-feature type reference is exactly what the module boundary forbids, so the wording is
 * duplicated as `profile_*` keys matching onboarding's.
 *
 * The toolbar and its back arrow come from `:app`'s `AppScaffold`, as on every route above a tab.
 */
@Composable
fun AboutYouScreen(viewModel: ProfileViewModel = koinViewModel()) {
    val uiState by viewModel.collectAsState()
    AboutYouContent(
        profile = uiState.profile,
        onSetSex = viewModel::setSex,
        onSetAge = viewModel::setAge,
        onSetHeightCm = viewModel::setHeightCm,
        onSetCurrentWeightKg = viewModel::setCurrentWeightKg,
        onSetTargetWeightKg = viewModel::setTargetWeightKg,
        onSetGoal = viewModel::setGoal,
        onSetActivityLevel = viewModel::setActivityLevel,
        onSetExerciseBudget = viewModel::setExerciseBudget,
    )
}

@Composable
private fun AboutYouContent(
    profile: Profile?,
    onSetSex: (Sex) -> Unit,
    onSetAge: (Int) -> Unit,
    onSetHeightCm: (Double) -> Unit,
    onSetCurrentWeightKg: (Double) -> Unit,
    onSetTargetWeightKg: (Double?) -> Unit,
    onSetGoal: (Goal) -> Unit,
    onSetActivityLevel: (ActivityLevel) -> Unit,
    onSetExerciseBudget: (Boolean) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        if (profile == null) return@Surface
        val unit = profile.preferredUnit
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.profile_about_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )

            AppCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(
                        text = stringResource(R.string.profile_about_sex),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    SegmentedToggle(
                        options = Sex.entries.map { stringResource(it.label()) },
                        selectedIndex = Sex.entries.indexOf(profile.sex),
                        onSelect = { onSetSex(Sex.entries[it]) },
                        trackColor = MaterialTheme.colorScheme.surfaceContainer,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                StepperRow(
                    label = stringResource(R.string.profile_about_age),
                    value = "${profile.age}",
                    unit = stringResource(R.string.profile_about_age_unit),
                    onIncrement = { onSetAge(profile.age + 1) },
                    onDecrement = { onSetAge(profile.age - 1) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                // Height and weight nudge in whatever unit is on screen and convert back on the
                // way in — stepping by a centimetre while showing inches would move the digit the
                // user is watching by a third of one.
                val height = profile.heightCm.cmToDisplayUnit(unit)
                StepperRow(
                    label = stringResource(R.string.profile_about_height),
                    value = formatBodyValue(height),
                    unit = unit.lengthUnitLabel(),
                    onIncrement = { onSetHeightCm((height + HEIGHT_STEP).displayUnitToCm(unit)) },
                    onDecrement = { onSetHeightCm((height - HEIGHT_STEP).displayUnitToCm(unit)) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val weight = profile.weightKg.kgToDisplayUnit(unit)
                StepperRow(
                    label = stringResource(R.string.profile_about_weight),
                    sublabel = stringResource(R.string.profile_about_weight_sub),
                    value = formatBodyValue(weight),
                    unit = unit.weightUnitLabel(),
                    onIncrement = { onSetCurrentWeightKg((weight + WEIGHT_STEP).displayUnitToKg(unit)) },
                    onDecrement = { onSetCurrentWeightKg((weight - WEIGHT_STEP).displayUnitToKg(unit)) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                // Unset is a real state — Progress's goal line and Home's projection hide together
                // when it is null — so the first nudge seeds from the current weight rather than
                // from zero. ponytail: no way back to unset once set; a clear affordance is the
                // upgrade if anyone asks for one.
                val target = (profile.targetWeightKg ?: profile.weightKg).kgToDisplayUnit(unit)
                StepperRow(
                    label = stringResource(R.string.profile_about_target),
                    sublabel = stringResource(R.string.profile_about_target_sub),
                    value = profile.targetWeightKg?.let { formatBodyValue(it.kgToDisplayUnit(unit)) } ?: UNSET,
                    unit = unit.weightUnitLabel(),
                    onIncrement = { onSetTargetWeightKg((target + WEIGHT_STEP).displayUnitToKg(unit)) },
                    onDecrement = { onSetTargetWeightKg((target - WEIGHT_STEP).displayUnitToKg(unit)) },
                )
            }

            SectionHeader(label = stringResource(R.string.profile_about_goal_title))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Goal.entries.forEach { goal ->
                    SelectableCard(
                        title = stringResource(goal.label()),
                        subtitle = stringResource(goal.sublabel()),
                        selected = profile.goal == goal,
                        onClick = { onSetGoal(goal) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            SectionHeader(label = stringResource(R.string.profile_about_activity_title))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActivityLevel.entries.forEach { level ->
                    SelectableCard(
                        title = stringResource(level.label()),
                        subtitle = stringResource(level.sublabel()),
                        selected = profile.activityLevel == level,
                        onClick = { onSetActivityLevel(level) },
                    )
                }
            }
            // The one remaining exercise preference, and it belongs beside activity level rather
            // than in a section of its own: your level already multiplies BMR, so crediting a
            // logged workout on top of it can count the same training twice.
            AppCard {
                AppListRow(
                    label = stringResource(R.string.profile_add_exercise),
                    sublabel = stringResource(R.string.profile_add_exercise_sub),
                    trailing = {
                        Switch(checked = profile.addExerciseToBudget, onCheckedChange = onSetExerciseBudget)
                    },
                )
            }

            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.profile_about_result_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = profile.dailyTargets().calories.toString(),
                            style = MaterialTheme.typography.titleLarge.tabularNums,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.profile_about_result_unit),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.profile_about_result_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** What a target weight reads as before one has been set. A dash, not a word — nothing to
 * translate, and the row below it says what the field is for. */
private const val UNSET = "—"

@PreviewLightDark
@PreviewScreenSizes
@Composable
private fun AboutYouScreenPreview() {
    AppTheme {
        AboutYouContent(
            profile = Profile(
                sex = Sex.Male,
                age = 26,
                heightCm = 170.0,
                weightKg = 82.7,
                activityLevel = ActivityLevel.Moderate,
                goal = Goal.Lose,
                targetWeightKg = 75.0,
            ),
            onSetSex = {},
            onSetAge = {},
            onSetHeightCm = {},
            onSetCurrentWeightKg = {},
            onSetTargetWeightKg = {},
            onSetGoal = {},
            onSetActivityLevel = {},
            onSetExerciseBudget = {},
        )
    }
}

/** Imperial with no target set: the units follow the preference and the target row shows a dash
 * rather than a zero. */
@PreviewLightDark
@Composable
private fun AboutYouScreenImperialPreview() {
    AppTheme {
        AboutYouContent(
            profile = Profile(
                sex = Sex.Female,
                age = 31,
                heightCm = 165.0,
                weightKg = 62.0,
                activityLevel = ActivityLevel.Light,
                goal = Goal.Maintain,
                preferredUnit = UnitSystem.Imperial,
            ),
            onSetSex = {},
            onSetAge = {},
            onSetHeightCm = {},
            onSetCurrentWeightKg = {},
            onSetTargetWeightKg = {},
            onSetGoal = {},
            onSetActivityLevel = {},
            onSetExerciseBudget = {},
        )
    }
}
