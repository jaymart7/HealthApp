package ph.mart.healthapp.feature.profile.ui.profile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.fasting.FAST_GOAL_HOURS
import ph.mart.healthapp.core.data.health.STEP_GOAL_STEPS
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.CALORIE_TARGET_KCAL
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.profile.round1
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.water.WATER_GOAL_GLASSES

/** What About you will let a body figure be nudged to. Wide enough to be nobody's problem and
 * narrow enough that a stuck finger can't write a profile that makes Mifflin–St Jeor meaningless —
 * the same "clamp at the edges rather than validate after" the goal setters below use. */
private val AGE_YEARS = 13..120
private val HEIGHT_CM = 90.0..250.0
private val WEIGHT_KG = 20.0..400.0

/** Whole years, a centimetre, and a tenth of a kilo: the granularity each figure is actually known
 * to. Height and weight nudge in *display* units, so the step is applied after conversion. */
const val HEIGHT_STEP = 1.0
const val WEIGHT_STEP = 0.1

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel(), OrbitContainerHost<ProfileUiState, ProfileUiState, Nothing> {

    override val container = orbitContainer<ProfileUiState, Nothing>(ProfileUiState()) {
        observeProfile()
    }

    /** Combined rather than two collectors: the header prints a weight and a trend beside the goal
     * they are measured against, and two independent emissions would let it draw one against the
     * other's profile for a frame. */
    private fun observeProfile() = intent {
        combine(
            profileRepository.observeProfile(),
            progressRepository.observeWeightEntries(),
        ) { profile, entries -> ProfileUiState(profile = profile, weightEntries = entries) }
            .collect { newState -> reduce { newState } }
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

    // --- About you -------------------------------------------------------------------------
    // The six Mifflin–St Jeor inputs, editable for the first time since onboarding. Every one of
    // them re-prices the calorie target the moment it lands, because nothing caches that figure —
    // which is exactly why the screen has no save button to press.

    fun setSex(sex: Sex) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(sex = sex))
    }

    fun setAge(years: Int) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(age = years.coerceIn(AGE_YEARS)))
    }

    fun setHeightCm(cm: Double) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(heightCm = round1(cm.coerceIn(HEIGHT_CM))))
    }

    /**
     * The *profile's* weight — the onboarding figure Mifflin–St Jeor runs on, not a weigh-in. It
     * deliberately writes no `WeightEntry`: the header reads the log for what you weigh today and
     * falls back to this only when the log is empty, so logging a weigh-in through the FAB is
     * still the one way to put a point on the Progress chart. Editing it here corrects the number
     * the target is computed from without inventing a measurement that never happened.
     */
    fun setCurrentWeightKg(kg: Double) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(weightKg = round1(kg.coerceIn(WEIGHT_KG))))
    }

    /** Null clears it, which is a real state: Progress's goal line, its goal chip and Home's
     * projection all hide together when there is no target rather than drawing against a guess. */
    fun setTargetWeightKg(kg: Double?) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(targetWeightKg = kg?.let { round1(it.coerceIn(WEIGHT_KG)) }))
    }

    fun setGoal(goal: Goal) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(goal = goal))
    }

    fun setActivityLevel(level: ActivityLevel) = intent {
        val profile = state.profile ?: return@intent
        profileRepository.saveProfile(profile.copy(activityLevel = level))
    }
}
