package ph.mart.healthapp.feature.profile.ui.profile

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.component.MascotCharacter
import ph.mart.healthapp.core.designsystem.component.MascotPalette
import ph.mart.healthapp.core.designsystem.component.mascotCharacterOf
import ph.mart.healthapp.core.designsystem.component.mascotPaletteOf
import ph.mart.healthapp.core.data.transfer.LocalBackup
import ph.mart.healthapp.core.designsystem.component.DiscardConfirmDialog
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.profile.components.ProfileAboutSection
import ph.mart.healthapp.feature.profile.ui.profile.components.ProfileAppearanceSection
import ph.mart.healthapp.feature.profile.ui.profile.components.ProfileConnectionsSection
import ph.mart.healthapp.feature.profile.ui.profile.components.ProfileCycleSection
import ph.mart.healthapp.feature.profile.ui.profile.components.ProfileDataSection
import ph.mart.healthapp.feature.profile.ui.profile.components.ProfileExerciseSection
import ph.mart.healthapp.feature.profile.ui.profile.components.ProfileFastingSection
import ph.mart.healthapp.feature.profile.ui.profile.components.ProfileGoalsSection
import ph.mart.healthapp.feature.profile.ui.profile.components.ProfileHomeLayoutSection
import ph.mart.healthapp.feature.profile.ui.profile.components.ProfileLibrarySection
import ph.mart.healthapp.feature.profile.ui.profile.components.ProfileRemindersSection
import ph.mart.healthapp.feature.profile.ui.profile.components.ProfileSupplementsSection
import ph.mart.healthapp.feature.profile.ui.profile.components.ProfileUnitsSection
import ph.mart.healthapp.feature.profile.ui.profile.components.ProfileWaterSection

// Stays in Kotlin, with the "application/json" MIME types below: a filename and a wire type,
// not copy.
private const val EXPORT_FILE_NAME = "fitpulse-export.json"

@StringRes
private val NOTIFICATIONS_DENIED =
    R.string.profile_notifications_blocked

/** Onboarding leaves meal and weigh-in reminders on by default, so a switch can be on without the
 * permission ever having been asked for. Say so rather than staying silently broken. */
@StringRes
private val NOTIFICATIONS_BLOCKED =
    R.string.profile_notifications_blocked_on

