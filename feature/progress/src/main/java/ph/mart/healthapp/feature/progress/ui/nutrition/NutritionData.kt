package ph.mart.healthapp.feature.progress.ui.nutrition

import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.Nutrients
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
)
