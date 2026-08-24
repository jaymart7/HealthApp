package ph.mart.healthapp.feature.onboarding.ui

import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.DietaryPreference
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.profile.UnitSystem

data class GoalOption(val goal: Goal, val title: String, val subtitle: String)
data class ActivityOption(val level: ActivityLevel, val title: String, val subtitle: String)
data class DietOption(val preference: DietaryPreference, val title: String)

val GOAL_OPTIONS = listOf(
    GoalOption(Goal.Lose, "Lose weight", "Gradual, sustainable calorie deficit"),
    GoalOption(Goal.Maintain, "Maintain", "Keep your current weight steady"),
    GoalOption(Goal.Build, "Build muscle", "Calorie surplus with more protein"),
)

val ACTIVITY_OPTIONS = listOf(
    ActivityOption(ActivityLevel.Sedentary, "Sedentary", "Desk job, little exercise"),
    ActivityOption(ActivityLevel.Light, "Light", "Light exercise 1–3 days/week"),
    ActivityOption(ActivityLevel.Moderate, "Moderate", "Moderate exercise 3–5 days/week"),
    ActivityOption(ActivityLevel.Very, "Very active", "Hard exercise 6–7 days/week"),
)

val DIET_OPTIONS = listOf(
    DietOption(DietaryPreference.None, "None"),
    DietOption(DietaryPreference.Vegetarian, "Vegetarian"),
    DietOption(DietaryPreference.Vegan, "Vegan"),
    DietOption(DietaryPreference.Other, "Other"),
)

/** What the user is actively editing across all 6 steps — held in [OnboardingState], not the
 * Orbit container, so it survives process death (see the orbit-mvi-screen-split skill's "Loading
 * a record to edit" note on why the Form belongs in *State.kt). */
data class OnboardingForm(
    val goal: Goal? = null,
    val age: Int? = null,
    val sex: Sex? = null,
    val heightCm: Double = 170.0,
    val weightKg: Double = 65.0,
    val targetWeightKg: Double? = null,
    val units: UnitSystem = UnitSystem.Metric,
    val activityLevel: ActivityLevel? = null,
    val dietaryPreference: DietaryPreference? = null,
    val calorieOverrideKcal: Int? = null,
    val proteinOverrideG: Int? = null,
    val carbsOverrideG: Int? = null,
    val fatOverrideG: Int? = null,
) {
    val isBasicsValid: Boolean
        get() = age != null && age in 13..100 && heightCm > 0 && weightKg > 0 && sex != null

    /** `null` until Basics + Goal + Activity are all answered — Confirm can't compute targets
     * before then. */
    fun toProfileOrNull(): Profile? {
        val goal = goal ?: return null
        val age = age ?: return null
        val sex = sex ?: return null
        val activityLevel = activityLevel ?: return null
        return Profile(
            sex = sex,
            age = age,
            heightCm = heightCm,
            weightKg = weightKg,
            activityLevel = activityLevel,
            goal = goal,
            targetWeightKg = targetWeightKg,
            dietaryPreference = dietaryPreference,
            preferredUnit = units,
            calorieOverrideKcal = calorieOverrideKcal,
            proteinOverrideG = proteinOverrideG,
            carbsOverrideG = carbsOverrideG,
            fatOverrideG = fatOverrideG,
        )
    }
}

/** Age/height/weight/sex/goal/activity changes all invalidate any manual Confirm-step override —
 * target weight, units, and diet do not (matches the prototype's `clearOverrides` triggers). */
fun OnboardingForm.clearOverrides(): OnboardingForm = copy(
    calorieOverrideKcal = null,
    proteinOverrideG = null,
    carbsOverrideG = null,
    fatOverrideG = null,
)

data class OnboardingUiState(
    val goalOptions: List<GoalOption> = GOAL_OPTIONS,
    val activityOptions: List<ActivityOption> = ACTIVITY_OPTIONS,
    val dietOptions: List<DietOption> = DIET_OPTIONS,
    val isCelebrating: Boolean = false,
)

sealed interface OnboardingEvent {
    data class OnFinish(val form: OnboardingForm) : OnboardingEvent
}

sealed interface OnboardingSideEffect {
    data object Finished : OnboardingSideEffect
}
