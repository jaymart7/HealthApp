package ph.mart.healthapp.feature.profile.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.water.DEFAULT_WATER_GOAL_GLASSES
import ph.mart.healthapp.core.data.water.WaterDay

class ProfileExportTest {

    private val profile = Profile(
        sex = Sex.Female,
        age = 31,
        heightCm = 165.0,
        weightKg = 62.0,
        activityLevel = ActivityLevel.Moderate,
        goal = Goal.Lose,
        targetWeightKg = 58.0,
        preferredUnit = UnitSystem.Imperial,
        calorieOverrideKcal = 1800,
        photoReminderOn = true,
        waterRemindersOn = true,
        waterGoalGlasses = 10,
        darkThemeOn = true,
    )

    private val foodEntries = listOf(
        FoodEntry(
            id = 7,
            name = "Oatmeal",
            dateEpochDay = 20_000,
            mealType = MealType.Breakfast,
            portionAmount = 1.5,
            portionUnit = "cup",
            calories = 310,
            proteinG = 11,
            carbsG = 54,
            fatG = 6,
        ),
    )

    private val weightEntries = listOf(WeightEntry(20_000, 62.4, "after the gym"))
    private val measurements = listOf(MeasurementEntry(MeasurementPart.Waist, 20_001, 78.5))
    private val waterDays = listOf(WaterDay(20_000, 6), WaterDay(20_001, 9))
    private val exercises = listOf(
        ExerciseEntry(id = 3, dateEpochDay = 20_001, type = ExerciseType.Run, name = "Riverside", minutes = 32, burnedKcal = 324),
    )

    @Test
    fun `round trips profile food weight measurements water and exercise`() {
        val json = buildExportJson(profile, foodEntries, weightEntries, measurements, waterDays, exercises)
        val payload = parseExport(json).getOrThrow()

        assertEquals(profile, payload.profile)
        assertEquals(weightEntries, payload.weightEntries)
        assertEquals(measurements, payload.measurements)
        assertEquals(waterDays, payload.waterDays)
        // The row id is storage-local and deliberately not exported; everything else survives.
        assertEquals(foodEntries.map { it.copy(id = 0) }, payload.foodEntries)
        assertEquals(exercises.map { it.copy(id = 0) }, payload.exercises)
    }

    @Test
    fun `export carries no photo data`() {
        val json = buildExportJson(profile, foodEntries, weightEntries, measurements, waterDays, exercises)
        assertFalse(json.contains("filePath"))
        assertFalse(json.contains("\"photos\""))
    }

    @Test
    fun `malformed json fails instead of throwing`() {
        assertTrue(parseExport("{ this is not json").isFailure)
        assertTrue(parseExport("").isFailure)
    }

    @Test
    fun `unrecognized enum value fails`() {
        val json = buildExportJson(profile, foodEntries, weightEntries, measurements, waterDays, exercises)
            .replace("\"Waist\"", "\"Elbow\"")
        assertTrue(parseExport(json).isFailure)
    }

    /** A file written before water existed must still import — every field added since is
     * defaulted, which is the whole reason the version gate only looks forward. Written out by
     * hand rather than derived from [buildExportJson], which can only emit the current schema. */
    @Test
    fun `a v1 file without water still imports`() {
        val v1 = """
            {
              "schemaVersion": 1,
              "profile": {
                "sex": "Female", "age": 31, "heightCm": 165.0, "weightKg": 62.0,
                "activityLevel": "Moderate", "goal": "Lose", "preferredUnit": "Imperial"
              },
              "weightEntries": [{ "dateEpochDay": 20000, "weightKg": 62.4 }]
            }
        """.trimIndent()
        val payload = parseExport(v1).getOrThrow()

        assertEquals(weightEntries.map { it.copy(note = "") }, payload.weightEntries)
        assertEquals(emptyList<WaterDay>(), payload.waterDays)
        assertEquals(emptyList<ExerciseEntry>(), payload.exercises)
        assertEquals(DEFAULT_WATER_GOAL_GLASSES, payload.profile?.waterGoalGlasses)
        // Defaulted on, so an older file doesn't silently drop the exercise credit.
        assertEquals(true, payload.profile?.addExerciseToBudget)
    }

    @Test
    fun `newer schema version is rejected`() {
        val json = buildExportJson(profile, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
            .replace("\"schemaVersion\": $EXPORT_SCHEMA_VERSION", "\"schemaVersion\": 99")
        assertTrue(parseExport(json).isFailure)
    }
}
