package ph.mart.healthapp.feature.food.ui.diary.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.Nutrients
import ph.mart.healthapp.core.designsystem.component.MacroBar
import ph.mart.healthapp.core.designsystem.component.MacroFieldGroup
import ph.mart.healthapp.core.designsystem.component.MealThumbnail
import ph.mart.healthapp.core.designsystem.component.MicronutrientInputGroup
import ph.mart.healthapp.core.designsystem.component.formatTimeOfDay
import ph.mart.healthapp.core.designsystem.component.rememberBitmapFromFile
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.components.CardLabel
import ph.mart.healthapp.feature.food.ui.shared.components.PhotoViewerOverlay
import ph.mart.healthapp.feature.food.ui.shared.components.SubjectCard

/** How far the content has to scroll before the bar takes over the food's name — one card's worth,
 * the same handover the review screen makes. */
private const val TITLE_HANDOVER_PX = 240

/**
 * "How much?" — reached from "Add it yourself", from any pick, or straight away when the sheet was
 * opened to correct a logged row.
 *
 * It is the barcode flow's review screen in sheet dialect: [SubjectCard] carrying the name, the
 * portion and the calories, the macros as three tiles under it, the micronutrients behind a
 * disclosure. One step up the tone ladder, because the sheet is already `surfaceContainerLow` and
 * the card on the review screen sits on plain `surface`.
 *
 * **[editing] removes everything that writes a *new* log**, which on this state is one thing: the
 * "Save as my food" switch. What it adds is what a correction needs and an addition does not — a
 * subtitle naming the row being corrected, and, when the row came from the camera, the plate with
 * an explanation of what happens to it. That photo used to be a naked 64dp square above the fields
 * with nothing saying why it was there.
 */
