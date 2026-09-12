package ph.mart.healthapp.feature.progress.ui.recap

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.exercise.volumeLabel
import ph.mart.healthapp.core.data.food.NutritionAverages
import ph.mart.healthapp.core.data.health.formatSteps
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.WeightTrendDisplay
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.GoalProjection
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.GRID_TILE_PX
import ph.mart.healthapp.core.designsystem.component.MacroBar
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.component.rememberBitmapFromFile
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.shared.components.Note
import ph.mart.healthapp.feature.progress.ui.recap.components.ShareRecapSheet
import ph.mart.healthapp.feature.progress.ui.shared.BestDay
import ph.mart.healthapp.feature.progress.ui.shared.Recap
import ph.mart.healthapp.feature.progress.ui.shared.RecapPeriod
import ph.mart.healthapp.feature.progress.ui.shared.components.RecapCard
import ph.mart.healthapp.feature.progress.ui.shared.components.sampleFrames
import ph.mart.healthapp.feature.progress.ui.weight.components.StatCell
import ph.mart.healthapp.feature.progress.ui.weight.components.formatKg

/**
 * The whole period in one page — the question the charts answer one metric at a time and Home
 * doesn't answer at all.
 *
 * A route rather than an overlay drawn over the Progress tab, so it wears the toolbar's back arrow
 * — which is what names the page, hence no heading of its own — and none of the tab's chrome, and
 * wires no back handler. Its Close buttons pop the same entry back does. [RecapViewModel] folds the
 * report for the period on show, so what arrives here is one [Recap] rather than the dozen series
 * behind it.
 *
 * Every section is omitted when its window holds nothing, rather than drawn as zeros — the recap
 * card's own rule, and Home's rule for the three watch cards.
 */
@Composable
internal fun RecapScreen(
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecapViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    RecapContent(
        uiState = uiState,
        onPeriodChange = { period -> viewModel.handleEvent(RecapEvent.OnPeriodChange(period)) },
        onExitFlow = onExitFlow,
        modifier = modifier,
    )
}

@Composable
private fun RecapContent(
    uiState: RecapUiState,
    onPeriodChange: (RecapPeriod) -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    state: RecapState = rememberRecapState(),
) {
    val report = uiState.report
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Three pills, so the toggle splits its width evenly rather than scrolling.
            SegmentedToggle(
                options = RecapPeriod.entries.map { stringResource(it.short) },
                selectedIndex = RecapPeriod.entries.indexOf(uiState.period),
                onSelect = { index -> onPeriodChange(RecapPeriod.entries[index]) },
            )
            if (report == null) {
                Box(modifier = Modifier.weight(1f)) { EmptyRecap(period = uiState.period, onExit = onExitFlow) }
                return@Column
            }
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RecapCard(
                    recap = report,
                    goal = uiState.goal,
                    unit = uiState.unit,
                    projection = uiState.projection,
                )
                BodySection(recap = report, unit = uiState.unit)
                NutritionSection(recap = report)
                MovementSection(recap = report, unit = uiState.unit)
                PhotoSection(recap = report)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(label = stringResource(R.string.progress_share), onClick = { state.sharing = true }, modifier = Modifier.weight(1f))
                SecondaryButton(label = stringResource(R.string.progress_close), onClick = onExitFlow, modifier = Modifier.weight(1f))
            }
        }
    }

    if (state.sharing && report != null) {
        ShareRecapSheet(
            recap = report,
            goal = uiState.goal,
            unit = uiState.unit,
            projection = uiState.projection,
            onDismiss = { state.sharing = false },
        )
    }
}

/** Nothing logged in the window — said plainly rather than drawn as a page of zeros. */
@Composable
private fun EmptyRecap(period: RecapPeriod, onExit: () -> Unit) {
    FullScreenState(
        icon = { MascotAvatar(state = MascotState.Idle, size = 96.dp) },
        heading = stringResource(R.string.progress_recap_empty_heading),
        body = stringResource(R.string.progress_recap_empty_body, stringResource(period.label).lowercase()),
        actions = {
            SecondaryButton(
                label = stringResource(R.string.progress_close),
                onClick = onExit,
                modifier = Modifier.fillMaxWidth(),
            )
        },
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
        SectionHeading(stringResource(R.string.progress_recap_body))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatCell(label = stringResource(R.string.progress_recap_started), value = stringResource(R.string.progress_weight_value, formatKg(start.kgToDisplayUnit(unit)), label))
            StatCell(label = stringResource(R.string.progress_recap_latest), value = stringResource(R.string.progress_weight_value, formatKg(end.kgToDisplayUnit(unit)), label))
            StatCell(
                label = stringResource(R.string.progress_recap_change),
                value = recap.weightArcKg?.let {
                    stringResource(
                        R.string.progress_weight_value,
                        "${if (it > 0) "+" else ""}${formatKg(it.kgToDisplayUnit(unit))}",
                        label,
                    )
                } ?: stringResource(R.string.progress_none),
            )
        }
        if (recap.weightArcKg == null) {
            Note(stringResource(R.string.progress_recap_one_weigh_in))
        }
    }
}

