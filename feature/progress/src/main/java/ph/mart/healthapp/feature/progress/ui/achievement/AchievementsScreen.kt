package ph.mart.healthapp.feature.progress.ui.achievement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.streak.streakStats
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.AppTopBar
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.achievement.components.BadgeGroupCard
import ph.mart.healthapp.feature.progress.ui.progress.Subject

/**
 * Every badge in the app on one surface — a route of its own, `SleepScreen`'s shape, and the
 * thirteenth and last subject page to become one.
 *
 * It is the odd member in three ways, all of them pre-existing: it has no chart, so no range and no
 * state holder at all; it has no `SubjectGroup`, so no sibling switcher to draw; and its container
 * is the widest of the thirteen, because an achievement list is a fold over everything the app
 * records. See [AchievementsViewModel] for what that costs and what it does not.
 */
@Composable
internal fun AchievementsScreen(
    onOpenRecap: () -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AchievementsViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    AchievementsContent(
        uiState = uiState,
        onOpenRecap = onOpenRecap,
        onExitFlow = onExitFlow,
        modifier = modifier,
    )
}

@Composable
private fun AchievementsContent(
    uiState: AchievementsUiState,
    onOpenRecap: () -> Unit,
    onExitFlow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Zero insets: this bar sits in the scaffold's content, which its `innerPadding` has
            // already cleared of the status bar. See `SleepScreen`.
            AppTopBar(
                title = stringResource(Subject.Badges.label),
                onBack = onExitFlow,
                windowInsets = WindowInsets(0),
                actions = {
                    IconButton(onClick = onOpenRecap) {
                        Icon(
                            imageVector = AppIcons.Share,
                            contentDescription = stringResource(R.string.progress_recap),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
            // Nothing logged anywhere is the only emptiness a badge page can have: every family
            // counts from zero, so a day-one visitor would otherwise read seven cards of unlit
            // dots. The overview's own row hides on the same field.
            if (uiState.activeDays.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    FullScreenState(
                        icon = { MascotAvatar(state = MascotState.Idle, size = 64.dp) },
                        heading = stringResource(R.string.progress_empty_badges_heading),
                        body = stringResource(R.string.progress_empty_badges_body),
                    )
                }
            } else {
                val groups = badgeGroups(
                    // Read here rather than at flow-construction time, so the streak can't freeze
                    // at whatever day the app was opened — HomeViewModel's reason for doing the
                    // same.
                    streak = uiState.activeDays.streakStats(todayEpochDay()),
                    weightProgressKg = uiState.weightProgressKg,
                    workoutCount = uiState.workoutCount,
                    fasts = uiState.fasts,
                    photoCount = uiState.photoCount,
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    groups.forEach { group -> BadgeGroupCard(group = group, unit = uiState.unit) }
                }
            }
        }
    }
}

private fun statePreview(): AchievementsUiState {
    val today = todayEpochDay()
    val hour = 3_600_000L
    return AchievementsUiState(
        activeDays = (today - 30..today).toSet(),
        weightProgressKg = 5.2,
        fasts = listOf(
            FastSession(startMillis = 0, endMillis = 17 * hour),
            FastSession(startMillis = 0, endMillis = 14 * hour),
        ),
        unit = UnitSystem.Metric,
    )
}

@PreviewLightDark
@Composable
private fun AchievementsScreenPreview() {
    AppTheme {
        AchievementsContent(uiState = statePreview(), onOpenRecap = {}, onExitFlow = {})
    }
}

/** Nothing logged anywhere yet — seven cards of unlit dots say less than one mascot does. */
@PreviewLightDark
@Composable
private fun AchievementsScreenEmptyPreview() {
    AppTheme {
        AchievementsContent(uiState = AchievementsUiState(), onOpenRecap = {}, onExitFlow = {})
    }
}
