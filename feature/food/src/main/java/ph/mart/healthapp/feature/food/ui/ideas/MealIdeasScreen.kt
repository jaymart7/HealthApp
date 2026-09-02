package ph.mart.healthapp.feature.food.ui.ideas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.food.FoodSuggestion
import ph.mart.healthapp.core.data.food.MealIdea
import ph.mart.healthapp.core.data.food.MealIdeaRequest
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.Recipe
import ph.mart.healthapp.core.data.food.localMealIdeas
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.designsystem.component.AIChip
import ph.mart.healthapp.core.designsystem.component.AIChipVariant
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.ui.ideas.components.MealIdeaCard

/**
 * The one screen in FitPulse that answers "what should I eat?" rather than "what did I eat?".
 *
 * A full-screen overlay inside the Food tab, not a route — `RecapScreen`'s call, and for its
 * reason: everything it shows (the day's gap, the recents, the recipes) is already combined by the
 * diary underneath, and a route would have earned its own `ViewModelStoreOwner` and a second copy
 * of that observer to draw a screen that writes nothing. [MealIdeasViewModel] therefore holds the
 * model call alone.
 *
 * Tapping an idea seeds the add-entry sheet rather than logging it: an estimate has to be
 * adjustable, and the sheet is where every other seeded path — a recipe, a recent, a search hit —
 * already lands.
 */
@Composable
internal fun MealIdeasScreen(
    request: MealIdeaRequest,
    suggestions: List<FoodSuggestion>,
    recipes: List<Recipe>,
    onSelect: (MealIdea) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MealIdeasViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    MealIdeasContent(
        uiState = uiState,
        request = request,
        suggestions = suggestions,
        recipes = recipes,
        onEvent = viewModel::handleEvent,
        onSelect = onSelect,
        onClose = onClose,
        modifier = modifier,
    )
}

