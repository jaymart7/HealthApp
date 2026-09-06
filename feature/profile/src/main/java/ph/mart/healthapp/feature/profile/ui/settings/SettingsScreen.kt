package ph.mart.healthapp.feature.profile.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
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
import ph.mart.healthapp.core.data.transfer.LocalBackup
import ph.mart.healthapp.core.designsystem.component.DiscardConfirmDialog
import ph.mart.healthapp.core.designsystem.component.MascotCharacter
import ph.mart.healthapp.core.designsystem.component.MascotPalette
import ph.mart.healthapp.core.designsystem.component.mascotCharacterOf
import ph.mart.healthapp.core.designsystem.component.mascotPaletteOf
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.settings.components.SettingsAboutSection
import ph.mart.healthapp.feature.profile.ui.settings.components.SettingsAppearanceSection
import ph.mart.healthapp.feature.profile.ui.settings.components.SettingsConnectionsSection
import ph.mart.healthapp.feature.profile.ui.settings.components.SettingsDataSection
import ph.mart.healthapp.feature.profile.ui.settings.components.SettingsHomeLayoutSection
import ph.mart.healthapp.feature.profile.ui.settings.components.SettingsRemindersSection
import ph.mart.healthapp.feature.profile.ui.settings.components.SettingsUnitsSection
import ph.mart.healthapp.feature.profile.ui.shared.components.SectionHeader

// Stays in Kotlin, with the "application/json" MIME types below: a filename and a wire type,
// not copy.
private const val EXPORT_FILE_NAME = "fitpulse-export.json"

/**
 * Everything about the app rather than about the person: how it renders, when it interrupts, what
 * it connects to, and what leaves the device.
 *
 * The toolbar and its back arrow come from `:app`'s `AppScaffold`, which draws one over every route
 * a level above a tab — this screen owns only its content, the same way the five sub-screens that
 * predate it do.
 */
@Composable
fun SettingsScreen(
    onOpenHomeLayout: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenHealth: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
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
            is SettingsSideEffect.ExportReady -> {
                pendingExport = effect.json
                exportLauncher.launch(EXPORT_FILE_NAME)
            }
            is SettingsSideEffect.ImportFinished -> {
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

    SettingsContent(
        profile = uiState.profile,
        backups = uiState.backups,
        onRestore = { pendingRestore = it },
        message = message,
        messageIsError = messageIsError,
        onSelectUnit = viewModel::setUnit,
        onSetDarkTheme = viewModel::setDarkTheme,
        onSelectMascot = viewModel::setMascot,
        onSelectMascotPalette = viewModel::setMascotPalette,
        onExport = viewModel::buildExport,
        onImport = { importLauncher.launch(arrayOf("application/json")) },
        onOpenHomeLayout = onOpenHomeLayout,
        onOpenReminders = onOpenReminders,
        onOpenHealth = onOpenHealth,
    )
}

@Composable
private fun SettingsContent(
    profile: Profile?,
    backups: List<LocalBackup>,
    onRestore: (LocalBackup) -> Unit,
    message: String?,
    messageIsError: Boolean,
    onSelectUnit: (UnitSystem) -> Unit,
    onSetDarkTheme: (Boolean) -> Unit,
    onSelectMascot: (MascotCharacter) -> Unit,
    onSelectMascotPalette: (MascotPalette) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onOpenHomeLayout: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenHealth: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        // A profile always exists by the time this route is reachable (AppRoot gates on it) — this
        // only covers the first frame before Room's first emission lands.
        if (profile == null) return@Surface
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 32.dp),
        ) {
            SectionHeader(label = stringResource(R.string.profile_settings_display))
            SettingsUnitsSection(unit = profile.preferredUnit, onSelect = onSelectUnit)
            // null profile field means "follow the device", so resolve it here rather than in the
            // section — same expression MainActivity uses to pick the scheme.
            SettingsAppearanceSection(
                darkTheme = profile.darkThemeOn ?: isSystemInDarkTheme(),
                onSetDarkTheme = onSetDarkTheme,
                mascot = mascotCharacterOf(profile.mascotName),
                onSelectMascot = onSelectMascot,
                palette = mascotPaletteOf(profile.mascotPaletteName),
                onSelectMascotPalette = onSelectMascotPalette,
            )
            SettingsHomeLayoutSection(onOpenHomeLayout = onOpenHomeLayout)

            SectionHeader(label = stringResource(R.string.profile_settings_notifications))
            SettingsRemindersSection(onOpenReminders = onOpenReminders)

            SectionHeader(label = stringResource(R.string.profile_section_connections))
            SettingsConnectionsSection(onOpenHealth = onOpenHealth)

            SectionHeader(label = stringResource(R.string.profile_section_data))
            SettingsDataSection(
                onExport = onExport,
                onImport = onImport,
                backups = backups,
                onRestore = onRestore,
                message = message,
                messageIsError = messageIsError,
            )

            SectionHeader(label = stringResource(R.string.profile_section_about))
            SettingsAboutSection()
        }
    }
}

@PreviewLightDark
@PreviewScreenSizes
@Composable
private fun SettingsScreenPreview() {
    AppTheme {
        SettingsContent(
            profile = Profile(
                sex = Sex.Female,
                age = 31,
                heightCm = 165.0,
                weightKg = 62.0,
                activityLevel = ActivityLevel.Moderate,
                goal = Goal.Lose,
            ),
            backups = emptyList(),
            onRestore = {},
            message = null,
            messageIsError = false,
            onSelectUnit = {},
            onSetDarkTheme = {},
            onSelectMascot = {},
            onSelectMascotPalette = {},
            onExport = {},
            onImport = {},
            onOpenHomeLayout = {},
            onOpenReminders = {},
            onOpenHealth = {},
        )
    }
}
