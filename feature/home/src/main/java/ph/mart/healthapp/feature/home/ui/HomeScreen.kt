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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.food.DiaryTotals
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureReading
import ph.mart.healthapp.core.data.health.HeartDay
import ph.mart.healthapp.core.data.health.SleepNight
import ph.mart.healthapp.core.data.health.StepDay
import ph.mart.healthapp.core.data.streak.StreakStats
import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.core.data.supplement.SupplementToday
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.Motion
import ph.mart.healthapp.feature.home.R
import ph.mart.healthapp.feature.home.ui.components.HomeCards

@Composable
fun HomeScreen(
    onAddPhoto: () -> Unit,
    onOpenCoach: () -> Unit,
    onStartRoutine: (Long) -> Unit,
    onOpenHomeLayout: () -> Unit,
    scrollState: ScrollState = rememberScrollState(),
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    val state = rememberHomeScreenState()
    HomeContent(
        uiState = uiState,
        state = state,
        scrollState = scrollState,
        onAddPhoto = onAddPhoto,
        onOpenCoach = onOpenCoach,
        onStartRoutine = onStartRoutine,
        onOpenHomeLayout = onOpenHomeLayout,
        onEvent = viewModel::handleEvent,
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    state: HomeScreenState,
    onAddPhoto: () -> Unit,
    onOpenCoach: () -> Unit,
    onStartRoutine: (Long) -> Unit,
    onOpenHomeLayout: () -> Unit,
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
                    heading = stringResource(R.string.home_empty_heading),
                    body = stringResource(R.string.home_empty_body),
                )

                HomePhase.Populated -> HomeCards(
                    uiState = uiState,
                    state = state,
                    scrollState = scrollState,
                    onAddPhoto = onAddPhoto,
                    onOpenCoach = onOpenCoach,
                    onStartRoutine = onStartRoutine,
                    onOpenHomeLayout = onOpenHomeLayout,
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
@PreviewScreenSizes
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
                lastNight = SleepNight(dateEpochDay = today, minutesAsleep = 432),
                steps = StepDay(dateEpochDay = today, steps = 8432, burnedKcal = 302),
                stepsCreditKcal = 302,
                heart = HeartDay(dateEpochDay = today, averageBpm = 68, minBpm = 52),
                latestBloodPressure = BloodPressureReading(
                    takenAtMillis = System.currentTimeMillis(),
                    systolic = 129,
                    diastolic = 85,
                    pulseBpm = 71,
                ),
            ),
            state = HomeScreenState(),
            onAddPhoto = {},
            onOpenCoach = {},
            onStartRoutine = {},
            onOpenHomeLayout = {},
            onEvent = {},
        )
    }
}

/**
 * The gated-cards-absent case, and the one this redesign has to get right.
 *
 * No profile (no Calories, no Macros), no watch (no Sleep, Steps or Heart), cycle tracking off, no
 * routine scheduled, no blood pressure ever logged, no insight and no running fast. Every one of
 * those cards is *absent*, never zeroed — and the half-width survivors re-pair around the holes
 * rather than leaving them, which is what `homeRows()` running after the gate buys.
 */
@PreviewLightDark
@Composable
private fun HomeScreenGatedPreview() {
    val today = todayEpochDay()
    AppTheme {
        HomeContent(
            uiState = HomeUiState(
                loaded = true,
                foodEntryCount = 2,
                weightEntries = listOf(WeightEntry(dateEpochDay = today, weightKg = 82.7)),
                waterGlasses = 2,
                streak = StreakStats(current = 90, best = 90, totalDaysLogged = 140),
                lastPhotoEpochDay = today - 3,
            ),
            state = HomeScreenState(),
            onAddPhoto = {},
            onOpenCoach = {},
            onStartRoutine = {},
            onOpenHomeLayout = {},
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
            onOpenCoach = {},
            onStartRoutine = {},
            onOpenHomeLayout = {},
            onEvent = {},
        )
    }
}
