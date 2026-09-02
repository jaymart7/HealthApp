package ph.mart.healthapp.feature.progress.ui.progress.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import ph.mart.healthapp.core.data.exercise.volumeLabel
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.health.formatSteps
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.GoalProjection
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MacroBar
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.progress.ui.photo.components.GRID_TILE_PX
import ph.mart.healthapp.feature.progress.ui.photo.components.rememberBitmapFromFile
import ph.mart.healthapp.feature.progress.ui.photo.components.sampleFrames
import ph.mart.healthapp.feature.progress.ui.progress.ProgressUiState
import ph.mart.healthapp.feature.progress.ui.progress.Recap
import ph.mart.healthapp.feature.progress.ui.progress.RecapPeriod
import ph.mart.healthapp.feature.progress.ui.progress.recap
import ph.mart.healthapp.feature.progress.ui.weight.components.StatCell
import ph.mart.healthapp.feature.progress.ui.weight.components.formatKg

/**
 * The whole period in one page — the question the charts answer one metric at a time and Home
 * doesn't answer at all.
 *
 * It reads [uiState] straight off the Progress screen's already-combined state and derives
 * everything else, so it needs no ViewModel, no route and no schema: a recap is a way of looking
 * at what is already on the tab, [TimelapseScreen]'s call. A route would have earned its own
 * `ViewModelStoreOwner` and with it a second copy of `ProgressViewModel`'s twelve repositories,
 * to render a page that writes nothing.
 *
 * Every section is omitted when its window holds nothing, rather than drawn as zeros — the recap
 * card's own rule, and Home's rule for the three watch cards.
 */
@Composable
internal fun RecapScreen(
    uiState: ProgressUiState,
    period: RecapPeriod,
    projection: GoalProjection?,
    onPeriodChange: (RecapPeriod) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sharing by rememberSaveable { mutableStateOf(false) }

    // A full-screen overlay, not a route: back has to close it rather than leave the Progress tab.
    val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(state = navigationState, onBackCompleted = onClose)

    val today = todayEpochDay()
    val report = remember(uiState, period, today) {
        recap(
            period = period,
            dailyNutrition = uiState.dailyNutrition,
            activeDays = uiState.activeDays,
            weightEntries = uiState.weightEntries,
            moodDays = uiState.moodDays,
            targets = uiState.targets,
            todayEpochDay = today,
            exerciseEntries = uiState.exerciseEntries,
            stepDays = uiState.stepDays,
            stepGoal = uiState.stepGoal,
            photos = uiState.photos,
        )
    }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Recap",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            // Three pills, so the toggle splits its width evenly rather than scrolling.
            SegmentedToggle(
                options = RecapPeriod.entries.map { it.short },
                selectedIndex = RecapPeriod.entries.indexOf(period),
                onSelect = { index -> onPeriodChange(RecapPeriod.entries[index]) },
            )
            if (report == null) {
                Box(modifier = Modifier.weight(1f)) { EmptyRecap(period = period, onClose = onClose) }
                return@Column
            }
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RecapCard(
                    recap = report,
                    goal = uiState.goal,
                    unit = uiState.preferredUnit,
                    projection = projection,
                )
                BodySection(recap = report, unit = uiState.preferredUnit)
                NutritionSection(recap = report)
                MovementSection(recap = report, unit = uiState.preferredUnit)
                PhotoSection(recap = report)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(label = "Share", onClick = { sharing = true }, modifier = Modifier.weight(1f))
                SecondaryButton(label = "Close", onClick = onClose, modifier = Modifier.weight(1f))
            }
        }
    }

    if (sharing && report != null) {
        ShareRecapSheet(
            recap = report,
            goal = uiState.goal,
            unit = uiState.preferredUnit,
            projection = projection,
            onDismiss = { sharing = false },
        )
    }
}

/** Nothing logged in the window — said plainly rather than drawn as a page of zeros. */
@Composable
private fun EmptyRecap(period: RecapPeriod, onClose: () -> Unit) {
    FullScreenState(
        icon = { MascotAvatar(state = MascotState.Idle, size = 96.dp) },
        heading = "Nothing logged yet",
        body = "There's nothing in the ${period.label.lowercase()} to recap. Log a meal, a glass " +
            "of water or a weigh-in and it'll show up here.",
        actions = { SecondaryButton(label = "Close", onClick = onClose, modifier = Modifier.fillMaxWidth()) },
    )
}

/**
 * The window's own weight arc, which is a different figure from the card's weight cell: that one
 * is always the seven-day trend (see `recap`), and on a year page a seven-day delta is not the
 * story. Both are labelled, so neither can be read as the other.
 */
