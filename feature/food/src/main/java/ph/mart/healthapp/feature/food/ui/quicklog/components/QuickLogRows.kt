package ph.mart.healthapp.feature.food.ui.quicklog.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.RecognitionConfidence
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.water.waterVolumeLabel
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.component.formatOneDecimal
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.Motion
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.components.ConfidenceChip
import ph.mart.healthapp.feature.food.ui.shared.components.PortionControl
import ph.mart.healthapp.feature.food.ui.shared.components.formatPortion
import ph.mart.healthapp.feature.food.ui.shared.withPortionAmount

/** A unit symbol, not copy — CLAUDE.md's rule for kcal, g and the rest. */
private const val KCAL = "kcal"

/**
 * The foods, as one card: which meal they are, each row with its figures, and the total.
 *
 * **A card, where it used to be rows on the sheet.** Four kinds of row in one flat list could only
 * be told apart by their value column, and the meal slot floated under all of them though only the
 * food was ever filed in it. Now the slot heads the only card it applies to, and the activities, the
 * water and the weigh-in are [OtherCard] beneath it (the handoff's A5 and A7).
 *
 * The slot is the app's own [SegmentedToggle] rather than M3's outlined segmented button the handoff
 * drew — the same single-select idiom every other screen here uses, reused as it is.
 *
 * Tapping a row's body opens the shared [PortionControl] under it — one row at a time, and the open
 * row takes a `surfaceContainerHigh` fill so the control reads as belonging to it. The ✕ is its own
 * 48dp node beside the body, so a mistap can never remove what it meant to adjust.
 */
@Composable
internal fun FoodCard(
    foods: List<AddEntryForm>,
    mealType: MealType,
    expandedIndex: Int?,
    onMealTypeSelect: (MealType) -> Unit,
    onToggle: (Int) -> Unit,
    onChange: (Int, AddEntryForm) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(bottom = 4.dp)) {
            SegmentedToggle(
                options = MealType.entries.map { stringResource(it.labelRes) },
                selectedIndex = mealType.ordinal,
                onSelect = { onMealTypeSelect(MealType.entries[it]) },
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 4.dp),
            )
            foods.forEachIndexed { index, food ->
                if (index > 0) RowDivider(start = 16.dp)
                FoodRow(
                    food = food,
                    expanded = expandedIndex == index,
                    onToggle = { onToggle(index) },
                    onChange = { onChange(index, it) },
                    onRemove = { onRemove(index) },
                )
            }
            if (foods.size > 1) {
                RowDivider(start = 16.dp)
                TotalRow(kcal = foods.sumOf { it.calories ?: 0 })
            }
        }
    }
}

/**
 * One food: name and portion (with the model's doubt beside it) on the left, calories and the three
 * macros in their fixed colours on the right. Inset 4dp inside the card so the open row's fill has
 * an edge; the body's own 12dp then puts the text on the card's 16dp line.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FoodRow(
    food: AddEntryForm,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (AddEntryForm) -> Unit,
    onRemove: () -> Unit,
) {
    val openFill = MaterialTheme.colorScheme.surfaceContainerHigh
    // From the same tone at zero alpha, not `Transparent` — a fade out of black would dip dark.
    val fill by animateColorAsState(
        targetValue = if (expanded) openFill else openFill.copy(alpha = 0f),
        animationSpec = tween(QuickLogMotion.Swap, easing = Motion.Standard),
        label = "openRow",
    )
    val requester = remember { BringIntoViewRequester() }
    val control = remember { MutableTransitionState(expanded) }.apply { targetState = expanded }
    // Once it has finished opening, and only then: a control that already fits is left where it is,
    // and one that would sit under the bar is scrolled just far enough to clear it.
    LaunchedEffect(control.isIdle, control.currentState) {
        if (control.isIdle && control.currentState) requester.bringIntoView()
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(fill)
            .bringIntoViewRequester(requester),
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClickLabel = stringResource(R.string.food_quick_adjust), onClick = onToggle)
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    Text(
                        text = food.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            // A figure and its unit, as the diary row prints them.
                            text = "${food.portionAmount.formatPortion()} ${food.portionUnit}",
                            style = MaterialTheme.typography.bodyMedium.tabularNums,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    KcalFigure(kcal = food.calories ?: 0)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MacroFigure("P", food.proteinG ?: 0, MaterialTheme.colorScheme.primary)
                        MacroFigure("C", food.carbsG ?: 0, MaterialTheme.colorScheme.tertiary)
                        MacroFigure("F", food.fatG ?: 0, MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            RemoveButton(label = food.name, onRemove = onRemove)
        }
        AnimatedVisibility(
            visibleState = control,
            enter = expandVertically(tween(QuickLogMotion.Enter, easing = Motion.EmphasizedDecelerate)) +
                fadeIn(tween(QuickLogMotion.Swap, delayMillis = QuickLogMotion.Enter - QuickLogMotion.Swap)),
            exit = shrinkVertically(tween(QuickLogMotion.Enter, easing = Motion.EmphasizedAccelerate)) +
                fadeOut(tween(QuickLogMotion.Fade)),
        ) {
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
                controlColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
            )
        }
    }
}

/** "Total" and the sum, its figure on the kcal column's line — the ✕ column's 48dp is the end inset. */
@Composable
private fun TotalRow(kcal: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .padding(start = 12.dp, top = 12.dp, end = 48.dp, bottom = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.food_quick_total_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        KcalFigure(kcal = kcal, weight = FontWeight.SemiBold)
    }
}

