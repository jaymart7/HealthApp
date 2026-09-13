package ph.mart.healthapp.feature.progress.ui

import androidx.compose.foundation.ScrollState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import ph.mart.healthapp.core.navigation.route.ProgressRoute
import ph.mart.healthapp.feature.progress.ui.activity.ActivityScreen
import ph.mart.healthapp.feature.progress.ui.comparison.PhotoComparisonScreen
import ph.mart.healthapp.feature.progress.ui.cycle.CycleScreen
import ph.mart.healthapp.feature.progress.ui.fasting.FastingScreen
import ph.mart.healthapp.feature.progress.ui.heart.HeartScreen
import ph.mart.healthapp.feature.progress.ui.measurement.MeasurementsScreen
import ph.mart.healthapp.feature.progress.ui.mood.MoodScreen
import ph.mart.healthapp.feature.progress.ui.photo.PhotosScreen
import ph.mart.healthapp.feature.progress.ui.pressure.BloodPressureScreen
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreen
import ph.mart.healthapp.feature.progress.ui.progress.Subject
import ph.mart.healthapp.feature.progress.ui.recap.RecapScreen
import ph.mart.healthapp.feature.progress.ui.sleep.SleepScreen
import ph.mart.healthapp.feature.progress.ui.strength.StrengthScreen
import ph.mart.healthapp.feature.progress.ui.supplement.SupplementsScreen
import ph.mart.healthapp.feature.progress.ui.timelapse.TimelapseScreen

/** The whole progress-photo set — a full-bleed grid that launches [PhotoComparisonRoute] and
 * [TimelapseRoute] rather than a chart. It was the first subject page to become a route; the other
 * thirteen are following. Carries nothing — the set is the page. */
@Serializable
data object PhotosRoute : NavKey

/** Every imported night, charted. Carries nothing: `SleepViewModel` reads the series itself. */
@Serializable
data object SleepRoute : NavKey

/** Mood and energy, charted. Carries nothing: `MoodViewModel` reads the series itself. */
@Serializable
data object MoodRoute : NavKey

/** Imported heart-rate days, charted. Carries nothing: `HeartViewModel` reads the series itself. */
@Serializable
data object HeartRoute : NavKey

/** Adherence per day, charted. Carries nothing: `SupplementsViewModel` reads the days itself. */
@Serializable
data object SupplementsRoute : NavKey

/** Lifting volume and the all-time records. Carries nothing: `StrengthViewModel` reads the entries itself. */
@Serializable
data object StrengthRoute : NavKey

/** Completed fasts, charted against the current goal. Carries nothing: `FastingViewModel` reads both itself. */
@Serializable
data object FastingRoute : NavKey

/** Imported steps and the burn series, charted. Carries nothing: `ActivityViewModel` reads all three flows itself. */
@Serializable
data object ActivityRoute : NavKey

/** Where the cycle is now and every period behind it. Carries nothing: `CycleViewModel` reads the days itself. */
@Serializable
data object CycleRoute : NavKey

/** Every cuff reading, charted and listed. Carries nothing: `BloodPressureViewModel` reads them itself. */
@Serializable
data object BloodPressureRoute : NavKey

/** Six tape-measure histories and the two figures derived from them. Carries nothing: `MeasurementsViewModel` reads them itself. */
@Serializable
data object MeasurementsRoute : NavKey

/**
 * The subject pages that are routes rather than `SubjectDetail` swap-ins.
 *
 * One set, read by `AppScaffold`'s `ownsTopBar` (each page draws its own `AppTopBar`), by
 * [route] and by `TabChromeTest`, so none of the three can disagree about which subjects have
 * converted. It grows by one per conversion commit.
 */
val ProgressSubjectRoutes: Set<NavKey> = setOf(PhotosRoute, SleepRoute, MoodRoute, HeartRoute, SupplementsRoute, StrengthRoute, FastingRoute, ActivityRoute, CycleRoute, BloodPressureRoute, MeasurementsRoute)

/**
 * A subject to the route that draws it, or null while it is still a swap-in.
 *
 * The one place the mapping lives, which is what keeps `AppScaffold` at a single push site rather
 * than one per subject. The null arm goes when the last subject converts.
 */
fun Subject.route(): NavKey? = when (this) {
    Subject.Photos -> PhotosRoute
    Subject.Sleep -> SleepRoute
    Subject.Mood -> MoodRoute
    Subject.Heart -> HeartRoute
    Subject.Supplements -> SupplementsRoute
    Subject.Strength -> StrengthRoute
    Subject.Fasting -> FastingRoute
    Subject.Activity -> ActivityRoute
    Subject.Cycle -> CycleRoute
    Subject.BloodPressure -> BloodPressureRoute
    Subject.Measurements -> MeasurementsRoute
    else -> null
}