@Composable
private fun MealIdeasContent(
    uiState: MealIdeasUiState,
    request: MealIdeaRequest,
    suggestions: List<FoodSuggestion>,
    recipes: List<Recipe>,
    onEvent: (MealIdeasEvent) -> Unit,
    onSelect: (MealIdea) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Asked once, on the request the screen opened with. The budget under it moves whenever
    // anything is logged, but nothing can be logged while this is up — and re-asking on every
    // recomposition would spend a model call per frame.
    LaunchedEffect(Unit) { onEvent(MealIdeasEvent.OnRequest(request)) }

    // An overlay, not a route: back closes it rather than leaving the Food tab.
    val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(state = navigationState, onBackCompleted = onClose)

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Meal ideas",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = request.remainingLine(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Box(modifier = Modifier.weight(1f)) {
                when (uiState) {
                    MealIdeasUiState.Idle, MealIdeasUiState.Loading -> Thinking()
                    is MealIdeasUiState.Ideas -> IdeaList(
                        ideas = uiState.ideas,
                        note = "Tap one to adjust the portion and log it.",
                        chip = { AIChip(label = "AI suggested", variant = AIChipVariant.Default) },
                        onSelect = onSelect,
                    )
                    is MealIdeasUiState.Failed -> {
                        val own = remember(suggestions, recipes, request.remainingKcal) {
                            localMealIdeas(suggestions, recipes, request.remainingKcal)
                        }
                        if (own.isEmpty()) {
                            NothingToSuggest(offline = uiState.offline)
                        } else {
                            IdeaList(
                                ideas = own,
                                // Named as the user's own, never as an estimate: these are rows
                                // they logged, and the AI accent would be a lie about where they
                                // came from.
                                note = if (uiState.offline) {
                                    "You're offline, so these are your own foods that still fit."
                                } else {
                                    "Couldn't reach the coach, so these are your own foods that still fit."
                                },
                                chip = null,
                                onSelect = onSelect,
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (uiState is MealIdeasUiState.Failed) {
                    SecondaryButton(
                        label = "Try again",
                        onClick = { onEvent(MealIdeasEvent.OnRequest(request)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                SecondaryButton(label = "Close", onClick = onClose, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun IdeaList(
    ideas: List<MealIdea>,
    note: String,
    chip: (@Composable () -> Unit)?,
    onSelect: (MealIdea) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        chip?.invoke()
        Text(
            text = note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ideas.forEach { idea -> MealIdeaCard(idea = idea, onSelect = { onSelect(idea) }) }
    }
}

/** The indeterminate bar and one honest line, `AnalyzingScreen`'s rule — nothing rotates, and
 * nothing promises progress the call can't report. */
@Composable
private fun Thinking() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        MascotAvatar(state = MascotState.Thinking, size = 88.dp)
        LinearProgressIndicator(modifier = Modifier.size(width = 200.dp, height = 4.dp))
        Text(
            text = "Thinking of something that fits…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Day one, offline: no model and nothing logged yet to fall back on. Saying so beats a heading
 * over an empty list. */
@Composable
private fun NothingToSuggest(offline: Boolean) {
    FullScreenState(
        icon = { MascotAvatar(state = MascotState.Sleepy, size = 88.dp) },
        heading = if (offline) "No ideas offline yet" else "No ideas right now",
        body = "Once you've logged a few meals, FitPulse can suggest the ones that fit what's " +
            "left of your day.",
    )
}

/** The gap, in the order the user reads it: calories decide whether they eat, protein decides
 * what. The macro half is dropped once protein is met — a "0g protein left" that is a *good* day
 * would read as a warning. */
private fun MealIdeaRequest.remainingLine(): String {
    val meal = mealType.name.lowercase()
    return if (remainingProteinG > 0) {
        "$remainingKcal kcal and ${remainingProteinG}g protein left for $meal."
    } else {
        "$remainingKcal kcal left for $meal."
    }
}

private val PREVIEW_REQUEST = MealIdeaRequest(
    goal = Goal.Lose,
    mealType = MealType.Dinner,
    remainingKcal = 640,
    remainingProteinG = 48,
    remainingCarbsG = 70,
    remainingFatG = 20,
    diet = null,
)

private val PREVIEW_IDEAS = listOf(
    MealIdea("Chicken stir-fry with rice", 1.0, "serving", 520, 42, 55, 12),
    MealIdea("Salmon, potatoes and greens", 1.0, "serving", 610, 38, 48, 26),
    MealIdea("Greek yogurt with berries", 1.0, "cup", 220, 22, 24, 4),
)

@PreviewLightDark
@Composable
private fun MealIdeasScreenPreview() {
    AppTheme {
        MealIdeasContent(
            uiState = MealIdeasUiState.Ideas(PREVIEW_IDEAS),
            request = PREVIEW_REQUEST,
            suggestions = emptyList(),
            recipes = emptyList(),
            onEvent = {},
            onSelect = {},
            onClose = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun MealIdeasLoadingPreview() {
    AppTheme {
        MealIdeasContent(
            uiState = MealIdeasUiState.Loading,
            request = PREVIEW_REQUEST,
            suggestions = emptyList(),
            recipes = emptyList(),
            onEvent = {},
            onSelect = {},
            onClose = {},
        )
    }
}

/** Offline with a diary behind it: the user's own foods, and no AI chip over them. */
@PreviewLightDark
@Composable
private fun MealIdeasOfflinePreview() {
    AppTheme {
        MealIdeasContent(
            uiState = MealIdeasUiState.Failed(offline = true),
            request = PREVIEW_REQUEST,
            suggestions = listOf(
                FoodSuggestion("Greek yogurt", 1.0, "cup", 150, 20, 8, 4, isFavorite = true),
                FoodSuggestion("Chicken salad", 1.0, "serving", 380, 35, 12, 20, isFavorite = false),
            ),
            recipes = emptyList(),
            onEvent = {},
            onSelect = {},
            onClose = {},
        )
    }
}

/** Day one, offline — nothing to fall back on. */
@PreviewLightDark
@Composable
private fun MealIdeasEmptyPreview() {
    AppTheme {
        MealIdeasContent(
            uiState = MealIdeasUiState.Failed(offline = true),
            request = PREVIEW_REQUEST,
            suggestions = emptyList(),
            recipes = emptyList(),
            onEvent = {},
            onSelect = {},
            onClose = {},
        )
    }
}
