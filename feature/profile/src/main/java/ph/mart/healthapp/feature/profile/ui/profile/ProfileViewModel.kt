package ph.mart.healthapp.feature.profile.ui.profile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.map
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.cycle.CycleRepository
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.fasting.FAST_GOAL_HOURS
import ph.mart.healthapp.core.data.health.STEP_GOAL_STEPS
import ph.mart.healthapp.core.data.fasting.FastingRepository
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.mood.MoodRepository
import ph.mart.healthapp.core.data.profile.CALORIE_TARGET_KCAL
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureRepository
import ph.mart.healthapp.core.data.supplement.SupplementRepository
import ph.mart.healthapp.core.data.transfer.DataTransferRepository
import ph.mart.healthapp.core.data.transfer.LocalBackups
import ph.mart.healthapp.core.data.transfer.exportJson
import ph.mart.healthapp.core.data.transfer.parseExport
import ph.mart.healthapp.core.data.water.WATER_GOAL_GLASSES
import ph.mart.healthapp.core.data.water.WaterRepository
import ph.mart.healthapp.core.designsystem.component.MascotCharacter
import ph.mart.healthapp.core.designsystem.component.MascotPalette

class ProfileViewModel(
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
) : ViewModel(), OrbitContainerHost<ProfileUiState, ProfileUiState, ProfileSideEffect> {

    override val container = orbitContainer<ProfileUiState, ProfileSideEffect>(ProfileUiState()) {
        observeProfile()
    }

    private fun observeProfile() = intent {
        profileRepository.observeProfile()
            .map { ProfileUiState(profile = it, backups = localBackups.list()) }
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

    /** The four target setters write an override on top of the Mifflin–St Jeor computation; see
     * [ph.mart.healthapp.core.data.profile.dailyTargets]. Clamped at the edges the way
     * [setWaterGoal] is, rather than validated after the fact — the calorie floor is a *warning*,
     * not a bound, so it is deliberately not clamped here. */
    fun setCalorieTarget(kcal: Int) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(calorieOverrideKcal = kcal.coerceIn(CALORIE_TARGET_KCAL)))
    }

    fun setProteinTarget(grams: Int) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(proteinOverrideG = grams.coerceAtLeast(0)))
    }

    fun setCarbsTarget(grams: Int) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(carbsOverrideG = grams.coerceAtLeast(0)))
    }

    fun setFatTarget(grams: Int) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(fatOverrideG = grams.coerceAtLeast(0)))
    }

    /** Back to null, which is what makes the targets track the profile again — an override is a
     * pin, and nothing else clears it. */
    fun resetTargets() = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(
            profile.copy(
                calorieOverrideKcal = null,
                proteinOverrideG = null,
                carbsOverrideG = null,
                fatOverrideG = null,
            ),
        )
    }

    fun setWaterGoal(glasses: Int) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(waterGoalGlasses = glasses.coerceIn(WATER_GOAL_GLASSES)))
    }

    fun setFastingGoal(hours: Int) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(fastingGoalHours = hours.coerceIn(FAST_GOAL_HOURS)))
    }

    fun setStepGoal(steps: Int) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(stepGoal = steps.coerceIn(STEP_GOAL_STEPS)))
    }

    fun setExerciseBudget(enabled: Boolean) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(addExerciseToBudget = enabled))
    }

    fun setCycleTracking(enabled: Boolean) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(cycleTrackingOn = enabled))
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
        postSideEffect(ProfileSideEffect.ExportReady(collectExport()))
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
        postSideEffect(ProfileSideEffect.ImportFinished(applyImport(text)))
    }

    /** The one backup on disk this row names, through the same parse-and-replace path a picked
     * file takes — the file is app-private, so there is no picker to point at it. */
    fun restoreBackup(name: String) = intent {
        val error = runCatching { localBackups.read(name) }
            .fold(onSuccess = { applyImport(it) }, onFailure = { it.message ?: FALLBACK_IMPORT_ERROR })
        postSideEffect(ProfileSideEffect.ImportFinished(error))
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