@Composable
private fun NutritionSection(recap: Recap) {
    val averages = recap.averages
    if (averages.daysLogged == 0) return
    AppCard {
        SectionHeading(stringResource(R.string.progress_recap_nutrition))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatCell(label = stringResource(R.string.progress_recap_protein), value = stringResource(R.string.progress_recap_grams, averages.proteinG))
            StatCell(label = stringResource(R.string.progress_recap_carbs), value = stringResource(R.string.progress_recap_grams, averages.carbsG))
            StatCell(label = stringResource(R.string.progress_recap_fat), value = stringResource(R.string.progress_recap_grams, averages.fatG))
        }
        MacroBar(
            proteinG = averages.proteinG,
            carbsG = averages.carbsG,
            fatG = averages.fatG,
            modifier = Modifier.padding(top = 12.dp),
        )
        Note(pluralStringResource(R.plurals.progress_recap_daily_average, averages.daysLogged, averages.daysLogged))
    }
}

@Composable
private fun MovementSection(recap: Recap, unit: UnitSystem) {
    if (recap.workouts == 0 && recap.steps.days == 0) return
    AppCard {
        SectionHeading(stringResource(R.string.progress_recap_movement))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatCell(label = stringResource(R.string.progress_recap_workouts), value = "${recap.workouts}")
            StatCell(label = stringResource(R.string.progress_recap_burned), value = stringResource(R.string.progress_kcal, recap.burnedKcal))
            StatCell(label = stringResource(R.string.progress_recap_best_steps), value = recap.steps.bestSteps?.let(::formatSteps) ?: stringResource(R.string.progress_none))
        }
        if (recap.steps.days > 0) {
            // The goal is the profile's current one and is not snapshotted per day, so the label
            // says which goal it means — `Profile.stepGoal`'s rule.
            Note(stringResource(R.string.progress_recap_step_goal_days, recap.steps.daysHitGoal, recap.steps.days))
        }
        if (recap.strength.workouts > 0) {
            Note(stringResource(R.string.progress_recap_lifted, volumeLabel(recap.strength.volumeKg, unit), recap.strength.sets))
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
        SectionHeading(stringResource(R.string.progress_recap_photos))
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
                    contentDescription = stringResource(R.string.progress_recap_photo_from, formatEpochDay(photo.dateEpochDay)),
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
        RecapContent(
            uiState = RecapUiState(
                report = Recap(
                    period = RecapPeriod.Month,
                    daysLogged = 26,
                    averages = NutritionAverages(1940, 141, 196, 68, daysLogged = 24),
                    targets = DailyTargets(calories = 2000, proteinG = 150, carbsG = 200, fatG = 67, floor = 1500),
                    weightTrend = WeightTrendDisplay(currentKg = 76.3, deltaKg = -0.8, hasPrior = true),
                    moodAverages = null,
                    bestDay = BestDay(dateEpochDay = today - 3, calories = 1985),
                    startWeightKg = 78.4,
                    endWeightKg = 76.3,
                    photos = listOf(
                        ProgressPhoto(id = 1, dateEpochDay = today - 28, filePath = ""),
                        ProgressPhoto(id = 2, dateEpochDay = today - 2, filePath = ""),
                    ),
                ),
                period = RecapPeriod.Month,
                goal = Goal.Lose,
                unit = UnitSystem.Metric,
            ),
            onPeriodChange = {},
            onExitFlow = {},
        )
    }
}

/** Nothing logged in the window — the page that replaces the page. */
@PreviewLightDark
@Composable
private fun RecapScreenEmptyPreview() {
    AppTheme {
        RecapContent(uiState = RecapUiState(period = RecapPeriod.Week), onPeriodChange = {}, onExitFlow = {})
    }
}
