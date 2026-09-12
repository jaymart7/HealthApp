package ph.mart.healthapp.feature.food.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.designsystem.component.AppTextField
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.history.components.HistoryDayGroup

/**
 * Search everything ever logged, by name — the one screen that reads the diary across days, where
 * the diary's own filter only ever narrows the day that is open.
 *
 * A route rather than an overlay on the diary: meal ideas is an overlay because it is built off
 * state the diary already holds, and this is the opposite — its own query, its own read, its own
 * ViewModel, which is what `CLAUDE.md` says earns a package.
 *
 * [dateEpochDay] is the day the diary was showing, carried the way `BarcodeScanRoute` carries it,
 * and it is where a re-logged row lands. [query] is whatever was already typed into the day's
 * filter, so walking up from a day that had nothing doesn't cost the user their word twice. It
 * seeds the field once; the search that follows is also what loads the opening list, since a blank
 * query is simply the newest rows.
 *
 * No back handler: there is one level here and nothing of its own to dismiss, so the toolbar's
 * arrow and system back are already the whole answer.
 */
@Composable
fun FoodHistoryScreen(
    dateEpochDay: Long,
    query: String,
    viewModel: FoodHistoryViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    LaunchedEffect(Unit) { viewModel.handleEvent(FoodHistoryEvent.OnQueryChange(query)) }
    FoodHistoryContent(
        uiState = uiState,
        dateEpochDay = dateEpochDay,
        onEvent = viewModel::handleEvent,
    )
}

@Composable
private fun FoodHistoryContent(
    uiState: FoodHistoryUiState,
    dateEpochDay: Long,
    onEvent: (FoodHistoryEvent) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // The confirmation is built in a coroutine, outside composition, so the meal's name is
    // resolved through the context rather than with `stringResource` — DiaryBody's rule.
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    // Opening this screen *is* the intent to type, so the field arrives focused — the same
    // handover the diary's filter icon makes.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                AppTextField(
                    value = uiState.query,
                    onValueChange = { onEvent(FoodHistoryEvent.OnQueryChange(it)) },
                    // No label: the placeholder says it, and AppTextField hands the placeholder to
                    // the screen reader when there is none.
                    placeholder = stringResource(R.string.food_history_placeholder),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).focusRequester(focusRequester),
                )
                // Nothing is drawn until the first read comes back, or the empty page flashes over
                // a list that is one frame away.
                if (uiState.results.isEmpty()) {
                    if (uiState.searched && !uiState.searching) EmptyHistory(uiState.query)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        for ((day, entries) in uiState.results.groupedByDay()) {
                            // Keyed by the group's first row rather than by its day: the
                            // fold only merges *adjacent* rows, so two groups could share a
                            // date if the query ever stopped ordering by it — and a
                            // duplicate key is a crash, not a glitch. An id is unique.
                            item(key = entries.first().id) {
                                HistoryDayGroup(
                                    dateEpochDay = day,
                                    entries = entries,
                                    onLogAgain = { entry ->
                                        onEvent(FoodHistoryEvent.OnLogAgain(entry, dateEpochDay))
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = context.getString(
                                                    R.string.food_history_logged,
                                                    entry.name,
                                                    context.getString(entry.mealType.labelRes),
                                                ),
                                                duration = SnackbarDuration.Short,
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

/** Two sentences, not one: a query that matched nothing is a different fact from a diary with
 * nothing in it yet, and the second is the one a first run needs to hear. */
@Composable
private fun EmptyHistory(query: String) {
    val heading = if (query.isBlank()) {
        stringResource(R.string.food_history_empty_diary)
    } else {
        stringResource(R.string.food_history_empty_query, query)
    }
    FullScreenState(
        icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
        heading = heading,
        body = stringResource(R.string.food_history_empty_body),
    )
}

@PreviewLightDark
@Composable
private fun FoodHistoryScreenPreview() {
    AppTheme {
        FoodHistoryContent(
            uiState = FoodHistoryUiState(
                query = "chicken",
                searched = true,
                results = listOf(
                    FoodEntry(id = 1, name = "Grilled chicken breast", dateEpochDay = 20_000L, mealType = MealType.Lunch, portionAmount = 150.0, portionUnit = "g", calories = 210, proteinG = 32, carbsG = 2, fatG = 8),
                    FoodEntry(id = 2, name = "Chicken curry", dateEpochDay = 19_994L, mealType = MealType.Dinner, portionAmount = 1.0, portionUnit = "cup", calories = 420, proteinG = 28, carbsG = 30, fatG = 20),
                ),
            ),
            dateEpochDay = 20_000L,
            onEvent = {},
        )
    }
}

/** A query that matched nothing — the page the mascot speaks on. */
@PreviewLightDark
@Composable
private fun FoodHistoryEmptyPreview() {
    AppTheme {
        FoodHistoryContent(
            uiState = FoodHistoryUiState(query = "quinoa", searched = true),
            dateEpochDay = 20_000L,
            onEvent = {},
        )
    }
}
