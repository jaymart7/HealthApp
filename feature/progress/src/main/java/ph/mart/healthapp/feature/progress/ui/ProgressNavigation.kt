package ph.mart.healthapp.feature.progress.ui

import androidx.compose.foundation.ScrollState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import ph.mart.healthapp.core.navigation.route.ProgressRoute
import ph.mart.healthapp.feature.progress.ui.achievement.AchievementsScreen
import ph.mart.healthapp.feature.progress.ui.activity.ActivityScreen
import ph.mart.healthapp.feature.progress.ui.capture.AddPhotoCaptureScreen
import ph.mart.healthapp.feature.progress.ui.comparison.PhotoComparisonScreen
import ph.mart.healthapp.feature.progress.ui.cycle.CycleScreen
import ph.mart.healthapp.feature.progress.ui.fasting.FastingScreen
import ph.mart.healthapp.feature.progress.ui.heart.HeartScreen
import ph.mart.healthapp.feature.progress.ui.measurement.MeasurementsScreen
import ph.mart.healthapp.feature.progress.ui.mood.MoodScreen
import ph.mart.healthapp.feature.progress.ui.nutrition.NutritionScreen
import ph.mart.healthapp.feature.progress.ui.photo.PhotosScreen
import ph.mart.healthapp.feature.progress.ui.pressure.BloodPressureScreen
import ph.mart.healthapp.feature.progress.ui.preview.AddPhotoPreviewScreen
import ph.mart.healthapp.feature.progress.ui.progress.ProgressScreen
import ph.mart.healthapp.feature.progress.ui.progress.Subject
import ph.mart.healthapp.feature.progress.ui.recap.RecapScreen
import ph.mart.healthapp.feature.progress.ui.sleep.SleepScreen
import ph.mart.healthapp.feature.progress.ui.strength.StrengthScreen
import ph.mart.healthapp.feature.progress.ui.supplement.SupplementsScreen
import ph.mart.healthapp.feature.progress.ui.timelapse.TimelapseScreen
import ph.mart.healthapp.feature.progress.ui.water.WaterScreen
import ph.mart.healthapp.feature.progress.ui.weight.WeightScreen

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

/** Weigh-ins, the goal line and the energy check-in. Carries nothing: `WeightViewModel` reads all three flows itself. */
@Serializable
data object WeightRoute : NavKey

/** Glasses a day against the current goal. Carries nothing: `WaterViewModel` reads both itself. */
@Serializable
data object WaterRoute : NavKey

/** Calories, macros and the kept plates. Carries nothing: `NutritionViewModel` reads all three flows itself. */
@Serializable
data object NutritionRoute : NavKey

/** Every badge in the app. Carries nothing: `AchievementsViewModel` folds the six repositories it
 * takes into the five numbers `badgeGroups()` needs. */
@Serializable
data object AchievementsRoute : NavKey

/**
 * The subject pages that are routes rather than `SubjectDetail` swap-ins.
 *
 * One set, read by `AppScaffold`'s `ownsTopBar` (each page draws its own `AppTopBar`), by
 * [route] and by `TabChromeTest`, so none of the three can disagree about which subjects have
 * converted. It grows by one per conversion commit.
 */
val ProgressSubjectRoutes: Set<NavKey> = setOf(PhotosRoute, SleepRoute, MoodRoute, HeartRoute, SupplementsRoute, StrengthRoute, FastingRoute, ActivityRoute, CycleRoute, BloodPressureRoute, MeasurementsRoute, WeightRoute, NutritionRoute, WaterRoute, AchievementsRoute)

/**
 * A subject to the route that draws it. The one place the mapping lives, which is what keeps
 * `AppScaffold` at a single push site rather than one per subject.
 *
 * It is also where the one name difference is reconciled: the subject is `Badges` and the row on
 * the overview says so, while the package, the derivation and the screen have always said
 * achievements.
 */
