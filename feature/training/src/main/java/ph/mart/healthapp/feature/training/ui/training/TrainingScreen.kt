package ph.mart.healthapp.feature.training.ui.training

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.exercise.PlanDay
import ph.mart.healthapp.core.data.exercise.Routine
import ph.mart.healthapp.core.data.exercise.RoutineLift
import ph.mart.healthapp.core.data.exercise.StrengthSet
import ph.mart.healthapp.core.data.exercise.anyScheduled
import ph.mart.healthapp.core.data.exercise.dayLabel
import ph.mart.healthapp.core.data.exercise.plannedOn
import ph.mart.healthapp.core.data.exercise.plannedSoFar
import ph.mart.healthapp.core.data.exercise.totalSets
import ph.mart.healthapp.core.data.exercise.trainedSoFar
import ph.mart.healthapp.core.data.exercise.weekdayInitials
import ph.mart.healthapp.core.data.exercise.weekdayNames
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PlanCell
import ph.mart.healthapp.core.designsystem.component.PlannedRoutine
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.component.TrainingPlanCard
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.training.R
import ph.mart.healthapp.feature.training.ui.training.components.RecentWorkoutsCard
import ph.mart.healthapp.feature.training.ui.training.components.TodaySessionsCard

@Composable
fun TrainingScreen(
    onLogExercise: (Long, Long) -> Unit,
    onOpenStrength: (Long, Long) -> Unit,
    onStartRoutine: (Long) -> Unit,
    scrollState: ScrollState = rememberScrollState(),
    viewModel: TrainingViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    TrainingContent(
        uiState = uiState,
        scrollState = scrollState,
        onLogExercise = onLogExercise,
        onOpenStrength = onOpenStrength,
        onStartRoutine = onStartRoutine,
    )
}

/**
 * Today and doing: what the plan asks for, what has been logged since midnight, the two doors that
 * log, and the last few sessions. History, charts and personal records are Progress's — this tab
 * deliberately draws none of them.
 *
 * One scrolling column at every width, like Home and for Home's reason: a second pane would need a
 * second card order to author, and nothing here stores one. No `NavigationEventHandler` either —
 * this is a tab root with no sub-views, and both of its doors are sheets or routes `AppScaffold`
 * already dispatches back for.
 */
