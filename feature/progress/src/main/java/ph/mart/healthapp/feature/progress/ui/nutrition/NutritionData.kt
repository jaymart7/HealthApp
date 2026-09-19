package ph.mart.healthapp.feature.progress.ui.nutrition

import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.Nutrients
import ph.mart.healthapp.core.data.food.div
import ph.mart.healthapp.core.data.food.plus
import ph.mart.healthapp.core.data.food.WeekBudget
import ph.mart.healthapp.core.data.profile.DailyTargets

/**
 * The one **dense** series on this tab — a row per day for the last year, so the page's window is a
 * plain tail slice rather than the date filter every sparse subject needs.
 *
 * [mealPhotos] are the newest kept plates, capped in the repository, and deliberately unranged: a
 * photo history that thinned out when someone picked "1M" would be lying about what it has.
 *
 * [targets] and [nutrientTargets] are **derived** from the profile rather than stored, which is why
 * the container folds them here instead of the screen — they are the same two calls
 * `ProgressViewModel` makes, from `:core:data/profile`, so the two cannot report different targets.
 */
data class NutritionUiState(
    val dailyNutrition: List<DayNutrition> = emptyList(),
    val mealPhotos: List<FoodEntry> = emptyList(),
    val targets: DailyTargets? = null,
    val nutrientTargets: Nutrients? = null,
    /** This Monday-to-Sunday week's calorie bank, derived in the container for [targets]' reason
     * and from the same `weekBudget()` Home reads — one fold, so the card and the page cannot
     * report different figures. Null with no profile. */
    val weekBudget: WeekBudget? = null,
    /** What each day's ticked supplements carried, keyed by day and sparse — days nobody ticked
     * are absent. Windowed and averaged on the screen, against the same denominator the food
     * averages use. */
    val supplementNutrients: Map<Long, Nutrients> = emptyMap(),
)

/**
 * The average day's supplement figures across [days], over the **same denominator the food
 * averages use** — logged days only.
 *
 * That denominator is the point. A month in which the user logged food on four days and took a
 * multivitamin on thirty would otherwise report a per-day vitamin D figure that no day of theirs
 * actually looked like, sitting in a panel whose other rows are averages of four. A day with
 * supplements but no food is therefore not counted, exactly as it is not counted for calories.
 */
internal fun supplementAverage(days: List<DayNutrition>, byDay: Map<Long, Nutrients>): Nutrients {
    val logged = days.filter { it.isLogged }
    if (logged.isEmpty()) return Nutrients()
    return logged.fold(Nutrients()) { acc, day -> acc + (byDay[day.dateEpochDay] ?: Nutrients()) } /
        logged.size
}