fun Subject.route(): NavKey = when (this) {
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
    Subject.Weight -> WeightRoute
    Subject.Nutrition -> NutritionRoute
    Subject.Water -> WaterRoute
    Subject.Badges -> AchievementsRoute
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

/** The viewfinder a body progress shot starts at. A route rather than the bottom sheet it used to
 * be, because a full-window camera is the one thing a sheet cannot hand it — the argument
 * `FoodCaptureRoute` and `BarcodeScanRoute` already settled. Carries nothing: it ends the moment
 * there is a picture, and [AddPhotoPreviewRoute] is what carries that. */
@Serializable
data object AddPhotoRoute : NavKey

/** The same shot, with the date and weight it gets filed under — the other half of the flow, and a
 * route of its own because a scrolling form has nothing in common with a viewfinder.
 *
 * [cachePath] is a staging JPEG in `cacheDir`, because a `Bitmap` cannot ride in a `NavKey` and a
 * holder living beside the back stack would be a second place to keep the flow's state. A path is
 * also what lets the shot survive a rotation and a process death, which the single-route flow's
 * remembered bitmap did not. */
@Serializable
data class AddPhotoPreviewRoute(val cachePath: String) : NavKey

/**
 * [scrollState] is hoisted for the usual reason: the FAB's scroll-collapse lives in AppScaffold,
 * which can't see a ScrollState created inside the screen. There is no `twoPane` flag any more:
 * every subject page is a route, so this tab draws one column at every width.
 *
 * Every surface but [ProgressRoute] itself is a route rather than an overlay drawn inside it, so
 * none of them wears the bottom bar or the FAB and none wires a back handler to leave itself with.
 * Not one of them holds a handler now: the add-photo flow's two steps used to need one and are two
 * routes instead, so a retake is the stack popping one entry and leaving is it popping the next.
 *
 * [onOpenSubject] pushes a subject page. The comparison and the timelapse are reached from the
 * Photos page rather than from the tab, which is why those callbacks land on a different entry.
 */
fun EntryProviderScope<NavKey>.progressEntries(
    scrollState: ScrollState,
    onOpenSubject: (Subject) -> Unit,
    onCompare: (Long, Long) -> Unit,
    onOpenTimelapse: () -> Unit,
    /** The staging path the viewfinder just wrote, pushed as [AddPhotoPreviewRoute]. */
    onOpenPhotoPreview: (String) -> Unit,
    onOpenRecap: () -> Unit,
    /** The page's own question, carried to the coach — which lives above this tab, so like
     * every other cross-feature jump it stays a callback `AppScaffold` resolves. Only the twelve
     * subjects the coach has tools for ever call it (see [Subject.coachQuestion]), plus the recap
     * on a week or a month (see [ph.mart.healthapp.feature.progress.ui.shared.RecapPeriod]). */
    onAskCoach: (question: String, source: String) -> Unit,
    onExitFlow: () -> Unit,
) {
    entry<ProgressRoute> {
        ProgressScreen(
            scrollState = scrollState,
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
            onOpenRecap = onOpenRecap,
            onAskCoach = onAskCoach,
            onExitFlow = onExitFlow,
        )
    }
    entry<MoodRoute> {
        MoodScreen(
            onOpenRecap = onOpenRecap,
            onAskCoach = onAskCoach,
            onExitFlow = onExitFlow,
        )
    }
    entry<HeartRoute> {
        HeartScreen(
            onOpenRecap = onOpenRecap,
            onAskCoach = onAskCoach,
            onExitFlow = onExitFlow,
        )
    }
    entry<SupplementsRoute> {
        SupplementsScreen(
            onOpenRecap = onOpenRecap,
            onAskCoach = onAskCoach,
            onExitFlow = onExitFlow,
        )
    }
    entry<StrengthRoute> {
        StrengthScreen(
            onOpenRecap = onOpenRecap,
            onAskCoach = onAskCoach,
            onExitFlow = onExitFlow,
        )
    }
    entry<FastingRoute> {
        FastingScreen(
            onOpenRecap = onOpenRecap,
            onAskCoach = onAskCoach,
            onExitFlow = onExitFlow,
        )
    }
    entry<ActivityRoute> {
        ActivityScreen(
            onOpenRecap = onOpenRecap,
            onAskCoach = onAskCoach,
            onExitFlow = onExitFlow,
        )
    }
    entry<CycleRoute> {
        CycleScreen(
            onOpenRecap = onOpenRecap,
            onExitFlow = onExitFlow,
        )
    }
    entry<BloodPressureRoute> {
        BloodPressureScreen(
            onOpenRecap = onOpenRecap,
            onAskCoach = onAskCoach,
            onExitFlow = onExitFlow,
        )
    }
    entry<MeasurementsRoute> {
        MeasurementsScreen(
            onOpenRecap = onOpenRecap,
            onAskCoach = onAskCoach,
            onExitFlow = onExitFlow,
        )
    }
    entry<WeightRoute> {
        WeightScreen(
            onOpenRecap = onOpenRecap,
            onAskCoach = onAskCoach,
            onExitFlow = onExitFlow,
        )
    }
    entry<NutritionRoute> {
        NutritionScreen(
            onOpenRecap = onOpenRecap,
            onAskCoach = onAskCoach,
            onExitFlow = onExitFlow,
        )
    }
    entry<WaterRoute> {
        WaterScreen(
            onOpenRecap = onOpenRecap,
            onAskCoach = onAskCoach,
            onExitFlow = onExitFlow,
        )
    }
    entry<AchievementsRoute> {
        AchievementsScreen(onOpenRecap = onOpenRecap, onExitFlow = onExitFlow)
    }
    entry<PhotoComparisonRoute> { key ->
        PhotoComparisonScreen(selectedIds = listOf(key.firstId, key.secondId))
    }
    entry<TimelapseRoute> { TimelapseScreen() }
    entry<RecapRoute> { RecapScreen(onAskCoach = onAskCoach, onExitFlow = onExitFlow) }
    entry<AddPhotoRoute> {
        AddPhotoCaptureScreen(onPreview = onOpenPhotoPreview, onExitFlow = onExitFlow)
    }
    entry<AddPhotoPreviewRoute> { key ->
        AddPhotoPreviewScreen(
            cachePath = key.cachePath,
            // A retake is one pop, which lands on the viewfinder the shot came from — or on the
            // screen explaining a refused camera, which is where the viewfinder actually is.
            onRetake = onExitFlow,
            // Saving or cancelling leaves the whole flow, and the flow is two entries deep.
            onExitFlow = { onExitFlow(); onExitFlow() },
        )
    }
}
