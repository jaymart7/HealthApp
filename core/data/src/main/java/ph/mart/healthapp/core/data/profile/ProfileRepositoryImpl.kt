package ph.mart.healthapp.core.data.profile

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import ph.mart.healthapp.core.data.profile.local.ProfileDao
import ph.mart.healthapp.core.data.profile.local.ProfileEntity
import ph.mart.healthapp.core.data.progress.local.WeightEntryDao

internal class ProfileRepositoryImpl(
    private val dao: ProfileDao,
    private val weightDao: WeightEntryDao,
) : ProfileRepository {

    /** The latest weigh-in rides along on every read, so every `dailyTargets()` caller re-prices
     * off it with no change of its own. `toEntity()` drops it again — it is a join, not a column. */
    override fun observeProfile(): Flow<Profile?> =
        combine(dao.observe(), weightDao.observeLatestKg()) { entity, latestKg ->
            entity?.toProfile()?.copy(latestWeighInKg = latestKg)
        }

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
    supplementRemindersOn = supplementRemindersOn,
    workoutRemindersOn = workoutRemindersOn,
    waterGoalGlasses = waterGoalGlasses,
    addExerciseToBudget = addExerciseToBudget,
    fastingGoalHours = fastingGoalHours,
    fastingRemindersOn = fastingRemindersOn,
    darkThemeOn = darkThemeOn,
    mascotName = mascotName,
    mascotPaletteName = mascotPaletteName,
    stepGoal = stepGoal,
    homeLayout = homeLayout,
    cycleTrackingOn = cycleTrackingOn,
    recapReminderOn = recapReminderOn,
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
    supplementRemindersOn = supplementRemindersOn,
    workoutRemindersOn = workoutRemindersOn,
    waterGoalGlasses = waterGoalGlasses,
    addExerciseToBudget = addExerciseToBudget,
    fastingGoalHours = fastingGoalHours,
    fastingRemindersOn = fastingRemindersOn,
    darkThemeOn = darkThemeOn,
    mascotName = mascotName,
    mascotPaletteName = mascotPaletteName,
    stepGoal = stepGoal,
    homeLayout = homeLayout,
    cycleTrackingOn = cycleTrackingOn,
    recapReminderOn = recapReminderOn,
)
