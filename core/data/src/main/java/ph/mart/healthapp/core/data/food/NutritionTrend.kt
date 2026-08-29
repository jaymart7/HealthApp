package ph.mart.healthapp.core.data.food

/** The widest [ph.mart.healthapp.core.data.progress.ChartRange], and so the deepest the Nutrition
 * tab ever reads. Anything older stays in the table for export and never leaves Room. */
const val TREND_WINDOW_DAYS = 365

/** One day of the diary rolled up. [isLogged] is what separates "ate nothing worth logging" from
 * "didn't open the app" — the series is zero-filled, so the flag is the only honest signal. */
data class DayNutrition(
    val dateEpochDay: Long,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
) {
    val isLogged: Boolean get() = calories > 0 || proteinG > 0 || carbsG > 0 || fatG > 0
}

/**
 * Dense — one row per day in [fromEpochDay]..[toEpochDay] inclusive, zero-filled where nothing was
 * logged, so a gap in the diary reads as a gap in the chart instead of silently closing up.
 * Per-day summing reuses [dailyTotals]; same fold, one implementation.
 */
fun List<FoodEntry>.dailySeries(fromEpochDay: Long, toEpochDay: Long): List<DayNutrition> {
    val byDate = groupBy { it.dateEpochDay }
    return (fromEpochDay..toEpochDay).map { day ->
        val totals = byDate[day].orEmpty().dailyTotals()
        DayNutrition(
            dateEpochDay = day,
            calories = totals.calories,
            proteinG = totals.proteinG,
            carbsG = totals.carbsG,
            fatG = totals.fatG,
        )
    }
}

data class NutritionAverages(
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val daysLogged: Int,
)

/** Mean over logged days only — averaging the zero-filled gaps in would report a number the user
 * never ate. [NutritionAverages.daysLogged] is shown alongside so a sparse range says so. */
fun List<DayNutrition>.averages(): NutritionAverages {
    val logged = filter { it.isLogged }
    if (logged.isEmpty()) return NutritionAverages(0, 0, 0, 0, 0)
    return NutritionAverages(
        calories = logged.sumOf { it.calories } / logged.size,
        proteinG = logged.sumOf { it.proteinG } / logged.size,
        carbsG = logged.sumOf { it.carbsG } / logged.size,
        fatG = logged.sumOf { it.fatG } / logged.size,
        daysLogged = logged.size,
    )
}
