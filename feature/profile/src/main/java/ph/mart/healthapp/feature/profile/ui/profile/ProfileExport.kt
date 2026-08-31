package ph.mart.healthapp.feature.profile.ui.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureReading
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.fasting.DEFAULT_FAST_GOAL_HOURS
import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.health.DEFAULT_STEP_GOAL
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.DietaryPreference
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.core.data.supplement.SupplementDay
import ph.mart.healthapp.core.data.transfer.ImportData
import ph.mart.healthapp.core.data.water.DEFAULT_WATER_GOAL_GLASSES
import ph.mart.healthapp.core.data.water.WaterDay

/**
 * The on-disk export format. Deliberately its own set of DTOs rather than `@Serializable` on the
 * `:core:data` models: enums travel as plain strings so a hand-edited or older file fails as a
 * [Result] rather than throwing, and the file format can drift from the Room schema without
 * dragging serialization annotations into the data layer.
 *
 * Progress photos are never included — they are image files, and the UI says so.
 */
@Serializable
internal data class FitPulseExport(
    @SerialName("schemaVersion") val schemaVersion: Int = EXPORT_SCHEMA_VERSION,
    val profile: ExportProfile? = null,
    val foodEntries: List<ExportFoodEntry> = emptyList(),
    val weightEntries: List<ExportWeightEntry> = emptyList(),
    val measurements: List<ExportMeasurement> = emptyList(),
    val waterDays: List<ExportWaterDay> = emptyList(),
    val exercises: List<ExportExercise> = emptyList(),
    val moodDays: List<ExportMoodDay> = emptyList(),
    val fastSessions: List<ExportFastSession> = emptyList(),
    val supplements: List<ExportSupplement> = emptyList(),
    val supplementDays: List<ExportSupplementDay> = emptyList(),
    val bloodPressure: List<ExportBloodPressureReading> = emptyList(),
)

/** 2 added [FitPulseExport.waterDays] and the profile's water fields; 3 added
 * [FitPulseExport.exercises] and [ExportProfile.addExerciseToBudget]; 4 added
 * [ExportProfile.darkThemeOn]; 5 added [FitPulseExport.moodDays]; 6 added
 * [FitPulseExport.fastSessions] and the profile's fasting fields; 7 added the food entries'
 * fiber, sugar and sodium; 8 added [FitPulseExport.supplements], [FitPulseExport.supplementDays]
 * and [ExportProfile.supplementRemindersOn]; 9 added [FitPulseExport.bloodPressure]; 10 added
 * [ExportProfile.mascotName]; 11 added [ExportProfile.stepGoal]; 12 added
 * [ExportProfile.mascotPaletteName]; 13 added [ExportExercise.steps].
 * Every addition is defaulted, so a v1 file still imports — the version gate only rejects files
 * from the future. */
internal const val EXPORT_SCHEMA_VERSION = 13

@Serializable
internal data class ExportProfile(
    val sex: String,
    val age: Int,
    val heightCm: Double,
    val weightKg: Double,
    val activityLevel: String,
    val goal: String,
    val targetWeightKg: Double? = null,
    val dietaryPreference: String? = null,
    val preferredUnit: String,
    val calorieOverrideKcal: Int? = null,
    val proteinOverrideG: Int? = null,
    val carbsOverrideG: Int? = null,
    val fatOverrideG: Int? = null,
    val mealRemindersOn: Boolean = true,
    val weighInReminderOn: Boolean = true,
    val photoReminderOn: Boolean = false,
    val waterRemindersOn: Boolean = false,
    val waterGoalGlasses: Int = DEFAULT_WATER_GOAL_GLASSES,
    val addExerciseToBudget: Boolean = true,
    val fastingGoalHours: Int = DEFAULT_FAST_GOAL_HOURS,
    val fastingRemindersOn: Boolean = false,
    val darkThemeOn: Boolean? = null,
    val supplementRemindersOn: Boolean = false,
    val mascotName: String? = null,
    val mascotPaletteName: String? = null,
    val stepGoal: Int = DEFAULT_STEP_GOAL,
)

@Serializable
internal data class ExportFoodEntry(
    val dateEpochDay: Long,
    val name: String,
    val mealType: String,
    val portionAmount: Double,
    val portionUnit: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val fiberG: Int = 0,
    val sugarG: Int = 0,
    val sodiumMg: Int = 0,
)

