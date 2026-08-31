package ph.mart.healthapp.feature.profile.ui.supplement.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.supplement.SUPPLEMENT_DOSE_MAX
import ph.mart.healthapp.core.data.supplement.SUPPLEMENT_NAME_MAX
import ph.mart.healthapp.core.data.supplement.SUPPLEMENT_TIMES_PER_DAY
import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.NumericStepperField
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * Adds a supplement or edits one, seeded from [supplement] — `id == 0` is the add. One sheet for
 * both, the same way `RenameSheet` is seeded with the name it is about to change.
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

    AppBottomSheet(onDismiss = onDismiss) {
        Text(
            text = if (supplement.id == 0L) "Add supplement" else "Edit supplement",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTextField(
                value = name,
                onValueChange = { if (it.length <= SUPPLEMENT_NAME_MAX) name = it },
                placeholder = "Name",
            )
            // Free text, never parsed: "2000 IU", "5 g", "one scoop" are all the same kind of
            // answer, and there is no target on the profile to price any of them against.
            AppTextField(
                value = dose,
                onValueChange = { if (it.length <= SUPPLEMENT_DOSE_MAX) dose = it },
                placeholder = "Dose (optional)",
            )
            NumericStepperField(
                label = "How often",
                value = "$timesPerDay",
                unitSuffix = if (timesPerDay == 1) "time a day" else "times a day",
                onIncrement = {
                    timesPerDay = (timesPerDay + 1).coerceAtMost(SUPPLEMENT_TIMES_PER_DAY.last)
                },
                onDecrement = {
                    timesPerDay = (timesPerDay - 1).coerceAtLeast(SUPPLEMENT_TIMES_PER_DAY.first)
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryButton(label = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                PrimaryButton(
                    label = "Save",
                    onClick = {
                        onSave(supplement.copy(name = name, dose = dose, timesPerDay = timesPerDay))
                    },
                    // A nameless supplement is unidentifiable, and unlike a diary entry it has no
                    // calorie figure to stand in for one — the same guard `RenameSheet` applies.
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                )
            }
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
