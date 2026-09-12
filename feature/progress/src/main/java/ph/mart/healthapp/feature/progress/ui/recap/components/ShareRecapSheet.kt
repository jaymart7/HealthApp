package ph.mart.healthapp.feature.progress.ui.recap.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import ph.mart.healthapp.core.data.food.NutritionAverages
import ph.mart.healthapp.core.data.mood.MoodAverages
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.WeightTrendDisplay
import ph.mart.healthapp.core.data.progress.GoalProjection
import ph.mart.healthapp.core.designsystem.component.ShareImageSheet
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.shared.BestDay
import ph.mart.healthapp.feature.progress.ui.shared.components.RecapCard
import ph.mart.healthapp.feature.progress.ui.shared.Recap
import ph.mart.healthapp.feature.progress.ui.shared.RecapPeriod

/**
 * Preview-then-share for the recap: the sheet shows exactly the PNG that leaves the app, which is
 * why the branding can exist here without ever appearing on the Progress screen.
 *
 * [RecapCard] is rendered verbatim — every figure, and every colour rule behind it, stays the
 * card's. The ground, the brand footer and the Share button are [ShareImageSheet]'s, along with
 * the reason the image is one card rather than the whole scrolling recap page.
 */
@Composable
internal fun ShareRecapSheet(
    recap: Recap,
    goal: Goal?,
    unit: UnitSystem,
    projection: GoalProjection?,
    onDismiss: () -> Unit,
) {
    ShareImageSheet(fileName = "fitpulse-recap.png", onDismiss = onDismiss) {
        RecapCard(recap = recap, goal = goal, unit = unit, projection = projection)
    }
}

@PreviewLightDark
@Composable
private fun ShareRecapSheetPreview() {
    AppTheme {
        ShareRecapSheet(
            recap = Recap(
                period = RecapPeriod.Week,
                daysLogged = 6,
                averages = NutritionAverages(1940, 141, 196, 68, daysLogged = 5),
                targets = DailyTargets(calories = 2000, proteinG = 150, carbsG = 200, fatG = 67, floor = 1500),
                weightTrend = WeightTrendDisplay(currentKg = 76.0, deltaKg = -0.8, hasPrior = true),
                moodAverages = MoodAverages(mood = 3.6, energy = 3.1, daysLogged = 6),
                bestDay = BestDay(dateEpochDay = 20_690, calories = 1985),
            ),
            goal = Goal.Lose,
            unit = UnitSystem.Metric,
            projection = GoalProjection(
                goalWeightKg = 72.0,
                kgPerWeek = -0.4,
                targetEpochDay = 20_760,
                reached = false,
            ),
            onDismiss = {},
        )
    }
}
