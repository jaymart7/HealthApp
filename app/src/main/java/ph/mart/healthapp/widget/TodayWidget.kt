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
import kotlinx.coroutines.flow.combine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.fasting.FastingRepository
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.dailyTotals
import ph.mart.healthapp.core.data.health.StepsRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.streak.loggedDays
import ph.mart.healthapp.core.data.streak.streakStats
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.data.water.WaterRepository
import ph.mart.healthapp.core.designsystem.theme.darkScheme
import ph.mart.healthapp.core.designsystem.theme.lightScheme

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
        // Built here, once per session, and deliberately not hoisted to a field: the today-only
        // repository overloads resolve todayEpochDay() at flow-construction time, so a session
        // that outlived midnight would keep reporting yesterday. The 30-minute updatePeriodMillis
        // in today_widget_info.xml is what restarts it. Same trap HomeViewModel notes for the
        // streak, solved the other way round because there is no recomposition to hang it on.
        val activeDays = combine(
            foodRepository.observeDailyNutrition(),
            waterRepository.observeLoggedDays(),
            progressRepository.observeWeightEntries(),
            exerciseRepository.observeLoggedDays(),
            ::loggedDays,
        )
        val states = combine(
            profileRepository.observeProfile(),
            foodRepository.observeTodayEntries(),
            // Water pairs with the running fast: the combine below is already at the arity the
            // typed overloads stop at.
            combine(waterRepository.observeToday(), fastingRepository.observeActive(), ::Pair),
            // Paired ahead of the combine, which is already at the arity the typed overloads
            // stop at.
            combine(
                exerciseRepository.observeTodayEntries(),
                stepsRepository.observeToday(),
                ::Pair,
            ),
            activeDays,
        ) { profile, entries, (glasses, activeFast), (exercise, steps), days ->
            todayWidgetState(
                profile = profile,
                totals = entries.dailyTotals(),
                glasses = glasses,
                activeFast = activeFast,
                exercise = exercise,
                steps = steps,
                streakDays = days.streakStats(todayEpochDay()).current,
            )
        }

        provideContent {
            val state by states.collectAsState(TodayWidgetState())
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