@Composable
fun ProfileScreen(
    onOpenHealth: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenRoutines: () -> Unit,
    onOpenSupplements: () -> Unit,
    onOpenHomeLayout: () -> Unit,
    scrollState: ScrollState = rememberScrollState(),
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // The screen owns the picker Uri and the file IO; the ViewModel only ever sees a JSON string.
    var pendingExport by remember { mutableStateOf<String?>(null) }

    // A restore replaces everything and is one tap from a settings list, unlike the import, where
    // picking the file in SAF is itself the confirmation.
    var pendingRestore by remember { mutableStateOf<LocalBackup?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var messageIsError by remember { mutableStateOf(false) }

    // Kept apart from [message] so the refusal renders under the Reminders card rather than Data.
    // The id, not the words: this is set from a permission callback, outside composition.
    var reminderMessage by remember { mutableStateOf<Int?>(null) }
    var pendingReminder by remember { mutableStateOf<ReminderKind?>(null) }

    // Held in state and refreshed on resume, not read during composition. The permission can be
    // granted from system Settings, and a bare call in the composable body is not a snapshot read
    // — the "notifications are blocked" line below would go on claiming so until something
    // unrelated happened to recompose this screen.
    var canPostNotifications by remember { mutableStateOf(context.canPostNotifications()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) canPostNotifications = context.canPostNotifications()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // A reminder that can't post a notification is a lie, so the switch only goes on once the
    // permission is actually granted.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val kind = pendingReminder
        pendingReminder = null
        canPostNotifications = granted
        if (granted && kind != null) {
            viewModel.setReminder(kind, true)
            reminderMessage = null
        } else if (!granted) {
            reminderMessage = NOTIFICATIONS_DENIED
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val json = pendingExport
        pendingExport = null
        if (uri == null || json == null) return@rememberLauncherForActivityResult
        scope.launch {
            withContext(Dispatchers.IO) { context.writeText(uri, json) }
                .onSuccess {
                    message = context.getString(R.string.profile_export_saved)
                    messageIsError = false
                }
                .onFailure {
                    message = context.getString(R.string.profile_export_failed)
                    messageIsError = true
                }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            withContext(Dispatchers.IO) { context.readText(uri) }
                .onSuccess { viewModel.import(it) }
                .onFailure {
                    message = context.getString(R.string.profile_import_failed)
                    messageIsError = true
                }
        }
    }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is ProfileSideEffect.ExportReady -> {
                pendingExport = effect.json
                exportLauncher.launch(EXPORT_FILE_NAME)
            }
            is ProfileSideEffect.ImportFinished -> {
                message = effect.error ?: context.getString(R.string.profile_import_done)
                messageIsError = effect.error != null
            }
        }
    }

    pendingRestore?.let { backup ->
        DiscardConfirmDialog(
            title = stringResource(R.string.profile_backup_title),
            body = stringResource(R.string.profile_backup_body),
            confirmLabel = stringResource(R.string.profile_backup_confirm),
            dismissLabel = stringResource(R.string.profile_cancel),
            onConfirm = {
                pendingRestore = null
                viewModel.restoreBackup(backup.name)
            },
            onDismiss = { pendingRestore = null },
        )
    }

    ProfileContent(
        profile = uiState.profile,
        backups = uiState.backups,
        onRestore = { pendingRestore = it },
        message = message,
        messageIsError = messageIsError,
        onSelectUnit = viewModel::setUnit,
        onSetDarkTheme = viewModel::setDarkTheme,
        onSelectMascot = viewModel::setMascot,
        onSelectMascotPalette = viewModel::setMascotPalette,
        onSetCalorieTarget = viewModel::setCalorieTarget,
        onSetProteinTarget = viewModel::setProteinTarget,
        onSetCarbsTarget = viewModel::setCarbsTarget,
        onSetFatTarget = viewModel::setFatTarget,
        onResetTargets = viewModel::resetTargets,
        onSetWaterGoal = viewModel::setWaterGoal,
        onSetFastingGoal = viewModel::setFastingGoal,
        onSetStepGoal = viewModel::setStepGoal,
        onSetExerciseBudget = viewModel::setExerciseBudget,
        onSetCycleTracking = viewModel::setCycleTracking,
        onToggleReminder = { kind, enabled ->
            if (!enabled || canPostNotifications) {
                viewModel.setReminder(kind, enabled)
                reminderMessage = null
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Always true once we're here — `canPostNotifications` is only ever false above
                // TIRAMISU — but stated at the point of use so the API-33 permission constant is
                // visibly guarded, which reading it out of a state variable no longer showed.
                pendingReminder = kind
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onExport = viewModel::buildExport,
        onImport = { importLauncher.launch(arrayOf("application/json")) },
        onOpenHealth = onOpenHealth,
        onOpenLibrary = onOpenLibrary,
        onOpenRoutines = onOpenRoutines,
        onOpenSupplements = onOpenSupplements,
        onOpenHomeLayout = onOpenHomeLayout,
        reminderMessage = (
            reminderMessage ?: NOTIFICATIONS_BLOCKED.takeIf {
                uiState.profile?.let { profile -> ReminderKind.entries.any(profile::reminderEnabled) } == true &&
                    !canPostNotifications
            }
            )?.let { stringResource(it) },
        scrollState = scrollState,
    )
}

@Composable
private fun ProfileContent(
    profile: Profile?,
    backups: List<LocalBackup>,
    onRestore: (LocalBackup) -> Unit,
    message: String?,
    messageIsError: Boolean,
    onSelectUnit: (UnitSystem) -> Unit,
    onSetDarkTheme: (Boolean) -> Unit,
    onSelectMascot: (MascotCharacter) -> Unit,
    onSelectMascotPalette: (MascotPalette) -> Unit,
    onSetCalorieTarget: (Int) -> Unit,
    onSetProteinTarget: (Int) -> Unit,
    onSetCarbsTarget: (Int) -> Unit,
    onSetFatTarget: (Int) -> Unit,
    onResetTargets: () -> Unit,
    onSetWaterGoal: (Int) -> Unit,
    onSetFastingGoal: (Int) -> Unit,
    onSetStepGoal: (Int) -> Unit,
    onSetExerciseBudget: (Boolean) -> Unit,
    onSetCycleTracking: (Boolean) -> Unit,
    onToggleReminder: (ReminderKind, Boolean) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onOpenHealth: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenRoutines: () -> Unit,
    onOpenSupplements: () -> Unit,
    onOpenHomeLayout: () -> Unit,
    reminderMessage: String? = null,
    scrollState: ScrollState = rememberScrollState(),
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        // A profile always exists by the time this tab is reachable (AppRoot gates on it) — this
        // only covers the first frame before Room's first emission lands.
        if (profile == null) return@Surface
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = DockedFabContentPadding),
        ) {
            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ProfileGoalsSection(
                profile = profile,
                onSetCalorieTarget = onSetCalorieTarget,
                onSetProteinTarget = onSetProteinTarget,
                onSetCarbsTarget = onSetCarbsTarget,
                onSetFatTarget = onSetFatTarget,
                onResetTargets = onResetTargets,
            )
            ProfileUnitsSection(unit = profile.preferredUnit, onSelect = onSelectUnit)
            // null profile field means "follow the device", so resolve it here rather than in the
            // section — same expression MainActivity uses to pick the scheme.
            ProfileAppearanceSection(
                darkTheme = profile.darkThemeOn ?: isSystemInDarkTheme(),
                onSetDarkTheme = onSetDarkTheme,
                mascot = mascotCharacterOf(profile.mascotName),
                onSelectMascot = onSelectMascot,
                palette = mascotPaletteOf(profile.mascotPaletteName),
                onSelectMascotPalette = onSelectMascotPalette,
            )
            ProfileHomeLayoutSection(onOpenHomeLayout = onOpenHomeLayout)
            ProfileWaterSection(
                goalGlasses = profile.waterGoalGlasses,
                unit = profile.preferredUnit,
                onSetGoal = onSetWaterGoal,
            )
            ProfileFastingSection(
                goalHours = profile.fastingGoalHours,
                onSetGoal = onSetFastingGoal,
            )
            ProfileExerciseSection(
                stepGoal = profile.stepGoal,
                onSetStepGoal = onSetStepGoal,
                addToBudget = profile.addExerciseToBudget,
                onSetAddToBudget = onSetExerciseBudget,
            )
            ProfileCycleSection(
                enabled = profile.cycleTrackingOn == true,
                onSetEnabled = onSetCycleTracking,
            )
            ProfileRemindersSection(
                enabled = profile::reminderEnabled,
                onToggle = onToggleReminder,
                message = reminderMessage,
                messageIsError = true,
            )
            ProfileSupplementsSection(onOpenSupplements = onOpenSupplements)
            ProfileLibrarySection(onOpenLibrary = onOpenLibrary, onOpenRoutines = onOpenRoutines)
            ProfileConnectionsSection(onOpenHealth = onOpenHealth)
            ProfileDataSection(
                onExport = onExport,
                onImport = onImport,
                backups = backups,
                onRestore = onRestore,
                message = message,
                messageIsError = messageIsError,
            )
            ProfileAboutSection()
        }
    }
}

/** Below API 33 the permission doesn't exist and notifications are on by default. */
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
            ),
            message = null,
            messageIsError = false,
            onSelectUnit = {},
            onSetDarkTheme = {},
            onSelectMascot = {},
            onSelectMascotPalette = {},
            onSetCalorieTarget = {},
            onSetProteinTarget = {},
            onSetCarbsTarget = {},
            onSetFatTarget = {},
            onResetTargets = {},
            onSetWaterGoal = {},
            onSetFastingGoal = {},
            onSetStepGoal = {},
            onSetExerciseBudget = {},
            onSetCycleTracking = {},
            onToggleReminder = { _, _ -> },
            backups = emptyList(),
            onRestore = {},
            onExport = {},
            onImport = {},
            onOpenHealth = {},
            onOpenLibrary = {},
            onOpenRoutines = {},
            onOpenSupplements = {},
            onOpenHomeLayout = {},
        )
    }
}
