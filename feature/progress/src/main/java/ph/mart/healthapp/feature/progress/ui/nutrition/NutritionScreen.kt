package ph.mart.healthapp.feature.progress.ui.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.Nutrients
import ph.mart.healthapp.core.data.food.averages
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.AppTopBar
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.nutrition.components.MealPhotoGallery
import ph.mart.healthapp.feature.progress.ui.nutrition.components.MealPhotoStrip
import ph.mart.healthapp.feature.progress.ui.nutrition.components.NutritionAverageCard
import ph.mart.healthapp.feature.progress.ui.nutrition.components.NutritionTrendChart
import ph.mart.healthapp.feature.progress.ui.progress.PREVIEW_POINTS
import ph.mart.healthapp.feature.progress.ui.progress.Subject
import ph.mart.healthapp.feature.progress.ui.progress.components.AskCoachAction
import ph.mart.healthapp.feature.progress.ui.progress.components.ChartCard
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChip
import ph.mart.healthapp.feature.progress.ui.progress.components.FactChipRow
import ph.mart.healthapp.feature.progress.ui.progress.components.HeroValue
import ph.mart.healthapp.feature.progress.ui.progress.components.LegendEntry
import ph.mart.healthapp.feature.progress.ui.progress.components.SubjectSwitcher

/**
 * Calories and macros over the picked window, then the plates — a route of its own,
 * `SleepScreen`'s shape.
 *
 * It draws the meal-photo gallery over itself, which is the page's one overlay and the only one in
 * this feature with **two** levels of back: the gallery closes the full-frame plate before closing
 * itself, and wires that handler on its own.
 */
@Composable
internal fun NutritionScreen(
    onSwitchSubject: (Subject) -> Unit,
    onOpenRecap: () -> Unit,
    onAskCoach: (String) -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NutritionViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    NutritionContent(
        dailyNutrition = uiState.dailyNutrition,
        mealPhotos = uiState.mealPhotos,
        targets = uiState.targets,
        nutrientTargets = uiState.nutrientTargets,
        onSwitchSubject = onSwitchSubject,
        onOpenRecap = onOpenRecap,
        onAskCoach = onAskCoach,
        onExitFlow = onExitFlow,
        modifier = modifier,
    )
}

@Composable
private fun NutritionContent(
    dailyNutrition: List<DayNutrition>,
    mealPhotos: List<FoodEntry>,
    targets: DailyTargets?,
    nutrientTargets: Nutrients?,
    onSwitchSubject: (Subject) -> Unit,
    onOpenRecap: () -> Unit,
    onAskCoach: (String) -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    state: NutritionState = rememberNutritionState(),
) {
    // The overview's own rule, from `summarize()`: nothing logged in the last week is what makes
    // the card dashed, so reading it the same way here keeps the card and the page agreeing about
    // whether there is anything to show.
    val anythingLogged = dailyNutrition.takeLast(PREVIEW_POINTS).averages().daysLogged > 0

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Zero insets: this bar sits in the scaffold's content, which its `innerPadding` has
            // already cleared of the status bar. See `SleepScreen`.
            AppTopBar(
                title = stringResource(Subject.Nutrition.label),
                onBack = onExitFlow,
                windowInsets = WindowInsets(0),
                actions = {
                    AskCoachAction(subject = Subject.Nutrition, onAskCoach = onAskCoach)
                    IconButton(onClick = onOpenRecap) {
                        Icon(
                            imageVector = AppIcons.Share,
                            contentDescription = stringResource(R.string.progress_recap),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
            if (!anythingLogged) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        FullScreenState(
                            icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
                            heading = stringResource(R.string.progress_empty_nutrition_heading),
                            body = stringResource(R.string.progress_empty_nutrition_body),
                        )
                    }
                    SubjectSwitcher(
                        subject = Subject.Nutrition,
                        onSelect = onSwitchSubject,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    NutritionBody(
                        dailyNutrition = dailyNutrition,
                        mealPhotos = mealPhotos,
                        targets = targets,
                        nutrientTargets = nutrientTargets,
                        state = state,
                    )
                    SubjectSwitcher(subject = Subject.Nutrition, onSelect = onSwitchSubject)
                }
            }
        }
    }

    if (state.galleryOpen) {
        MealPhotoGallery(
            photos = mealPhotos,
            viewedId = state.viewedMealPhotoId,
            onView = { state.viewedMealPhotoId = it },
            onClose = state::closeGallery,
        )
    }
}

