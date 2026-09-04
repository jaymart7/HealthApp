package ph.mart.healthapp.feature.progress.ui.progress

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.mood.MoodDay
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.EnergyCheckIn
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.energyCheckIn
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.progress.goalProjection
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.energy.EnergyCheckInEvent
import ph.mart.healthapp.feature.progress.ui.energy.EnergyCheckInScreen
import ph.mart.healthapp.feature.progress.ui.energy.EnergyCheckInViewModel
import ph.mart.healthapp.feature.progress.ui.measurement.AddMeasurementSheet
import ph.mart.healthapp.feature.progress.ui.photo.components.PhotoComparisonScreen
import ph.mart.healthapp.feature.progress.ui.photo.components.TimelapseScreen
import ph.mart.healthapp.feature.progress.ui.pressure.LogBloodPressureSheet
import ph.mart.healthapp.feature.progress.ui.progress.components.ProgressOverview
import ph.mart.healthapp.feature.progress.ui.progress.components.RecapScreen
import ph.mart.healthapp.feature.progress.ui.progress.components.SubjectDetail

@Composable
fun ProgressScreen(scrollState: ScrollState = rememberScrollState(), viewModel: ProgressViewModel = koinViewModel()) {
    val uiState by viewModel.collectAsState()
    val state = rememberProgressScreenState()
    // The one thing on this tab that writes has its own container, so ProgressViewModel stays
    // read-only. It is read here rather than inside the card because the card and the overlay
    // must fold the same numbers, and the profile is the one input ProgressUiState doesn't carry.
    val energyViewModel: EnergyCheckInViewModel = koinViewModel()
    val energyState by energyViewModel.collectAsState()
    val today = todayEpochDay()
    val checkIn = energyState.profile?.let { profile ->
        remember(uiState.dailyNutrition, uiState.weightEntries, profile, today) {
            energyCheckIn(uiState.dailyNutrition, uiState.weightEntries, profile, today)
        }
    }
    ProgressContent(
        uiState = uiState,
        state = state,
        scrollState = scrollState,
        checkIn = checkIn,
        addExerciseToBudget = energyState.profile?.addExerciseToBudget ?: true,
        onApplyTarget = { kcal -> energyViewModel.handleEvent(EnergyCheckInEvent.OnApply(kcal)) },
    )
}

/**
 * Overview or one subject's page — [ProgressScreenState.selectedSubject] is the whole navigator.
 *
 * A subject page is a swap-in rather than a Nav3 route for the reason every read-only surface on
 * this tab is: a route earns its own `ViewModelStoreOwner`, and with it a second copy of
 * [ProgressViewModel]'s twelve repositories, to draw something that writes nothing. The overview's
 * scroll position survives the round trip because [scrollState] is hoisted all the way up in
 * `AppScaffold`, and a detail page holds its own.
 *
 * The four overlays and two sheets sit outside the swap, so a comparison, a timelapse, the recap or
 * the energy check-in can be opened from either surface and drawn over both.
 */