/**
 * The activities, the water and the weigh-in: not food and not filed in a meal, so a card of their
 * own under the food. Each kind has its glyph in a circle, because here the value column is all
 * that told a run from a weigh-in.
 */
@Composable
internal fun OtherCard(
    rows: List<OtherRow>,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            rows.forEachIndexed { index, row ->
                // Past the 40dp glyph and its 16dp gap, so the rule starts under the text.
                if (index > 0) RowDivider(start = 72.dp)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                        ) {
                            Icon(
                                imageVector = row.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                            Text(
                                text = row.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            row.detail?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium.tabularNums,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (row.unsure) ConfidenceChip(label = stringResource(R.string.food_review_check_this))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = row.value,
                                style = MaterialTheme.typography.bodyLarge.tabularNums.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = row.unit,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    RemoveButton(label = row.name, onRemove = row.onRemove)
                }
            }
        }
    }
}

/** One row of [OtherCard], already in words — the card only lays it out. */
internal class OtherRow(
    val icon: ImageVector,
    val name: String,
    val detail: String?,
    val value: String,
    val unit: String,
    val unsure: Boolean,
    val onRemove: () -> Unit,
)

/**
 * The rows [OtherCard] draws, in the review's order: activities, then water, then the weigh-in.
 * [unsure] is the offline match — every row a guess, and each says so.
 */
