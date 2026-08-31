package ph.mart.healthapp.debug

import android.graphics.Bitmap
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.Koin
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.estimateBurnedKcal
import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.fasting.FastingRepository
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.mood.MoodRepository
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.DietaryPreference
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.profile.round1
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.MeasurementPart
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.core.data.supplement.SupplementDay
import ph.mart.healthapp.core.data.supplement.SupplementRepository
import ph.mart.healthapp.core.data.water.WaterDay
import ph.mart.healthapp.core.data.water.WaterRepository
import ph.mart.healthapp.core.data.epochDayStartMillis
import ph.mart.healthapp.core.data.todayEpochDay

/**
 * Fills a fresh debug install with one varied dataset so every data-driven screen — Home's rings,
 * the weight chart at each [ph.mart.healthapp.core.data.progress.ChartRange], measurements, photo
 * comparison, the diary — renders populated instead of empty.
 *
 * Runs only when no profile exists, so it fires once per install and leaves onboarding testable by
 * clearing app data. Everything goes through the public repository interfaces — no DAO or Entity
 * reaches `:app`. The release source set has a no-op counterpart, so none of this ships.
 */
fun seedDebugData(koin: Koin) {
    CoroutineScope(Dispatchers.IO).launch {
        val profiles = koin.get<ProfileRepository>()
        if (profiles.observeProfile().first() != null) return@launch

        val today = todayEpochDay()
        profiles.saveProfile(seedProfile)
        koin.get<ProgressRepository>().seedProgress(today)
        koin.get<FoodRepository>().seedFood(today)
        koin.get<WaterRepository>().seedWater(today)
        koin.get<ExerciseRepository>().seedExercise(today)
        koin.get<MoodRepository>().seedMood(today)
        koin.get<FastingRepository>().seedFasting(today)
        koin.get<SupplementRepository>().seedSupplements(today)
    }
}

private val seedProfile = Profile(
    sex = Sex.Male,
    age = 32,
    heightCm = 178.0,
    weightKg = 82.4,
    activityLevel = ActivityLevel.Moderate,
    goal = Goal.Lose,
    targetWeightKg = 74.0,
    dietaryPreference = DietaryPreference.None,
    preferredUnit = UnitSystem.Metric,
    photoReminderOn = true,
    // On, unlike the shipping default, so the goal notification scheduled by the running fast in
    // [seedFasting] actually fires on a debug install.
    fastingRemindersOn = true,
)

private suspend fun ProgressRepository.seedProgress(today: Long) {
    // Deterministic noise: same fixture every reinstall, so a screenshot diff isn't chasing dice.
    val random = Random(seed = 7)
    // 90 days so 1M and 3M differ visibly; 6M/1Y then show the same series with more empty runway.
    for (daysAgo in 89 downTo 0) {
        val trend = 87.6 - (89 - daysAgo) * 0.058
        upsertWeightEntry(
            WeightEntry(
                dateEpochDay = today - daysAgo,
                weightKg = round1(trend + random.nextDouble(-0.4, 0.4)),
                note = if (daysAgo == 0) "Morning, after gym" else "",
            ),
        )
    }

    // Six monthly-ish checkpoints per part, each drifting toward the goal at its own rate.
    val measurements = mapOf(
        MeasurementPart.Chest to (104.0 to -0.9),
        MeasurementPart.Waist to (94.5 to -1.6),
        MeasurementPart.Hips to (101.0 to -0.7),
        MeasurementPart.Arms to (35.5 to 0.2),
        MeasurementPart.Thighs to (60.0 to -0.5),
    )
    measurements.forEach { (part, start) ->
        val (startCm, perStep) = start
        repeat(6) { step ->
            upsertMeasurementEntry(
                MeasurementEntry(
                    part = part,
                    dateEpochDay = today - (5 - step) * 18L,
                    valueCm = round1(startCm + perStep * step),
                ),
            )
        }
    }

    // Flat-colour stand-ins — real progress photos come from the camera flow; these just need to be
    // three distinguishable images at spread dates so before/after comparison has something to show.
    listOf(
        Triple(84L, 0xFF8D6E63.toInt(), 87.6),
        Triple(42L, 0xFF4E7C8A.toInt(), 85.1),
        Triple(3L, 0xFF6D8B4E.toInt(), 82.6),
    ).forEach { (daysAgo, color, weightKg) ->
        addPhoto(
            bitmap = Bitmap.createBitmap(720, 1280, Bitmap.Config.ARGB_8888).apply { eraseColor(color) },
            dateEpochDay = today - daysAgo,
            weightKg = weightKg,
        )
    }
}

