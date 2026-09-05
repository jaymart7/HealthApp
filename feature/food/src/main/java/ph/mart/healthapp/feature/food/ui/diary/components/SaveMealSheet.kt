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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.labelRes

/** Names the snapshot of [mealType]'s entries. Seeded with the meal's own name, so the fast path
 * is Save without typing; [itemCount] is there so the user can see what they're about to keep. */
@Composable
internal fun SaveMealSheet(
    mealType: MealType,
    name: String,
    itemCount: Int,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss) {
        Text(
            // Word-for-word the header button's own accessible name, so the sheet confirms the
            // thing that was tapped. "Save this Snacks" also read as a grammatical slip.
            text = stringResource(R.string.food_save_meal_title, stringResource(mealType.labelRes())),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = stringResource(
                R.string.food_save_meal_body,
                pluralStringResource(R.plurals.food_items, itemCount, itemCount),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTextField(value = name, onValueChange = onNameChange, placeholder = stringResource(R.string.food_save_meal_placeholder))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryButton(label = stringResource(R.string.food_cancel), onClick = onDismiss, modifier = Modifier.weight(1f))
                PrimaryButton(label = stringResource(R.string.food_save), onClick = onSave, enabled = name.isNotBlank(), modifier = Modifier.weight(1f))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SaveMealSheetPreview() {
    AppTheme {
        SaveMealSheet(
            mealType = MealType.Breakfast,
            name = "Usual breakfast",
            itemCount = 3,
            onNameChange = {},
            onDismiss = {},
            onSave = {},
        )
    }
}
