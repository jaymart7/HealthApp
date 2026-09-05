package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.Calendar
import ph.mart.healthapp.core.data.cycle.cycleDayNumber
import ph.mart.healthapp.core.data.cycle.cyclePrediction
import ph.mart.healthapp.core.data.cycle.periods
import ph.mart.healthapp.core.data.exercise.anyScheduled
import ph.mart.healthapp.core.data.exercise.budgetKcal
import ph.mart.healthapp.core.data.exercise.plannedOn
import ph.mart.healthapp.core.data.health.formatSteps
import ph.mart.healthapp.core.data.insight.insightFor
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.WeightTrendDisplay
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.trendVsSevenDaysAgo
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.GoalProjection
import ph.mart.healthapp.core.data.progress.goalProjection
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.component.HomeCard
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.component.homeCardLayout
import ph.mart.healthapp.core.designsystem.theme.Motion
import ph.mart.healthapp.feature.home.R
import ph.mart.healthapp.feature.home.ui.HomeEvent
import ph.mart.healthapp.feature.home.ui.HomeScreenState
import ph.mart.healthapp.feature.home.ui.HomeUiState
import ph.mart.healthapp.feature.home.ui.daysSincePhoto
import ph.mart.healthapp.feature.home.ui.greetingFor
import ph.mart.healthapp.feature.home.ui.greetingSubFor
import ph.mart.healthapp.feature.home.ui.homeRows
import ph.mart.healthapp.feature.home.ui.todayStripCards

/** 8dp top, and the FAB's clearance plus a gap at the bottom. */
private val ContentTop = 8.dp
private val ContentBottom = 24.dp

/**
 * Every row of a populated Home, in the user's order, with the stagger applied.
 *
 * The pinned block ([HomeHeaderBlock]) sits above the reorderable rows and is not part of the
 * layout the user edits — see [HomeCard] for why the greeting and the insight never move.
 *
 * Below it, cards are laid out in **rows**, not one per line: `homeRows()` pairs two adjacent
 * half-width cards and gives everything else a row of its own. Gating happens first, on the layout
 * itself rather than inside each `when` branch, because a card hidden for want of data would
 * otherwise still claim a slot and split a pair that should have closed up.
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
    onOpenHomeLayout: () -> Unit,
    onEvent: (HomeEvent) -> Unit,
) {
    // ponytail: the greeting is fixed for the life of the composition — it won't re-read the
    // clock if the app sits open across noon. Key it off a ticker if that ever matters.
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
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
    val unit = uiState.profile?.preferredUnit ?: UnitSystem.Metric
    val budget = targets?.let { budgetKcal(it.calories, uiState.burnedKcal, uiState.addExerciseToBudget) } ?: 0

    // Derived here beside `targets` and `projection`, off the profile the state already holds —
    // nothing about the layout is stored on HomeUiState. Null (an untouched install) is the
    // declaration order with nothing hidden.
    //
    // ponytail: a hidden card's flows are still collected — HomeViewModel combines everything in
    // one chain, and splitting it per card would be a large conditional-flow change for a gain
    // nobody has measured. Gate the combine if a profiler ever says it costs something.
    val visible = homeCardLayout(uiState.profile?.homeLayout)
        .filter { it.visible && it.card.hasData(uiState) }
        .map { it.card }
    val rows = homeRows(visible, fastRunning = uiState.activeFast != null)

    // Keyed on Unit, so the entrance runs once when the screen arrives and never again — logging
    // a glass of water must not re-run the curtain.
    var appear by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appear = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 16.dp, end = 16.dp, top = ContentTop)
            .padding(bottom = ContentBottom + DockedFabContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HomeHeaderBlock(
            greeting = greetingFor(hour),
            greetingSub = greetingSubFor(hour),
            strip = todayStripCards(visible).mapNotNull { stripCell(it, uiState, budget, unit) },
            insight = insight,
            insightDismissed = state.insightDismissed,
            onOpenCoach = onOpenCoach,
            onDismissInsight = { state.insightDismissed = true },
            modifier = appearModifier(0, appear),
        )

        rows.forEachIndexed { index, row ->
            // Intrinsic height plus `fillMaxHeight` below is what makes a pair sit as one row
            // rather than two cards of whatever height their own text happened to need — a weight
            // card whose projection line wraps must not leave a step beside the streak card.
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = appearModifier(index + 1, appear)
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                row.forEach { card ->
                    // Keyed on the card, not its position, so state stays with the card the user
                    // just moved rather than with the slot it left.
                    key(card) {
                        HomeCardContent(
                            card = card,
                            uiState = uiState,
                            wide = row.size == 1,
                            budget = budget,
                            unit = unit,
                            trend = trend,
                            projection = projection,
                            targets = targets,
                            onAddPhoto = onAddPhoto,
                            onStartRoutine = onStartRoutine,
                            onEvent = onEvent,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }

        RearrangeLink(onClick = onOpenHomeLayout, modifier = appearModifier(rows.size + 1, appear))
    }
}

/**
 * Whether this card has anything to draw.
 *
 * Every one of these gates was already there, inside the card's own `when` branch; they moved up
 * here so gating runs before pairing. Each keeps the reasoning it was written with. A visibility
 * switch can still only ever *remove* a card, never force one — a Sleep card left on with no watch
 * synced is as absent as it was before the layout editor existed.
 */
