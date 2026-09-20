package ph.mart.healthapp.feature.profile.ui.supplement.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.Nutrients
import ph.mart.healthapp.core.data.supplement.SUPPLEMENT_DOSE_MAX
import ph.mart.healthapp.core.data.supplement.SUPPLEMENT_NAME_MAX
import ph.mart.healthapp.core.data.supplement.SUPPLEMENT_TIMES_PER_DAY
import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.NumericStepperField
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.shared.components.SheetDeleteAction
import ph.mart.healthapp.feature.profile.ui.shared.components.WeekdayPicker

/**
 * Adds a supplement or edits one, seeded from [supplement] — `id == 0` is the add. One sheet for
 * both, the same way `RenameSheet` is seeded with the name it is about to change — and one sheet
 * for the scan too, which seeds it from a panel a model read rather than from a row.
 *
 * It is also where a supplement is deleted, now that the row's overflow menu is gone: below the
 * fields, under a rule, and only on a row that exists. [SheetDeleteAction] argues the placement;
 * the screen is what asks before anything goes.
 *
 * The draft lives here rather than in the screen: it is discarded on dismiss, and there is nothing
 * on the other side of Save that needs to have seen it. Back dismisses the sheet rather than the
 * screen under it — that comes from [AppBottomSheet]'s `ModalBottomSheet`, so there is no separate
 * handler to wire.
 *
 * The dose *label* and the dose's *figures* are two different fields and always have been: the
 * label is what the user would say out loud ("2000 IU") and nothing parses it, while
 * [DoseNutrientFields] below it holds what actually counts toward the day.
 *
 * Times-per-day is the read-only [NumericStepperField], not the typable one: the range is 1–6, so
 * a keyboard would be a heavier gesture than the two taps it replaces — the same call the water
 * goal and a recipe's servings count make.
 *
 * **[onLookUp] is the third way figures get in here**, and the only one that needs nothing but the
 * name already being typed: the field grows a sparkle, the model is asked what that product
 * declares, and the answer re-seeds every field below. It is a parameter and defaults to null, so
 * the scan flow's confirmation — which got its figures off a panel — draws no sparkle at all. The
 * caller owns the call and its message; this sheet owns the gesture. [estimated] is what the
 * readout below is told, and the difference it makes there is the difference between a transcript
 * and a recollection.
 *
 * Under it, *which days* — [WeekdayPicker], the row the routine editor already draws. The two
 * answer different halves of one question and sit together for that reason: how many times, and on
 * which days. **The mask can never be emptied here.** A routine with no days is unscheduled and
 * says so; a supplement with no days could not be taken at all, so the last selected cell refuses
 * to turn itself off rather than saving a row nothing could ever tick.
 */
