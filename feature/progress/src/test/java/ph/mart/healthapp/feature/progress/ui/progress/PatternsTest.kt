package ph.mart.healthapp.feature.progress.ui.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.epochDayStartMillis
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.health.SleepNight
import ph.mart.healthapp.core.data.health.StepDay
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.feature.progress.R

private const val TODAY = 20_000L

/** Dense, oldest first, ending today — the shape `observeDailyNutrition()` emits. A zero day is an
 * unlogged one, which is what [DayNutrition.isLogged] means. */
private fun nutrition(calories: List<Int>, protein: (Int) -> Int = { it / 16 }): List<DayNutrition> =
    calories.mapIndexed { index, kcal ->
        DayNutrition(TODAY - (calories.size - 1 - index), kcal, protein(kcal), kcal / 10, kcal / 30)
    }

private fun found(
    dailyNutrition: List<DayNutrition> = emptyList(),
    sleepNights: List<SleepNight> = emptyList(),
    stepDays: List<StepDay> = emptyList(),
    moodDays: List<MoodDay> = emptyList(),
    exerciseEntries: List<ExerciseEntry> = emptyList(),
    fastSessions: List<FastSession> = emptyList(),
) = patterns(
    dailyNutrition = dailyNutrition,
    sleepNights = sleepNights,
    stepDays = stepDays,
    moodDays = moodDays,
    exerciseEntries = exerciseEntries,
    fastSessions = fastSessions,
    todayEpochDay = TODAY,
)

private fun List<Pattern>.withTitle(title: Int): Pattern? = firstOrNull { it.title == title }

/** Twenty days: the ten oldest slept 8h and ate 1,700, the ten newest slept 6h and ate 2,100. */
private fun sleepAndFood(days: Int = 20): Pair<List<SleepNight>, List<DayNutrition>> {
    val calories = (0 until days).map { if (it < days / 2) 1_700 else 2_100 }
    val nights = (0 until days).map {
        SleepNight(TODAY - (days - 1 - it), if (it < days / 2) 480 else 360)
    }
    return nights to nutrition(calories)
}

class PatternsTest {

    @Test
    fun `nothing logged means no patterns at all`() {
        assertTrue(found().isEmpty())
    }

    @Test
    fun `a clean split reports both sides and their day counts`() {
        val (nights, food) = sleepAndFood()
        val pattern = found(dailyNutrition = food, sleepNights = nights)
            .withTitle(R.string.progress_pattern_sleep_calories_title)
        assertNotNull(pattern)
        assertEquals(10, pattern!!.highDays)
        assertEquals(10, pattern.lowDays)
        assertEquals(1_700.0, pattern.highOutcome, 0.01)
        assertEquals(2_100.0, pattern.lowOutcome, 0.01)
        assertEquals(-400.0, pattern.delta, 0.01)
    }

    @Test
    fun `a night is filed under the morning it ended, so sleep pairs with the same day's food`() {
        // Alternating days: slept 8h and ate 1,700, slept 6h and ate 2,100, all the way down.
        val nights = (0 until 20).map { SleepNight(TODAY - (19 - it), if (it % 2 == 0) 480 else 360) }
        val food = nutrition((0 until 20).map { if (it % 2 == 0) 1_700 else 2_100 })
        val sameDay = found(dailyNutrition = food, sleepNights = nights)
            .withTitle(R.string.progress_pattern_sleep_calories_title)
        assertNotNull(sameDay)
        assertEquals(-400.0, sameDay!!.delta, 0.01)

        // Move every night one day later and the 8h nights land on the 2,100 kcal days — the sign
        // flips, which is what makes this a real check on the alignment rather than on the split.
        val shifted = found(
            dailyNutrition = food,
            sleepNights = nights.map { it.copy(dateEpochDay = it.dateEpochDay + 1) },
        ).withTitle(R.string.progress_pattern_sleep_calories_title)
        assertNotNull(shifted)
        assertEquals(400.0, shifted!!.delta, 0.01)
    }

    @Test
    fun `too few paired days is no pattern`() {
        val (nights, food) = sleepAndFood(days = MIN_PAIRED_DAYS - 1)
        assertTrue(found(dailyNutrition = food, sleepNights = nights).isEmpty())
    }

    @Test
    fun `a side thinner than the floor is no pattern`() {
        // Nineteen days of 6h sleep and one of 8h: a median split leaves one day on the high side.
        val calories = (0 until 20).map { if (it == 19) 1_600 else 2_200 }
        val nights = (0 until 20).map { SleepNight(TODAY - (19 - it), if (it == 19) 480 else 360) }
        assertTrue(found(dailyNutrition = nutrition(calories), sleepNights = nights).isEmpty())
    }

