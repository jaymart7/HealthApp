package ph.mart.healthapp.feature.profile.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.ui.components.ProfileAboutSection
import ph.mart.healthapp.feature.profile.ui.components.ProfileDataSection
import ph.mart.healthapp.feature.profile.ui.components.ProfileExerciseSection
import ph.mart.healthapp.feature.profile.ui.components.ProfileGoalsSection
import ph.mart.healthapp.feature.profile.ui.components.ProfileRemindersSection
import ph.mart.healthapp.feature.profile.ui.components.ProfileUnitsSection
import ph.mart.healthapp.feature.profile.ui.components.ProfileWaterSection

private const val EXPORT_FILE_NAME = "fitpulse-export.json"

private const val NOTIFICATIONS_DENIED =
    "Allow notifications for FitPulse in your system settings to use reminders."

/** Onboarding leaves meal and weigh-in reminders on by default, so a switch can be on without the
 * permission ever having been asked for. Say so rather than staying silently broken. */
private const val NOTIFICATIONS_BLOCKED =
    "Reminders are on, but notifications are blocked. Turn a reminder off and on again to allow them."

@Composable
fun ProfileScreen(scrollState: ScrollState = rememberScrollState(), viewModel: ProfileViewModel = koinViewModel()) {
    val uiState by viewModel.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // The screen owns the picker Uri and the file IO; the ViewModel only ever sees a JSON string.
    var pendingExport by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var messageIsError by remember { mutableStateOf(false) }

    // Kept apart from [message] so the refusal renders under the Reminders card rather than Data.
    var reminderMessage by remember { mutableStateOf<String?>(null) }
    var pendingReminder by remember { mutableStateOf<ReminderKind?>(null) }

    // A reminder that can't post a notification is a lie, so the switch only goes on once the
    // permission is actually granted.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val kind = pendingReminder
        pendingReminder = null
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
                    message = "Export saved."
                    messageIsError = false
                }
                .onFailure {
                    message = "Couldn't write that file."
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
                    message = "Couldn't read that file."
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
                message = effect.error ?: "Import complete."
                messageIsError = effect.error != null
            }
        }
    }

    ProfileContent(
        profile = uiState.profile,
        message = message,
        messageIsError = messageIsError,
        onSelectUnit = viewModel::setUnit,
        onSetWaterGoal = viewModel::setWaterGoal,
        onSetExerciseBudget = viewModel::setExerciseBudget,
        onToggleReminder = { kind, enabled ->
            if (!enabled || context.canPostNotifications()) {
                viewModel.setReminder(kind, enabled)
                reminderMessage = null
            } else {
                pendingReminder = kind
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onExport = viewModel::buildExport,
        onImport = { importLauncher.launch(arrayOf("application/json")) },
        // Re-read on each recomposition rather than watched: the only in-app source of a change is
        // the launcher above, which sets state anyway.
        reminderMessage = reminderMessage ?: NOTIFICATIONS_BLOCKED.takeIf {
            uiState.profile?.let { profile -> ReminderKind.entries.any(profile::reminderEnabled) } == true &&
                !context.canPostNotifications()
        },
        scrollState = scrollState,
    )
}

@Composable
private fun ProfileContent(
    profile: Profile?,
    message: String?,
    messageIsError: Boolean,
    onSelectUnit: (UnitSystem) -> Unit,
    onSetWaterGoal: (Int) -> Unit,
    onSetExerciseBudget: (Boolean) -> Unit,
    onToggleReminder: (ReminderKind, Boolean) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
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
                text = "Profile",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ProfileGoalsSection(profile = profile)
            ProfileUnitsSection(unit = profile.preferredUnit, onSelect = onSelectUnit)
            ProfileWaterSection(
                goalGlasses = profile.waterGoalGlasses,
                unit = profile.preferredUnit,
                onSetGoal = onSetWaterGoal,
            )
            ProfileExerciseSection(
                addToBudget = profile.addExerciseToBudget,
                onSetAddToBudget = onSetExerciseBudget,
            )
            ProfileRemindersSection(
                enabled = profile::reminderEnabled,
                onToggle = onToggleReminder,
                message = reminderMessage,
                messageIsError = true,
            )
            ProfileDataSection(
                onExport = onExport,
                onImport = onImport,
                message = message,
                messageIsError = messageIsError,
            )
            ProfileAboutSection()
        }
    }
}

/** Below API 33 the permission doesn't exist and notifications are on by default. */
private fun Context.canPostNotifications(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun Context.writeText(uri: Uri, text: String): Result<Unit> = runCatching {
    contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
        ?: error("No output stream")
}

private fun Context.readText(uri: Uri): Result<String> = runCatching {
    contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        ?: error("No input stream")
}

@PreviewLightDark
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
            onSetWaterGoal = {},
            onSetExerciseBudget = {},
            onToggleReminder = { _, _ -> },
            onExport = {},
            onImport = {},
        )
    }
}