@Composable
internal fun otherRows(
    exercises: List<ExerciseEntry>,
    waterGlasses: Int?,
    weightKg: Double?,
    unit: UnitSystem,
    unsure: Boolean,
    onRemoveExercise: (Int) -> Unit,
    onRemoveWater: () -> Unit,
    onRemoveWeight: () -> Unit,
): List<OtherRow> = buildList {
    exercises.forEachIndexed { index, exercise ->
        add(
            OtherRow(
                icon = exercise.type.icon(),
                name = stringResource(exercise.type.label),
                detail = listOfNotNull(
                    stringResource(R.string.food_exercise_row, exercise.minutes),
                    exercise.name.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                value = exercise.burnedKcal.toString(),
                unit = stringResource(R.string.food_quick_burned_unit),
                unsure = unsure,
                onRemove = { onRemoveExercise(index) },
            ),
        )
    }
    waterGlasses?.let { glasses ->
        // "750 ml" or "1.2 L" — always a figure, a space and a symbol, so the split is the label's.
        val volume = waterVolumeLabel(glasses, unit)
        add(
            OtherRow(
                icon = AppIcons.Water,
                name = stringResource(R.string.food_quick_water),
                detail = pluralStringResource(R.plurals.food_copy_glasses, glasses, glasses),
                value = volume.substringBefore(' '),
                unit = volume.substringAfter(' '),
                unsure = unsure,
                onRemove = onRemoveWater,
            ),
        )
    }
    weightKg?.let { kg ->
        add(
            OtherRow(
                icon = AppIcons.Weight,
                name = stringResource(R.string.food_quick_weight),
                detail = null,
                value = formatOneDecimal(kg.kgToDisplayUnit(unit)),
                unit = unit.weightUnitLabel(),
                unsure = unsure,
                onRemove = onRemoveWeight,
            ),
        )
    }
}

/** The two activities with a glyph of their own; every other kind is a workout. */
private fun ExerciseType.icon(): ImageVector = when (this) {
    ExerciseType.Run -> AppIcons.Run
    ExerciseType.Walk -> AppIcons.Steps
    else -> AppIcons.Workout
}

/** The ✕: a 20dp glyph, lighter than the row's text, in a full 48dp target. [label] names the row
 * for TalkBack, which otherwise hears "Remove" eight times over and cannot tell which. */
@Composable
private fun RemoveButton(label: String, onRemove: () -> Unit) {
    IconButton(onClick = onRemove) {
        Icon(
            imageVector = AppIcons.Close,
            contentDescription = stringResource(R.string.food_quick_remove, label),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun RowDivider(start: Dp) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = start, end = 16.dp),
    )
}

/** The figure in the body's size and the unit a step down, on one baseline. */
@Composable
private fun KcalFigure(kcal: Int, weight: FontWeight = FontWeight.Medium) {
    Row {
        Text(
            text = kcal.toString(),
            style = MaterialTheme.typography.bodyLarge.tabularNums.copy(fontWeight = weight),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.alignByBaseline(),
        )
        Text(
            text = " $KCAL",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

/** "P 12g" — the whole figure in its macro's colour, the handoff's call for a line this short. */
@Composable
private fun MacroFigure(initial: String, grams: Int, color: Color) {
    Text(
        text = "$initial ${grams}g",
        style = MaterialTheme.typography.labelMedium.tabularNums,
        color = color,
    )
}

private val PREVIEW_FOODS = listOf(
    AddEntryForm(MealType.Breakfast, "Scrambled eggs", 2.0, "egg", 180, 12, 1, 14),
    AddEntryForm(MealType.Breakfast, "Wholemeal toast", 1.0, "slice", 80, 4, 14, 1)
        .copy(confidence = RecognitionConfidence.Low, uncertainAbout = "a slice"),
)

private val PREVIEW_EXERCISES = listOf(
    ExerciseEntry(type = ExerciseType.Run, name = "along the river", minutes = 30, burnedKcal = 343),
)

@PreviewLightDark
@Composable
private fun FoodCardPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            FoodCard(
                foods = PREVIEW_FOODS,
                mealType = MealType.Breakfast,
                expandedIndex = null,
                onMealTypeSelect = {},
                onToggle = {},
                onChange = { _, _ -> },
                onRemove = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** The first row open: its fill, and the stepper under it. */
@PreviewLightDark
@Composable
private fun FoodCardPortionPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            FoodCard(
                foods = PREVIEW_FOODS,
                mealType = MealType.Breakfast,
                expandedIndex = 0,
                onMealTypeSelect = {},
                onToggle = {},
                onChange = { _, _ -> },
                onRemove = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun OtherCardPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            OtherCard(
                rows = otherRows(
                    exercises = PREVIEW_EXERCISES,
                    waterGlasses = 3,
                    weightKg = 72.4,
                    unit = UnitSystem.Metric,
                    unsure = false,
                    onRemoveExercise = {},
                    onRemoveWater = {},
                    onRemoveWeight = {},
                ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** Offline: every row a guess, and each says so. */
@PreviewLightDark
@Composable
private fun OtherCardOfflinePreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            OtherCard(
                rows = otherRows(
                    exercises = PREVIEW_EXERCISES,
                    waterGlasses = 3,
                    weightKg = null,
                    unit = UnitSystem.Imperial,
                    unsure = true,
                    onRemoveExercise = {},
                    onRemoveWater = {},
                    onRemoveWeight = {},
                ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
