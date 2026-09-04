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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import java.util.Calendar
import ph.mart.healthapp.core.data.exercise.anyScheduled
import ph.mart.healthapp.core.data.exercise.budgetKcal
import ph.mart.healthapp.core.data.exercise.plannedOn
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.data.profile.trendVsSevenDaysAgo
import ph.mart.healthapp.core.data.progress.goalProjection
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.component.HomeCard
import ph.mart.healthapp.core.designsystem.component.homeCardLayout
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.theme.Motion
import ph.mart.healthapp.feature.home.ui.HomeEvent
import ph.mart.healthapp.feature.home.ui.daysSincePhoto
import ph.mart.healthapp.feature.home.ui.greetingFor
import ph.mart.healthapp.core.data.insight.insightFor
import ph.mart.healthapp.feature.home.ui.HomeScreenState
import ph.mart.healthapp.feature.home.ui.HomeUiState
import ph.mart.healthapp.core.designsystem.component.AIInsightCard
import ph.mart.healthapp.feature.home.ui.components.BloodPressureCard
import ph.mart.healthapp.feature.home.ui.components.CalorieRingCard
import ph.mart.healthapp.feature.home.ui.components.FastingCard
import ph.mart.healthapp.feature.home.ui.components.HeartCard
import ph.mart.healthapp.feature.home.ui.components.MacroSummaryCard
import ph.mart.healthapp.feature.home.ui.components.MascotGreetingCard
import ph.mart.healthapp.feature.home.ui.components.MoodCard
import ph.mart.healthapp.feature.home.ui.components.ProgressPhotoReminderCard
import ph.mart.healthapp.feature.home.ui.components.SleepCard
import ph.mart.healthapp.feature.home.ui.components.StepsCard
import ph.mart.healthapp.feature.home.ui.components.StreakCard
import ph.mart.healthapp.feature.home.ui.components.SupplementsCard
import ph.mart.healthapp.feature.home.ui.components.TrainingPlanCard
import ph.mart.healthapp.feature.home.ui.components.WaterCard
import ph.mart.healthapp.feature.home.ui.components.WeightMetricCard

