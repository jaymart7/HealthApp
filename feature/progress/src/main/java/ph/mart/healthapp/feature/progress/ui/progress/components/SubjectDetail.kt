package ph.mart.healthapp.feature.progress.ui.progress.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreenState
import ph.mart.healthapp.feature.progress.ui.progress.ProgressUiState
import ph.mart.healthapp.feature.progress.ui.progress.Subject
import ph.mart.healthapp.feature.progress.ui.progress.summarize

/**
 * One subject's page — the surface behind every card on the overview, for the subjects that have
 * not yet become routes of their own. **This file is being dismantled**, one subject per commit;
 * it goes entirely when the last of them lands. See `DECISIONS.md` ->
 * **Progress, recap & the energy check-in**.
 *
 * It is a **swap-in inside the Progress tab, not a route**. A route would earn its own
 * `ViewModelStoreOwner` and with it a second copy of `ProgressViewModel`'s twelve repositories, to
 * draw a page that writes nothing — the same argument `RecapScreen` and `TimelapseScreen` make. It
 * follows that back has to be handled here, or it would leave the tab entirely, and that the bottom
 * bar and the FAB stay up, which is what the handoff draws.
 *
 * [embedded] is that same page drawn as a *pane*, beside the overview it came from, on a window with
 * room for both. Two things go, and both for one reason — there is no level to come back from: it
 * registers no back handler (back would close a page whose list is already on screen, and then leave
 * the tab on the next press) and its header draws no arrow. Everything else is identical, so the two
 * widths cannot show different pages.
 *
 * The chrome is fixed for the ones that are left and the body varies: hero, chips, a chart card holding its
 * own range toggle, the stat rows. A subject with no data yet is still a real page — its
 * `FullScreenState` and the switcher to its siblings, and **no call to action**: Progress reads,
 * and gains no logging entry point. Blood pressure is the single exception, because the sheet it
 * would open already lives on this screen.
 */
@Composable
internal fun SubjectDetail(
    subject: Subject,
    uiState: ProgressUiState,
    state: ProgressScreenState,
    canShare: Boolean,
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
) {
    if (!embedded) {
        val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(state = navigationState, onBackCompleted = state::closeSubject)
    }

    val today = todayEpochDay()
    val summary = remember(uiState, today) { summarize(subject, uiState, today) }
    // Keyed on the subject, so hopping to a sibling opens at the top rather than at the offset the
    // page before it was left at. The overview's own scroll is hoisted in `AppScaffold` and
    // untouched by any of this, which is what preserves it across the round trip.
    val scrollState = rememberScrollState()

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            DetailHeader(
                title = stringResource(subject.label),
                onBack = if (embedded) null else state::closeSubject,
                onShare = if (canShare) state::openRecap else null,
            )
            when {
                !summary.tracked -> EmptyDetail(
                    subject = subject,
                    state = state,
                    cycleTracking = uiState.cycleTrackingOn,
                )

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = DockedFabContentPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Body(subject, uiState, state)
                    SubjectSwitcher(
                        subject = subject,
                        cycleTracking = uiState.cycleTrackingOn,
                        onSelect = state::open,
                    )
                }
            }
        }
    }
}

/** The per-subject page content. Each body owns its own hero, chips, chart card and stat rows —
 * the shapes differ enough (a photo grid has no chart, Badges has no range) that a single slot
 * table would be a struct of nullable lambdas describing nothing. */
@Composable
private fun ColumnScope.Body(
    subject: Subject,
    uiState: ProgressUiState,
    state: ProgressScreenState,
) {
    when (subject) {
        // All fourteen, and therefore dead: `ProgressScreenState.open` pushes a route for every
        // subject now, so nothing reaches this page. The next commit deletes the file.
        Subject.Photos, Subject.Sleep, Subject.Mood, Subject.Heart, Subject.Supplements,
        Subject.Strength, Subject.Fasting, Subject.Activity, Subject.Cycle, Subject.BloodPressure,
        Subject.Measurements, Subject.Weight, Subject.Nutrition, Subject.Badges,
        -> Unit
    }
}