@Composable
private fun TrainingContent(
    uiState: TrainingUiState,
    onLogExercise: (Long, Long) -> Unit,
    onOpenStrength: (Long, Long) -> Unit,
    onStartRoutine: (Long) -> Unit,
    scrollState: ScrollState = rememberScrollState(),
) {
    val today = todayEpochDay()
    val sessions = uiState.recent.recentSessions(today)
    // Day 0 is today, the convention every caller of these two uses — the tab is always today.
    val onLogActivity = { onLogExercise(0L, 0L) }
    val onLift = { onOpenStrength(0L, 0L) }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        // Nothing planned, nothing logged, nothing behind: the mascot page every empty Progress
        // subject draws, with the same two doors the tab would otherwise offer under its cards.
        if (uiState.loaded && !uiState.routines.anyScheduled() && uiState.today.isEmpty() && sessions.isEmpty()) {
            FullScreenState(
                icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
                heading = stringResource(R.string.training_empty_title),
                body = stringResource(R.string.training_empty_body),
                actions = {
                    PrimaryButton(
                        label = stringResource(R.string.training_log_activity),
                        onClick = onLogActivity,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SecondaryButton(
                        label = stringResource(R.string.training_lift),
                        onClick = onLift,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            )
            return@Surface
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            // Hidden until a routine has days on it, exactly as Home gates its own card: a plan
            // card with no plan is a card about nothing.
            if (uiState.routines.anyScheduled()) {
                TrainingPlanCard(
                    todayRoutines = uiState.routines.plannedOn(today).map { it.toPlannedRoutine() },
                    week = uiState.trainingWeek.toPlanCells(),
                    trained = uiState.trainingWeek.any { it.isToday && it.trained },
                    plannedSoFar = uiState.trainingWeek.plannedSoFar(),
                    trainedSoFar = uiState.trainingWeek.trainedSoFar(),
                    onStart = onStartRoutine,
                )
            }
            TodaySessionsCard(
                entries = uiState.today,
                unit = uiState.unit,
                onLogActivity = onLogActivity,
                onLift = onLift,
                // A workout with sets reopens on the screen that can show them; everything else
                // reopens in the sheet that logged it. The diary's own block says the same thing.
                onEditEntry = { entry ->
                    if (entry.sets.isEmpty()) onLogExercise(0, entry.id) else onOpenStrength(0, entry.id)
                },
            )
            if (sessions.isNotEmpty()) {
                RecentWorkoutsCard(entries = sessions, unit = uiState.unit)
            }
            Spacer(modifier = Modifier.height(DockedFabContentPadding))
        }
    }
}

/**
 * `:core:data`'s plan types into the card's own — the same twelve lines Home's `HomeCards` runs
 * against its own `R`, for the reason named there: the card lives in `:core:designsystem`, which
 * has no project dependencies at all.
 */
@Composable
private fun Routine.toPlannedRoutine(): PlannedRoutine = PlannedRoutine(
    id = id,
    name = name,
    summary = stringResource(
        R.string.training_plan_summary,
        pluralStringResource(R.plurals.training_routine_lifts, lifts.size, lifts.size),
        totalSets(),
        dayLabel(),
    ),
)

private fun List<PlanDay>.toPlanCells(): List<PlanCell> {
    val initials = weekdayInitials()
    val names = weekdayNames()
    return mapIndexed { index, day ->
        PlanCell(
            initial = initials[index],
            name = names[index],
            planned = day.planned,
            trained = day.trained,
            isToday = day.isToday,
        )
    }
}

private val PREVIEW_TODAY = listOf(
    ExerciseEntry(id = 1, type = ExerciseType.Run, name = "Riverside loop", minutes = 30, burnedKcal = 310),
    ExerciseEntry(id = 2, type = ExerciseType.Yoga, minutes = 25, burnedKcal = 110),
)

private val PREVIEW_RECENT = listOf(
    ExerciseEntry(
        id = 3,
        dateEpochDay = 20_100,
        type = ExerciseType.Strength,
        name = "Push day",
        minutes = 55,
        burnedKcal = 280,
        sets = listOf(
            StrengthSet("Bench press", 8, 60.0),
            StrengthSet("Bench press", 8, 60.0),
            StrengthSet("Overhead press", 8, 35.0),
        ),
    ),
)

private val PREVIEW_ROUTINE = Routine(
    id = 1,
    name = "Push day",
    lifts = listOf(RoutineLift("Bench press", 3, 8), RoutineLift("Overhead press", 3, 8)),
    days = 0b1111111,
)

@PreviewLightDark
@Composable
private fun TrainingScreenPreview() {
    AppTheme {
        TrainingContent(
            uiState = TrainingUiState(
                loaded = true,
                today = PREVIEW_TODAY,
                recent = PREVIEW_RECENT,
                routines = listOf(PREVIEW_ROUTINE),
                trainingWeek = (0..6).map { index ->
                    PlanDay(epochDay = 20_100L + index, planned = true, trained = index < 2, isToday = index == 2)
                },
            ),
            onLogExercise = { _, _ -> },
            onOpenStrength = { _, _ -> },
            onStartRoutine = {},
        )
    }
}

/** Nothing planned and nothing logged — the first-run page, and the only state with no cards. */
@PreviewLightDark
@Composable
private fun TrainingScreenEmptyPreview() {
    AppTheme {
        TrainingContent(
            uiState = TrainingUiState(loaded = true),
            onLogExercise = { _, _ -> },
            onOpenStrength = { _, _ -> },
            onStartRoutine = {},
        )
    }
}