/**
 * Every card on a populated Home, in the user's order, with the stagger applied.
 *
 * The greeting and the insight are pinned above the customizable block — see
 * [ph.mart.healthapp.core.designsystem.component.HomeCard] for why those two never move.
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
    onOpenCoach: () -> Unit,
    onStartRoutine: (Long) -> Unit,
    onEvent: (HomeEvent) -> Unit,
) {
    // ponytail: the greeting is fixed for the life of the composition — it won't re-read the
    // clock if the app sits open across noon. Key it off a ticker if that ever matters.
    val greeting = remember { greetingFor(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    val targets = uiState.profile?.dailyTargets()
    val trend = uiState.weightEntries.trendVsSevenDaysAgo(
        fallbackKg = uiState.profile?.weightKg ?: 0.0,
    )
    // Derived here beside the trend, not stored on HomeUiState — a fold over entries the state
    // already holds, the same way `targets` and `trend` are. Null omits the line.
    val projection = goalProjection(
        weightEntries = uiState.weightEntries,
        goalWeightKg = uiState.profile?.targetWeightKg,
        goal = uiState.profile?.goal,
        todayEpochDay = todayEpochDay(),
    )
    // The model's line when it answered, the rules when it didn't — offline, a failed call
    // and a model with nothing to say all land on the same three rules that shipped before
    // there was a model at all.
    val insight = uiState.aiInsight ?: targets?.let { insightFor(uiState.totals, it, trend) }

    // Derived here beside `targets` and `projection`, off the profile the state already holds —
    // nothing about the layout is stored on HomeUiState. Null (an untouched install) is the
    // declaration order with nothing hidden.
    //
    // ponytail: a hidden card's flows are still collected — HomeViewModel combines everything in
    // one chain, and splitting it per card would be a large conditional-flow change for a gain
    // nobody has measured. Gate the combine if a profiler ever says it costs something.
    val layout = homeCardLayout(uiState.profile?.homeLayout)

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
        MascotGreetingCard(
            greeting = greeting,
            onOpenCoach = onOpenCoach,
            modifier = appearModifier(0, appear),
        )

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

        // One pass over the user's order. Every data gate below is the one that was already
        // there: the switch can only ever *remove* a card, never force one that has nothing to
        // draw, so a Sleep card left on with no watch stays as absent as it was before.
        layout.filter { it.visible }.forEachIndexed { index, (card, _) ->
            // Keyed on the card, not its position, so the entrance state stays with the card the
            // user just moved rather than with the slot it left.
            key(card) {
                val appearance = appearModifier(index, appear)
                when (card) {
                    HomeCard.Calories -> if (targets != null) {
                        CalorieRingCard(
                            consumedKcal = uiState.totals.calories,
                            goalKcal = budgetKcal(targets.calories, uiState.burnedKcal, uiState.addExerciseToBudget),
                            burnedKcal = if (uiState.addExerciseToBudget) uiState.burnedKcal else 0,
                            modifier = appearance,
                        )
                    }

                    HomeCard.Streak -> StreakCard(
                        streak = uiState.streak,
                        weightProgressKg = uiState.weightProgressKg,
                        unit = uiState.profile?.preferredUnit ?: UnitSystem.Metric,
                        modifier = appearance,
                    )

                    HomeCard.Water -> WaterCard(
                        glasses = uiState.waterGlasses,
                        goalGlasses = uiState.waterGoalGlasses,
                        unit = uiState.profile?.preferredUnit ?: UnitSystem.Metric,
                        onSetGlasses = { glasses -> onEvent(HomeEvent.OnSetWaterGlasses(glasses)) },
                        modifier = appearance,
                    )

                    // Always shown when it's on, unlike the two Google Health cards below: a fast
                    // that hasn't started is an invitation, not an absence of data.
                    HomeCard.Fasting -> FastingCard(
                        activeFast = uiState.activeFast,
                        goalHours = uiState.fastingGoalHours,
                        onStart = { onEvent(HomeEvent.OnStartFast) },
                        onEnd = { onEvent(HomeEvent.OnEndFast) },
                        onDiscard = { onEvent(HomeEvent.OnDiscardFast) },
                        modifier = appearance,
                    )

                    // Hidden until a routine has days set — nothing is missing, nothing has been
                    // planned yet (`SupplementsCard`'s rule). A day with nothing planned still
                    // draws: "Rest day" is the answer, not an absence.
                    HomeCard.Workout -> if (uiState.routines.anyScheduled()) {
                        TrainingPlanCard(
                            todayRoutines = uiState.routines.plannedOn(todayEpochDay()),
                            week = uiState.trainingWeek,
                            trained = uiState.trainingWeek.any { it.isToday && it.trained },
                            onStart = onStartRoutine,
                            modifier = appearance,
                        )
                    }

                    // Hidden rather than zeroed when Google Health isn't connected or hasn't synced.
                    HomeCard.Sleep -> uiState.lastNight?.let { night ->
                        SleepCard(night = night, modifier = appearance)
                    }

                    HomeCard.Steps -> uiState.steps?.let { steps ->
                        StepsCard(
                            steps = steps,
                            goal = uiState.stepGoal,
                            creditKcal = if (uiState.addExerciseToBudget) uiState.stepsCreditKcal else 0,
                            modifier = appearance,
                        )
                    }

                    HomeCard.Heart -> uiState.heart?.let { heart ->
                        HeartCard(heart = heart, modifier = appearance)
                    }

                    // The latest reading, not today's, and hidden until there is one — see the card.
                    HomeCard.BloodPressure -> uiState.latestBloodPressure?.let { reading ->
                        BloodPressureCard(
                            reading = reading,
                            todayEpochDay = todayEpochDay(),
                            modifier = appearance,
                        )
                    }

                    HomeCard.Mood -> MoodCard(
                        mood = uiState.moodLevel,
                        energy = uiState.energyLevel,
                        onSetMood = { level -> onEvent(HomeEvent.OnSetMood(level)) },
                        onSetEnergy = { level -> onEvent(HomeEvent.OnSetEnergy(level)) },
                        modifier = appearance,
                    )

                    // Hidden when the list is empty, like the three watch cards above — but for a
                    // different reason: there is nothing to import, there is nothing the user has
                    // authored yet.
                    HomeCard.Supplements -> SupplementsCard(
                        supplements = uiState.supplements,
                        onSetTaken = { id, taken -> onEvent(HomeEvent.OnSetSupplementTaken(id, taken)) },
                        modifier = appearance,
                    )

                    HomeCard.Weight -> WeightMetricCard(
                        trend = trend,
                        goal = uiState.profile?.goal,
                        unit = uiState.profile?.preferredUnit ?: UnitSystem.Metric,
                        projection = projection,
                        modifier = appearance,
                    )

                    HomeCard.Macros -> if (targets != null) {
                        MacroSummaryCard(
                            consumed = uiState.totals,
                            targets = targets,
                            modifier = appearance,
                        )
                    }

                    HomeCard.ProgressPhoto -> ProgressPhotoReminderCard(
                        daysSinceLastPhoto = daysSincePhoto(uiState.lastPhotoEpochDay, todayEpochDay()),
                        onTakePhoto = onAddPhoto,
                        modifier = appearance,
                    )
                }
            }
        }
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