@Composable
private fun BodySection(recap: Recap, unit: UnitSystem) {
    val start = recap.startWeightKg ?: return
    val end = recap.endWeightKg ?: return
    val label = unit.weightUnitLabel()
    AppCard {
        SectionHeading("Body")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatCell(label = "Started", value = "${formatKg(start.kgToDisplayUnit(unit))} $label")
            StatCell(label = "Latest", value = "${formatKg(end.kgToDisplayUnit(unit))} $label")
            StatCell(
                label = "Change",
                value = recap.weightArcKg?.let {
                    "${if (it > 0) "+" else ""}${formatKg(it.kgToDisplayUnit(unit))} $label"
                } ?: "—",
            )
        }
        if (recap.weightArcKg == null) {
            Note("One weigh-in in this window — there's nothing to compare it against yet.")
        }
    }
}

@Composable
private fun NutritionSection(recap: Recap) {
    val averages = recap.averages
    if (averages.daysLogged == 0) return
    AppCard {
        SectionHeading("Nutrition")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatCell(label = "Protein", value = "${averages.proteinG} g")
            StatCell(label = "Carbs", value = "${averages.carbsG} g")
            StatCell(label = "Fat", value = "${averages.fatG} g")
        }
        MacroBar(
            proteinG = averages.proteinG,
            carbsG = averages.carbsG,
            fatG = averages.fatG,
            modifier = Modifier.padding(top = 12.dp),
        )
        Note("Daily average over ${averages.daysLogged} ${if (averages.daysLogged == 1) "day" else "days"} with food logged.")
    }
}

@Composable
private fun MovementSection(recap: Recap, unit: UnitSystem) {
    if (recap.workouts == 0 && recap.steps.days == 0) return
    AppCard {
        SectionHeading("Movement")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatCell(label = "Workouts", value = "${recap.workouts}")
            StatCell(label = "Burned", value = "${recap.burnedKcal} kcal")
            StatCell(label = "Best day", value = recap.steps.bestSteps?.let(::formatSteps) ?: "—")
        }
        if (recap.steps.days > 0) {
            // The goal is the profile's current one and is not snapshotted per day, so the label
            // says which goal it means — `Profile.stepGoal`'s rule.
            Note("Hit today's step goal on ${recap.steps.daysHitGoal} of ${recap.steps.days} days with steps.")
        }
        if (recap.strength.workouts > 0) {
            Note("Lifted ${volumeLabel(recap.strength.volumeKg, unit)} across ${recap.strength.sets} sets.")
        }
    }
}

/** The same two-photo floor the comparison slider and the timelapse hold: one photo is not a
 * before and after. Frames are spread by [sampleFrames], so the ends are always the window's. */
@Composable
private fun PhotoSection(recap: Recap) {
    if (recap.photos.size < 2) return
    val frames = remember(recap.photos) { sampleFrames(recap.photos) }
    AppCard {
        SectionHeading("Photos")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            frames.forEach { photo ->
                RecapFrame(photo = photo, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RecapFrame(photo: ProgressPhoto, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            rememberBitmapFromFile(photo.filePath, GRID_TILE_PX)?.let {
                Image(
                    bitmap = it,
                    contentDescription = "Progress photo from ${formatEpochDay(photo.dateEpochDay)}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            text = formatEpochDay(photo.dateEpochDay),
            style = MaterialTheme.typography.labelSmall.tabularNums,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

@PreviewLightDark
@Composable
private fun RecapScreenPreview() {
    val today = todayEpochDay()
    AppTheme {
        RecapScreen(
            uiState = ProgressUiState(
                weightEntries = listOf(
                    WeightEntry(dateEpochDay = today - 28, weightKg = 78.4),
                    WeightEntry(dateEpochDay = today - 12, weightKg = 77.1),
                    WeightEntry(dateEpochDay = today - 1, weightKg = 76.3),
                ),
                photos = listOf(
                    ProgressPhoto(id = 1, dateEpochDay = today - 28, filePath = ""),
                    ProgressPhoto(id = 2, dateEpochDay = today - 2, filePath = ""),
                ),
                goal = Goal.Lose,
                preferredUnit = UnitSystem.Metric,
                dailyNutrition = (0..29).map { offset ->
                    val calories = if (offset % 5 == 0) 0 else 1_800 + offset * 7
                    DayNutrition(today - 29 + offset, calories, calories / 16, calories / 10, calories / 30)
                },
                activeDays = (today - 25..today).toSet(),
                targets = DailyTargets(calories = 2000, proteinG = 150, carbsG = 200, fatG = 67, floor = 1500),
            ),
            period = RecapPeriod.Month,
            projection = null,
            onPeriodChange = {},
            onClose = {},
        )
    }
}
