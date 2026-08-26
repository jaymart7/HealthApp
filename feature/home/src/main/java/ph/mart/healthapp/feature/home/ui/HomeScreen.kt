package ph.mart.healthapp.feature.home.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import java.util.Calendar
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.food.DiaryTotals
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.designsystem.component.FabBottomClearance
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.todayEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.home.ui.components.AIInsightCard
import ph.mart.healthapp.feature.home.ui.components.CalorieRingCard
import ph.mart.healthapp.feature.home.ui.components.MacroSummaryCard
import ph.mart.healthapp.feature.home.ui.components.MascotGreetingCard
import ph.mart.healthapp.feature.home.ui.components.ProgressPhotoReminderCard
import ph.mart.healthapp.feature.home.ui.components.WeightMetricCard

@Composable
fun HomeScreen(onAddPhoto: () -> Unit, scrollState: ScrollState = rememberScrollState(), viewModel: HomeViewModel = koinViewModel()) {
    val uiState by viewModel.collectAsState()
    val state = rememberHomeScreenState()
    HomeContent(uiState = uiState, state = state, scrollState = scrollState, onAddPhoto = onAddPhoto)
}

@Composable
private fun HomeContent(uiState: HomeUiState, state: HomeScreenState, onAddPhoto: () -> Unit, scrollState: ScrollState = rememberScrollState()) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        if (uiState.isDayOne) {
            FullScreenState(
                icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
                heading = "Let's log your first meal",
                body = "Tap Log below to add food by photo or search.",
            )
            return@Surface
        }

        // ponytail: the greeting is fixed for the life of the composition — it won't re-read the
        // clock if the app sits open across noon. Key it off a ticker if that ever matters.
        val greeting = remember { greetingFor(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
        val targets = uiState.profile?.dailyTargets()
        val trend = uiState.weightEntries.trendVsSevenDaysAgo(
            fallbackKg = uiState.profile?.weightKg ?: 0.0,
        )
        val insight = targets?.let { insightFor(uiState.totals, it, trend) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = FabBottomClearance),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MascotGreetingCard(greeting = greeting)

            if (insight != null && !state.insightDismissed) {
                AIInsightCard(text = insight, onDismiss = { state.insightDismissed = true })
            }

            if (targets != null) {
                CalorieRingCard(consumedKcal = uiState.totals.calories, goalKcal = targets.calories)
            }

            WeightMetricCard(
                trend = trend,
                goal = uiState.profile?.goal,
                unit = uiState.profile?.preferredUnit ?: UnitSystem.Metric,
            )

            if (targets != null) {
                MacroSummaryCard(consumed = uiState.totals, targets = targets)
            }

            ProgressPhotoReminderCard(
                daysSinceLastPhoto = daysSincePhoto(uiState.lastPhotoEpochDay, todayEpochDay()),
                onTakePhoto = onAddPhoto,
            )
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
                profile = PreviewProfile,
                totals = DiaryTotals(calories = 940, proteinG = 62, carbsG = 88, fatG = 31),
                foodEntryCount = 3,
                weightEntries = listOf(
                    WeightEntry(dateEpochDay = today - 8, weightKg = 77.1),
                    WeightEntry(dateEpochDay = today, weightKg = 76.5),
                ),
                lastPhotoEpochDay = today - 12,
            ),
            state = HomeScreenState(),
            onAddPhoto = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun HomeScreenDayOnePreview() {
    AppTheme {
        HomeContent(
            uiState = HomeUiState(profile = PreviewProfile),
            state = HomeScreenState(),
            onAddPhoto = {},
        )
    }
}