@Serializable
internal data class ExportWeightEntry(val dateEpochDay: Long, val weightKg: Double, val note: String = "")

@Serializable
internal data class ExportMeasurement(val part: String, val dateEpochDay: Long, val valueCm: Double)

@Serializable
internal data class ExportWaterDay(val dateEpochDay: Long, val glasses: Int)

/** [steps] travels because `ExerciseRepository.addEntry` re-derives a missing one from
 * `estimatedSteps()` — so without it an imported watch swim came back as 0 and an imported walk
 * came back with a figure nobody measured, quietly changing what `stepsCreditKcal()` subtracts.
 * Defaulted, so a file written before v13 still imports and simply gets the estimate it always
 * did. */
@Serializable
internal data class ExportExercise(
    val dateEpochDay: Long,
    val type: String,
    val name: String = "",
    val minutes: Int,
    val burnedKcal: Int,
    val steps: Int = 0,
)

/** Mood is history, not convenience data, so unlike favourites and saved meals it belongs in the
 * file. A 0 on either field means that row was never tapped. */
@Serializable
internal data class ExportMoodDay(val dateEpochDay: Long, val mood: Int, val energy: Int)

/** Completed fasts only — `endMillis` is non-null here because a running fast is a timer, not
 * history, and restoring one on another device would resume a clock nobody started there. The row
 * id is dropped: it is an autoGenerate key with no meaning outside the database it came from. */
@Serializable
internal data class ExportFastSession(val startMillis: Long, val endMillis: Long, val goalHours: Int)

/**
 * The id travels, unlike every other row in this file. A [ExportSupplementDay] points at a
 * supplement by id, so an import that let Room generate fresh ones would restore a log of ticks
 * with nothing to tick. Soft-deleted supplements ride along for the same reason — a day that
 * recorded one still has to be able to name it.
 */
@Serializable
internal data class ExportSupplement(
    val id: Long,
    val name: String,
    val dose: String = "",
    val timesPerDay: Int = 1,
    val deleted: Boolean = false,
    val createdAt: Long = 0,
)

/** [dueTimes] is the day's own snapshot of the target, not today's — restoring it is what stops a
 * later edit rewriting how a past day scored. Days where nothing was taken aren't written. */
@Serializable
internal data class ExportSupplementDay(
    val dateEpochDay: Long,
    val supplementId: Long,
    val taken: Int,
    val dueTimes: Int,
)

/**
 * Readings are history, not convenience data, so unlike favourites and saved meals they belong in
 * the file. The row id is dropped — nothing points at a reading, which makes this the
 * [ExportFastSession] case rather than the [ExportSupplement] one. A [pulseBpm] of 0 means the
 * figure was never entered.
 */
@Serializable
internal data class ExportBloodPressureReading(
    val takenAtMillis: Long,
    val systolic: Int,
    val diastolic: Int,
    val pulseBpm: Int = 0,
)

private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

internal fun buildExportJson(
    profile: Profile?,
    foodEntries: List<FoodEntry>,
    weightEntries: List<WeightEntry>,
    measurements: List<MeasurementEntry>,
    waterDays: List<WaterDay>,
    exercises: List<ExerciseEntry>,
    moodDays: List<MoodDay>,
    fastSessions: List<FastSession>,
    supplements: List<Supplement>,
    supplementDays: List<SupplementDay>,
    bloodPressure: List<BloodPressureReading>,
): String = json.encodeToString(
    FitPulseExport(
        profile = profile?.toExport(),
        foodEntries = foodEntries.map { it.toExport() },
        weightEntries = weightEntries.map { ExportWeightEntry(it.dateEpochDay, it.weightKg, it.note) },
        measurements = measurements.map { ExportMeasurement(it.part.name, it.dateEpochDay, it.valueCm) },
        waterDays = waterDays.map { ExportWaterDay(it.dateEpochDay, it.glasses) },
        exercises = exercises.map {
            ExportExercise(it.dateEpochDay, it.type.name, it.name, it.minutes, it.burnedKcal, it.steps)
        },
        moodDays = moodDays.map { ExportMoodDay(it.dateEpochDay, it.mood, it.energy) },
        fastSessions = fastSessions.mapNotNull { session ->
            session.endMillis?.let { ExportFastSession(session.startMillis, it, session.goalHours) }
        },
        supplements = supplements.map {
            ExportSupplement(it.id, it.name, it.dose, it.timesPerDay, it.deleted, it.createdAt)
        },
        supplementDays = supplementDays.map {
            ExportSupplementDay(it.dateEpochDay, it.supplementId, it.taken, it.dueTimes)
        },
        bloodPressure = bloodPressure.map {
            ExportBloodPressureReading(it.takenAtMillis, it.systolic, it.diastolic, it.pulseBpm)
        },
    ),
)

