package ph.mart.healthapp.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.material3.ColorProviders
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.fasting.FastingRepository
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.health.StepsRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.water.WaterRepository
import ph.mart.healthapp.core.designsystem.theme.darkScheme
import ph.mart.healthapp.core.designsystem.theme.lightScheme
import ph.mart.healthapp.core.today.TodaySnapshot
import ph.mart.healthapp.today.todaySnapshotFlow

/** Below this the water row is dropped rather than squeezed — a clipped control is worse than an
 * absent one. Matches the 2x1 cell the widget can be resized down to. */

private val SMALL = DpSize(180.dp, 60.dp)
private val WIDE = DpSize(250.dp, 110.dp)

/**
 * The home-screen surface: today's calorie budget, the streak, and one-tap water.
 *
 * It lives in `:app` beside `ph.mart.healthapp.reminder` for the same reason reminders do — a
 * widget is a system surface, not a screen, and `:feature:*` modules hold screens. It reads the
 * repository interfaces directly; there is no ViewModel because there is no Compose UI lifecycle
 * to hold one.
 *
 * What it draws comes from [todaySnapshotFlow], shared with the watch app and the tile so the
 * three cannot disagree about today.
 *
 * Koin's global context is already started (Application.onCreate runs before any widget
 * broadcast), so [KoinComponent] is enough — same as [ph.mart.healthapp.reminder.ReminderWorker].
 */
class TodayWidget : GlanceAppWidget(), KoinComponent {

    private val profileRepository: ProfileRepository by inject()
    private val foodRepository: FoodRepository by inject()
    private val waterRepository: WaterRepository by inject()
    private val exerciseRepository: ExerciseRepository by inject()
    private val progressRepository: ProgressRepository by inject()
    private val stepsRepository: StepsRepository by inject()
    private val fastingRepository: FastingRepository by inject()

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshots = todaySnapshotFlow(
            profileRepository = profileRepository,
            foodRepository = foodRepository,
            waterRepository = waterRepository,
            exerciseRepository = exerciseRepository,
            progressRepository = progressRepository,
            stepsRepository = stepsRepository,
            fastingRepository = fastingRepository,
        )

        provideContent {
            val state by snapshots.collectAsState(TodaySnapshot())
            // `darkThemeOn == null` hands both schemes to Glance and lets the system pick, which
            // is the same `?: isSystemInDarkTheme()` resolution MainActivity does. An explicit
            // choice pins both slots so the widget follows the app rather than the device.
            val colors = when (state.darkThemeOn) {
                null -> ColorProviders(light = lightScheme, dark = darkScheme)
                true -> ColorProviders(darkScheme)
                false -> ColorProviders(lightScheme)
            }
            GlanceTheme(colors = colors) { TodayWidgetContent(state) }
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
