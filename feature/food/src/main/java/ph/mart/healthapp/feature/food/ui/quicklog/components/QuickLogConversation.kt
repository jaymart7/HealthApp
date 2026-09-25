package ph.mart.healthapp.feature.food.ui.quicklog.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.RecognitionConfidence
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.components.ConfidenceChip
import ph.mart.healthapp.feature.food.ui.shared.components.MealTypeChipRow
import ph.mart.healthapp.feature.food.ui.shared.components.PortionControl
import ph.mart.healthapp.feature.food.ui.shared.withPortionAmount

/**
 * Everything above the field: the follow-up the model asked, or the rows it came back with, and
 * the one line that says why neither is there.
 *
 * **The question is the AI accent** — `tertiaryContainer` under `onTertiaryContainer` — because it
 * is the model talking, and that pairing is the app's one way of saying so. The user's own last
 * words sit above it, so the question reads as an answer to something rather than out of nowhere.
 *
 * **The rows are a confirmation with one lever.** Most corrections are words in the field ("make
 * it two cups"), which re-reads the whole conversation; the lever is the portion, because nudging
 * one figure is faster than a sentence and needs no request. Tapping a food row opens the shared
 * [PortionControl] under it — one row at a time, talk-to-log's rule — and [withPortionAmount]
 * reprices every macro with it. A row the model was unsure of carries the same [ConfidenceChip]
 * talk-to-log's review draws, quoting the words it could not pin down. Food rows are the diary's
 * own [FoodItemRow]; activities are the diary exercise block's shape, with "burned" said out loud
 * because here they sit beside food.
 *
 * The meal-slot chips are drawn only when there is food to file — an activity has no slot.
 */