    @Test
    fun `an outcome gap inside the noise floor is no pattern`() {
        val calories = (0 until 20).map { if (it < 10) 2_000 else 2_050 }
        val nights = (0 until 20).map { SleepNight(TODAY - (19 - it), if (it < 10) 480 else 360) }
        assertTrue(found(dailyNutrition = nutrition(calories), sleepNights = nights).isEmpty())
    }

    @Test
    fun `a driver that barely moved is no pattern`() {
        // A real calorie gap, but 6h01 against 5h59 of sleep is a median, not a split.
        val calories = (0 until 20).map { if (it < 10) 1_700 else 2_200 }
        val nights = (0 until 20).map { SleepNight(TODAY - (19 - it), if (it < 10) 361 else 359) }
        assertTrue(found(dailyNutrition = nutrition(calories), sleepNights = nights).isEmpty())
    }

    @Test
    fun `days outside the window are ignored`() {
        val (nights, food) = sleepAndFood()
        val old = PATTERN_WINDOW_DAYS + 10
        assertTrue(
            found(
                dailyNutrition = food.map { it.copy(dateEpochDay = it.dateEpochDay - old) },
                sleepNights = nights.map { it.copy(dateEpochDay = it.dateEpochDay - old) },
            ).isEmpty(),
        )
    }

    @Test
    fun `an unlogged day is a gap, not a zero-calorie day`() {
        // Ten 8h nights over days with nothing eaten, ten 6h nights over days with food: only the
        // second half pairs, which is under the floor.
        val calories = (0 until 20).map { if (it < 10) 0 else 2_100 }
        val nights = (0 until 20).map { SleepNight(TODAY - (19 - it), if (it < 10) 480 else 360) }
        assertTrue(found(dailyNutrition = nutrition(calories), sleepNights = nights).isEmpty())
    }

    @Test
    fun `a yes-no driver splits into its two real groups, whichever answer is commoner`() {
        // Six workout days out of twenty, protein 40g higher on them.
        val workoutDays = (0 until 20).filter { it % 3 == 0 && it < 18 }.map { TODAY - (19 - it) }.toSet()
        val food = (0 until 20).map { index ->
            val day = TODAY - (19 - index)
            val protein = if (day in workoutDays) 160 else 120
            DayNutrition(day, 2_000, protein, 200, 66)
        }
        val pattern = found(
            dailyNutrition = food,
            exerciseEntries = workoutDays.map {
                ExerciseEntry(dateEpochDay = it, type = ExerciseType.Run, minutes = 30, burnedKcal = 300)
            },
        ).withTitle(R.string.progress_pattern_training_protein_title)
        assertNotNull(pattern)
        assertEquals(workoutDays.size, pattern!!.highDays)
        assertEquals(20 - workoutDays.size, pattern.lowDays)
        assertEquals(40.0, pattern.delta, 0.01)
    }

    @Test
    fun `a fast is compared against the day after it ended`() {
        // Every third day ends a 16-hour fast; the day *after* one eats 1,700, every other day
        // 2,200. Pair the fast with its own day instead and both sides read 2,200.
        val days = (0 until 30).map { TODAY - (29 - it) }
        val hitDays = days.filterIndexed { index, _ -> index % 3 == 0 && index < 27 }.toSet()
        val food = days.map { day ->
            DayNutrition(day, if (day - 1 in hitDays) 1_700 else 2_200, 120, 200, 66)
        }
        val pattern = found(
            dailyNutrition = food,
            fastSessions = hitDays.map {
                // Sixteen hours ending at 08:00 — a fast is filed under the day it ended.
                FastSession(
                    startMillis = epochDayStartMillis(it) - 8 * 3_600_000L,
                    endMillis = epochDayStartMillis(it) + 8 * 3_600_000L,
                    goalHours = 16,
                )
            },
        ).withTitle(R.string.progress_pattern_fasting_calories_title)
        assertNotNull(pattern)
        assertEquals(hitDays.size, pattern!!.highDays)
        // The high side is the fasted days; what it reports is what was eaten the day after.
        assertEquals(1_700.0, pattern.highOutcome, 0.01)
        assertEquals(2_200.0, pattern.lowOutcome, 0.01)
    }

    @Test
    fun `the strongest pattern comes first and the list is capped`() {
        val (nights, food) = sleepAndFood()
        // Mood tracks sleep perfectly; calories only partly, so the mood split is the cleaner one.
        val moods = (0 until 20).map { index ->
            val day = TODAY - (19 - index)
            MoodDay(day, if (index < 10) 5 else 2, if (index < 10) 5 else 2)
        }
        val noisy = food.mapIndexed { index, day ->
            if (index % 4 == 0) day.copy(calories = 2_400, proteinG = 150) else day
        }
        val result = found(dailyNutrition = noisy, sleepNights = nights, moodDays = moods)
        assertTrue(result.size <= MAX_PATTERNS)
        assertEquals(R.string.progress_pattern_sleep_mood_title, result.first().title)
        assertNull(result.withTitle(R.string.progress_pattern_steps_mood_title))
    }
}