/** Ten days of hydration, today deliberately part-way through so the card shows a partly-filled
 * row rather than an all-or-nothing one. */
/** Every other day, so the streak sees exercise-only days too. */
private suspend fun ExerciseRepository.seedExercise(today: Long) {
    val sessions = listOf(
        ExerciseType.Run to 32,
        ExerciseType.Strength to 45,
        ExerciseType.Cycle to 50,
        ExerciseType.Yoga to 40,
        ExerciseType.Walk to 25,
    )
    sessions.forEachIndexed { index, (type, minutes) ->
        addEntry(
            ExerciseEntry(
                dateEpochDay = today - index * 2L,
                type = type,
                minutes = minutes,
                burnedKcal = estimateBurnedKcal(type, minutes, seedProfile.weightKg),
            ),
        )
    }
}

/**
 * Twenty days of reflections with two deliberate holes: day 6 is skipped entirely so the chart
 * has a real gap to draw, and day 3 records a mood without an energy so the tab's two averages
 * report different denominators.
 */
private suspend fun MoodRepository.seedMood(today: Long) {
    val random = Random(seed = 13)
    for (daysAgo in 19 downTo 0) {
        if (daysAgo == 6) continue
        upsertDay(
            MoodDay(
                dateEpochDay = today - daysAgo,
                mood = random.nextInt(2, 6),
                energy = if (daysAgo == 3) 0 else random.nextInt(2, 6),
            ),
        )
    }
}

/**
 * Three supplements and twenty days of ticks.
 *
 * Written through [SupplementRepository.upsertSupplement]/[SupplementRepository.upsertDay] rather
 * than `addSupplement`/`setTakenToday`, which stamp today and generate their own ids — a backdated
 * log needs both fixed. Day 8 is skipped entirely so the adherence chart has a real gap, day 4 is
 * a zero across the board (a day seen and missed, which the chart draws differently from a gap),
 * and the creatine rows before day 12 carry `dueTimes = 3` against a supplement that now says 2,
 * so the snapshot rule is visible on the chart rather than only in a test.
 */
private suspend fun SupplementRepository.seedSupplements(today: Long) {
    val supplements = listOf(
        Supplement(id = 1, name = "Vitamin D", dose = "2000 IU", timesPerDay = 1, createdAt = 1),
        Supplement(id = 2, name = "Creatine", dose = "5 g", timesPerDay = 2, createdAt = 2),
        Supplement(id = 3, name = "Magnesium", dose = "300 mg", timesPerDay = 1, createdAt = 3),
    )
    supplements.forEach { upsertSupplement(it) }

    val random = Random(seed = 23)
    for (daysAgo in 19 downTo 0) {
        if (daysAgo == 8) continue
        supplements.forEach { supplement ->
            val due = if (supplement.id == 2L && daysAgo >= 12) 3 else supplement.timesPerDay
            upsertDay(
                SupplementDay(
                    dateEpochDay = today - daysAgo,
                    supplementId = supplement.id,
                    taken = if (daysAgo == 4) 0 else random.nextInt(0, due + 1),
                    dueTimes = due,
                ),
            )
        }
    }
}

/**
 * Twenty days of completed fasts plus one still running.
 *
 * Lengths straddle the 16-hour goal on purpose so the Progress chart has bars on both sides of its
 * goal line and "Goals hit" reads something other than N of N. Two days are skipped so the sparse
 * series has real gaps, and one fast carries an 18-hour goal to prove the per-row snapshot is what
 * the chart judges each bar against rather than the profile's current target.
 *
 * The running fast is written through [FastingRepository.upsertSession] rather than `start()`,
 * which stamps `System.currentTimeMillis()` and could only ever produce a zero-length fast. It is
 * backdated to two minutes short of its goal, which is what makes the goal notification testable
 * on a debug install without sitting through sixteen hours.
 */
private suspend fun FastingRepository.seedFasting(today: Long) {
    val random = Random(seed = 17)
    for (daysAgo in 20 downTo 1) {
        if (daysAgo == 5 || daysAgo == 12) continue
        val goalHours = if (daysAgo == 9) 18 else 16
        val hours = random.nextInt(13, 20)
        // Ended at noon, so the row lands on the day it ended even in a zone whose offset pushes
        // local midnight across a UTC boundary.
        val endMillis = epochDayStartMillis(today - daysAgo) + 12 * HOUR_MILLIS
        upsertSession(
            FastSession(
                startMillis = endMillis - hours * HOUR_MILLIS,
                endMillis = endMillis,
                goalHours = goalHours,
            ),
        )
    }
    upsertSession(
        FastSession(
            startMillis = System.currentTimeMillis() - 16 * HOUR_MILLIS + 2 * MINUTE_MILLIS,
            endMillis = null,
            goalHours = 16,
        ),
    )
}