/** Parses and validates in one step — a malformed file, an unknown enum name, or a future schema
 * version all come back as [Result.failure] so the caller can show a message and write nothing. */
internal fun parseExport(text: String): Result<ImportData> = runCatching {
    val export = json.decodeFromString<FitPulseExport>(text)
    require(export.schemaVersion <= EXPORT_SCHEMA_VERSION) {
        "This file was written by a newer version of FitPulse."
    }
    ImportData(
        profile = export.profile?.toProfile(),
        foodEntries = export.foodEntries.map { it.toFoodEntry() },
        weightEntries = export.weightEntries.map { WeightEntry(it.dateEpochDay, it.weightKg, it.note) },
        measurements = export.measurements.map {
            MeasurementEntry(enumOf<MeasurementPart>(it.part, MeasurementPart.entries), it.dateEpochDay, it.valueCm)
        },
        waterDays = export.waterDays.map { WaterDay(it.dateEpochDay, it.glasses) },
        exercises = export.exercises.map {
            ExerciseEntry(
                dateEpochDay = it.dateEpochDay,
                type = enumOf(it.type, ExerciseType.entries),
                name = it.name,
                minutes = it.minutes,
                burnedKcal = it.burnedKcal,
                steps = it.steps,
            )
        },
        moodDays = export.moodDays.map { MoodDay(it.dateEpochDay, it.mood, it.energy) },
        fastSessions = export.fastSessions.map {
            FastSession(startMillis = it.startMillis, endMillis = it.endMillis, goalHours = it.goalHours)
        },
        supplements = export.supplements.map {
            Supplement(
                id = it.id,
                name = it.name,
                dose = it.dose,
                timesPerDay = it.timesPerDay,
                deleted = it.deleted,
                createdAt = it.createdAt,
            )
        },
        supplementDays = export.supplementDays.map {
            SupplementDay(it.dateEpochDay, it.supplementId, it.taken, it.dueTimes)
        },
        bloodPressure = export.bloodPressure.map {
            BloodPressureReading(
                takenAtMillis = it.takenAtMillis,
                systolic = it.systolic,
                diastolic = it.diastolic,
                pulseBpm = it.pulseBpm,
            )
        },
    )
}

private inline fun <reified T : Enum<T>> enumOf(name: String, values: List<T>): T =
    values.firstOrNull { it.name == name } ?: error("Unrecognized ${T::class.simpleName}: $name")

private fun Profile.toExport() = ExportProfile(
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
    supplementRemindersOn = supplementRemindersOn,
    mascotName = mascotName,
    mascotPaletteName = mascotPaletteName,
    stepGoal = stepGoal,
)

private fun ExportProfile.toProfile() = Profile(
    sex = enumOf(sex, Sex.entries),
    age = age,
    heightCm = heightCm,
    weightKg = weightKg,
    activityLevel = enumOf(activityLevel, ActivityLevel.entries),
    goal = enumOf(goal, Goal.entries),
    targetWeightKg = targetWeightKg,
    dietaryPreference = dietaryPreference?.let { enumOf(it, DietaryPreference.entries) },
    preferredUnit = enumOf(preferredUnit, UnitSystem.entries),
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
    supplementRemindersOn = supplementRemindersOn,
    mascotName = mascotName,
    mascotPaletteName = mascotPaletteName,
    stepGoal = stepGoal,
)

private fun FoodEntry.toExport() = ExportFoodEntry(
    dateEpochDay = dateEpochDay,
    name = name,
    mealType = mealType.name,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    fiberG = fiberG,
    sugarG = sugarG,
    sodiumMg = sodiumMg,
)

private fun ExportFoodEntry.toFoodEntry() = FoodEntry(
    name = name,
    dateEpochDay = dateEpochDay,
    mealType = enumOf(mealType, MealType.entries),
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    fiberG = fiberG,
    sugarG = sugarG,
    sodiumMg = sodiumMg,
)