@Composable
private fun EmptyDetail(
    subject: Subject,
    state: ProgressScreenState,
    cycleTracking: Boolean,
    modifier: Modifier = Modifier,
) {
    val copy = emptyCopy(subject)
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.weight(1f)) {
            FullScreenState(
                icon = { MascotAvatar(state = copy.mascot, size = 64.dp) },
                heading = stringResource(copy.heading),
                body = stringResource(copy.body),
                // The two subjects whose sheets are already on this screen, so pointing at one
                // adds no entry point. Every other subject is filled from somewhere else in the
                // app, and a button that only navigated would be a button explaining a screen.
                actions = when (subject) {
                    Subject.BloodPressure -> {
                        {
                            PrimaryButton(
                                label = stringResource(R.string.progress_hint_pressure),
                                onClick = state::openBloodPressureSheet,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    Subject.Cycle -> {
                        {
                            PrimaryButton(
                                label = stringResource(R.string.progress_hint_cycle),
                                onClick = state::openCycleSheet,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    else -> null
                },
            )
        }
        SubjectSwitcher(
            subject = subject,
            cycleTracking = cycleTracking,
            onSelect = state::open,
        )
        Box(modifier = Modifier.padding(bottom = DockedFabContentPadding))
    }
}

/** What a subject with nothing in it says. The copy each tab already carried, moved here so the
 * empty page and the empty card can be read against each other in one place. Resource ids, not
 * words — the screen resolves them, the way [Subject.label] is already carried. */
private data class EmptyCopy(@StringRes val heading: Int, @StringRes val body: Int, val mascot: MascotState)

private fun emptyCopy(subject: Subject): EmptyCopy = when (subject) {
    Subject.Weight -> EmptyCopy(
        R.string.progress_empty_weight_heading,
        R.string.progress_empty_weight_body,
        MascotState.Sleepy,
    )
    Subject.Photos -> EmptyCopy(
        R.string.progress_empty_photos_heading,
        R.string.progress_empty_photos_body,
        MascotState.Sleepy,
    )
    Subject.Measurements -> EmptyCopy(
        R.string.progress_empty_measurements_heading,
        R.string.progress_empty_measurements_body,
        MascotState.Idle,
    )
    Subject.Nutrition -> EmptyCopy(
        R.string.progress_empty_nutrition_heading,
        R.string.progress_empty_nutrition_body,
        MascotState.Sleepy,
    )
    Subject.Fasting -> EmptyCopy(
        R.string.progress_empty_fasting_heading,
        R.string.progress_empty_fasting_body,
        MascotState.Sleepy,
    )
    Subject.Supplements -> EmptyCopy(
        R.string.progress_empty_supplements_heading,
        R.string.progress_empty_supplements_body,
        MascotState.Sleepy,
    )
    Subject.Activity -> EmptyCopy(
        R.string.progress_empty_activity_heading,
        R.string.progress_empty_activity_body,
        MascotState.Idle,
    )
    Subject.Strength -> EmptyCopy(
        R.string.progress_empty_strength_heading,
        R.string.progress_empty_strength_body,
        MascotState.Idle,
    )
    Subject.Sleep -> EmptyCopy(
        R.string.progress_empty_sleep_heading,
        R.string.progress_empty_sleep_body,
        MascotState.Sleepy,
    )
    Subject.Mood -> EmptyCopy(
        R.string.progress_empty_mood_heading,
        R.string.progress_empty_mood_body,
        MascotState.Sleepy,
    )
    Subject.Cycle -> EmptyCopy(
        R.string.progress_empty_cycle_heading,
        R.string.progress_empty_cycle_body,
        MascotState.Idle,
    )
    Subject.Heart -> EmptyCopy(
        R.string.progress_empty_heart_heading,
        R.string.progress_empty_heart_body,
        MascotState.Idle,
    )
    Subject.BloodPressure -> EmptyCopy(
        R.string.progress_empty_pressure_heading,
        R.string.progress_empty_pressure_body,
        MascotState.Idle,
    )
    Subject.Badges -> EmptyCopy(
        R.string.progress_empty_badges_heading,
        R.string.progress_empty_badges_body,
        MascotState.Idle,
    )
}

/** A subject with nothing in it is still a real page, with a way on to its siblings. */
@PreviewLightDark
@Composable
private fun SubjectDetailEmptyPreview() {
    AppTheme {
        SubjectDetail(
            subject = Subject.Heart,
            uiState = ProgressUiState(),
            state = ProgressScreenState(selectedSubject = Subject.Heart),
            canShare = false,
        )
    }
}
