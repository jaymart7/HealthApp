package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import java.util.Calendar
import ph.mart.healthapp.core.data.exercise.budgetKcal
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.data.profile.trendVsSevenDaysAgo
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.theme.Motion
import ph.mart.healthapp.feature.home.ui.HomeEvent
import ph.mart.healthapp.feature.home.ui.daysSincePhoto
import ph.mart.healthapp.feature.home.ui.greetingFor
import ph.mart.healthapp.feature.home.ui.insightFor
import ph.mart.healthapp.feature.home.ui.HomeScreenState
import ph.mart.healthapp.feature.home.ui.HomeUiState
import ph.mart.healthapp.feature.home.ui.components.AIInsightCard
import ph.mart.healthapp.feature.home.ui.components.CalorieRingCard
import ph.mart.healthapp.feature.home.ui.components.MacroSummaryCard
import ph.mart.healthapp.feature.home.ui.components.MascotGreetingCard
import ph.mart.healthapp.feature.home.ui.components.MoodCard
import ph.mart.healthapp.feature.home.ui.components.ProgressPhotoReminderCard
import ph.mart.healthapp.feature.home.ui.components.SleepCard
import ph.mart.healthapp.feature.home.ui.components.StepsCard
import ph.mart.healthapp.feature.home.ui.components.StreakCard
import ph.mart.healthapp.feature.home.ui.components.WaterCard
import ph.mart.healthapp.feature.home.ui.components.WeightMetricCard

/**
 * Every card on a populated Home, in order, with the stagger applied.
 *
 * No `@PreviewLightDark` of its own: this *is* the populated screen, so `HomeScreenPreview` in
 * HomeScreen.kt already renders it — a second preview here would be the same picture.
 */
@Composable
internal fun HomeCards(
    uiState: HomeUiState,
    state: HomeScreenState,
    scrollState: ScrollState,
    onAddPhoto: () -> Unit,
    onEvent: (HomeEvent) -> Unit,
) {
    // ponytail: the greeting is fixed for the life of the composition — it won't re-read the
    // clock if the app sits open across noon. Key it off a ticker if that ever matters.
    val greeting = remember { greetingFor(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    val targets = uiState.profile?.dailyTargets()
    val trend = uiState.weightEntries.trendVsSevenDaysAgo(
        fallbackKg = uiState.profile?.weightKg ?: 0.0,
    )
    val insight = targets?.let { insightFor(uiState.totals, it, trend) }

    // Keyed on Unit, so the entrance runs once when the screen arrives and never again — logging
    // a glass of water must not re-run the curtain.
    var appear by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appear = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .padding(bottom = DockedFabContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MascotGreetingCard(greeting = greeting, modifier = appearModifier(0, appear))

        // The insight's *arrival* is the news, so it gets its own entrance rather than a slot in
        // the stagger — and its exit is what stops the seven cards below from jumping on dismiss.
        AnimatedVisibility(
            visible = insight != null && !state.insightDismissed,
            enter = expandVertically(tween(Motion.Enter, easing = Motion.EmphasizedDecelerate)) +
                fadeIn(tween(Motion.Enter)),
            exit = shrinkVertically(tween(Motion.State, easing = Motion.EmphasizedAccelerate)) +
                fadeOut(tween(Motion.Feedback)),
        ) {
            // Same shape as rememberUpdatedState: hold the last text so the card keeps its words
            // through the exit instead of blanking before it collapses.
            val shown = remember { mutableStateOf("") }.apply { if (insight != null) value = insight }
            AIInsightCard(text = shown.value, onDismiss = { state.insightDismissed = true })
        }

        if (targets != null) {
            CalorieRingCard(
                consumedKcal = uiState.totals.calories,
                goalKcal = budgetKcal(targets.calories, uiState.burnedKcal, uiState.addExerciseToBudget),
                burnedKcal = if (uiState.addExerciseToBudget) uiState.burnedKcal else 0,
                modifier = appearModifier(1, appear),
            )
        }

        StreakCard(
            streak = uiState.streak,
            weightProgressKg = uiState.weightProgressKg,
            unit = uiState.profile?.preferredUnit ?: UnitSystem.Metric,
            modifier = appearModifier(2, appear),
        )

        WaterCard(
            glasses = uiState.waterGlasses,
            goalGlasses = uiState.waterGoalGlasses,
            unit = uiState.profile?.preferredUnit ?: UnitSystem.Metric,
            onSetGlasses = { glasses -> onEvent(HomeEvent.OnSetWaterGlasses(glasses)) },
            modifier = appearModifier(3, appear),
        )

        // Hidden rather than zeroed when Google Health isn't connected or hasn't synced a night.
        uiState.lastNight?.let { night ->
            SleepCard(night = night, modifier = appearModifier(4, appear))
        }

        uiState.steps?.let { steps ->
            StepsCard(
                steps = steps,
                creditKcal = if (uiState.addExerciseToBudget) uiState.stepsCreditKcal else 0,
                modifier = appearModifier(5, appear),
            )
        }

        MoodCard(
            mood = uiState.moodLevel,
            energy = uiState.energyLevel,
            onSetMood = { level -> onEvent(HomeEvent.OnSetMood(level)) },
            onSetEnergy = { level -> onEvent(HomeEvent.OnSetEnergy(level)) },
            modifier = appearModifier(6, appear),
        )

        WeightMetricCard(
            trend = trend,
            goal = uiState.profile?.goal,
            unit = uiState.profile?.preferredUnit ?: UnitSystem.Metric,
            modifier = appearModifier(7, appear),
        )

        if (targets != null) {
            MacroSummaryCard(
                consumed = uiState.totals,
                targets = targets,
                modifier = appearModifier(8, appear),
            )
        }

        ProgressPhotoReminderCard(
            daysSinceLastPhoto = daysSincePhoto(uiState.lastPhotoEpochDay, todayEpochDay()),
            onTakePhoto = onAddPhoto,
            modifier = appearModifier(9, appear),
        )
    }
}

/**
 * Fade and 8dp rise, [Motion.StaggerStep] apart, capped at [Motion.StaggerCap] so the curtain
 * can't grow if the screen gains cards. The animated value is read inside the `graphicsLayer`
 * lambda, so the whole entrance settles in the Draw phase and recomposes nothing.
 */
@Composable
private fun appearModifier(index: Int, appear: Boolean): Modifier {
    val progress = animateFloatAsState(
        targetValue = if (appear) 1f else 0f,
        animationSpec = tween(
            durationMillis = Motion.Enter,
            delayMillis = minOf(index, Motion.StaggerCap) * Motion.StaggerStep,
            easing = Motion.Standard,
        ),
        label = "cardAppear$index",
    )
    return Modifier.graphicsLayer {
        val value = progress.value
        alpha = value
        translationY = (1f - value) * 8.dp.toPx()
    }
}
