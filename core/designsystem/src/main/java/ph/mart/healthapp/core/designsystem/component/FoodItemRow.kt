package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.R
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

enum class FoodItemRowVariant { Display, Result, SearchResult, Editable }

/**
 * One component, mode-switched, per the prototype's [FoodItemRowVariant.Display] (food diary
 * rows) / [FoodItemRowVariant.Editable] (Confirmation screen, Phase 5) split — never forked into
 * two components. [FoodItemRowVariant.Result] is the third: a row in a *search*, where the row is
 * the thing being chosen rather than a record of something already eaten. It differs from
 * [FoodItemRowVariant.Display] in exactly two ways and both follow from that — the calorie figure
 * is the heaviest thing in the row because it is what a picker aims at, and the row is 72dp so a
 * list of them can be scanned rather than read. It draws no container of its own: a list that
 * separates its rows with rules reads as one list, where a stack of cards reads as a stack of
 * things. [proteinG]/[carbsG]/[fatG] are always display-only here; macro editing is a
 * separately-composed [MacroFieldGroup], per the prototype's Confirmation screen layout.
 *
 * [FoodItemRowVariant.SearchResult] is the fourth and the diary-history one: a row that *is* a
 * record of something eaten, like [FoodItemRowVariant.Display], but listed among rows from other
 * days rather than under the meal it belongs to. That is the whole of the difference — it carries
 * the meal slot it was logged in, it marks what the query matched, and it says with a chevron that
 * a tap opens it rather than logging it. [FoodItemRowVariant.Display] is untouched by it: the
 * diary, meal ideas, the recipe builder and the review card are all still the row they were.
 */
@Composable
fun FoodItemRow(
    variant: FoodItemRowVariant,
    name: String,
    portionAmount: Double,
    portionUnit: String,
    calories: Int,
    proteinG: Int,
    carbsG: Int,
    fatG: Int,
    modifier: Modifier = Modifier,
    /** The plate this row was logged from, if the camera flow kept one — a 40dp tile leading the
     * [FoodItemRowVariant.Display] row, and nothing at all in the editable one, where the photo is
     * shown once above the whole form instead of beside one field of it. */
    photoPath: String? = null,
    /** The meal this row was logged in, already resolved — this module never sees `MealType`.
     * [FoodItemRowVariant.SearchResult] only; every other variant sits under a heading that
     * already says it. */
    mealLabel: String? = null,
    /** The query that found this row, marked in [name] where it matched, case-insensitively.
     * [FoodItemRowVariant.SearchResult] only. Blank or absent marks nothing. */
    highlight: String? = null,
    onNameChange: (String) -> Unit = {},
    onPortionAmountChange: (Double) -> Unit = {},
    onPortionUnitChange: (String) -> Unit = {},
    onCaloriesChange: (Int) -> Unit = {},
    // Stays in Kotlin: these values are compared, not just shown — `portionStep` switches on
    // them, and a translated "cup" would silently take the 10-per-tap branch meant for grams.
    portionUnitOptions: List<String> = listOf("g", "oz", "cup"),
) {
    when (variant) {
        FoodItemRowVariant.Display -> DisplayRow(name, portionAmount, portionUnit, calories, proteinG, carbsG, fatG, photoPath, modifier)
        FoodItemRowVariant.Result -> ResultRow(name, portionAmount, portionUnit, calories, proteinG, carbsG, fatG, modifier)
        FoodItemRowVariant.SearchResult -> SearchResultRow(
            name, portionAmount, portionUnit, calories, proteinG, carbsG, fatG, photoPath, mealLabel, highlight, modifier,
        )
        FoodItemRowVariant.Editable -> EditableRow(
            name, portionAmount, portionUnit, calories, portionUnitOptions,
            onNameChange, onPortionAmountChange, onPortionUnitChange, onCaloriesChange, modifier,
        )
    }
}

/**
 * A stored plate, square-cropped and rounded. Null while the decode is in flight and forever if the
 * file is gone — a pruned photo leaves a path-less row, but a file deleted underneath one leaves a
 * path that decodes to nothing, and either way the placeholder tone is what the row shows.
 *
 * [contentDescription] is null on purpose: the row beside it already says the food's name, the
 * portion and the calories, and "photo of grilled chicken" after all of that is noise to a screen
 * reader, not information.
 */
