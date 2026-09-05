package ph.mart.healthapp.feature.progress.ui.progress.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ph.mart.healthapp.core.data.food.DayNutrition
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.GoalProjection
import ph.mart.healthapp.core.data.progress.PROJECTION_WINDOW_DAYS
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.AIInsightCard
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.goalProjectionLine
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreenState
import ph.mart.healthapp.feature.progress.ui.progress.ProgressUiState
import ph.mart.healthapp.feature.progress.ui.progress.Recap
import ph.mart.healthapp.feature.progress.ui.progress.Subject
import ph.mart.healthapp.feature.progress.ui.progress.SubjectGroup
import ph.mart.healthapp.feature.progress.ui.progress.badgeTally
import ph.mart.healthapp.feature.progress.ui.progress.subjectsIn
import ph.mart.healthapp.feature.progress.ui.progress.summarizeAll
import ph.mart.healthapp.feature.progress.ui.weight.components.formatKg

/**
 * "What's moving, and what do I have data for" — answered without tapping anything.
 *
 * This is the surface that replaced thirteen peer tabs. Everything on it is a fold over
 * [ProgressUiState]: [summarizeAll] for the cards, [ph.mart.healthapp.feature.progress.ui.progress.recap]
 * for the week card, [ph.mart.healthapp.core.data.progress.goalProjection] for the insight.
 * No new ViewModel, no new route, no schema.
 *
 * The insight card is the screen's **one** `tertiaryContainer` background, and it is null-hidden
 * rather than degraded: with no target weight, a Maintain goal, or too few recent weigh-ins there
 * is no projection, and a card that said so would be a card explaining its own absence.
 */
@Composable
internal fun ProgressOverview(
    uiState: ProgressUiState,
    state: ProgressScreenState,
    weekRecap: Recap?,
    projection: GoalProjection?,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    val today = todayEpochDay()
    // Thirteen folds over up to a year of data — cheap, but not free on every scroll frame.
    val summaries = remember(uiState, today) { summarizeAll(uiState, today) }
    val untouchedGroups = SubjectGroup.entries.count { group ->
        subjectsIn(group, uiState.cycleTrackingOn).none { summaries[it]?.tracked == true }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(bottom = DockedFabContentPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.progress_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            // No recap, nothing to share — the same rule the card itself follows.
            if (weekRecap != null) {
                IconButton(onClick = state::openRecap) {
                    Icon(
                        imageVector = AppIcons.Share,
                        contentDescription = stringResource(R.string.progress_recap),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Says why most of the screen is outlines rather than cards. Gone the moment every family
        // has something in it, which is also when it would stop being true.
        if (untouchedGroups > 0) {
            val tracked = summaries.values.count { it.tracked && it.subject.group != null }
            MascotNote(trackedCount = tracked, modifier = Modifier.padding(bottom = 12.dp))
        }

        if (weekRecap != null) {
            RecapCard(
                recap = weekRecap,
                goal = uiState.goal,
                unit = uiState.preferredUnit,
                // The projection has its own card here, so the recap doesn't repeat the line.
                projection = null,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        projection?.let {
            AIInsightCard(
                text = goalProjectionLine(
                    goalWeightLabel = stringResource(
                        R.string.progress_weight_value,
                        formatKg(it.goalWeightKg.kgToDisplayUnit(uiState.preferredUnit)),
                        uiState.preferredUnit.weightUnitLabel(),
                    ),
                    targetEpochDay = it.targetEpochDay,
                    reached = it.reached,
                    windowDays = PROJECTION_WINDOW_DAYS,
                ),
                subline = rateLine(it, uiState.preferredUnit),
                headlineStyle = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        SubjectGroup.entries.forEach { group ->
            GroupSection(
                group = group,
                summaries = summaries,
                cycleTracking = uiState.cycleTrackingOn,
                expanded = group in state.expandedGroups,
                onToggle = { state.toggleGroup(group) },
                onOpen = state::open,
                onHint = { subject ->
                    // The one hint that isn't a door to the detail page: Blood pressure's sheet is
                    // already on this screen, so "Log a reading" means it.
                    when (subject) {
                        // The two hints that aren't doors to a detail page: both sheets are
                        // already on this screen, so "Log a reading"/"Log a day" mean them.
                        Subject.BloodPressure -> state.openBloodPressureSheet()
                        Subject.Cycle -> state.openCycleSheet()
                        else -> state.open(subject)
                    }
                },
            )
        }

        val tally = badgeTally(uiState, today)
        BadgesRow(
            earned = tally.earned,
            total = tally.total,
            families = tally.families,
            onClick = { state.open(Subject.Badges) },
        )
    }
}

/** The rate under the projection headline. Always reported, even when there is no date —
 * [GoalProjection.kgPerWeek] is exactly the figure that is useful when the date isn't. */
@Composable
private fun rateLine(projection: GoalProjection, unit: UnitSystem): String {
    val perWeek = abs(projection.kgPerWeek).kgToDisplayUnit(unit)
    return when {
        projection.reached -> stringResource(R.string.progress_trend_holding)
        perWeek == 0.0 -> stringResource(R.string.progress_trend_flat)
        projection.kgPerWeek < 0 -> stringResource(R.string.progress_trend_down, formatKg(perWeek), unit.weightUnitLabel())
        else -> stringResource(R.string.progress_trend_up, formatKg(perWeek), unit.weightUnitLabel())
    }
}

/** The sparse account's one piece of copy. A mascot rather than a warning icon: nothing is wrong,
 * there is simply nothing there yet. */
@Composable
private fun MascotNote(trackedCount: Int, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MascotAvatar(state = MascotState.Idle, size = 56.dp)
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = when (trackedCount) {
                    0 -> stringResource(R.string.progress_sparse_none)
                    1 -> stringResource(R.string.progress_sparse_one)
                    else -> stringResource(R.string.progress_sparse_many, trackedCount)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val PREVIEW_TARGETS =
    DailyTargets(calories = 2261, proteinG = 170, carbsG = 226, fatG = 75, floor = 1500)

private fun previewState(): ProgressUiState {
    val today = todayEpochDay()
    return ProgressUiState(
        weightEntries = (0..8).map {
            WeightEntry(dateEpochDay = today - (8 - it) * 3, weightKg = 84.8 - it * 0.26)
        },
        goalWeightKg = 82.0,
        goal = Goal.Lose,
        dailyNutrition = listOf(1850, 2100, 0, 1720, 2340, 1610, 1490).mapIndexed { index, calories ->
            DayNutrition(today - 6 + index, calories, calories / 16, calories / 10, calories / 30)
        },
        activeDays = (today - 6..today).toSet(),
        targets = PREVIEW_TARGETS,
    )
}

@PreviewLightDark
@Composable
private fun ProgressOverviewPreview() {
    val uiState = previewState()
    AppTheme {
        Surface {
            ProgressOverview(
                uiState = uiState,
                state = ProgressScreenState(),
                weekRecap = null,
                projection = null,
                scrollState = rememberScrollState(),
            )
        }
    }
}

/** The card grid alone, so the preview doesn't depend on a recap fold. */
@PreviewLightDark
@Composable
private fun ProgressOverviewSparsePreview() {
    AppTheme {
        Surface {
            ProgressOverview(
                uiState = ProgressUiState(),
                state = ProgressScreenState(),
                weekRecap = null,
                projection = null,
                scrollState = rememberScrollState(),
            )
        }
    }
}
