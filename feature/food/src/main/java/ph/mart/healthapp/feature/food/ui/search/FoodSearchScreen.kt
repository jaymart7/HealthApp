package ph.mart.healthapp.feature.food.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.WindowInsetsRulers
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
import ph.mart.healthapp.core.designsystem.component.FoodItemRow
import ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.search.components.ListTailStatus
import ph.mart.healthapp.feature.food.ui.search.components.SearchViewBar

/**
 * The food search as a screen of its own — the photo flow's no-food-detected fallback, and every
 * other path that gives up on a picture.
 *
 * It lives in `ui/search/` rather than under `ui/photo/` where it started, because grouping in this
 * app is by *subject*: the thing on screen is the search, and `FoodSearchViewModel` and
 * [FoodSearchPanel][ph.mart.healthapp.feature.food.ui.search.components.FoodSearchPanel] are already
 * here. The photo flow is one caller of it, not its owner.
 *
 * **It is the search and nothing else.** What used to open it — a sleepy mascot apologising for the
 * photo — is gone: an apology is a thing to read on a screen whose job is to be typed into, it
 * pushed the field a row down, and by the time anyone reaches here they know the photo failed. The
 * exits moved with it. Back is the arrow in the bar, beside the finger that is already typing, and
 * hand entry is a quiet text button in a docked bar rather than a filled button competing with the
 * rows above it for the tap.
 *
 * The three source tiers still draw as one list with no badge and no divider between them — the
 * order *is* the ranking — and the last row still sits cut in half at the bottom edge, which is the
 * only thing that says there is more.
 */
@Composable
internal fun FoodSearchScreen(
    onSelectProduct: (ScannedProduct) -> Unit,
    onEnterManually: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FoodSearchViewModel = koinViewModel(),
) {
    // koinViewModel() has no graph to resolve against under @Preview — the same trick the panel
    // plays, so every state below still previews.
    if (LocalInspectionMode.current) {
        FoodSearchContent(FoodSearchUiState(), {}, onSelectProduct, onEnterManually, onBack, modifier)
        return
    }
    val uiState by viewModel.collectAsState()
    FoodSearchContent(uiState, viewModel::handleEvent, onSelectProduct, onEnterManually, onBack, modifier)
}

@Composable
private fun FoodSearchContent(
    uiState: FoodSearchUiState,
    onEvent: (FoodSearchEvent) -> Unit,
    onSelectProduct: (ScannedProduct) -> Unit,
    onEnterManually: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Nothing matched only once the online tier has stopped having something to say — while it is
    // in flight the honest answer is that we are still looking.
    val nothingMatched = uiState.results.isEmpty() && uiState.onlineStatus == OnlineSearch.Idle

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // The whole screen sits above the keyboard, which is what keeps the docked bar
                // docked: the bar is at the bottom of the space that is left, not under the IME.
                .fitInside(WindowInsetsRulers.Ime.current)
                .padding(vertical = 8.dp),
        ) {
            SearchViewBar(
                query = uiState.query,
                onQueryChange = { onEvent(FoodSearchEvent.OnQueryChange(it)) },
                onBack = onBack,
                searching = uiState.onlineStatus == OnlineSearch.Searching,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    // The list, the count and "nothing matched" all arrive with no visible change
                    // of focus, so a screen reader needs telling. Polite: it waits for the
                    // keystroke to finish being announced.
                    .semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                if (nothingMatched) {
                    NoMatches(query = uiState.query, onEnterManually = onEnterManually)
                } else {
                    Results(
                        uiState = uiState,
                        onEvent = onEvent,
                        onSelectProduct = onSelectProduct,
                    )
                }
            }
            // Hidden with nothing to pick: the escape hatch is the only action left there, so it
            // becomes the screen's call to action instead of a footnote under one.
            if (!nothingMatched) {
                SearchActionBar(uiState = uiState, onEnterManually = onEnterManually)
            }
        }
    }
}

