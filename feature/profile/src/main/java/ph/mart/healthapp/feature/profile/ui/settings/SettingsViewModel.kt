package ph.mart.healthapp.feature.profile.ui.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.map
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureRepository
import ph.mart.healthapp.core.data.cycle.CycleRepository
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.fasting.FastingRepository
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.mood.MoodRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.supplement.SupplementRepository
import ph.mart.healthapp.core.data.transfer.DataTransferRepository
import ph.mart.healthapp.core.data.transfer.LocalBackups
import ph.mart.healthapp.core.data.transfer.exportJson
import ph.mart.healthapp.core.data.transfer.parseExport
import ph.mart.healthapp.core.data.water.WaterRepository
import ph.mart.healthapp.core.designsystem.component.MascotCharacter
import ph.mart.healthapp.core.designsystem.component.MascotPalette

/**
 * Settings and Reminders, which are one screen and its sub-screen, share this — the reminder flags
 * are the same eight fields either way, and a second host for them would be a second copy of the
 * same profile row. Reminders takes it by [org.koin.androidx.compose.koinViewModel] like any other
 * screen; nothing about it is scoped.
 *
 * It exists at all — rather than Settings borrowing `ProfileViewModel` — because this is the half
 * of the old Profile screen that has nothing to do with the person: rendering preferences, the
 * notification schedule, and the export. The ten domain repositories below are here and not there
 * for one reason: [exportJson] reads every one of them, and the export is the only thing that does.
 */
class SettingsViewModel(
    private val profileRepository: ProfileRepository,
    private val foodRepository: FoodRepository,
    private val progressRepository: ProgressRepository,
    private val waterRepository: WaterRepository,
    private val exerciseRepository: ExerciseRepository,
    private val moodRepository: MoodRepository,
    private val cycleRepository: CycleRepository,
    private val fastingRepository: FastingRepository,
    private val supplementRepository: SupplementRepository,
    private val bloodPressureRepository: BloodPressureRepository,
    private val dataTransferRepository: DataTransferRepository,
    private val localBackups: LocalBackups,
) : ViewModel(), OrbitContainerHost<SettingsUiState, SettingsUiState, SettingsSideEffect> {

    override val container = orbitContainer<SettingsUiState, SettingsSideEffect>(SettingsUiState()) {
        observeProfile()
    }

    private fun observeProfile() = intent {
        profileRepository.observeProfile()
            .map { SettingsUiState(profile = it, backups = localBackups.list()) }
            .collect { newState -> reduce { newState } }
    }

    /** Units and reminders both write the whole profile back through the same interface — there is
     * no per-field setter, and no settings store separate from the profile row. */
    fun setUnit(unit: UnitSystem) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(preferredUnit = unit))
    }

    fun setReminder(kind: ReminderKind, enabled: Boolean) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.withReminder(kind, enabled))
    }

    fun setDarkTheme(enabled: Boolean) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(darkThemeOn = enabled))
    }

    fun setMascot(character: MascotCharacter) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(mascotName = character.name))
    }

    fun setMascotPalette(palette: MascotPalette) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(mascotPaletteName = palette.name))
    }

    /** The reads live in `:core:data`'s [exportJson] rather than here, because `BackupWorker`
     * makes exactly the same ones — two copies of that list is two places to edit at the next
     * schema version. */
    fun buildExport() = intent {
        postSideEffect(SettingsSideEffect.ExportReady(collectExport()))
    }

    private suspend fun collectExport(): String = exportJson(
        profileRepository, foodRepository, progressRepository, waterRepository, exerciseRepository,
        moodRepository, cycleRepository, fastingRepository, supplementRepository,
        bloodPressureRepository,
    )

    /** Parse here, write there. The whole replay is one transaction inside `:core:data` — see
     * [DataTransferRepository]; running it from this file a row at a time meant a crash mid-import
     * left the diary wiped and half-restored. Nothing is written at all if the file fails to
     * parse. Photos are never touched. */
    fun import(text: String) = intent {
        postSideEffect(SettingsSideEffect.ImportFinished(applyImport(text)))
    }

    /** The one backup on disk this row names, through the same parse-and-replace path a picked
     * file takes — the file is app-private, so there is no picker to point at it. */
    fun restoreBackup(name: String) = intent {
        val error = runCatching { localBackups.read(name) }
            .fold(onSuccess = { applyImport(it) }, onFailure = { it.message ?: FALLBACK_IMPORT_ERROR })
        postSideEffect(SettingsSideEffect.ImportFinished(error))
    }

    /** null when it worked; the message to show when it didn't. */
    private suspend fun applyImport(text: String): String? = parseExport(text).fold(
        onSuccess = { dataTransferRepository.replaceAll(it); null },
        onFailure = { it.message ?: FALLBACK_IMPORT_ERROR },
    )
}

// The fallback stays in Kotlin beside the `require()` message it stands in for — see
// `parseExport`: what surfaces here is an exception's own text either way.
private const val FALLBACK_IMPORT_ERROR = "That file couldn't be read."
