package ph.mart.healthapp.feature.food.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.food.ScannedProduct
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.ui.FoodSearchEvent
import ph.mart.healthapp.feature.food.ui.FoodSearchUiState
import ph.mart.healthapp.feature.food.ui.FoodSearchViewModel
import ph.mart.healthapp.feature.food.ui.SearchStatus

/**
 * ponytail: shows the first few hits and no inner scroller — the panel sits inside a bottom sheet
 * and a full result list would push the form off-screen. Narrowing the query beats scrolling a
 * crowd-sourced database. Add a bounded LazyColumn if that's ever reported as too few.
 */
private const val MAX_VISIBLE_HITS = 5

/**
 * Free-text food search, shared by the diary's add-entry sheet and the photo flow's manual-search
 * state. Picking a hit hands a [ScannedProduct] to the host, which seeds its own form from it —
 * the panel never logs anything itself.
 */
@Composable
internal fun FoodSearchPanel(
    onSelect: (ScannedProduct) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FoodSearchViewModel = koinViewModel(),
) {
    // koinViewModel() has no graph to resolve against under @Preview — render the idle panel so
    // every caller's @PreviewLightDark still shows this screen, same trick as AppBottomSheet.
    if (LocalInspectionMode.current) {
        FoodSearchPanelContent(FoodSearchUiState(), onQueryChange = {}, onSelect = onSelect, modifier = modifier)
        return
    }

    val uiState by viewModel.collectAsState()
    FoodSearchPanelContent(
        uiState = uiState,
        onQueryChange = { viewModel.handleEvent(FoodSearchEvent.OnQueryChange(it)) },
        onSelect = onSelect,
        modifier = modifier,
    )
}

@Composable
private fun FoodSearchPanelContent(
    uiState: FoodSearchUiState,
    onQueryChange: (String) -> Unit,
    onSelect: (ScannedProduct) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppTextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            placeholder = "Search foods…",
        )
        when (val status = uiState.status) {
            SearchStatus.Idle -> Hint("Search the food database, or fill in the details yourself.")

            SearchStatus.Searching -> Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
                Hint("Searching…")
            }

            is SearchStatus.Results -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                status.products.take(MAX_VISIBLE_HITS).forEach { product ->
                    SearchHitRow(product = product, onClick = { onSelect(product) })
                }
            }

            SearchStatus.Empty -> Hint("No matches — enter it by hand instead.")

            SearchStatus.Failed ->
                Hint("Couldn't search just now — check your connection, or enter it by hand.")
        }
    }
}

@Composable
private fun SearchHitRow(product: ScannedProduct, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        FoodItemRow(
            variant = FoodItemRowVariant.Display,
            name = product.name,
            portionAmount = product.portionAmount,
            portionUnit = product.portionUnit,
            calories = product.calories,
            proteinG = product.proteinG,
            carbsG = product.carbsG,
            fatG = product.fatG,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@PreviewLightDark
@Composable
private fun FoodSearchPanelResultsPreview() {
    AppTheme {
        Surface {
            FoodSearchPanelContent(
                uiState = FoodSearchUiState(
                    query = "yogurt",
                    status = SearchStatus.Results(
                        listOf(
                            ScannedProduct("Greek yogurt", 100.0, "g", 97, 9, 4, 5),
                            ScannedProduct("Greek yogurt, plain", 100.0, "g", 59, 10, 4, 0),
                        ),
                    ),
                ),
                onQueryChange = {},
                onSelect = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FoodSearchPanelEmptyPreview() {
    AppTheme {
        Surface {
            FoodSearchPanelContent(
                uiState = FoodSearchUiState(query = "zzzz", status = SearchStatus.Empty),
                onQueryChange = {},
                onSelect = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
