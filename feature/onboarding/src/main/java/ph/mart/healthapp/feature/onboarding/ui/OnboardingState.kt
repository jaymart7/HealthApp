package ph.mart.healthapp.feature.onboarding.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.DietaryPreference
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.profile.UnitSystem

@Composable
internal fun rememberOnboardingState(): OnboardingState =
    rememberSaveable(saver = OnboardingState.Saver()) { OnboardingState() }

internal class OnboardingState(form: OnboardingForm = OnboardingForm(), step: Int = 0) {
    var form: OnboardingForm by mutableStateOf(form)
    var step: Int by mutableIntStateOf(step)

    companion object {
        fun Saver(): Saver<OnboardingState, Any> = listSaver(
            save = {
                val f = it.form
                listOf(
                    f.goal?.name, f.age, f.sex?.name, f.heightCm, f.weightKg, f.targetWeightKg,
                    f.units.name, f.activityLevel?.name, f.dietaryPreference?.name,
                    f.calorieOverrideKcal, f.proteinOverrideG, f.carbsOverrideG, f.fatOverrideG,
                    it.step,
                )
            },
            restore = { saved ->
                OnboardingState(
                    form = OnboardingForm(
                        goal = (saved[0] as String?)?.let(Goal::valueOf),
                        age = saved[1] as Int?,
                        sex = (saved[2] as String?)?.let(Sex::valueOf),
                        heightCm = saved[3] as Double,
                        weightKg = saved[4] as Double,
                        targetWeightKg = saved[5] as Double?,
                        units = UnitSystem.valueOf(saved[6] as String),
                        activityLevel = (saved[7] as String?)?.let(ActivityLevel::valueOf),
                        dietaryPreference = (saved[8] as String?)?.let(DietaryPreference::valueOf),
                        calorieOverrideKcal = saved[9] as Int?,
                        proteinOverrideG = saved[10] as Int?,
                        carbsOverrideG = saved[11] as Int?,
                        fatOverrideG = saved[12] as Int?,
                    ),
                    step = saved[13] as Int,
                )
            },
        )
    }
}
