package ph.mart.healthapp.feature.food.ui.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.food.ScannedProduct
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.search.FoodSearchEvent
import ph.mart.healthapp.feature.food.ui.search.FoodSearchUiState
import ph.mart.healthapp.feature.food.ui.search.FoodSearchViewModel
import ph.mart.healthapp.feature.food.ui.search.OnlineSearch
import ph.mart.healthapp.feature.food.ui.search.hasMore
import ph.mart.healthapp.feature.food.ui.search.visibleItems

/** Four rows and a sliver of the fifth — the cap on a panel with a form underneath it. */
private val RESULTS_MAX_HEIGHT = 280.dp

/**
 * Food search over the user's own foods, the built-in
 * [COMMON_FOODS][ph.mart.healthapp.core.data.food.COMMON_FOODS] list and an Open Food Facts tier
 * behind them, shared by the diary's add-entry sheet and the recipe ingredient editor — the two
 * hosts that draw a form directly beneath it. Picking a hit hands a [ScannedProduct] to the host, which seeds its own
 * form from it — the panel never logs anything itself.
 *
 * The three tiers draw as one list with no badge or divider between them. They are all per-100 g
 * figures a row can be seeded from, the panel has always mixed the first two silently, and a
 * "where this came from" mark is a thing to explain on a surface whose job is to be picked from.
 * Their *order* is the ranking — see [searchFoods][ph.mart.healthapp.core.data.food.searchFoods].
 *
 * An empty field is not an empty panel: it lists every local food, eight rows at a time, appending
 * the next eight when the results box is scrolled to its bottom. The box is bounded and scrolls
 * itself rather than growing, because **both** its hosts draw their own form directly beneath it —
 * a list that got taller as you read it would walk that form down the screen.
 *
 * There was a third host and a `fillHeight` flag for it: the photo flow's manual search, the one
 * host with a whole screen to give. That screen is now
 * [FoodSearchScreen][ph.mart.healthapp.feature.food.ui.search.FoodSearchScreen] and draws its own
 * list, so the flag and the `weight(1f)` branch it switched are gone — every host left is a bounded
 * one, which is the shape this panel was always for.
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
        FoodSearchPanelContent(
            uiState = FoodSearchUiState(),
            onEvent = {},
            onSelect = onSelect,
            modifier = modifier,
        )
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
            placeholder = stringResource(R.string.food_search_placeholder),
        )
        // The panel's whole answer — the rows, the count, nothing matched — arrives without any
        // visible change of focus, so a screen reader needs telling. Polite: it waits for the
        // keystroke to finish being announced.
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            val items = uiState.visibleItems
            // "No matches" is only true once the online tier has stopped having something to say:
            // while it is in flight the honest answer is that we are still looking, and when it
            // failed the honest answer is that we could not ask.
            if (items.isEmpty() && uiState.onlineStatus == OnlineSearch.Idle) {
                Hint(stringResource(R.string.food_search_no_matches))
            } else {
                ResultsBox(
                    uiState = uiState,
                    onEvent = onEvent,
                    onSelect = onSelect,
                    modifier = Modifier.heightIn(max = RESULTS_MAX_HEIGHT),
                )
                if (uiState.hasMore) {
                    Hint(stringResource(R.string.food_search_showing, items.size, uiState.results.size))
                }
            }
            when (uiState.onlineStatus) {
                OnlineSearch.Searching -> Hint(stringResource(R.string.food_search_online_searching))
                OnlineSearch.Failed -> Hint(stringResource(R.string.food_search_online_failed))
                OnlineSearch.Idle -> Unit
            }
        }
    }
}

/**
 * The rows, and only the rows: the hints stay outside so "searching online…" never needs scrolling
 * to. [RESULTS_MAX_HEIGHT] is deliberately not a multiple of the row height: the row cut in half at
 * the bottom edge is what says there is more, the job the Next button used to do.
 *
 * Scrolling to the bottom asks for the next page. `maxValue` grows with each one, so the flag falls
 * back to false and re-arms; at the end of the list the state stops changing and the ask is dropped.
 */
@Composable
private fun ResultsBox(
    uiState: FoodSearchUiState,
    onEvent: (FoodSearchEvent) -> Unit,
    onSelect: (ScannedProduct) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    Column(modifier = modifier.verticalScroll(scroll)) {
        uiState.visibleItems.forEachIndexed { index, product ->
            if (index > 0) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
            SearchHitRow(product = product, onClick = { onSelect(product) })
        }
    }
    LaunchedEffect(scroll) {
        snapshotFlow { scroll.value >= scroll.maxValue }
            .distinctUntilChanged()
            .collect { atBottom -> if (atBottom) onEvent(FoodSearchEvent.OnLoadMore) }
    }
}

/**
 * One hit. The same [FoodItemRowVariant.Result] the full-screen search draws, separated by a rule
 * rather than wrapped in a `surfaceContainerHighest` card.
 *
 * The card was a per-row box, which is what a *stack of things* looks like; a search result is one
 * line of a list being scanned down, and a list is what the rules say. It also freed the row's
 * whole width for the name — a card ate 12dp of padding either side of every row — and promoted
 * the calorie figure, which is what a picker actually aims at. The sheet and the recipe editor draw
 * it too: one search, one row, wherever it appears.
 */
@Composable
private fun SearchHitRow(product: ScannedProduct, onClick: () -> Unit) {
    FoodItemRow(
        variant = FoodItemRowVariant.Result,
        name = product.name,
        portionAmount = product.portionAmount,
        portionUnit = product.portionUnit,
        calories = product.calories,
        proteinG = product.proteinG,
        carbsG = product.carbsG,
        fatG = product.fatG,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
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
                uiState = FoodSearchUiState(shown = 24),
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
private fun FoodSearchPanelSearchingOnlinePreview() {
    AppTheme {
        Surface {
            FoodSearchPanelContent(
                uiState = FoodSearchUiState(
                    query = "sky flakes",
                    results = emptyList(),
                    onlineStatus = OnlineSearch.Searching,
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
private fun FoodSearchPanelOnlineFailedPreview() {
    AppTheme {
        Surface {
            FoodSearchPanelContent(
                uiState = FoodSearchUiState(
                    query = "sky flakes",
                    results = listOf(ScannedProduct("Cracker", 100.0, "g", 500, 7, 70, 20)),
                    onlineStatus = OnlineSearch.Failed,
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