@Composable
fun MealThumbnail(path: String, size: Dp, modifier: Modifier = Modifier) {
    val bitmap = rememberBitmapFromFile(path, THUMB_PX)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * How far one tap of the portion stepper moves, per unit. Ten grams is a sensible nudge; ten cups
 * is not, and ten servings is nonsense — a stepper that steps in the wrong unit is why a seeded
 * recipe row could never be turned into half a portion.
 *
 * **One rule, both steppers.** `PortionControl` kept a second copy that claimed to be this one and
 * wasn't: it stepped ounces by 10 and servings by 10 too. Public rather than internal so there is
 * nowhere left for a third copy to appear. Ounces moved to a half — nobody nudges a portion by ten
 * ounces — and a cup to a quarter, which is a measure people actually own a scoop for.
 */
fun portionStep(unit: String): Double = when (unit) {
    "g" -> 10.0
    "cup" -> 0.25
    // Ounces, servings, and anything a food was logged in that this app does not offer.
    else -> 0.5
}

@Composable
private fun DisplayRow(
    name: String,
    portionAmount: Double,
    portionUnit: String,
    calories: Int,
    proteinG: Int,
    carbsG: Int,
    fatG: Int,
    photoPath: String?,
    modifier: Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // Drawn only when there is one, so a row without a photo is the exact layout it always
        // was — the diary is mostly typed entries and they must not indent to make room for a
        // column three rows in four leave empty.
        photoPath?.let { path ->
            MealThumbnail(path = path, size = 40.dp)
            Spacer(modifier = Modifier.size(12.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = macroLine(portionAmount, portionUnit, proteinG, carbsG, fatG),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.ds_food_calories_value, calories),
            style = MaterialTheme.typography.titleMedium.tabularNums,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun EditableRow(
    name: String,
    portionAmount: Double,
    portionUnit: String,
    calories: Int,
    portionUnitOptions: List<String>,
    onNameChange: (String) -> Unit,
    onPortionAmountChange: (Double) -> Unit,
    onPortionUnitChange: (String) -> Unit,
    onCaloriesChange: (Int) -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppTextField(label = stringResource(R.string.ds_food_name), value = name, onValueChange = onNameChange)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.ds_food_portion), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepperValueField(
                    value = portionAmount.formatPortion(),
                    onValueChange = { onPortionAmountChange(it.toDoubleOrNull() ?: 0.0) },
                    contentDescription = stringResource(R.string.ds_food_portion_amount, portionUnit),
                    decimal = true,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = portionUnit,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StepperButton(
                    symbol = "−",
                    label = stringResource(R.string.ds_food_portion_decrease),
                    onClick = {
                        onPortionAmountChange((portionAmount - portionStep(portionUnit)).coerceAtLeast(0.0))
                    },
                )
                StepperButton(
                    symbol = "+",
                    label = stringResource(R.string.ds_food_portion_increase),
                    onClick = { onPortionAmountChange(portionAmount + portionStep(portionUnit)) },
                )
            }
            // Its own row rather than crammed into the one above: four unit pills, a value field
            // and two steppers do not share 48dp of width, and they share it even less once the
            // system font scale goes up. [SegmentedToggle] is the system's own single-select row —
            // it brings the track that tells the unselected units apart from plain text, the
            // selection semantics, and a width floor that scrolls rather than squeezes.
            SegmentedToggle(
                options = portionUnitOptions,
                selectedIndex = portionUnitOptions.indexOf(portionUnit).coerceAtLeast(0),
                onSelect = { index -> onPortionUnitChange(portionUnitOptions[index]) },
                // This row is the first place the toggle sits on a bottom sheet, which is already
                // surfaceContainerLow — its default track would vanish into the sheet. One step up
                // the tone ladder is how the system separates a surface from what it carries.
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
        }

        NumericStepperField(
            label = stringResource(R.string.ds_food_calories),
            value = calories.toString(),
            unitSuffix = "kcal",
            onIncrement = { onCaloriesChange(calories + 10) },
            onDecrement = { onCaloriesChange((calories - 10).coerceAtLeast(0)) },
            onValueChange = { onCaloriesChange(it.toIntOrNull() ?: 0) },
        )
    }
}

/**
 * A search hit. Same two lines as [DisplayRow] and the same [macroLine] beneath them — one detail
 * line, not two dialects of one — with the calorie figure promoted from `titleMedium` to the
 * largest thing on the row.
 *
 * No photo column: a search hit has no plate behind it, and nothing here is a record of a meal.
 */
@Composable
private fun ResultRow(
    name: String,
    portionAmount: Double,
    portionUnit: String,
    calories: Int,
    proteinG: Int,
    carbsG: Int,
    fatG: Int,
    modifier: Modifier,
) {
    val caloriesSpoken = stringResource(R.string.ds_food_calories_value, calories)
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 72.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = macroLine(portionAmount, portionUnit, proteinG, carbsG, fatG),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Two texts drawn, one thing said: "210" then "kcal" is two announcements for one figure.
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .padding(start = 16.dp)
                .clearAndSetSemantics { contentDescription = caloriesSpoken },
        ) {
            Text(
                text = calories.toString(),
                style = MaterialTheme.typography.titleLarge.tabularNums.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                // Not copy: kcal is kcal in every language this app could ship in.
                text = "kcal",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
        }
    }
}

/**
 * A logged row seen from the history search — the diary's own row, read a long way from the day it
 * belongs to.
 *
 * Three things it carries that [DisplayRow] does not, and each is a fact the diary's own heading
 * would otherwise have supplied: the meal slot, because there is no meal section above it here;
 * the match mark, because the user is scanning for a word rather than reading a day; and the
 * chevron, because a tap here *opens* the row rather than being the end of it.
 *
 * **The photo column is reserved even when empty**, which is the one place this contradicts
 * [DisplayRow]'s rule. That rule is about a day's meal section, where most rows are typed and the
 * whole list would indent for the one that isn't. Here the list is scanned down for a name, and a
 * name that starts in a different place on every fourth row is what breaks that.
 *
 * No container and no radius: the rule beneath each row is what makes a list of them one list. The
 * caller owns the tap, so the state layer lands on the whole row rather than on a card inside it.
 */
@Composable
private fun SearchResultRow(
    name: String,
    portionAmount: Double,
    portionUnit: String,
    calories: Int,
    proteinG: Int,
    carbsG: Int,
    fatG: Int,
    photoPath: String?,
    mealLabel: String?,
    highlight: String?,
    modifier: Modifier,
) {
    val caloriesSpoken = stringResource(R.string.ds_food_calories_value, calories)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Drawn when there is one, reserved when there isn't — see this function's KDoc.
            if (photoPath != null) MealThumbnail(path = photoPath, size = 40.dp) else Spacer(modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = highlighted(name, highlight),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mealLabel != null) {
                        MealChip(label = mealLabel)
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    Text(
                        // The app's one macro line, not a second dialect of it — see [macroLine].
                        text = macroLine(portionAmount, portionUnit, proteinG, carbsG, fatG),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            // Two texts drawn, one thing said — [ResultRow]'s rule, for the same figure.
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .clearAndSetSemantics { contentDescription = caloriesSpoken },
            ) {
                Text(
                    text = calories.toString(),
                    style = MaterialTheme.typography.titleLarge.tabularNums.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    // Not copy: kcal is kcal in every language this app could ship in.
                    text = "kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                )
            }
            Icon(
                imageVector = AppIcons.ChevronRight,
                // Decorative: the row already announces what activating it does.
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 4.dp).size(20.dp),
            )
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** The meal slot a history row was logged in. Quiet by construction — it is context, not the row. */
@Composable
private fun MealChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/**
 * [name] with the first occurrence of [query] marked.
 *
 * The first only, not every one: "Chicken with chicken rice" marked twice is a row that looks like
 * it matched twice as hard. Case-insensitive, because the query is typed and the name was typed by
 * someone else. `primaryContainer` rather than a bold weight — the name is already the heaviest
 * thing on its line, so emphasis has to come from somewhere the line is not already spending.
 */
@Composable
private fun highlighted(name: String, query: String?): AnnotatedString {
    val term = query?.trim().orEmpty()
    val start = if (term.isEmpty()) -1 else name.indexOf(term, ignoreCase = true)
    if (start < 0) return AnnotatedString(name)
    return buildAnnotatedString {
        append(name.substring(0, start))
        withStyle(
            SpanStyle(
                background = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) { append(name.substring(start, start + term.length)) }
        append(name.substring(start + term.length))
    }
}

/**
 * "150 g · P 32g · C 2g · F 8g", with each macro's initial in that macro's own colour.
 *
 * The app has a fixed colour for protein, carbs and fat and spends it on bars and charts, while
 * the most-repeated element in the whole product — a diary row — said all three in the same grey.
 * Colouring the *letter* rather than the number is what keeps the line quiet: the letter is the
 * label the Fixed Macro Rule requires beside every macro colour, so the colour lands exactly on
 * the glyph that already carries the meaning, and the figures stay one uniform weight to scan
 * down. SemiBold because sage and moss are neighbours at 12sp, and a marker has to read as chosen.
 *
 * Public because the review card draws the same line on a row this file does not own: a
 * collapsed estimate, whose calories sit at the end in their own type. A second copy of the
 * separator and the three colour assignments is a second place for the Fixed Macro Rule to drift.
 */
@Composable
fun macroLine(
    portionAmount: Double,
    portionUnit: String,
    proteinG: Int,
    carbsG: Int,
    fatG: Int,
): AnnotatedString = buildAnnotatedString {
    append("${portionAmount.formatPortion()} $portionUnit")
    macroToken("P", proteinG, MaterialTheme.colorScheme.primary)
    macroToken("C", carbsG, MaterialTheme.colorScheme.tertiary)
    macroToken("F", fatG, MaterialTheme.colorScheme.secondary)
}

private fun AnnotatedString.Builder.macroToken(initial: String, grams: Int, color: Color) {
    append(" · ")
    withStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold)) { append(initial) }
    append(" ${grams}g")
}

private fun Double.formatPortion(): String = if (this == this.toLong().toDouble()) toLong().toString() else toString()

@PreviewLightDark
@Composable
private fun FoodItemRowDisplayPreview() {
    AppTheme {
        Surface {
            FoodItemRow(
                variant = FoodItemRowVariant.Display,
                name = "Grilled chicken breast",
                portionAmount = 150.0,
                portionUnit = "g",
                calories = 210,
                proteinG = 32,
                carbsG = 2,
                fatG = 8,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/**
 * The history row: two of them, so the reserved photo column is visible as the thing that keeps
 * the two names starting in the same place. The query is "chick", marked in both.
 */
@PreviewLightDark
@Composable
private fun FoodItemRowSearchResultPreview() {
    AppTheme {
        Surface {
            Column {
                FoodItemRow(
                    variant = FoodItemRowVariant.SearchResult,
                    name = "Grilled chicken breast",
                    portionAmount = 150.0,
                    portionUnit = "g",
                    calories = 412,
                    proteinG = 38,
                    carbsG = 0,
                    fatG = 9,
                    mealLabel = "Lunch",
                    highlight = "chick",
                )
                FoodItemRow(
                    variant = FoodItemRowVariant.SearchResult,
                    name = "Chicken & rice bowl",
                    portionAmount = 1.0,
                    portionUnit = "bowl",
                    calories = 520,
                    proteinG = 34,
                    carbsG = 61,
                    fatG = 14,
                    mealLabel = "Dinner",
                    highlight = "chick",
                )
            }
        }
    }
}

/** The search hit: no card, no photo, and the calorie figure carrying the row. */
@PreviewLightDark
@Composable
private fun FoodItemRowResultPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                FoodItemRow(
                    variant = FoodItemRowVariant.Result,
                    name = "Grilled chicken breast",
                    portionAmount = 100.0,
                    portionUnit = "g",
                    calories = 165,
                    proteinG = 31,
                    carbsG = 0,
                    fatG = 4,
                )
                FoodItemRow(
                    variant = FoodItemRowVariant.Result,
                    name = "Nutella hazelnut spread with cocoa, 400g jar",
                    portionAmount = 100.0,
                    portionUnit = "g",
                    calories = 539,
                    proteinG = 6,
                    carbsG = 58,
                    fatG = 31,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun FoodItemRowEditablePreview() {
    AppTheme {
        Surface {
            FoodItemRow(
                variant = FoodItemRowVariant.Editable,
                name = "Grilled chicken breast",
                portionAmount = 150.0,
                portionUnit = "g",
                calories = 210,
                proteinG = 32,
                carbsG = 2,
                fatG = 8,
                onNameChange = {},
                onPortionAmountChange = {},
                onPortionUnitChange = {},
                onCaloriesChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** With a kept plate: the row the camera flow leaves behind. The preview's path decodes to
 * nothing, so what it shows is the placeholder tile — which is also what a row whose file went
 * missing shows on a device. */
@PreviewLightDark
@Composable
private fun FoodItemRowPhotoPreview() {
    AppTheme {
        Surface {
            FoodItemRow(
                variant = FoodItemRowVariant.Display,
                name = "Chicken adobo",
                portionAmount = 1.0,
                portionUnit = "serving",
                calories = 430,
                proteinG = 28,
                carbsG = 12,
                fatG = 29,
                photoPath = "/preview/none.jpg",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** The seeded-recipe shape: a serving, where the stepper has to move in halves rather than tens. */
@PreviewLightDark
@Composable
private fun FoodItemRowEditableServingPreview() {
    AppTheme {
        Surface {
            FoodItemRow(
                variant = FoodItemRowVariant.Editable,
                name = "Chili",
                portionAmount = 1.0,
                portionUnit = "serving",
                calories = 395,
                proteinG = 32,
                carbsG = 20,
                fatG = 21,
                onNameChange = {},
                onPortionAmountChange = {},
                onPortionUnitChange = {},
                onCaloriesChange = {},
                portionUnitOptions = listOf("g", "oz", "cup", "serving"),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
