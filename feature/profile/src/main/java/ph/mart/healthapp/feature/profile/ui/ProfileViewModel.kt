package ph.mart.healthapp.feature.profile.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.water.WATER_GOAL_GLASSES
import ph.mart.healthapp.core.data.water.WaterRepository

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val foodRepository: FoodRepository,
    private val progressRepository: ProgressRepository,
    private val waterRepository: WaterRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel(), OrbitContainerHost<ProfileUiState, ProfileUiState, ProfileSideEffect> {

    override val container = orbitContainer<ProfileUiState, ProfileSideEffect>(ProfileUiState()) {
        observeProfile()
    }

    private fun observeProfile() = intent {
        profileRepository.observeProfile()
            .map { ProfileUiState(profile = it) }
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

    fun setWaterGoal(glasses: Int) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(waterGoalGlasses = glasses.coerceIn(WATER_GOAL_GLASSES)))
    }

    fun setExerciseBudget(enabled: Boolean) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(addExerciseToBudget = enabled))
    }

    fun buildExport() = intent {
        val json = buildExportJson(
            profile = state.profile,
            foodEntries = foodRepository.allEntries(),
            weightEntries = progressRepository.observeWeightEntries().first(),
            measurements = progressRepository.observeMeasurements().first().values.flatten(),
            waterDays = waterRepository.allDays(),
            exercises = exerciseRepository.allEntries(),
        )
        postSideEffect(ProfileSideEffect.ExportReady(json))
    }

    /** Replaces the profile, the food diary, the water log and the exercise log; weight and
     * measurements are
     * upserted by date, so importing merges history rather than discarding entries the
     * file doesn't mention. Nothing is written at all if the file fails to parse. Photos are never
     * touched. */
    fun import(text: String) = intent {
        parseExport(text).fold(
            onSuccess = { payload ->
                payload.profile?.let { profileRepository.saveProfile(it) }
                foodRepository.deleteAllEntries()
                payload.foodEntries.forEach { foodRepository.addEntry(it) }
                payload.weightEntries.forEach { progressRepository.upsertWeightEntry(it) }
                payload.measurements.forEach { progressRepository.upsertMeasurementEntry(it) }
                waterRepository.clearAllDays()
                payload.waterDays.forEach { waterRepository.upsertDay(it) }
                exerciseRepository.deleteAllEntries()
                payload.exercises.forEach { exerciseRepository.addEntry(it) }
                postSideEffect(ProfileSideEffect.ImportFinished(error = null))
            },
            onFailure = {
                postSideEffect(ProfileSideEffect.ImportFinished(it.message ?: "That file couldn't be read."))
            },
        )
    }
}