@Composable
private fun ProgressContent(
    uiState: ProgressUiState,
    state: ProgressScreenState,
    scrollState: ScrollState = rememberScrollState(),
    checkIn: EnergyCheckIn? = null,
    addExerciseToBudget: Boolean = true,
    onApplyTarget: (Int) -> Unit = {},
) {
    val today = todayEpochDay()
    // Above everything and inside nothing: the recap spans nutrition, weight and consistency at
    // once. Null (nothing logged this week) omits the card entirely rather than rendering an
    // all-zero one on day one — and takes the share door with it, since there is then nothing to
    // report. Always the week here; the longer periods are the recap screen's, which folds its own.
    val weekRecap = recap(
        period = DEFAULT_RECAP_PERIOD,
        dailyNutrition = uiState.dailyNutrition,
        activeDays = uiState.activeDays,
        weightEntries = uiState.weightEntries,
        moodDays = uiState.moodDays,
        targets = uiState.targets,
        todayEpochDay = today,
    )
    val projection = goalProjection(
        weightEntries = uiState.weightEntries,
        goalWeightKg = uiState.goalWeightKg,
        goal = uiState.goal,
        todayEpochDay = today,
    )
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            val subject = state.selectedSubject
            if (subject == null) {
                ProgressOverview(
                    uiState = uiState,
                    state = state,
                    weekRecap = weekRecap,
                    projection = projection,
                    scrollState = scrollState,
                )
            } else {
                SubjectDetail(
                    subject = subject,
                    uiState = uiState,
                    state = state,
                    checkIn = checkIn,
                    projection = projection,
                    canShare = weekRecap != null,
                )
            }

            val selectedPhotos = uiState.photos.filter { it.id in state.selectedPhotoIds }
            if (selectedPhotos.size == 2) {
                val (older, newer) = selectedPhotos.sortedBy { it.dateEpochDay }
                PhotoComparisonScreen(
                    photoA = older,
                    photoB = newer,
                    unit = uiState.preferredUnit,
                    onClose = { state.selectedPhotoIds = emptyList() },
                )
            }

            if (state.activeTimelapse && uiState.photos.size >= 2) {
                TimelapseScreen(
                    photos = uiState.photos,
                    unit = uiState.preferredUnit,
                    onClose = state::closeTimelapse,
                )
            }

            // A full-screen overlay inside the tab, like the two photo ones — it reads the same
            // combined state, so it needs neither a route nor a second ViewModel. It owns its own
            // share sheet, which is why nothing here does.
            if (state.activeRecap) {
                RecapScreen(
                    uiState = uiState,
                    period = state.recapPeriod,
                    projection = projection,
                    onPeriodChange = { state.recapPeriod = it },
                    onClose = state::closeRecap,
                )
            }

            // The third overlay, and the only one that writes — the apply goes back up to the
            // container that owns the profile rather than being reached for down here.
            if (state.activeEnergyCheckIn && checkIn != null) {
                EnergyCheckInScreen(
                    checkIn = checkIn,
                    unit = uiState.preferredUnit,
                    addExerciseToBudget = addExerciseToBudget,
                    onApply = onApplyTarget,
                    onClose = state::closeEnergyCheckIn,
                )
            }

            if (state.activeBloodPressureSheet) {
                LogBloodPressureSheet(onDismiss = state::closeBloodPressureSheet)
            }

            if (state.activeMeasurementSheet) {
                AddMeasurementSheet(
                    trackedParts = uiState.measurements.keys,
                    preselectedPart = state.measurementSheetPart,
                    unit = uiState.preferredUnit,
                    onDismiss = state::closeMeasurementSheet,
                )
            }
        }
    }
}

private fun previewState(): ProgressUiState {
    val today = todayEpochDay()
    return ProgressUiState(
        weightEntries = (0..8).map {
            WeightEntry(dateEpochDay = today - (8 - it) * 3, weightKg = 84.8 - it * 0.26)
        },
        goalWeightKg = 82.0,
        goal = Goal.Lose,
        preferredUnit = UnitSystem.Metric,
        dailyNutrition = listOf(1850, 2100, 0, 1720, 2340, 1610, 1490).mapIndexed { index, calories ->
            DayNutrition(today - 6 + index, calories, calories / 16, calories / 10, calories / 30)
        },
        activeDays = (today - 6..today).toSet(),
        moodDays = listOf(4 to 3, 5 to 4, 3 to 2, 4 to 4).mapIndexed { index, (mood, energy) ->
            MoodDay(today - 4 + index, mood, energy)
        },
        targets = DailyTargets(calories = 2261, proteinG = 170, carbsG = 226, fatG = 75, floor = 1500),
    )
}

@PreviewLightDark
@Composable
private fun ProgressScreenPreview() {
    AppTheme {
        ProgressContent(uiState = previewState(), state = ProgressScreenState())
    }
}

/** One subject open — the same screen, one field different. */
@PreviewLightDark
@Composable
private fun ProgressDetailPreview() {
    AppTheme {
        ProgressContent(
            uiState = previewState(),
            state = ProgressScreenState(selectedSubject = Subject.Weight),
        )
    }
}