@Composable
internal fun AddEntryFormView(
    form: AddEntryForm,
    mealType: MealType,
    editing: Boolean,
    seededFromProduct: Boolean,
    scrolledPastTitle: Boolean,
    onFormChange: (AddEntryForm) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    loggedAt: Long? = null,
) {
    val handedOver = scrolledPastTitle && form.name.isNotBlank()
    Column(modifier = modifier.fillMaxWidth()) {
        FormTopBar(
            mealType = mealType,
            editing = editing,
            name = form.name,
            handedOver = handedOver,
            loggedAt = loggedAt,
            onClose = onClose,
        )
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (editing) form.photoPath?.let { PhotoRow(path = it) }

            SubjectCard(
                form = form,
                // Nothing seeded it, or what seeded it was already priced for the portion shown —
                // either way the per-100 g caveat would be a claim about arithmetic nobody did, and
                // there is nothing to preset 50 g and 150 g *against*.
                manualEntry = !seededFromProduct,
                onFormChange = onFormChange,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                controlColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CardLabel(stringResource(R.string.food_macros))
                MacroBar(
                    proteinG = form.proteinG ?: 0,
                    carbsG = form.carbsG ?: 0,
                    fatG = form.fatG ?: 0,
                )
                MacroFieldGroup(
                    proteinG = form.proteinG,
                    carbsG = form.carbsG,
                    fatG = form.fatG,
                    onProteinChange = { onFormChange(form.copy(proteinG = it)) },
                    onCarbsChange = { onFormChange(form.copy(carbsG = it)) },
                    onFatChange = { onFormChange(form.copy(fatG = it)) },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
            }

            MicronutrientInputGroup(
                fiberG = form.nutrients.fiberG.takeIf { it > 0 },
                sugarG = form.nutrients.sugarG.takeIf { it > 0 },
                sodiumMg = form.nutrients.sodiumMg.takeIf { it > 0 },
                onFiberChange = { onFormChange(form.copy(nutrients = form.nutrients.copy(fiberG = it ?: 0))) },
                onSugarChange = { onFormChange(form.copy(nutrients = form.nutrients.copy(sugarG = it ?: 0))) },
                onSodiumChange = { onFormChange(form.copy(nutrients = form.nutrients.copy(sodiumMg = it ?: 0))) },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

/** Whether the content has scrolled far enough for the bar to take over the food's name. Hoisted so
 * the sheet, which owns the scroll, can answer it — see [TITLE_HANDOVER_PX]. */
internal fun scrolledPastTitle(scrollPx: Int): Boolean = scrollPx > TITLE_HANDOVER_PX

/**
 * The title — which becomes the food's name once the card carrying it has scrolled away, so the bar
 * is never the only thing on screen that has forgotten what is being edited — and, at its right, the
 * ✕ every other sheet in the app closes with. It was a leading back arrow, which is the one
 * affordance this state does not need drawn: system back already steps Form → Browse, and the icon
 * in the corner is the sheet's escape, not a level.
 */
@Composable
private fun FormTopBar(
    mealType: MealType,
    editing: Boolean,
    name: String,
    handedOver: Boolean,
    loggedAt: Long?,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when {
                    handedOver -> name
                    editing -> stringResource(R.string.food_edit_meal_entry, stringResource(mealType.labelRes))
                    else -> stringResource(R.string.food_add_to, stringResource(mealType.labelRes))
                },
                style = if (handedOver) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.titleLarge
                },
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            // 48dp, the size `AppBottomSheet` draws its own close at, for the same reason.
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = AppIcons.Close,
                    contentDescription = stringResource(R.string.food_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Which row is being corrected — the meal it sits in and when it was logged. Aligned with
        // the title, because it belongs to it.
        if (editing && loggedAt != null && !handedOver) {
            Text(
                text = stringResource(
                    R.string.food_edit_subtitle,
                    stringResource(mealType.labelRes),
                    formatTimeOfDay(loggedAt),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
            )
        }
        // Only once something has scrolled under it: a rule over an unscrolled page separates a
        // heading from the thing it heads.
        if (handedOver) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/**
 * The plate a correction is keeping.
 *
 * It was a bare 64dp square above the fields, which looked like a thing the form might be about to
 * throw away. The photo survives the supersede — `toFoodEntry()` carries `photoPath` through — and
 * this row is where that gets said. Tapping it opens the plate full-bleed, because a 56dp
 * centre-crop is the worst possible look at a meal.
 */
@Composable
private fun PhotoRow(path: String) {
    // A view toggle over a path the form already holds, so it stays here rather than in
    // FoodScreenState — whose saver is a positional list, and this survives a rotation on its own.
    var viewing by rememberSaveable { mutableStateOf(false) }
    val viewLabel = stringResource(R.string.food_photo_view)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = viewLabel) { viewing = true }
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MealThumbnail(
            path = path,
            size = 56.dp,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .semantics { contentDescription = viewLabel },
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.food_photo_logged_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.food_photo_logged_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (viewing) {
        // Its own window: the sheet's content column clips, so a full-bleed viewer cannot live
        // inside it, and anything drawn outside it lands behind the sheet. Back is this window's
        // own — it closes the viewer and leaves the form as it was.
        Dialog(
            onDismissRequest = { viewing = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            PhotoViewerOverlay(photo = rememberBitmapFromFile(path), onClose = { viewing = false })
        }
    }
}

/**
 * "Save as my food", as a switch and not a button, pinned above the action bar.
 *
 * **Mounted from the moment the form opens and dimmed until there is something worth keeping**,
 * rather than appearing when the form turns valid — which is what used to shove the action row down
 * the screen under the user's thumb.
 *
 * It became a switch because it is now beside the commit rather than above it: a switch states an
 * intention and the button acts on it, where the old button kept the food on the spot and left the
 * sheet open. Keeping and logging are still two things — the switch is what lets you say you want
 * both without pressing twice.
 */
@Composable
internal fun SaveMyFoodRow(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.food_save_as_my_food),
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                text = stringResource(R.string.food_save_as_my_food_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@PreviewLightDark
@Composable
private fun AddEntryFormViewPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column {
                AddEntryFormView(
                    form = AddEntryForm(
                        name = "Nutella",
                        portionAmount = 150.0,
                        portionUnit = "g",
                        calories = 809,
                        proteinG = 9,
                        carbsG = 87,
                        fatG = 47,
                        servingSize = "1 tbsp (15 g)",
                    ),
                    mealType = MealType.Breakfast,
                    editing = false,
                    seededFromProduct = true,
                    scrolledPastTitle = false,
                    onFormChange = {},
                    onClose = {},
                )
                SaveMyFoodRow(checked = false, enabled = true, onCheckedChange = {})
            }
        }
    }
}

/** A correction: the subtitle names the row, and there is no keeping a food from here. */
@PreviewLightDark
@Composable
private fun AddEntryFormViewEditPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            AddEntryFormView(
                form = AddEntryForm(
                    name = "Grilled chicken breast",
                    portionAmount = 150.0,
                    portionUnit = "g",
                    calories = 210,
                    proteinG = 32,
                    carbsG = 2,
                    fatG = 8,
                    nutrients = Nutrients(sodiumMg = 74),
                ),
                mealType = MealType.Breakfast,
                editing = true,
                seededFromProduct = false,
                scrolledPastTitle = false,
                loggedAt = 1_757_925_720_000,
                onFormChange = {},
                onClose = {},
            )
        }
    }
}

/** The blank form "Add it yourself" opens: every figure an em dash, and nothing claimed. */
@PreviewLightDark
@Composable
private fun AddEntryFormViewBlankPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column {
                AddEntryFormView(
                    form = AddEntryForm(),
                    mealType = MealType.Snacks,
                    editing = false,
                    seededFromProduct = false,
                    scrolledPastTitle = false,
                    onFormChange = {},
                    onClose = {},
                )
                SaveMyFoodRow(checked = false, enabled = false, onCheckedChange = {})
            }
        }
    }
}