@Composable
internal fun QuickLogConversation(
    lastSaid: String?,
    question: String?,
    foods: List<AddEntryForm>,
    exercises: List<ExerciseEntry>,
    mealType: MealType,
    expandedIndex: Int?,
    message: String?,
    onToggleFood: (Int) -> Unit,
    onFoodChange: (Int, AddEntryForm) -> Unit,
    onRemoveFood: (Int) -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onMealTypeSelect: (MealType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier.fillMaxWidth()) {
        if (question != null) {
            if (lastSaid != null) {
                Text(
                    text = stringResource(R.string.food_voice_you_said, lastSaid),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            QuestionBubble(question)
        }

        foods.forEachIndexed { index, food ->
            FoodRow(
                food = food,
                expanded = expandedIndex == index,
                onToggle = { onToggleFood(index) },
                onChange = { onFoodChange(index, it) },
                onRemove = { onRemoveFood(index) },
            )
        }
        if (foods.size > 1) {
            Text(
                text = stringResource(R.string.food_quick_total, foods.sumOf { it.calories ?: 0 }),
                style = MaterialTheme.typography.titleSmall.tabularNums,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        exercises.forEachIndexed { index, exercise ->
            val label = stringResource(exercise.type.label)
            RemovableRow(label = label, onRemove = { onRemoveExercise(index) }) {
                ExerciseRow(exercise = exercise, label = label, modifier = Modifier.weight(1f))
            }
        }

        if (foods.isNotEmpty()) {
            MealTypeChipRow(selected = mealType, onSelect = onMealTypeSelect)
        }

        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The row, its doubt, and — once tapped — its portion. The tap target is the row's body, not the
 * ✕, which stays its own 48dp so a mistap cannot remove what it meant to adjust. */
@Composable
private fun FoodRow(
    food: AddEntryForm,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (AddEntryForm) -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RemovableRow(label = food.name, onRemove = onRemove) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClickLabel = stringResource(R.string.food_quick_adjust), onClick = onToggle),
            ) {
                FoodItemRow(
                    variant = FoodItemRowVariant.Display,
                    name = food.name,
                    portionAmount = food.portionAmount,
                    portionUnit = food.portionUnit,
                    calories = food.calories ?: 0,
                    proteinG = food.proteinG ?: 0,
                    carbsG = food.carbsG ?: 0,
                    fatG = food.fatG ?: 0,
                )
                if (food.confidence == RecognitionConfidence.Low) {
                    ConfidenceChip(
                        label = food.uncertainAbout
                            ?.let { stringResource(R.string.food_review_rough_guess, it) }
                            ?: stringResource(R.string.food_review_check_this),
                    )
                }
            }
        }
        if (expanded) {
            // `SubjectCard`'s wiring exactly: repricing is the point, and a unit switch moves the
            // unit alone. `manualEntry` hides the presets — a parsed portion is the one the user
            // said, not a per-100 g row waiting for a serving. Its caveat is replaced, because
            // "enter the values" is wrong where nothing is entered, and the base is the amount on
            // screen so no "×1.5" is printed against a seed that was never per 100 g.
            PortionControl(
                amount = food.portionAmount,
                unit = food.portionUnit,
                manualEntry = true,
                onAmountChange = { onChange(food.withPortionAmount(it)) },
                onUnitChange = { onChange(food.copy(portionUnit = it)) },
                caveat = stringResource(R.string.food_quick_portion_caveat),
                caveatBaseAmount = food.portionAmount,
            )
        }
    }
}

@Composable
private fun QuestionBubble(question: String) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(16.dp),
        ) {
            Icon(
                imageVector = AppIcons.AiSparkle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = question,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

/** A row and the ✕ that drops it. [label] names the row for TalkBack, which otherwise hears
 * "Remove" eight times over and cannot tell which. */
@Composable
private fun RemovableRow(
    label: String,
    onRemove: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        content()
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = AppIcons.Close,
                contentDescription = stringResource(R.string.food_quick_remove, label),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExerciseRow(exercise: ExerciseEntry, label: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = listOfNotNull(
                    stringResource(R.string.food_exercise_row, exercise.minutes),
                    exercise.name.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.food_quick_burned, exercise.burnedKcal),
            style = MaterialTheme.typography.titleMedium.tabularNums,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private val PREVIEW_FOODS = listOf(
    AddEntryForm(MealType.Breakfast, "Scrambled eggs", 2.0, "egg", 180, 12, 2, 14),
    AddEntryForm(MealType.Breakfast, "Wholemeal toast", 1.0, "slice", 80, 4, 14, 1)
        .copy(confidence = RecognitionConfidence.Low, uncertainAbout = "a slice"),
)

private val PREVIEW_EXERCISES = listOf(
    ExerciseEntry(type = ExerciseType.Run, name = "along the river", minutes = 30, burnedKcal = 343),
)

@PreviewLightDark
@Composable
private fun QuickLogConversationQuestionPreview() {
    AppTheme {
        Surface {
            QuickLogConversation(
                lastSaid = "rice and chicken adobo",
                question = "About how much rice — one cup or two?",
                foods = emptyList(),
                exercises = emptyList(),
                mealType = MealType.Lunch,
                expandedIndex = null,
                message = null,
                onToggleFood = {},
                onFoodChange = { _, _ -> },
                onRemoveFood = {},
                onRemoveExercise = {},
                onMealTypeSelect = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** A meal and a run from one sentence — both kinds land in the same review, and the one guess is
 * tagged. */
@PreviewLightDark
@Composable
private fun QuickLogConversationReviewPreview() {
    AppTheme {
        Surface {
            QuickLogConversation(
                lastSaid = "two eggs and toast, then a 30 min run along the river",
                question = null,
                foods = PREVIEW_FOODS,
                exercises = PREVIEW_EXERCISES,
                mealType = MealType.Breakfast,
                expandedIndex = null,
                message = null,
                onToggleFood = {},
                onFoodChange = { _, _ -> },
                onRemoveFood = {},
                onRemoveExercise = {},
                onMealTypeSelect = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** A row opened for its portion: the stepper sits under it, and the rest stay closed. */
@PreviewLightDark
@Composable
private fun QuickLogConversationPortionPreview() {
    AppTheme {
        Surface {
            QuickLogConversation(
                lastSaid = null,
                question = null,
                foods = PREVIEW_FOODS,
                exercises = emptyList(),
                mealType = MealType.Breakfast,
                expandedIndex = 0,
                message = null,
                onToggleFood = {},
                onFoodChange = { _, _ -> },
                onRemoveFood = {},
                onRemoveExercise = {},
                onMealTypeSelect = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
