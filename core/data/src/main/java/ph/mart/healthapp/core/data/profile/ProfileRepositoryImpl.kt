package ph.mart.healthapp.core.data.profile

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ph.mart.healthapp.core.data.profile.local.ProfileDao
import ph.mart.healthapp.core.data.profile.local.ProfileEntity

internal class ProfileRepositoryImpl(private val dao: ProfileDao) : ProfileRepository {

    override fun observeProfile(): Flow<Profile?> = dao.observe().map { it?.toProfile() }

    override suspend fun saveProfile(profile: Profile) {
        dao.upsert(profile.toEntity())
    }
}

private fun ProfileEntity.toProfile() = Profile(
    sex = Sex.valueOf(sex),
    age = age,
    heightCm = heightCm,
    weightKg = weightKg,
    activityLevel = ActivityLevel.valueOf(activityLevel),
    goal = Goal.valueOf(goal),
    targetWeightKg = targetWeightKg,
    dietaryPreference = dietaryPreference?.let { DietaryPreference.valueOf(it) },
    preferredUnit = UnitSystem.valueOf(preferredUnit),
    calorieOverrideKcal = calorieOverrideKcal,
    proteinOverrideG = proteinOverrideG,
    carbsOverrideG = carbsOverrideG,
    fatOverrideG = fatOverrideG,
    mealRemindersOn = mealRemindersOn,
    weighInReminderOn = weighInReminderOn,
    photoReminderOn = photoReminderOn,
    waterRemindersOn = waterRemindersOn,
    waterGoalGlasses = waterGoalGlasses,
    addExerciseToBudget = addExerciseToBudget,
    fastingGoalHours = fastingGoalHours,
    fastingRemindersOn = fastingRemindersOn,
    darkThemeOn = darkThemeOn,
)

private fun Profile.toEntity() = ProfileEntity(
    sex = sex.name,
    age = age,
    heightCm = heightCm,
    weightKg = weightKg,
    activityLevel = activityLevel.name,
    goal = goal.name,
    targetWeightKg = targetWeightKg,
    dietaryPreference = dietaryPreference?.name,
    preferredUnit = preferredUnit.name,
    calorieOverrideKcal = calorieOverrideKcal,
    proteinOverrideG = proteinOverrideG,
    carbsOverrideG = carbsOverrideG,
    fatOverrideG = fatOverrideG,
    mealRemindersOn = mealRemindersOn,
    weighInReminderOn = weighInReminderOn,
    photoReminderOn = photoReminderOn,
    waterRemindersOn = waterRemindersOn,
    waterGoalGlasses = waterGoalGlasses,
    addExerciseToBudget = addExerciseToBudget,
    fastingGoalHours = fastingGoalHours,
    fastingRemindersOn = fastingRemindersOn,
    darkThemeOn = darkThemeOn,
)
