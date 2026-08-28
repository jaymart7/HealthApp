package ph.mart.healthapp.feature.profile.ui

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.DietaryPreference
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.data.progress.WeightEntry

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
)

internal const val EXPORT_SCHEMA_VERSION = 1

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
)

@Serializable
internal data class ExportWeightEntry(val dateEpochDay: Long, val weightKg: Double, val note: String = "")

@Serializable
internal data class ExportMeasurement(val part: String, val dateEpochDay: Long, val valueCm: Double)

/** What an import hands back to the ViewModel — domain types only, already validated. */
internal data class ImportPayload(
    val profile: Profile?,
    val foodEntries: List<FoodEntry>,
    val weightEntries: List<WeightEntry>,
    val measurements: List<MeasurementEntry>,
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
): String = json.encodeToString(
    FitPulseExport(
        profile = profile?.toExport(),
        foodEntries = foodEntries.map { it.toExport() },
        weightEntries = weightEntries.map { ExportWeightEntry(it.dateEpochDay, it.weightKg, it.note) },
        measurements = measurements.map { ExportMeasurement(it.part.name, it.dateEpochDay, it.valueCm) },
    ),
)

/** Parses and validates in one step — a malformed file, an unknown enum name, or a future schema
 * version all come back as [Result.failure] so the caller can show a message and write nothing. */
internal fun parseExport(text: String): Result<ImportPayload> = runCatching {
    val export = json.decodeFromString<FitPulseExport>(text)
    require(export.schemaVersion <= EXPORT_SCHEMA_VERSION) {
        "This file was written by a newer version of FitPulse."
    }
    ImportPayload(
        profile = export.profile?.toProfile(),
        foodEntries = export.foodEntries.map { it.toFoodEntry() },
        weightEntries = export.weightEntries.map { WeightEntry(it.dateEpochDay, it.weightKg, it.note) },
        measurements = export.measurements.map {
            MeasurementEntry(enumOf<MeasurementPart>(it.part, MeasurementPart.entries), it.dateEpochDay, it.valueCm)
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
)