private fun HomeCard.hasData(uiState: HomeUiState): Boolean = when (this) {
    // No profile, no targets, so no ring and no macro goals to price a bar against.
    HomeCard.Calories, HomeCard.Macros -> uiState.profile != null

    // Hidden rather than zeroed when Google Health isn't connected or hasn't synced.
    HomeCard.Sleep -> uiState.lastNight != null
    HomeCard.Steps -> uiState.steps != null
    HomeCard.Heart -> uiState.heart != null

    // The latest reading, not today's, and hidden until there is one — see the card.
    HomeCard.BloodPressure -> uiState.latestBloodPressure != null

    // Hidden until a routine has days set — nothing is missing, nothing has been planned yet
    // (`SupplementsCard`'s rule). A day with nothing planned still draws: "Rest day" is the
    // answer, not an absence.
    HomeCard.Workout -> uiState.routines.anyScheduled()

    // Gated on the Profile switch, not on whether anything is logged: with tracking on and
    // nothing logged the card is what asks for the first tap.
    HomeCard.Cycle -> uiState.profile?.cycleTrackingOn == true

    // Hidden when the list is empty, like the three watch cards above — but for a different
    // reason: there is nothing to import, there is nothing the user has authored yet.
    HomeCard.Supplements -> uiState.supplements.isNotEmpty()

    // Always shown. A fast that hasn't started is an invitation, not an absence of data.
    HomeCard.Streak, HomeCard.Water, HomeCard.Fasting, HomeCard.Mood,
    HomeCard.Weight, HomeCard.ProgressPhoto,
    -> true
}

