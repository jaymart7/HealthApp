package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.dailyTotals
import ph.mart.healthapp.core.data.health.dayBurnedKcal
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.designsystem.component.ShareImageSheet
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.diary.FoodUiState
import ph.mart.healthapp.feature.food.ui.diary.dayBudgetKcal
import ph.mart.healthapp.feature.food.ui.shared.SERVING_UNIT

/**
 * The day as a picture: preview-then-share, the recap's contract, with [ShareImageSheet] supplying
 * the ground, the brand footer and the Share button.
 *
 * Two things about the card are deliberate.
 *
 * **The date is absolute**, where the screen above it says "Today". `diaryDateLabel` is right on a
 * screen the user is looking at now; a PNG outlives the day it was made, and "Today" in a chat
 * thread tomorrow names the wrong day.
 *
 * **Meal totals, never food names.** A shared image is read by people the diary was never written
 * for. The subtotal is the part of a day that says how it went; what was actually eaten stays on
 * the phone, which is the same answer the app gives everywhere else it exports something.
 *
 * [DiarySummaryBar] is rendered verbatim, expanded, with exactly the arguments the diary passes it
 * — the figures in the image are the figures on the screen or the card is lying.
 */
@Composable
internal fun ShareDaySheet(uiState: FoodUiState, targets: DailyTargets, onDismiss: () -> Unit) {
    ShareImageSheet(fileName = "fitpulse-day.png", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatEpochDay(uiState.selectedDate),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            DiarySummaryBar(
                consumed = uiState.entries.dailyTotals(),
                goalKcal = uiState.dayBudgetKcal(targets),
                proteinGoalG = targets.proteinG,
                carbsGoalG = targets.carbsG,
                fatGoalG = targets.fatG,
                nutrientTargets = uiState.nutrientTargets,
                burnedKcal = dayBurnedKcal(uiState.exercise, uiState.steps),
                exerciseCredited = uiState.addExerciseToBudget,
            )
            MealTotals(entries = uiState.entries)
        }
    }
}

/** One line per meal that has food in it. A meal logged nothing into is absent rather than a
 * "0 kcal" row: four of those would be the day's four absences restated. */
@Composable
private fun MealTotals(entries: List<FoodEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        MealType.entries.forEach { mealType ->
            val kcal = entries.filter { it.mealType == mealType }.dailyTotals().calories
            if (kcal > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(mealType.labelRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.food_section_kcal, kcal),
                        style = MaterialTheme.typography.bodyMedium.tabularNums,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ShareDaySheetPreview() {
    AppTheme {
        ShareDaySheet(
            uiState = FoodUiState(
                selectedDate = 20_690,
                entries = PREVIEW_ENTRIES,
                targets = PREVIEW_TARGETS,
            ),
            targets = PREVIEW_TARGETS,
            onDismiss = {},
        )
    }
}

private val PREVIEW_TARGETS =
    DailyTargets(calories = 2200, proteinG = 165, carbsG = 220, fatG = 73, floor = 1500)

private val PREVIEW_ENTRIES = listOf(
    FoodEntry(
        name = "Oats and berries",
        dateEpochDay = 20_690,
        mealType = MealType.Breakfast,
        portionAmount = 1.0,
        portionUnit = SERVING_UNIT,
        calories = 420,
        proteinG = 18,
        carbsG = 62,
        fatG = 11,
    ),
    FoodEntry(
        name = "Chicken rice bowl",
        dateEpochDay = 20_690,
        mealType = MealType.Lunch,
        portionAmount = 1.0,
        portionUnit = SERVING_UNIT,
        calories = 640,
        proteinG = 48,
        carbsG = 71,
        fatG = 18,
    ),
)
