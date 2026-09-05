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
import ph.mart.healthapp.core.data.profile.EnergyCheckIn
import ph.mart.healthapp.core.data.progress.GoalProjection
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.achievement.components.AchievementsDetailBody
import ph.mart.healthapp.feature.progress.ui.activity.components.ActivityDetailBody
import ph.mart.healthapp.feature.progress.ui.cycle.components.CycleDetailBody
import ph.mart.healthapp.feature.progress.ui.fasting.components.FastingDetailBody
import ph.mart.healthapp.feature.progress.ui.heart.components.HeartDetailBody
import ph.mart.healthapp.feature.progress.ui.measurement.components.MeasurementsDetailBody
import ph.mart.healthapp.feature.progress.ui.mood.components.MoodDetailBody
import ph.mart.healthapp.feature.progress.ui.nutrition.components.NutritionDetailBody
import ph.mart.healthapp.feature.progress.ui.photo.components.PhotosDetailBody
import ph.mart.healthapp.feature.progress.ui.pressure.components.BloodPressureDetailBody
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreenState
import ph.mart.healthapp.feature.progress.ui.progress.ProgressUiState
import ph.mart.healthapp.feature.progress.ui.progress.Subject
import ph.mart.healthapp.feature.progress.ui.progress.SubjectSummary
import ph.mart.healthapp.feature.progress.ui.progress.subjectsIn
import ph.mart.healthapp.feature.progress.ui.progress.summarizeAll
import ph.mart.healthapp.feature.progress.ui.sleep.components.SleepDetailBody
import ph.mart.healthapp.feature.progress.ui.strength.components.StrengthDetailBody
import ph.mart.healthapp.feature.progress.ui.supplement.components.SupplementsDetailBody
import ph.mart.healthapp.feature.progress.ui.weight.components.WeightDetailBody

/**
 * Photos draws a `LazyVerticalGrid` and Blood pressure a `LazyColumn`; nesting either in a
 * `verticalScroll` column measures it with infinite height and throws. They own their scroll, so
 * the page gives them the room and keeps the switcher off the bottom of it.
 */
private val SelfScrolling = setOf(Subject.Photos, Subject.BloodPressure)

/**
 * One subject's page — the surface behind every card on the overview.
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
 * The chrome is fixed for all thirteen and the body varies: hero, chips, a chart card holding its
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
    checkIn: EnergyCheckIn?,
    projection: GoalProjection?,
    canShare: Boolean,
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
) {
    if (!embedded) {
        val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(state = navigationState, onBackCompleted = state::closeSubject)
    }

    val today = todayEpochDay()
    val summaries = remember(uiState, today) { summarizeAll(uiState, today) }
    val summary = summaries[subject] ?: SubjectSummary(subject)
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
                    summaries = summaries,
                    cycleTracking = uiState.cycleTrackingOn,
                )

                subject in SelfScrolling -> Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                ) {
                    Body(subject, uiState, state, checkIn, projection)
                }

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = DockedFabContentPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Body(subject, uiState, state, checkIn, projection)
                    Switcher(
                        subject = subject,
                        summaries = summaries,
                        state = state,
                        cycleTracking = uiState.cycleTrackingOn,
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
    checkIn: EnergyCheckIn?,
    projection: GoalProjection?,
) {
    when (subject) {
        Subject.Weight -> WeightDetailBody(uiState, state, checkIn, projection)
        Subject.Photos -> PhotosDetailBody(uiState, state)
        Subject.Measurements -> MeasurementsDetailBody(uiState, state)
        Subject.Nutrition -> NutritionDetailBody(uiState, state)
        Subject.Fasting -> FastingDetailBody(uiState, state)
        Subject.Supplements -> SupplementsDetailBody(uiState, state)
        Subject.Activity -> ActivityDetailBody(uiState, state)
        Subject.Strength -> StrengthDetailBody(uiState, state)
        Subject.Sleep -> SleepDetailBody(uiState, state)
        Subject.Mood -> MoodDetailBody(uiState, state)
        Subject.Cycle -> CycleDetailBody(uiState, state)
        Subject.Heart -> HeartDetailBody(uiState, state)
        Subject.BloodPressure -> BloodPressureDetailBody(uiState, state)
        Subject.Badges -> AchievementsDetailBody(uiState)
    }
}

@Composable
private fun EmptyDetail(
    subject: Subject,
    state: ProgressScreenState,
    summaries: Map<Subject, SubjectSummary>,
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
        Switcher(
            subject = subject,
            summaries = summaries,
            state = state,
            cycleTracking = cycleTracking,
        )
        Box(modifier = Modifier.padding(bottom = DockedFabContentPadding))
    }
}

@Composable
private fun Switcher(
    subject: Subject,
    summaries: Map<Subject, SubjectSummary>,
    state: ProgressScreenState,
    /** `Profile.cycleTrackingOn` — off drops Cycle from the sibling row, or a page would offer a
     * door to the one subject the overview has taken away. */
    cycleTracking: Boolean,
) {
    val group = subject.group ?: return
    val siblings = subjectsIn(group, cycleTracking)
        .filter { it != subject }
        .map { sibling ->
            val summary = summaries[sibling]
            sibling to summary?.takeIf { it.tracked }?.let { "${it.value} ${it.unit.orEmpty()}".trim() }
        }
    SiblingSwitcher(groupLabel = stringResource(group.label), siblings = siblings, onSelect = state::open)
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
            subject = Subject.Sleep,
            uiState = ProgressUiState(),
            state = ProgressScreenState(selectedSubject = Subject.Sleep),
            checkIn = null,
            projection = null,
            canShare = false,
        )
    }
}