@Composable
private fun Results(
    uiState: FoodSearchUiState,
    onEvent: (FoodSearchEvent) -> Unit,
    onSelectProduct: (ScannedProduct) -> Unit,
) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
        val items = uiState.visibleItems
        items.forEachIndexed { index, product ->
            // Inset at the start and absent above the first row: a rule is what separates two
            // rows, not a box drawn round each of them.
            if (index > 0) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
            FoodItemRow(
                variant = FoodItemRowVariant.Result,
                name = product.name,
                portionAmount = product.portionAmount,
                portionUnit = product.portionUnit,
                calories = product.calories,
                proteinG = product.proteinG,
                carbsG = product.carbsG,
                fatG = product.fatG,
                modifier = Modifier
                    .clickable { onSelectProduct(product) }
                    .padding(horizontal = 16.dp),
            )
        }
        ListTailStatus(
            status = uiState.onlineStatus,
            onRetry = { onEvent(FoodSearchEvent.OnRetryOnline) },
        )
    }
    // Scrolling to the bottom asks for the next page. `maxValue` grows with each one, so the flag
    // falls back to false and re-arms; at the end of the list the state stops changing and the ask
    // is dropped. Same shape the panel's bounded box uses.
    LaunchedEffect(scroll) {
        snapshotFlow { scroll.value >= scroll.maxValue }
            .distinctUntilChanged()
            .collect { atBottom -> if (atBottom) onEvent(FoodSearchEvent.OnLoadMore) }
    }
}

/**
 * Nothing matched. The heading names the query back, because "no matches" on its own leaves open
 * whether the app looked at all — and the body says exactly where it looked, which is the only
 * thing that makes giving up on the search a reasonable next step.
 */
@Composable
private fun NoMatches(query: String, onEnterManually: () -> Unit) {
    FullScreenState(
        icon = {
            Icon(
                imageVector = AppIcons.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        },
        heading = stringResource(R.string.food_search_no_matches_title, query),
        body = stringResource(R.string.food_search_no_matches_body),
        actions = {
            SecondaryButton(
                label = stringResource(R.string.food_photo_enter_manually),
                onClick = onEnterManually,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

/** The count, and the way out of the search. Ruled off rather than floated: it is the edge of the
 * list, and a shadow under a list that is cut off mid-row would fight the thing that cut it. */
@Composable
private fun SearchActionBar(uiState: FoodSearchUiState, onEnterManually: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(
                    if (uiState.countIsLocalOnly) R.string.food_search_showing_local else R.string.food_search_showing,
                    uiState.visibleItems.size,
                    uiState.results.size,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                label = stringResource(R.string.food_photo_enter_manually),
                onClick = onEnterManually,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FoodSearchScreenResultsPreview() {
    AppTheme {
        FoodSearchContent(
            uiState = FoodSearchUiState(shown = 8),
            onEvent = {},
            onSelectProduct = {},
            onEnterManually = {},
            onBack = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun FoodSearchScreenSearchingOnlinePreview() {
    AppTheme {
        FoodSearchContent(
            uiState = FoodSearchUiState(
                query = "sky flakes",
                results = listOf(ScannedProduct("Cracker", 100.0, "g", 500, 7, 70, 20)),
                onlineStatus = OnlineSearch.Searching,
            ),
            onEvent = {},
            onSelectProduct = {},
            onEnterManually = {},
            onBack = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun FoodSearchScreenLookupFailedPreview() {
    AppTheme {
        FoodSearchContent(
            uiState = FoodSearchUiState(
                query = "sky flakes",
                results = listOf(ScannedProduct("Cracker", 100.0, "g", 500, 7, 70, 20)),
                onlineStatus = OnlineSearch.Failed,
            ),
            onEvent = {},
            onSelectProduct = {},
            onEnterManually = {},
            onBack = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun FoodSearchScreenNoMatchesPreview() {
    AppTheme {
        FoodSearchContent(
            uiState = FoodSearchUiState(query = "zzzz", results = emptyList()),
            onEvent = {},
            onSelectProduct = {},
            onEnterManually = {},
            onBack = {},
        )
    }
}
