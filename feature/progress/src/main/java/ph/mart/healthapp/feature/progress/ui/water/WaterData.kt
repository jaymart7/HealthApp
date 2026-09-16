package ph.mart.healthapp.feature.progress.ui.water

import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.water.DEFAULT_WATER_GOAL_GLASSES
import ph.mart.healthapp.core.data.water.WaterDay

/**
 * Days with a glass on them, oldest first — a day nobody logged has no row, so the chart draws a
 * gap rather than a zero.
 *
 * [goalGlasses] is the profile's **current** target and only ever moves the chart's dashed line:
 * raising the goal next month prices the next day, it never un-hits one already drawn. That is
 * `FastingUiState`'s rule, and it is the same rule for the same reason.
 *
 * [unit] rides along because a glass is a fixed serving the user reads in their own units —
 * `waterVolumeLabel()` turns the count into "2.0 L" or "64 fl oz" under the figure.
 */
data class WaterUiState(
    val days: List<WaterDay> = emptyList(),
    val goalGlasses: Int = DEFAULT_WATER_GOAL_GLASSES,
    val unit: UnitSystem = UnitSystem.Metric,
)
