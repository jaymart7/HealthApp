package ph.mart.healthapp.feature.food.ui.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.food.ScannedProduct
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.ui.search.FOOD_PAGE_SIZE
import ph.mart.healthapp.feature.food.ui.search.FoodSearchEvent
import ph.mart.healthapp.feature.food.ui.search.FoodSearchUiState
import ph.mart.healthapp.feature.food.ui.search.FoodSearchViewModel
import ph.mart.healthapp.feature.food.ui.search.pageCount
import ph.mart.healthapp.feature.food.ui.search.pageItems

/**
 * Food search over the built-in [COMMON_FOODS][ph.mart.healthapp.core.data.food.COMMON_FOODS]
 * list, shared by the diary's add-entry sheet, the photo flow's manual-search state and the recipe
 * ingredient editor. Picking a hit hands a [ScannedProduct] to the host, which seeds its own form
 * from it — the panel never logs anything itself.
 *
 * An empty field is not an empty panel: it lists every food, a page at a time. That is the whole
 * reason for the pager — the list is a couple of hundred rows and this is drawn inside a bottom
 * sheet with a form underneath it.
 */
@Composable
internal fun FoodSearchPanel(
    onSelect: (ScannedProduct) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FoodSearchViewModel = koinViewModel(),
) {
    // koinViewModel() has no graph to resolve against under @Preview — render the first page so
    // every caller's @PreviewLightDark still shows this screen, same trick as AppBottomSheet.
    if (LocalInspectionMode.current) {
        FoodSearchPanelContent(FoodSearchUiState(), onEvent = {}, onSelect = onSelect, modifier = modifier)
        return
    }

    val uiState by viewModel.collectAsState()
    FoodSearchPanelContent(
        uiState = uiState,
        onEvent = viewModel::handleEvent,
        onSelect = onSelect,
        modifier = modifier,
    )
}

@Composable
private fun FoodSearchPanelContent(
    uiState: FoodSearchUiState,
    onEvent: (FoodSearchEvent) -> Unit,
    onSelect: (ScannedProduct) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppTextField(
            value = uiState.query,
            onValueChange = { onEvent(FoodSearchEvent.OnQueryChange(it)) },
            placeholder = "Search foods…",
        )
        // The panel's whole answer — the page, the count, nothing matched — arrives without any
        // visible change of focus, so a screen reader needs telling. Polite: it waits for the
        // keystroke to finish being announced.
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            val page = uiState.pageItems
            if (page.isEmpty()) {
                Hint("No matches — enter it by hand instead.")
            } else {
                page.forEach { product ->
                    SearchHitRow(product = product, onClick = { onSelect(product) })
                }
                Pager(uiState = uiState, onEvent = onEvent)
            }
        }
    }
}

/**
 * Each button is *hidden* at its end of the list rather than disabled — the rule the meal-ideas
 * button and the supplements card follow. The count is what tells the user there is more.
 */
@Composable
private fun Pager(uiState: FoodSearchUiState, onEvent: (FoodSearchEvent) -> Unit) {
    val pages = uiState.pageCount
    if (pages <= 1) return

    val first = uiState.page * FOOD_PAGE_SIZE + 1
    val last = first + uiState.pageItems.size - 1
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (uiState.page > 0) {
            TextButton(label = "Previous", onClick = { onEvent(FoodSearchEvent.OnPrevPage) })
        } else {
            Spacer(modifier = Modifier.size(1.dp))
        }
        Hint("$first–$last of ${uiState.results.size}")
        if (uiState.page < pages - 1) {
            TextButton(label = "Next", onClick = { onEvent(FoodSearchEvent.OnNextPage) })
        } else {
            Spacer(modifier = Modifier.size(1.dp))
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
private fun FoodSearchPanelBrowsingPreview() {
    AppTheme {
        Surface {
            FoodSearchPanelContent(
                uiState = FoodSearchUiState(page = 1),
                onEvent = {},
                onSelect = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FoodSearchPanelResultsPreview() {
    AppTheme {
        Surface {
            FoodSearchPanelContent(
                uiState = FoodSearchUiState(
                    query = "yogurt",
                    results = listOf(
                        ScannedProduct("Greek yogurt, plain nonfat", 100.0, "g", 59, 10, 4, 0),
                        ScannedProduct("Yogurt, plain whole milk", 100.0, "g", 61, 4, 5, 3),
                    ),
                ),
                onEvent = {},
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
                uiState = FoodSearchUiState(query = "zzzz", results = emptyList()),
                onEvent = {},
                onSelect = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
