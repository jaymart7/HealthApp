package ph.mart.healthapp.feature.food.ui.photo.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.WindowInsetsRulers
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.food.ScannedProduct
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.ui.search.components.FoodSearchPanel

/**
 * The no-food-detected state. Doesn't reuse [ph.mart.healthapp.core.designsystem.component.FullScreenState]
 * despite `COMPONENTS.md` describing it as a "FullScreenState variant": the prototype's layout —
 * mascot + message inline in a leading row, a search field below, Cancel pinned to the bottom —
 * is a different shape than FullScreenState's centered icon/heading/body/actions column, so
 * forcing it into that shape would mean fighting the component rather than reusing it.
 *
 * Every path that gives up on the photo lands here — including the offline one, which the search
 * itself no longer minds: it reads the shipped food list. Hand entry still has to be reachable
 * from this screen, for anything that list doesn't carry.
 */
@Composable
internal fun ManualSearchScreen(
    onSelectProduct: (ScannedProduct) -> Unit,
    onEnterManually: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .fitInside(WindowInsetsRulers.Ime.current)
                .padding(horizontal = 16.dp, vertical = 24.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                MascotAvatar(state = MascotState.Sleepy, size = 56.dp)
                Text(
                    text = "I couldn't spot a food item in that photo — let's search instead.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            FoodSearchPanel(
                onSelect = onSelectProduct,
                modifier = Modifier.padding(top = 16.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            SecondaryButton(
                label = "Enter it manually",
                onClick = onEnterManually,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                label = "Cancel",
                onClick = onCancel,
                modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ManualSearchScreenPreview() {
    AppTheme {
        ManualSearchScreen(onSelectProduct = {}, onEnterManually = {}, onCancel = {})
    }
}
