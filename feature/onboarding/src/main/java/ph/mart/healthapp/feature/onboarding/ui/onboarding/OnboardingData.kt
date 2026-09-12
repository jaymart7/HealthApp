package ph.mart.healthapp.feature.onboarding.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.DietaryPreference
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.feature.onboarding.R

/** [bubble] is what Rui says once this option is the answer. The line belongs to the option rather
 * than to the screen because a guide that says the same thing whatever you picked is not reacting
 * to you — and because that keeps the copy beside the choice it describes. */
data class GoalOption(
    val goal: Goal,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    @StringRes val bubble: Int,
    val icon: ImageVector,
)

data class ActivityOption(
    val level: ActivityLevel,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    val icon: ImageVector,
)

data class DietOption(
    val preference: DietaryPreference,
    @StringRes val title: Int,
    @StringRes val bubble: Int,
    val icon: ImageVector,
)

val GOAL_OPTIONS = listOf(
    GoalOption(
        Goal.Lose,
        R.string.onboarding_goal_lose,
        R.string.onboarding_goal_lose_sub,
        R.string.onboarding_goal_lose_bubble,
        AppIcons.TrendingDown,
    ),
    GoalOption(
        Goal.Maintain,
        R.string.onboarding_goal_maintain,
        R.string.onboarding_goal_maintain_sub,
        R.string.onboarding_goal_maintain_bubble,
        AppIcons.Balance,
    ),
    GoalOption(
        Goal.Build,
        R.string.onboarding_goal_build,
        R.string.onboarding_goal_build_sub,
        R.string.onboarding_goal_build_bubble,
        AppIcons.Dumbbell,
    ),
)

val ACTIVITY_OPTIONS = listOf(
    ActivityOption(ActivityLevel.Sedentary, R.string.onboarding_activity_sedentary, R.string.onboarding_activity_sedentary_sub, AppIcons.Chair),
    ActivityOption(ActivityLevel.Light, R.string.onboarding_activity_light, R.string.onboarding_activity_light_sub, AppIcons.Steps),
    ActivityOption(ActivityLevel.Moderate, R.string.onboarding_activity_moderate, R.string.onboarding_activity_moderate_sub, AppIcons.Run),
    ActivityOption(ActivityLevel.Very, R.string.onboarding_activity_very, R.string.onboarding_activity_very_sub, AppIcons.Workout),
)

val DIET_OPTIONS = listOf(
    DietOption(DietaryPreference.None, R.string.onboarding_diet_none, R.string.onboarding_diet_none_bubble, AppIcons.Food.filled),
    DietOption(DietaryPreference.Vegetarian, R.string.onboarding_diet_vegetarian, R.string.onboarding_diet_vegetarian_bubble, AppIcons.Egg),
    DietOption(DietaryPreference.Vegan, R.string.onboarding_diet_vegan, R.string.onboarding_diet_vegan_bubble, AppIcons.Plant),
    DietOption(DietaryPreference.Other, R.string.onboarding_diet_other, R.string.onboarding_diet_other_bubble, AppIcons.More),
)

/** What the user is actively editing across all 6 steps — held in [OnboardingState], not the
 * Orbit container, so it survives process death (see the orbit-mvi-screen-split skill's "Loading
 * a record to edit" note on why the Form belongs in *State.kt). */
data class OnboardingForm(
    val goal: Goal? = null,
    val age: Int? = null,
    val sex: Sex? = null,
    // Null until the ruler is touched. A height and a weight the user never chose are two numbers
    // the confirm step would compute a calorie target from, and they would look like answers.
    val heightCm: Double? = null,
    val weightKg: Double? = null,
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
        get() = age != null && age in 13..100 && heightCm != null && weightKg != null && sex != null

    /** `null` until Basics + Goal + Activity are all answered — Confirm can't compute targets
     * before then. */
    fun toProfileOrNull(): Profile? {
        val goal = goal ?: return null
        val age = age ?: return null
        val sex = sex ?: return null
        val heightCm = heightCm ?: return null
        val weightKg = weightKg ?: return null
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