/**
 * The one dense series on this screen — a row per day for the last year, so its window is a plain
 * tail slice rather than the date filter every sparse subject needs.
 *
 * The averages are over **logged days only**: averaging the zero-filled gaps in would report a
 * number the user never ate, which is also why the chip says how many days it counted.
 * [NutritionAverageCard] carries the macro breakdown, so there are no stat rows under it — the
 * three macro colours are fixed app-wide and a second, colourless table of the same figures would
 * only invite them to disagree.
 */
@Composable
private fun ColumnScope.NutritionBody(
    dailyNutrition: List<DayNutrition>,
    mealPhotos: List<FoodEntry>,
    targets: DailyTargets?,
    nutrientTargets: Nutrients?,
    state: NutritionState,
) {
    val range = state.range
    val days = dailyNutrition.takeLast(range.days)
    val averages = days.averages()
    val target = targets?.calories

    HeroValue(value = "${averages.calories}", caption = stringResource(R.string.progress_nutrition_hero))
    FactChipRow(
        chips = listOfNotNull(
            target?.let {
                FactChip(
                    when {
                        averages.calories < it -> stringResource(R.string.progress_nutrition_under, it - averages.calories)
                        averages.calories > it -> stringResource(R.string.progress_nutrition_over, averages.calories - it)
                        else -> stringResource(R.string.progress_nutrition_on_target)
                    },
                )
            },
            FactChip(stringResource(R.string.progress_nutrition_days, averages.daysLogged)),
        ),
    )
    ChartCard(
        title = stringResource(R.string.progress_nutrition_calories),
        range = range,
        onRangeChange = { state.range = it },
        legend = listOfNotNull(
            LegendEntry(stringResource(R.string.progress_nutrition_intake), MaterialTheme.colorScheme.primary),
            target?.let { LegendEntry(stringResource(R.string.progress_nutrition_target, it), MaterialTheme.colorScheme.onSurfaceVariant, dashed = true) },
        ),
    ) {
        NutritionTrendChart(days = days, targetCalories = target)
    }
    NutritionAverageCard(averages = averages, targets = targets, nutrientTargets = nutrientTargets)
    // Last, and unranged: the plates are the newest ones kept, not a slice of the toggle above —
    // a photo history that thinned out when someone picked "1M" would be lying about what it has.
    MealPhotoStrip(photos = mealPhotos, onOpen = state::openGallery)
}

private fun daysPreview(today: Long): List<DayNutrition> =
    listOf(1850, 2100, 0, 1720, 2340).mapIndexed { index, calories ->
        DayNutrition(today - 4 + index, calories, calories / 16, calories / 10, calories / 30)
    }

private fun targetsPreview() = DailyTargets(
    calories = 1941,
    proteinG = 146,
    carbsG = 194,
    fatG = 65,
    floor = 1500,
)

@PreviewLightDark
@Composable
private fun NutritionScreenPreview() {
    AppTheme {
        NutritionContent(
            dailyNutrition = daysPreview(todayEpochDay()),
            mealPhotos = emptyList(),
            targets = targetsPreview(),
            nutrientTargets = null,
            onSwitchSubject = {},
            onOpenRecap = {},
            onAskCoach = {},
            onExitFlow = {},
        )
    }
}

/** Nothing eaten this week — the page is still a real page, with the way on to its siblings. */
@PreviewLightDark
@Composable
private fun NutritionScreenEmptyPreview() {
    AppTheme {
        NutritionContent(
            dailyNutrition = emptyList(),
            mealPhotos = emptyList(),
            targets = targetsPreview(),
            nutrientTargets = null,
            onSwitchSubject = {},
            onOpenRecap = {},
            onAskCoach = {},
            onExitFlow = {},
        )
    }
}
