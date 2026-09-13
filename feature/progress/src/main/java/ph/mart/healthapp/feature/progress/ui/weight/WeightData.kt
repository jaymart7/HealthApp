package ph.mart.healthapp.feature.progress.ui.weight

import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.progress.WeightEntry

/**
 * The weigh-ins, the calories behind them, and the profile all three of this page's derivations
 * read.
 *
 * The whole [Profile] rides here rather than the four fields the chips and the chart need, because
 * `energyCheckIn()` takes a profile and this is the one subject page that folds one — the same
 * reading `EnergyCheckInUiState` already takes. [dailyNutrition] is here for that fold alone;
 * nothing on this page charts it.
 *
 * The projection and the check-in are **not** here: both are date-dependent, so the screen folds
 * them against `todayEpochDay()` where a recomposition can pick up a day boundary.
 */
data class WeightUiState(
    val entries: List<WeightEntry> = emptyList(),
    val dailyNutrition: List<DayNutrition> = emptyList(),
    val profile: Profile? = null,
)