/** Two progress photos read against each other. Carries the grid's selection, and the order of the
 * two ids does not matter — `comparisonPair()` sorts by date, so the route names a pair rather than
 * a before and an after. */
@Serializable
data class PhotoComparisonRoute(val firstId: Long, val secondId: Long) : NavKey

/** Every progress photo played in date order — the whole-set answer to [PhotoComparisonRoute]'s
 * two-photo one. Carries nothing: the player reads the set itself. */
@Serializable
data object TimelapseRoute : NavKey

/** The period in one page. Carries nothing — `RecapViewModel` owns the period, which is what lets
 * the weekly notification and the overview's own icon push the same route. */
@Serializable
data object RecapRoute : NavKey

/**
 * [scrollState] is hoisted for the usual reason: the FAB's scroll-collapse lives in AppScaffold,
 * which can't see a ScrollState created inside the screen. [twoPane] comes from there too: that is
 * the one place in the app that reads the window's width, so this tab is told rather than asking —
 * which is also why `:feature:progress` needs no adaptive dependency of its own.
 *
 * Every surface but [ProgressRoute] itself is a route rather than an overlay drawn inside it, so
 * none of them wears the bottom bar or the FAB and none wires a back handler of its own.
 *
 * [onOpenSubject] **pushes** a subject page and [onSwitchSubject] **replaces** the one showing —
 * the sibling switcher's promise that Sleep -> Mood -> Heart leaves one back step, not three. The
 * comparison and the timelapse are reached from the Photos page rather than from the tab, which is
 * why those callbacks land on a different entry.
 */
fun EntryProviderScope<NavKey>.progressEntries(
    scrollState: ScrollState,
    twoPane: Boolean = false,
    onOpenSubject: (Subject) -> Unit,
    onSwitchSubject: (Subject) -> Unit,
    onCompare: (Long, Long) -> Unit,
    onOpenTimelapse: () -> Unit,
    onOpenRecap: () -> Unit,
    onExitFlow: () -> Unit,
) {
    entry<ProgressRoute> {
        ProgressScreen(
            scrollState = scrollState,
            twoPane = twoPane,
            onOpenSubject = onOpenSubject,
            onOpenRecap = onOpenRecap,
        )
    }
    entry<PhotosRoute> {
        PhotosScreen(
            onCompare = onCompare,
            onOpenTimelapse = onOpenTimelapse,
            onExitFlow = onExitFlow,
        )
    }
    entry<SleepRoute> {
        SleepScreen(
            onSwitchSubject = onSwitchSubject,
            onOpenRecap = onOpenRecap,
            onExitFlow = onExitFlow,
        )
    }
    entry<MoodRoute> {
        MoodScreen(
            onSwitchSubject = onSwitchSubject,
            onOpenRecap = onOpenRecap,
            onExitFlow = onExitFlow,
        )
    }
    entry<HeartRoute> {
        HeartScreen(
            onSwitchSubject = onSwitchSubject,
            onOpenRecap = onOpenRecap,
            onExitFlow = onExitFlow,
        )
    }
    entry<SupplementsRoute> {
        SupplementsScreen(
            onSwitchSubject = onSwitchSubject,
            onOpenRecap = onOpenRecap,
            onExitFlow = onExitFlow,
        )
    }
    entry<StrengthRoute> {
        StrengthScreen(
            onSwitchSubject = onSwitchSubject,
            onOpenRecap = onOpenRecap,
            onExitFlow = onExitFlow,
        )
    }
    entry<FastingRoute> {
        FastingScreen(
            onSwitchSubject = onSwitchSubject,
            onOpenRecap = onOpenRecap,
            onExitFlow = onExitFlow,
        )
    }
    entry<ActivityRoute> {
        ActivityScreen(
            onSwitchSubject = onSwitchSubject,
            onOpenRecap = onOpenRecap,
            onExitFlow = onExitFlow,
        )
    }
    entry<CycleRoute> {
        CycleScreen(
            onSwitchSubject = onSwitchSubject,
            onOpenRecap = onOpenRecap,
            onExitFlow = onExitFlow,
        )
    }
    entry<BloodPressureRoute> {
        BloodPressureScreen(
            onSwitchSubject = onSwitchSubject,
            onOpenRecap = onOpenRecap,
            onExitFlow = onExitFlow,
        )
    }
    entry<MeasurementsRoute> {
        MeasurementsScreen(
            onSwitchSubject = onSwitchSubject,
            onOpenRecap = onOpenRecap,
            onExitFlow = onExitFlow,
        )
    }
    entry<PhotoComparisonRoute> { key ->
        PhotoComparisonScreen(selectedIds = listOf(key.firstId, key.secondId))
    }
    entry<TimelapseRoute> { TimelapseScreen() }
    entry<RecapRoute> { RecapScreen(onExitFlow = onExitFlow) }
}
