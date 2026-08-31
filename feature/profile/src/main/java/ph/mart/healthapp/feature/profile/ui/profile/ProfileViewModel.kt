package ph.mart.healthapp.feature.profile.ui.profile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.fasting.FAST_GOAL_HOURS
import ph.mart.healthapp.core.data.fasting.FastingRepository
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.mood.MoodRepository
import ph.mart.healthapp.core.data.profile.CALORIE_TARGET_KCAL
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureRepository
import ph.mart.healthapp.core.data.supplement.SupplementRepository
import ph.mart.healthapp.core.data.water.WATER_GOAL_GLASSES
import ph.mart.healthapp.core.data.water.WaterRepository

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val foodRepository: FoodRepository,
    private val progressRepository: ProgressRepository,
    private val waterRepository: WaterRepository,
    private val exerciseRepository: ExerciseRepository,
    private val moodRepository: MoodRepository,
    private val fastingRepository: FastingRepository,
    private val supplementRepository: SupplementRepository,
    private val bloodPressureRepository: BloodPressureRepository,
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

    fun setExerciseBudget(enabled: Boolean) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(addExerciseToBudget = enabled))
    }

    fun setDarkTheme(enabled: Boolean) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(darkThemeOn = enabled))
    }

    fun buildExport() = intent {
        val json = buildExportJson(
            profile = state.profile,
            foodEntries = foodRepository.allEntries(),
            weightEntries = progressRepository.observeWeightEntries().first(),
            measurements = progressRepository.observeMeasurements().first().values.flatten(),
            waterDays = waterRepository.allDays(),
            exercises = exerciseRepository.allEntries(),
            moodDays = moodRepository.allDays(),
            fastSessions = fastingRepository.allSessions(),
            supplements = supplementRepository.allSupplements(),
            supplementDays = supplementRepository.allDays(),
            bloodPressure = bloodPressureRepository.allReadings(),
        )
        postSideEffect(ProfileSideEffect.ExportReady(json))
    }

    /** Replaces the profile, the food diary, the water log, the exercise log, the mood log, the
     * fasting log, the supplement list with its ticks and the blood pressure readings; weight and
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
                moodRepository.clearAllDays()
                payload.moodDays.forEach { moodRepository.upsertDay(it) }
                // Clears a running fast too, which is the honest reading of replace-in-full: the
                // timer belongs to the history being replaced, not to the device.
                fastingRepository.clearAllSessions()
                payload.fastSessions.forEach { fastingRepository.upsertSession(it) }
                // Supplements before their days: a day row points at a supplement id, and the ids
                // are restored verbatim rather than regenerated so the ticks keep their subject.
                supplementRepository.clearAll()
                payload.supplements.forEach { supplementRepository.upsertSupplement(it) }
                payload.supplementDays.forEach { supplementRepository.upsertDay(it) }
                bloodPressureRepository.clearAllReadings()
                payload.bloodPressure.forEach { bloodPressureRepository.addReading(it) }
                postSideEffect(ProfileSideEffect.ImportFinished(error = null))
            },
            onFailure = {
                postSideEffect(ProfileSideEffect.ImportFinished(it.message ?: "That file couldn't be read."))
            },
        )
    }
}
