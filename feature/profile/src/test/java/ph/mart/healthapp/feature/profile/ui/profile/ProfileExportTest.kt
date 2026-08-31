package ph.mart.healthapp.feature.profile.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureReading
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.fasting.DEFAULT_FAST_GOAL_HOURS
import ph.mart.healthapp.core.data.health.DEFAULT_STEP_GOAL
import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.core.data.supplement.SupplementDay
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
        stepGoal = 12_000,
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
            fiberG = 8,
            sugarG = 3,
            sodiumMg = 210,
        ),
    )

    private val weightEntries = listOf(WeightEntry(20_000, 62.4, "after the gym"))
    private val measurements = listOf(MeasurementEntry(MeasurementPart.Waist, 20_001, 78.5))
    private val waterDays = listOf(WaterDay(20_000, 6), WaterDay(20_001, 9))
    private val exercises = listOf(
        ExerciseEntry(id = 3, dateEpochDay = 20_001, type = ExerciseType.Run, name = "Riverside", minutes = 32, burnedKcal = 324),
    )
    // The second day is mood-only — 0 is "not tapped", and it has to survive the round trip as 0
    // rather than being dropped or promoted to a score.
    private val moodDays = listOf(MoodDay(20_000, mood = 4, energy = 3), MoodDay(20_001, mood = 2, energy = 0))
    // The third is still running: history only, so it must not reach the file.
    private val fastSessions = listOf(
        FastSession(id = 4, startMillis = 1_700_000_000_000L, endMillis = 1_700_057_600_000L, goalHours = 16),
        FastSession(id = 5, startMillis = 1_700_100_000_000L, endMillis = 1_700_165_600_000L, goalHours = 18),
        FastSession(id = 6, startMillis = 1_700_200_000_000L, endMillis = null, goalHours = 16),
    )

    // The third is soft-deleted: it still travels, because supplementDays below names it by id.
    private val supplements = listOf(
        Supplement(id = 1, name = "Vitamin D", dose = "2000 IU", createdAt = 1_700_000_000_000L),
        Supplement(id = 2, name = "Creatine", dose = "5 g", timesPerDay = 2, createdAt = 1_700_000_001_000L),
        Supplement(id = 3, name = "Zinc", deleted = true, createdAt = 1_700_000_002_000L),
    )
    // The second day carries a dueTimes the supplement no longer has — the snapshot has to survive
    // the round trip, or restoring the file would rescore a day against today's target.
    private val supplementDays = listOf(
        SupplementDay(dateEpochDay = 20_000, supplementId = 1, taken = 1, dueTimes = 1),
        SupplementDay(dateEpochDay = 20_000, supplementId = 2, taken = 3, dueTimes = 3),
    )
    // The second reading carries no pulse: a cuff that shows none writes 0, not a pulse of zero.
    private val bloodPressure = listOf(
        BloodPressureReading(id = 1, takenAtMillis = 1_756_600_000_000, systolic = 128, diastolic = 82, pulseBpm = 71),
        BloodPressureReading(id = 2, takenAtMillis = 1_756_640_000_000, systolic = 121, diastolic = 79),
    )

    @Test
    fun `round trips profile food weight measurements water exercise mood and fasting`() {
        val json = buildExportJson(profile, foodEntries, weightEntries, measurements, waterDays, exercises, moodDays, fastSessions, supplements, supplementDays, bloodPressure)
        val payload = parseExport(json).getOrThrow()

        assertEquals(profile, payload.profile)
        assertEquals(weightEntries, payload.weightEntries)
        assertEquals(measurements, payload.measurements)
        assertEquals(waterDays, payload.waterDays)
        assertEquals(moodDays, payload.moodDays)
        // The row id is storage-local and deliberately not exported; everything else survives.
        assertEquals(foodEntries.map { it.copy(id = 0) }, payload.foodEntries)
        assertEquals(exercises.map { it.copy(id = 0) }, payload.exercises)
        // The running fast is dropped and the ids go with the storage they came from.
        assertEquals(fastSessions.take(2).map { it.copy(id = 0) }, payload.fastSessions)
    }

    @Test
    fun `export carries no photo data`() {
        val json = buildExportJson(profile, foodEntries, weightEntries, measurements, waterDays, exercises, moodDays, fastSessions, supplements, supplementDays, bloodPressure)
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
        val json = buildExportJson(profile, foodEntries, weightEntries, measurements, waterDays, exercises, moodDays, fastSessions, supplements, supplementDays, bloodPressure)
            .replace("\"Waist\"", "\"Elbow\"")
        assertTrue(parseExport(json).isFailure)
    }

    /** A v5 file — the schema one version back, written before fasting existed. Same guarantee as
     * the v1 case below, checked at the boundary that just moved. */
    @Test
    fun `a v5 file without fasting still imports`() {
        val v5 = buildExportJson(profile, foodEntries, weightEntries, measurements, waterDays, exercises, moodDays, emptyList(), supplements, supplementDays, emptyList())
            .replace("\"schemaVersion\": $EXPORT_SCHEMA_VERSION", "\"schemaVersion\": 5")
        val payload = parseExport(v5).getOrThrow()

        assertEquals(emptyList<FastSession>(), payload.fastSessions)
        assertEquals(moodDays, payload.moodDays)
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
        assertEquals(emptyList<MoodDay>(), payload.moodDays)
        assertEquals(emptyList<FastSession>(), payload.fastSessions)
        assertEquals(emptyList<Supplement>(), payload.supplements)
        assertEquals(emptyList<SupplementDay>(), payload.supplementDays)
        assertEquals(DEFAULT_WATER_GOAL_GLASSES, payload.profile?.waterGoalGlasses)
        assertEquals(DEFAULT_FAST_GOAL_HOURS, payload.profile?.fastingGoalHours)
        assertEquals(DEFAULT_STEP_GOAL, payload.profile?.stepGoal)
        // Defaulted on, so an older file doesn't silently drop the exercise credit.
        assertEquals(true, payload.profile?.addExerciseToBudget)
    }

    /** A file written before fiber, sugar and sodium existed: the three default to 0, which is
     * exactly what those fields mean everywhere else in the app. */
    @Test
    fun `a v6 file without the micronutrients still imports`() {
        val v6 = """
            {
              "schemaVersion": 6,
              "foodEntries": [{
                "dateEpochDay": 20000, "name": "Oatmeal", "mealType": "Breakfast",
                "portionAmount": 1.5, "portionUnit": "cup",
                "calories": 310, "proteinG": 11, "carbsG": 54, "fatG": 6
              }]
            }
        """.trimIndent()

        val entry = parseExport(v6).getOrThrow().foodEntries.single()

        assertEquals(310, entry.calories)
        assertEquals(0, entry.fiberG)
        assertEquals(0, entry.sugarG)
        assertEquals(0, entry.sodiumMg)
    }

    /** The one place an id crosses the file boundary: a supplement day names its supplement by id,
     * so regenerating ids on import would restore a log of ticks with nothing to tick. */
    @Test
    fun `supplement ids and their day snapshots survive the round trip`() {
        val json = buildExportJson(profile, foodEntries, weightEntries, measurements, waterDays, exercises, moodDays, fastSessions, supplements, supplementDays, bloodPressure)
        val payload = parseExport(json).getOrThrow()

        assertEquals(supplements, payload.supplements)
        assertEquals(supplementDays, payload.supplementDays)
        // Every restored day still points at a supplement the file also carried — including the
        // soft-deleted one.
        assertTrue(payload.supplementDays.all { day -> payload.supplements.any { it.id == day.supplementId } })
    }

    /** A v7 file — the schema one version back, written before supplements existed. */
    @Test
    fun `a v7 file without supplements still imports`() {
        val v7 = buildExportJson(profile, foodEntries, weightEntries, measurements, waterDays, exercises, moodDays, fastSessions, emptyList(), emptyList(), emptyList())
            .replace("\"schemaVersion\": $EXPORT_SCHEMA_VERSION", "\"schemaVersion\": 7")
        val payload = parseExport(v7).getOrThrow()

        assertEquals(emptyList<Supplement>(), payload.supplements)
        assertEquals(emptyList<SupplementDay>(), payload.supplementDays)
        assertFalse(payload.profile!!.supplementRemindersOn)
    }

    /** Readings are history, so they ride the file. The id is dropped — nothing points at one. */
    @Test
    fun `blood pressure readings survive the round trip`() {
        val json = buildExportJson(profile, foodEntries, weightEntries, measurements, waterDays, exercises, moodDays, fastSessions, supplements, supplementDays, bloodPressure)
        val restored = parseExport(json).getOrThrow().bloodPressure

        assertEquals(2, restored.size)
        assertEquals(128, restored.first().systolic)
        assertEquals(82, restored.first().diastolic)
        assertEquals(71, restored.first().pulseBpm)
        assertEquals(1_756_600_000_000, restored.first().takenAtMillis)
        // A reading logged off a cuff that showed no pulse keeps its 0 — "not entered", not zero.
        assertEquals(0, restored.last().pulseBpm)
    }

    /** A v8 file — the schema one version back, written before blood pressure existed. */
    @Test
    fun `a v8 file without blood pressure still imports`() {
        val v8 = buildExportJson(profile, foodEntries, weightEntries, measurements, waterDays, exercises, moodDays, fastSessions, supplements, supplementDays, emptyList())
            .replace("\"schemaVersion\": $EXPORT_SCHEMA_VERSION", "\"schemaVersion\": 8")
        val payload = parseExport(v8).getOrThrow()

        assertEquals(emptyList<BloodPressureReading>(), payload.bloodPressure)
        assertEquals(supplements, payload.supplements)
    }

    @Test
    fun `newer schema version is rejected`() {
        val json = buildExportJson(profile, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
            .replace("\"schemaVersion\": $EXPORT_SCHEMA_VERSION", "\"schemaVersion\": 99")
        assertTrue(parseExport(json).isFailure)
    }
}
