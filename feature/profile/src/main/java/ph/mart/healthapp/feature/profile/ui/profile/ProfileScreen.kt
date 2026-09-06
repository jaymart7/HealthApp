package ph.mart.healthapp.feature.profile.ui.profile

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.profile.components.ProfileCaloriesSection
import ph.mart.healthapp.feature.profile.ui.profile.components.ProfileCycleSection
import ph.mart.healthapp.feature.profile.ui.profile.components.ProfileDayTargetsSection
import ph.mart.healthapp.feature.profile.ui.profile.components.ProfileIdentityHeader
import ph.mart.healthapp.feature.profile.ui.profile.components.ProfileYourStuffSection
import ph.mart.healthapp.feature.profile.ui.shared.components.SectionHeader

/**
 * The Profile tab, which is now about the *person* rather than about the app: who you are, what
 * you are aiming at, and the handful of things you have saved. Everything configurable — units,
 * appearance, reminders, connections, export — moved one level up to Settings, behind the gear.
 *
 * Four blocks and three headings, where there used to be fourteen identical caption-over-card
 * sections. The tiers are the point: one `surfaceContainerHigh` header for the subject, plain cards
 * for what you adjust, and rows with an accent tile for what leaves the screen.
 *
 * A tab has no toolbar — `AppScaffold` draws one only above a tab — so the title and its gear are a
 * row at the top of this screen's own scroll, the way the other three tabs carry theirs.
 */
@Composable
fun ProfileScreen(
    onOpenSettings: () -> Unit,
    onOpenAboutYou: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenRoutines: () -> Unit,
    onOpenSupplements: () -> Unit,
    scrollState: ScrollState = rememberScrollState(),
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    ProfileContent(
        profile = uiState.profile,
        weightEntries = uiState.weightEntries,
        onOpenSettings = onOpenSettings,
        onOpenAboutYou = onOpenAboutYou,
        onSetCalorieTarget = viewModel::setCalorieTarget,
        onSetProteinTarget = viewModel::setProteinTarget,
        onSetCarbsTarget = viewModel::setCarbsTarget,
        onSetFatTarget = viewModel::setFatTarget,
        onResetTargets = viewModel::resetTargets,
        onSetWaterGoal = viewModel::setWaterGoal,
        onSetFastingGoal = viewModel::setFastingGoal,
        onSetStepGoal = viewModel::setStepGoal,
        onSetCycleTracking = viewModel::setCycleTracking,
        onOpenLibrary = onOpenLibrary,
        onOpenRoutines = onOpenRoutines,
        onOpenSupplements = onOpenSupplements,
        scrollState = scrollState,
    )
}

@Composable
private fun ProfileContent(
    profile: Profile?,
    weightEntries: List<WeightEntry>,
    onOpenSettings: () -> Unit,
    onOpenAboutYou: () -> Unit,
    onSetCalorieTarget: (Int) -> Unit,
    onSetProteinTarget: (Int) -> Unit,
    onSetCarbsTarget: (Int) -> Unit,
    onSetFatTarget: (Int) -> Unit,
    onResetTargets: () -> Unit,
    onSetWaterGoal: (Int) -> Unit,
    onSetFastingGoal: (Int) -> Unit,
    onSetStepGoal: (Int) -> Unit,
    onSetCycleTracking: (Boolean) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenRoutines: () -> Unit,
    onOpenSupplements: () -> Unit,
    scrollState: ScrollState = rememberScrollState(),
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        // A profile always exists by the time this tab is reachable (AppRoot gates on it) — this
        // only covers the first frame before Room's first emission lands.
        if (profile == null) return@Surface
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
                .padding(bottom = DockedFabContentPadding),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.profile_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = AppIcons.Settings,
                        contentDescription = stringResource(R.string.profile_open_settings),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            ProfileIdentityHeader(
                profile = profile,
                weightEntries = weightEntries,
                onEdit = onOpenAboutYou,
            )

            SectionHeader(label = stringResource(R.string.profile_targets_title))
            ProfileCaloriesSection(
                profile = profile,
                onSetCalorieTarget = onSetCalorieTarget,
                onSetProteinTarget = onSetProteinTarget,
                onSetCarbsTarget = onSetCarbsTarget,
                onSetFatTarget = onSetFatTarget,
                onResetTargets = onResetTargets,
            )
            ProfileDayTargetsSection(
                waterGoalGlasses = profile.waterGoalGlasses,
                onSetWaterGoal = onSetWaterGoal,
                fastingGoalHours = profile.fastingGoalHours,
                onSetFastingGoal = onSetFastingGoal,
                stepGoal = profile.stepGoal,
                onSetStepGoal = onSetStepGoal,
                unit = profile.preferredUnit,
            )

            SectionHeader(label = stringResource(R.string.profile_body_title))
            ProfileCycleSection(
                enabled = profile.cycleTrackingOn == true,
                onSetEnabled = onSetCycleTracking,
            )

            SectionHeader(label = stringResource(R.string.profile_stuff_title))
            ProfileYourStuffSection(
                onOpenSupplements = onOpenSupplements,
                onOpenLibrary = onOpenLibrary,
                onOpenRoutines = onOpenRoutines,
            )
        }
    }
}

@PreviewLightDark
@PreviewScreenSizes
@Composable
private fun ProfileScreenPreview() {
    AppTheme {
        ProfileContent(
            profile = Profile(
                sex = Sex.Female,
                age = 31,
                heightCm = 165.0,
                weightKg = 62.0,
                activityLevel = ActivityLevel.Moderate,
                goal = Goal.Lose,
                targetWeightKg = 58.0,
                cycleTrackingOn = true,
            ),
            weightEntries = listOf(
                WeightEntry(dateEpochDay = 20_000, weightKg = 62.6),
                WeightEntry(dateEpochDay = 20_007, weightKg = 62.0),
            ),
            onOpenSettings = {},
            onOpenAboutYou = {},
            onSetCalorieTarget = {},
            onSetProteinTarget = {},
            onSetCarbsTarget = {},
            onSetFatTarget = {},
            onResetTargets = {},
            onSetWaterGoal = {},
            onSetFastingGoal = {},
            onSetStepGoal = {},
            onSetCycleTracking = {},
            onOpenLibrary = {},
            onOpenRoutines = {},
            onOpenSupplements = {},
        )
    }
}
