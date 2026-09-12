package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.dailyTotals
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.diary.CopyDay
import ph.mart.healthapp.feature.food.ui.diary.isEmpty

/**
 * What another day held, ticked down to the parts worth bringing over. The counterpart to the
 * calendar that picked [source] — that sheet answers "which day", this one "which of it".
 *
 * The ticks live here in a `remember` keyed on the source day rather than in `FoodScreenState`:
 * a rotation re-asks a question with every answer still visible on screen, which is the same call
 * [DiarySheets] makes for its two delete confirmations. What would actually cost the user
 * something — the day itself, and what is on it — is in the Orbit container and survives.
 *
 * A part with nothing in it draws disabled rather than being left out, so the sheet reads as a
 * report of that day: an empty Dinner row says the day had no dinner, where a missing one would
 * just look like the sheet had forgotten about dinner.
 */
@Composable
internal fun CopyDaySheet(
    source: CopyDay,
    today: Long,
    onDismiss: () -> Unit,
    onCopy: (meals: Set<MealType>, water: Boolean, exercise: Boolean) -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss) {
        Text(
            // Relative where the diary's own header is — "Copy from Yesterday" is the sentence
            // someone would say. Absolute past that, which is what diaryDateLabel already does.
            text = stringResource(R.string.food_copy_title, diaryDateLabel(source.dateEpochDay, today)),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        if (source.isEmpty) {
            Text(
                text = stringResource(R.string.food_copy_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            SecondaryButton(
                label = stringResource(R.string.food_close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
            return@AppBottomSheet
        }

        Text(
            text = stringResource(R.string.food_copy_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        // Everything the day actually holds starts ticked: the common copy is the whole day, and
        // un-ticking one meal is a shorter path to the rest than ticking three.
        var meals by remember(source.dateEpochDay) {
            mutableStateOf(source.entries.map { it.mealType }.toSet())
        }
        var water by remember(source.dateEpochDay) { mutableStateOf(source.waterGlasses > 0) }
        var exercise by remember(source.dateEpochDay) { mutableStateOf(source.exercise.isNotEmpty()) }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MealType.entries.forEach { mealType ->
                val mealEntries = source.entries.filter { it.mealType == mealType }
                CopyChoice(
                    label = stringResource(mealType.labelRes),
                    detail = stringResource(
                        R.string.food_meal_summary,
                        pluralStringResource(R.plurals.food_items, mealEntries.size, mealEntries.size),
                        mealEntries.dailyTotals().calories,
                    ),
                    checked = mealType in meals,
                    enabled = mealEntries.isNotEmpty(),
                    onToggle = { meals = if (it) meals + mealType else meals - mealType },
                )
            }
            CopyChoice(
                label = stringResource(R.string.food_copy_water),
                detail = pluralStringResource(
                    R.plurals.food_copy_glasses,
                    source.waterGlasses,
                    source.waterGlasses,
                ),
                checked = water,
                enabled = source.waterGlasses > 0,
                onToggle = { water = it },
            )
            CopyChoice(
                label = stringResource(R.string.food_exercise),
                detail = stringResource(
                    R.string.food_meal_summary,
                    pluralStringResource(
                        R.plurals.food_copy_workouts,
                        source.exercise.size,
                        source.exercise.size,
                    ),
                    source.exercise.sumOf { it.burnedKcal },
                ),
                checked = exercise,
                enabled = source.exercise.isNotEmpty(),
                onToggle = { exercise = it },
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                SecondaryButton(
                    label = stringResource(R.string.food_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    label = stringResource(R.string.food_copy_confirm),
                    onClick = { onCopy(meals, water, exercise) },
                    // Nothing ticked is a copy with nothing in it — the same "a control that
                    // can't answer stays out of the way" rule the rest of the app follows.
                    enabled = meals.isNotEmpty() || water || exercise,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** One tickable part of the day. The whole card toggles, the way `DisconnectSheet`'s choices do —
 * a 20dp checkbox is not the target. */
@Composable
private fun CopyChoice(
    label: String,
    detail: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    AppCard(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        onClick = if (enabled) ({ onToggle(!checked) }) else null,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked && enabled, onCheckedChange = onToggle, enabled = enabled)
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun CopyDaySheetPreview() {
    AppTheme {
        CopyDaySheet(
            source = CopyDay(
                dateEpochDay = 20_000,
                entries = listOf(
                    FoodEntry(name = "Greek yogurt", mealType = MealType.Breakfast, portionAmount = 1.0, portionUnit = "cup", calories = 150, proteinG = 20, carbsG = 8, fatG = 4),
                    FoodEntry(name = "Adobo", mealType = MealType.Lunch, portionAmount = 150.0, portionUnit = "g", calories = 320, proteinG = 22, carbsG = 8, fatG = 21),
                ),
                exercise = listOf(ExerciseEntry(type = ExerciseType.Walk, minutes = 40, burnedKcal = 150)),
                waterGlasses = 6,
            ),
            today = 20_001,
            onDismiss = {},
            onCopy = { _, _, _ -> },
        )
    }
}

/** The dead end: a day with nothing on it. */
@PreviewLightDark
@Composable
private fun CopyDaySheetEmptyPreview() {
    AppTheme {
        CopyDaySheet(
            source = CopyDay(dateEpochDay = 20_000),
            today = 20_001,
            onDismiss = {},
            onCopy = { _, _, _ -> },
        )
    }
}