@Composable
internal fun SupplementEditSheet(
    supplement: Supplement,
    onDismiss: () -> Unit,
    onSave: (Supplement) -> Unit,
    onDelete: () -> Unit = {},
    onLookUp: ((String) -> Unit)? = null,
    lookingUp: Boolean = false,
    lookupError: String? = null,
    estimated: Boolean = false,
) {
    var name by remember(supplement) { mutableStateOf(supplement.name) }
    var dose by remember(supplement) { mutableStateOf(supplement.dose) }
    var timesPerDay by remember(supplement) { mutableIntStateOf(supplement.timesPerDay) }
    // The whole value rather than seven fields: a cell writes only its own, so a figure the user
    // never touched survives a save it was only rounded for display.
    var nutrients by remember(supplement) { mutableStateOf(supplement.nutrients) }
    var days by remember(supplement) { mutableIntStateOf(supplement.days) }

    AppBottomSheet(
        title = stringResource(
            if (supplement.id == 0L) R.string.profile_supplements_add else R.string.profile_supplements_edit,
        ),
        onDismiss = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // The lookup is offered on a name there is something to look up, and never twice at
            // once: the same two conditions gate the button and the keyboard's own key, so the
            // return key can't do what the sparkle refuses to.
            val canLookUp = onLookUp != null && name.isNotBlank() && !lookingUp
            AppTextField(
                value = name,
                onValueChange = { if (it.length <= SUPPLEMENT_NAME_MAX) name = it },
                placeholder = stringResource(R.string.profile_name),
                error = lookupError,
                imeAction = if (onLookUp != null) ImeAction.Search else ImeAction.Default,
                onImeAction = if (canLookUp) ({ onLookUp?.invoke(name) }) else null,
                trailing = if (onLookUp == null) {
                    null
                } else {
                    {
                        if (lookingUp) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp),
                            )
                        } else {
                            IconButton(onClick = { onLookUp(name) }, enabled = canLookUp) {
                                Icon(
                                    imageVector = AppIcons.AiSparkle,
                                    contentDescription = stringResource(R.string.profile_supplements_lookup),
                                    tint = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        }
                    }
                },
            )
            // Free text, never parsed: "2000 IU", "5 g", "one scoop" are all the same kind of
            // answer, and there is no target on the profile to price any of them against.
            AppTextField(
                value = dose,
                onValueChange = { if (it.length <= SUPPLEMENT_DOSE_MAX) dose = it },
                placeholder = stringResource(R.string.profile_supplements_dose),
            )
            // What one dose carries, and the only way to get it without a camera. Typing the
            // four this app grades is the exception to a rule that holds everywhere else —
            // [DoseNutrientFields] argues it.
            DoseNutrientFields(nutrients = nutrients, onChange = { nutrients = it })
            // What the panel said, when a panel was read: the transcript, not the figures. It is
            // read-only and stays that way — a correction belongs in the fields above, and the
            // two are allowed to differ afterwards, which is exactly the case this sheet exists
            // to serve. Absent on every supplement nobody scanned.
            PanelReadout(panel = supplement.panel, estimated = estimated)
            NumericStepperField(
                label = stringResource(R.string.profile_supplements_how_often),
                value = "$timesPerDay",
                unitSuffix = pluralStringResource(R.plurals.profile_supplements_times_a_day, timesPerDay),
                onIncrement = {
                    timesPerDay = (timesPerDay + 1).coerceAtMost(SUPPLEMENT_TIMES_PER_DAY.last)
                },
                onDecrement = {
                    timesPerDay = (timesPerDay - 1).coerceAtLeast(SUPPLEMENT_TIMES_PER_DAY.first)
                },
            )
            Text(
                text = stringResource(R.string.profile_supplements_which_days),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
            // The guard, and the whole of it: a toggle that would leave nothing selected is
            // dropped, so the cell the user taps twice stays on rather than the sheet having to
            // explain itself afterwards.
            WeekdayPicker(days = days, onDaysChange = { if (it != 0) days = it })
            PrimaryButton(
                label = stringResource(R.string.profile_save),
                onClick = {
                    onSave(
                        supplement.copy(
                            name = name,
                            dose = dose,
                            timesPerDay = timesPerDay,
                            nutrients = nutrients,
                            days = days,
                        ),
                    )
                },
                // A nameless supplement is unidentifiable, and unlike a diary entry it has no
                // calorie figure to stand in for one — the same guard `RenameSheet` applies.
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            // The same `id == 0` the title branches on: an add has nothing to delete, and neither
            // does the scan flow's confirmation, which seeds a row Room has never seen.
            if (supplement.id != 0L) SheetDeleteAction(onDelete = onDelete)
        }
    }
}

@PreviewLightDark
@Composable
private fun SupplementEditSheetAddPreview() {
    AppTheme {
        SupplementEditSheet(supplement = Supplement(name = ""), onDismiss = {}, onSave = {})
    }
}

/** The scanned shape: the same sheet, seeded, with the panel under the fields. */
@PreviewLightDark
@Composable
private fun SupplementEditSheetScannedPreview() {
    AppTheme {
        SupplementEditSheet(
            supplement = Supplement(
                name = "Daily Multivitamin",
                dose = "2 tablets",
                nutrients = Nutrients(vitaminDUg = 25, calciumMg = 210),
                panel = "Vitamin D 25 µg\nCalcium 210 mg\nVitamin C 90 mg\nZinc 11 mg",
            ),
            onDismiss = {},
            onSave = {},
        )
    }
}

/** The typed shape: a name and a sparkle, with nothing looked up yet. */
@PreviewLightDark
@Composable
private fun SupplementEditSheetLookupPreview() {
    AppTheme {
        SupplementEditSheet(
            supplement = Supplement(
                name = "Centrum Adults",
                dose = "1 tablet",
                nutrients = Nutrients(vitaminDUg = 25, calciumMg = 210),
                panel = "Vitamin D 25 µg\nCalcium 210 mg\nVitamin A 900 µg\nZinc 11 mg",
            ),
            onDismiss = {},
            onSave = {},
            onLookUp = {},
            estimated = true,
        )
    }
}

@PreviewLightDark
@Composable
private fun SupplementEditSheetEditPreview() {
    AppTheme {
        SupplementEditSheet(
            supplement = Supplement(
                id = 1,
                name = "Creatine",
                dose = "5 g",
                timesPerDay = 2,
                days = 0b0010101,
            ),
            onDismiss = {},
            onSave = {},
        )
    }
}
