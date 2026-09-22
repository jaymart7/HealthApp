package ph.mart.healthapp.core.data.transfer

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.epochDayOf
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.StrengthSet
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.note.DayNote
import ph.mart.healthapp.core.data.progress.WeightEntry

/**
 * The CSV zip, read back out of its own bytes. Everything here goes through
 * [buildExportCsvZip] rather than poking the private writers, because the quoting and the date
 * conversions only matter as they land in the file.
 */
class CsvExportTest {

    private fun zipOf(data: ImportData): Map<String, String> {
        val files = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(buildExportCsvZip(data))).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                files[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
            }
        }
        return files
    }

    private fun data(
        foodEntries: List<FoodEntry> = emptyList(),
        weightEntries: List<WeightEntry> = emptyList(),
        exercises: List<ExerciseEntry> = emptyList(),
        dayNotes: List<DayNote> = emptyList(),
    ) = ImportData(
        profile = null,
        foodEntries = foodEntries,
        weightEntries = weightEntries,
        measurements = emptyList(),
        waterDays = emptyList(),
        exercises = exercises,
        moodDays = emptyList(),
        fastSessions = emptyList(),
        supplements = emptyList(),
        supplementDays = emptyList(),
        bloodPressure = emptyList(),
        cycleDays = emptyList(),
        dayNotes = dayNotes,
    )

    private fun food(name: String) = FoodEntry(
        name = name,
        dateEpochDay = 20_000,
        mealType = MealType.Breakfast,
        portionAmount = 1.5,
        portionUnit = "cup",
        calories = 310,
        proteinG = 11,
        carbsG = 54,
        fatG = 6,
    )

    @Test
    fun `the zip holds one file per table`() {
        assertEquals(
            setOf(
                "profile.csv", "food_entries.csv", "weight_entries.csv", "measurements.csv",
                "water_days.csv", "exercises.csv", "strength_sets.csv", "mood_days.csv",
                "fast_sessions.csv", "supplements.csv", "supplement_days.csv",
                "blood_pressure.csv", "cycle_days.csv", "day_notes.csv",
            ),
            zipOf(data()).keys,
        )
    }

    /** A file that is zero bytes looks broken; a file that is one line is an empty table. */
    @Test
    fun `an empty table still writes its header`() {
        assertEquals(
            "date,name,mealType,portionAmount,portionUnit,calories,proteinG,carbsG,fatG," +
                "fiberG,sugarG,sodiumMg,vitaminDUg,calciumMg,ironUg,potassiumMg\r\n",
            zipOf(data())["food_entries.csv"],
        )
    }

    @Test
    fun `a comma and a quote in a food name stay in one cell`() {
        val row = zipOf(data(foodEntries = listOf(food("Rice, \"fried\""))))["food_entries.csv"]!!
            .lines()[1]
        // One cell, whole, between the date before it and the meal after it.
        assertTrue(row, row.contains(",\"Rice, \"\"fried\"\"\",Breakfast,"))
    }

    /** A note is the one thing in the file the user wrote rather than logged, and prose has
     * newlines in it. */
    @Test
    fun `a newline in a note is quoted rather than ending the row`() {
        val csv = zipOf(data(dayNotes = listOf(DayNote(20_000, "Rough one.\nAte out."))))["day_notes.csv"]!!
        assertTrue(csv, csv.endsWith("\"Rough one.\nAte out.\"\r\n"))
        // Header, then exactly one record — the embedded newline must not have split it.
        assertEquals(2, csv.removeSuffix("\r\n").split("\r\n").size)
    }

    /**
     * The conversion the whole format exists for. The expected day is derived with [epochDayOf]
     * from a timestamp in the same zone the writer formats in, so the assertion holds wherever
     * this runs.
     */
    @Test
    fun `an epoch day renders as an ISO date and a missing time renders empty`() {
        val day = epochDayOf(1_756_600_000_000L) // 2025-08-31 local, whatever local is here
        val csv = zipOf(data(weightEntries = listOf(WeightEntry(day, 62.4, "after the gym"))))
        val row = csv["weight_entries.csv"]!!.lines()[1]
        assertEquals("date,weightKg,note,time", csv["weight_entries.csv"]!!.lines()[0])
        // yyyy-MM-dd, then the figure, then the note, then an empty cell — never a midnight
        // nobody recorded.
        assertTrue(row, Regex("^\\d{4}-\\d{2}-\\d{2},62\\.4,after the gym,\$").matches(row))
    }

    @Test
    fun `a recorded time renders as a clock`() {
        val row = zipOf(
            data(weightEntries = listOf(WeightEntry(20_000, 62.4, minuteOfDay = 7 * 60 + 5))),
        )["weight_entries.csv"]!!.lines()[1]
        assertTrue(row, row.endsWith(",07:05"))
    }

    /** `sets` is a list and no cell holds one, so it leaves `exercises.csv` for a file of its own
     * joined on the workout's row index. A bodyweight set is 0 kg and is a real row. */
    @Test
    fun `strength sets land in their own file pointing at the right workout`() {
        val csv = zipOf(
            data(
                exercises = listOf(
                    ExerciseEntry(dateEpochDay = 20_001, type = ExerciseType.Run, minutes = 32, burnedKcal = 324),
                    ExerciseEntry(
                        dateEpochDay = 20_002, type = ExerciseType.Strength, minutes = 45, burnedKcal = 260,
                        sets = listOf(
                            StrengthSet("Bench press", reps = 8, weightKg = 62.5),
                            StrengthSet("Dip", reps = 12, weightKg = 0.0),
                        ),
                    ),
                ),
            ),
        )
        assertTrue(csv["exercises.csv"]!!, "sets" !in csv["exercises.csv"]!!.lines()[0])
        val sets = csv["strength_sets.csv"]!!.removeSuffix("\r\n").split("\r\n")
        assertEquals(3, sets.size)
        // Workout 1 is the strength session — index 0 is the run, which contributes no rows.
        assertTrue(sets[1], sets[1].startsWith("1,"))
        assertTrue(sets[1], sets[1].endsWith(",Bench press,8,62.5"))
        assertTrue(sets[2], sets[2].endsWith(",Dip,12,0.0"))
    }
}
