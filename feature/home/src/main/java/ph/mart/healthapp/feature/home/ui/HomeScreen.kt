package ph.mart.healthapp.feature.home.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.food.DiaryTotals
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.streak.StreakStats
import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.core.data.supplement.SupplementToday
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.Motion
import ph.mart.healthapp.feature.home.ui.components.HomeCards

@Composable
fun HomeScreen(onAddPhoto: () -> Unit, scrollState: ScrollState = rememberScrollState(), viewModel: HomeViewModel = koinViewModel()) {
    val uiState by viewModel.collectAsState()
    val state = rememberHomeScreenState()
    HomeContent(
        uiState = uiState,
        state = state,
        scrollState = scrollState,
        onAddPhoto = onAddPhoto,
        onEvent = viewModel::handleEvent,
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    state: HomeScreenState,
    onAddPhoto: () -> Unit,
    onEvent: (HomeEvent) -> Unit,
    scrollState: ScrollState = rememberScrollState(),
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = homePhase(uiState),
            // Material fade-through: the outgoing phase clears before the incoming one arrives.
            // These are unrelated contents, not a shared container, so nothing should slide.
            transitionSpec = {
                fadeIn(tween(Motion.Enter, delayMillis = 90)) togetherWith fadeOut(tween(90))
            },
            label = "homePhase",
        ) { phase ->
            when (phase) {
                HomePhase.Loading -> Box(modifier = Modifier.fillMaxSize())

                HomePhase.DayOne -> FullScreenState(
                    icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
                    heading = "Let's log your first meal",
                    body = "Tap Log below to add food by photo or search.",
                )

                HomePhase.Populated -> HomeCards(
                    uiState = uiState,
                    state = state,
                    scrollState = scrollState,
                    onAddPhoto = onAddPhoto,
                    onEvent = onEvent,
                )
            }
        }
    }
}

private val PreviewProfile = Profile(
    sex = Sex.Male,
    age = 26,
    heightCm = 170.0,
    weightKg = 76.5,
    activityLevel = ActivityLevel.Sedentary,
    goal = Goal.Lose,
    targetWeightKg = 72.0,
)

@PreviewLightDark
@Composable
private fun HomeScreenPreview() {
    val today = todayEpochDay()
    AppTheme {
        HomeContent(
            uiState = HomeUiState(
                loaded = true,
                profile = PreviewProfile,
                totals = DiaryTotals(calories = 940, proteinG = 62, carbsG = 88, fatG = 31),
                foodEntryCount = 3,
                weightEntries = listOf(
                    WeightEntry(dateEpochDay = today - 8, weightKg = 77.1),
                    WeightEntry(dateEpochDay = today, weightKg = 76.5),
                ),
                lastPhotoEpochDay = today - 12,
                waterGlasses = 5,
                moodLevel = 4,
                energyLevel = 3,
                burnedKcal = 320,
                supplements = listOf(
                    SupplementToday(Supplement(id = 1, name = "Vitamin D", dose = "2000 IU"), taken = 1),
                    SupplementToday(Supplement(id = 2, name = "Creatine", dose = "5 g", timesPerDay = 2), taken = 1),
                ),
                streak = StreakStats(current = 12, best = 31, totalDaysLogged = 74),
                weightProgressKg = 5.2,
            ),
            state = HomeScreenState(),
            onAddPhoto = {},
            onEvent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun HomeScreenDayOnePreview() {
    AppTheme {
        HomeContent(
            uiState = HomeUiState(loaded = true, profile = PreviewProfile),
            state = HomeScreenState(),
            onAddPhoto = {},
            onEvent = {},
        )
    }
}
