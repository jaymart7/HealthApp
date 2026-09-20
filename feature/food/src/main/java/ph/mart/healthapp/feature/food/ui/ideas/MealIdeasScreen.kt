package ph.mart.healthapp.feature.food.ui.ideas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.food.MealIdea
import ph.mart.healthapp.core.data.food.MealIdeaRequest
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.designsystem.component.AIChip
import ph.mart.healthapp.core.designsystem.component.AIChipVariant
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.ideas.components.MealIdeaCard
import ph.mart.healthapp.feature.food.ui.shared.components.ThinkingState

/**
 * The one screen in FitPulse that answers "what should I eat?" rather than "what did I eat?".
 *
 * A route ([MealIdeasRoute][ph.mart.healthapp.feature.food.ui.MealIdeasRoute]), which is what gets
 * it the three things it was hand-rolling as an overlay: no bottom bar, no FAB, and a back press
 * that leaves rather than closes. `AppScaffold` draws its toolbar — there is no `actions` slot to
 * fill, so nothing here has to.
 *
 * The day's gap rides the route key, already worked out by the diary, so [MealIdeasViewModel] still
 * holds no copy of that observer: the model call, and the user's own foods to fall back on.
 *
 * Tapping an idea seeds the add-entry sheet rather than logging it: an estimate has to be
 * adjustable, and the sheet is where every other seeded path — a recipe, a recent, a search hit —
 * already lands. The diary is a back-stack entry below by then, so the pick travels back through
 * `:app`, which pops this route and hands the diary the idea.
 */
@Composable
internal fun MealIdeasScreen(
    request: MealIdeaRequest,
    onSelect: (MealIdea) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MealIdeasViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    MealIdeasContent(
        uiState = uiState,
        request = request,
        onEvent = viewModel::handleEvent,
        onSelect = onSelect,
        modifier = modifier,
    )
}

@Composable
private fun MealIdeasContent(
    uiState: MealIdeasUiState,
    request: MealIdeaRequest,
    onEvent: (MealIdeasEvent) -> Unit,
    onSelect: (MealIdea) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Asked once, on the request the screen opened with. The budget under it moves whenever
    // anything is logged, but nothing can be logged while this is up — and re-asking on every
    // recomposition would spend a model call per frame.
    LaunchedEffect(Unit) { onEvent(MealIdeasEvent.OnRequest(request)) }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = request.remainingLine(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Box(modifier = Modifier.weight(1f)) {
                when (uiState) {
                    MealIdeasUiState.Idle, MealIdeasUiState.Loading ->
                        ThinkingState(line = stringResource(R.string.food_ideas_thinking))
                    is MealIdeasUiState.Ideas -> IdeaList(
                        ideas = uiState.ideas,
                        note = stringResource(R.string.food_ideas_note),
                        chip = { AIChip(label = stringResource(R.string.food_ideas_chip), variant = AIChipVariant.Default) },
                        onSelect = onSelect,
                    )
                    is MealIdeasUiState.Failed -> {
                        val own = uiState.own
                        if (own.isEmpty()) {
                            NothingToSuggest(offline = uiState.offline)
                        } else {
                            IdeaList(
                                ideas = own,
                                // Named as the user's own, never as an estimate: these are rows
                                // they logged, and the AI accent would be a lie about where they
                                // came from.
                                note = if (uiState.offline) {
                                    stringResource(R.string.food_ideas_offline)
                                } else {
                                    stringResource(R.string.food_ideas_failed)
                                },
                                chip = null,
                                onSelect = onSelect,
                            )
                        }
                    }
                }
            }

            // Retry alone: closing is the toolbar's arrow now, and a second button saying what
            // back already says is one more thing to aim at.
            if (uiState is MealIdeasUiState.Failed) {
                SecondaryButton(
                    label = stringResource(R.string.food_try_again),
                    onClick = { onEvent(MealIdeasEvent.OnRequest(request)) },
                    modifier = Modifier.fillMaxWidth(),
                )
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

/** Day one, offline: no model and nothing logged yet to fall back on. Saying so beats a heading
 * over an empty list. */
@Composable
private fun NothingToSuggest(offline: Boolean) {
    FullScreenState(
        icon = { MascotAvatar(state = MascotState.Sleepy, size = 88.dp) },
        heading = stringResource(if (offline) R.string.food_ideas_empty_offline else R.string.food_ideas_empty),
        body = stringResource(R.string.food_ideas_empty_body),
    )
}

/** The gap, in the order the user reads it: calories decide whether they eat, protein decides
 * what. The macro half is dropped once protein is met — a "0g protein left" that is a *good* day
 * would read as a warning. */
@Composable
private fun MealIdeaRequest.remainingLine(): String {
    val meal = stringResource(mealType.labelRes).lowercase()
    return if (remainingProteinG > 0) {
        stringResource(R.string.food_ideas_budget_protein, remainingKcal, remainingProteinG, meal)
    } else {
        stringResource(R.string.food_ideas_budget, remainingKcal, meal)
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
            onEvent = {},
            onSelect = {},
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
            onEvent = {},
            onSelect = {},
        )
    }
}

/** Offline with a diary behind it: the user's own foods, and no AI chip over them. */
@PreviewLightDark
@Composable
private fun MealIdeasOfflinePreview() {
    AppTheme {
        MealIdeasContent(
            uiState = MealIdeasUiState.Failed(
                offline = true,
                own = listOf(
                    MealIdea("Greek yogurt", 1.0, "cup", 150, 20, 8, 4),
                    MealIdea("Chicken salad", 1.0, "serving", 380, 35, 12, 20),
                ),
            ),
            request = PREVIEW_REQUEST,
            onEvent = {},
            onSelect = {},
        )
    }
}

/** Day one, offline — nothing to fall back on. */
@PreviewLightDark
@Composable
private fun MealIdeasEmptyPreview() {
    AppTheme {
        MealIdeasContent(
            uiState = MealIdeasUiState.Failed(offline = true, own = emptyList()),
            request = PREVIEW_REQUEST,
            onEvent = {},
            onSelect = {},
        )
    }
}
