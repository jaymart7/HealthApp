package ph.mart.healthapp.feature.profile.ui.supplement.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R

/**
 * Adds a supplement or edits one, seeded from [supplement] — `id == 0` is the add. One sheet for
 * both, the same way `RenameSheet` is seeded with the name it is about to change — and one sheet
 * for the scan too, which seeds it from a panel a model read rather than from a row.
 *
 * The draft lives here rather than in the screen: it is discarded on dismiss, and there is nothing
 * on the other side of Save that needs to have seen it. Back dismisses the sheet rather than the
 * screen under it — that comes from [AppBottomSheet]'s `ModalBottomSheet`, so there is no separate
 * handler to wire.
 *
 * Times-per-day is the read-only [NumericStepperField], not the typable one: the range is 1–6, so
 * a keyboard would be a heavier gesture than the two taps it replaces — the same call the water
 * goal and a recipe's servings count make.
 */
@Composable
internal fun SupplementEditSheet(
    supplement: Supplement,
    onDismiss: () -> Unit,
    onSave: (Supplement) -> Unit,
) {
    var name by remember(supplement) { mutableStateOf(supplement.name) }
    var dose by remember(supplement) { mutableStateOf(supplement.dose) }
    var timesPerDay by remember(supplement) { mutableIntStateOf(supplement.timesPerDay) }

    AppBottomSheet(
        title = stringResource(
            if (supplement.id == 0L) R.string.profile_supplements_add else R.string.profile_supplements_edit,
        ),
        onDismiss = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTextField(
                value = name,
                onValueChange = { if (it.length <= SUPPLEMENT_NAME_MAX) name = it },
                placeholder = stringResource(R.string.profile_name),
            )
            // Free text, never parsed: "2000 IU", "5 g", "one scoop" are all the same kind of
            // answer, and there is no target on the profile to price any of them against.
            AppTextField(
                value = dose,
                onValueChange = { if (it.length <= SUPPLEMENT_DOSE_MAX) dose = it },
                placeholder = stringResource(R.string.profile_supplements_dose),
            )
            // What the panel said, when a panel was read. The figures themselves are carried on
            // [supplement] and are not editable: they were copied off a bottle, and a typed
            // correction to a number nobody typed is a worse claim than the reading. Re-scan to
            // change them, or clear them by adding the supplement by hand.
            PanelReadout(panel = supplement.panel)
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
            PrimaryButton(
                label = stringResource(R.string.profile_save),
                onClick = {
                    onSave(supplement.copy(name = name, dose = dose, timesPerDay = timesPerDay))
                },
                // A nameless supplement is unidentifiable, and unlike a diary entry it has no
                // calorie figure to stand in for one — the same guard `RenameSheet` applies.
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
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

@PreviewLightDark
@Composable
private fun SupplementEditSheetEditPreview() {
    AppTheme {
        SupplementEditSheet(
            supplement = Supplement(id = 1, name = "Creatine", dose = "5 g", timesPerDay = 2),
            onDismiss = {},
            onSave = {},
        )
    }
}