@Suppress("LongParameterList")
@Composable
private fun HomeCardContent(
    card: HomeCard,
    uiState: HomeUiState,
    wide: Boolean,
    budget: Int,
    unit: UnitSystem,
    trend: WeightTrendDisplay,
    projection: GoalProjection?,
    targets: DailyTargets?,
    onAddPhoto: () -> Unit,
    onStartRoutine: (Long) -> Unit,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (card) {
        HomeCard.Calories -> CalorieRingCard(
            consumedKcal = uiState.totals.calories,
            goalKcal = budget,
            burnedKcal = if (uiState.addExerciseToBudget) uiState.burnedKcal else 0,
            modifier = modifier,
        )

        HomeCard.Water -> WaterCard(
            glasses = uiState.waterGlasses,
            goalGlasses = uiState.waterGoalGlasses,
            unit = unit,
            onSetGlasses = { glasses -> onEvent(HomeEvent.OnSetWaterGlasses(glasses)) },
            modifier = modifier,
        )

        HomeCard.Macros -> targets?.let {
            MacroSummaryCard(consumed = uiState.totals, targets = it, modifier = modifier)
        }

        HomeCard.Streak -> StreakCard(streak = uiState.streak, wide = wide, modifier = modifier)

        HomeCard.Weight -> WeightMetricCard(
            trend = trend,
            goal = uiState.profile?.goal,
            unit = unit,
            projection = projection,
            wide = wide,
            modifier = modifier,
        )

        HomeCard.Steps -> uiState.steps?.let { steps ->
            StepsCard(
                steps = steps,
                goal = uiState.stepGoal,
                creditKcal = if (uiState.addExerciseToBudget) uiState.stepsCreditKcal else 0,
                wide = wide,
                modifier = modifier,
            )
        }

        HomeCard.Sleep -> uiState.lastNight?.let { night ->
            SleepCard(night = night, wide = wide, modifier = modifier)
        }

        HomeCard.Heart -> uiState.heart?.let { heart ->
            HeartCard(heart = heart, wide = wide, modifier = modifier)
        }

        HomeCard.BloodPressure -> uiState.latestBloodPressure?.let { reading ->
            BloodPressureCard(
                reading = reading,
                todayEpochDay = todayEpochDay(),
                wide = wide,
                modifier = modifier,
            )
        }

        HomeCard.Fasting -> FastingCard(
            activeFast = uiState.activeFast,
            goalHours = uiState.fastingGoalHours,
            onStart = { onEvent(HomeEvent.OnStartFast) },
            onEnd = { onEvent(HomeEvent.OnEndFast) },
            onDiscard = { onEvent(HomeEvent.OnDiscardFast) },
            wide = wide,
            modifier = modifier,
        )

        HomeCard.Mood -> MoodCard(
            mood = uiState.moodLevel,
            energy = uiState.energyLevel,
            onSetMood = { level -> onEvent(HomeEvent.OnSetMood(level)) },
            onSetEnergy = { level -> onEvent(HomeEvent.OnSetEnergy(level)) },
            modifier = modifier,
        )

        HomeCard.Supplements -> SupplementsCard(
            supplements = uiState.supplements,
            onSetTaken = { id, taken -> onEvent(HomeEvent.OnSetSupplementTaken(id, taken)) },
            modifier = modifier,
        )

        HomeCard.Cycle -> {
            val today = todayEpochDay()
            // Folded here off the days the state already holds, beside `targets` and
            // `projection` — nothing derived is stored on HomeUiState.
            val periods = uiState.cycleDays.periods()
            CycleCard(
                cycleDay = periods.cycleDayNumber(today),
                prediction = periods.cyclePrediction(),
                todayEpochDay = today,
                flow = uiState.cycleDays.firstOrNull { it.dateEpochDay == today }?.flow ?: 0,
                onSetFlow = { flow -> onEvent(HomeEvent.OnSetCycleFlow(flow)) },
                modifier = modifier,
            )
        }

        HomeCard.Workout -> TrainingPlanCard(
            todayRoutines = uiState.routines.plannedOn(todayEpochDay()),
            week = uiState.trainingWeek,
            trained = uiState.trainingWeek.any { it.isToday && it.trained },
            onStart = onStartRoutine,
            modifier = modifier,
        )

        HomeCard.ProgressPhoto -> ProgressPhotoReminderCard(
            daysSinceLastPhoto = daysSincePhoto(uiState.lastPhotoEpochDay, todayEpochDay()),
            onTakePhoto = onAddPhoto,
            wide = wide,
            modifier = modifier,
        )
    }
}

/**
 * One Today-strip cell for a card that is on screen. Null for a card the strip has no figure for —
 * `todayStripCards()` only ever names the five it does, so this is a total function in practice.
 */
@Composable
private fun stripCell(card: HomeCard, uiState: HomeUiState, budget: Int, unit: UnitSystem): StripCell? =
    when (card) {
        HomeCard.Calories -> StripCell(
            value = "${budget - uiState.totals.calories}",
            label = stringResource(R.string.home_strip_calories),
        )

        HomeCard.Water -> StripCell(
            value = stringResource(
                R.string.home_strip_water_value,
                uiState.waterGlasses,
                uiState.waterGoalGlasses,
            ),
            label = stringResource(R.string.home_strip_water),
        )

        HomeCard.Steps -> uiState.steps?.let {
            StripCell(value = it.formatSteps(), label = stringResource(R.string.home_strip_steps))
        }

        HomeCard.Streak -> StripCell(
            value = "${uiState.streak.current}",
            label = stringResource(R.string.home_strip_streak),
        )

        HomeCard.Weight -> uiState.weightEntries.lastOrNull()?.let {
            StripCell(
                value = "%.1f %s".format(it.weightKg.kgToDisplayUnit(unit), unit.weightUnitLabel()),
                label = stringResource(R.string.home_strip_weight),
            )
        }

        else -> null
    }

/**
 * The one door from Home to Profile → Home layout.
 *
 * It adds no reorder capability here and no per-card drag handles — the editor is a screen with a
 * ViewModel of its own, and duplicating it into a tab would be a second way to write one column.
 * Its whole job is to say the block is the user's to edit, which is otherwise undiscoverable.
 */
@Composable
private fun RearrangeLink(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        TextButton(
            label = stringResource(R.string.home_rearrange),
            onClick = onClick,
        )
    }
}

/**
 * Fade and 8dp rise, [Motion.StaggerStep] apart, capped at [Motion.StaggerCap] so the curtain
 * can't grow if the screen gains cards. The animated value is read inside the `graphicsLayer`
 * lambda, so the whole entrance settles in the Draw phase and recomposes nothing.
 *
 * The index is the **row's**, not the card's: two cards sharing a row arrive together, because a
 * pair that staggered against itself would read as two rows that happened to line up.
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