private const val HOUR_MILLIS = 3_600_000L
private const val MINUTE_MILLIS = 60_000L

private suspend fun WaterRepository.seedWater(today: Long) {
    val random = Random(seed = 11)
    for (daysAgo in 9 downTo 1) {
        upsertDay(WaterDay(dateEpochDay = today - daysAgo, glasses = random.nextInt(5, 10)))
    }
    upsertDay(WaterDay(dateEpochDay = today, glasses = 4))
}

private suspend fun FoodRepository.seedFood(today: Long) {
    // Five menus, rotated across the 90-day window below. Only today's shows in the diary screen;
    // the rest feed the Nutrition trend, export, and any future date-scrolling.
    val days = listOf(
        listOf(
            food("Oatmeal with banana", MealType.Breakfast, 1.0, "bowl", 320, 11, 58, 6),
            food("Black coffee", MealType.Breakfast, 1.0, "cup", 5, 0, 1, 0),
            food("Grilled chicken salad", MealType.Lunch, 1.0, "plate", 480, 42, 18, 26),
            food("Greek yogurt", MealType.Snacks, 170.0, "g", 145, 17, 9, 4),
            food("Salmon with rice", MealType.Dinner, 1.0, "plate", 610, 38, 62, 22),
        ),
        listOf(
            food("Scrambled eggs", MealType.Breakfast, 3.0, "egg", 270, 19, 2, 20),
            food("Wholegrain toast", MealType.Breakfast, 2.0, "slice", 180, 7, 30, 3),
            food("Beef burrito", MealType.Lunch, 1.0, "burrito", 720, 34, 78, 29),
            food("Apple", MealType.Snacks, 1.0, "medium", 95, 0, 25, 0),
            food("Chicken adobo with rice", MealType.Dinner, 1.0, "plate", 650, 40, 70, 20),
        ),
        listOf(
            food("Protein shake", MealType.Breakfast, 400.0, "ml", 240, 30, 18, 4),
            food("Tuna sandwich", MealType.Lunch, 1.0, "sandwich", 430, 28, 44, 14),
            food("Mixed nuts", MealType.Snacks, 40.0, "g", 250, 8, 8, 22),
            food("Vegetable stir-fry", MealType.Dinner, 1.0, "plate", 390, 15, 48, 15),
        ),
        listOf(
            food("Pancakes with syrup", MealType.Breakfast, 3.0, "pancake", 520, 10, 84, 16),
            food("Caesar salad", MealType.Lunch, 1.0, "bowl", 360, 12, 20, 26),
            food("Dark chocolate", MealType.Snacks, 30.0, "g", 165, 2, 14, 11),
            food("Pork sinigang", MealType.Dinner, 1.0, "bowl", 480, 32, 24, 27),
        ),
        listOf(
            food("Fried rice with egg", MealType.Breakfast, 1.0, "plate", 540, 16, 72, 20),
            food("Ramen", MealType.Lunch, 1.0, "bowl", 690, 26, 84, 28),
            food("Banana", MealType.Snacks, 1.0, "medium", 105, 1, 27, 0),
            food("Grilled tilapia with vegetables", MealType.Dinner, 1.0, "plate", 420, 39, 22, 18),
        ),
    )
    // 90 days, same window as seedProgress, so 1M and 3M differ visibly on the Nutrition tab.
    // Every 11th day is skipped: the chart's gap handling and the "averaged over N logged days"
    // caption both need a real gap to show.
    for (daysAgo in 0..89) {
        if (daysAgo > 0 && daysAgo % 11 == 0) continue
        days[daysAgo % days.size].forEach { addEntry(it.copy(dateEpochDay = today - daysAgo)) }
    }
}

private fun food(
    name: String,
    mealType: MealType,
    portionAmount: Double,
    portionUnit: String,
    calories: Int,
    proteinG: Int,
    carbsG: Int,
    fatG: Int,
) = FoodEntry(
    name = name,
    mealType = mealType,
    portionAmount = portionAmount,
    portionUnit = portionUnit,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
)
