package ph.mart.healthapp.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.flow.combine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ph.mart.healthapp.MainActivity
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.health.StepsRepository
import ph.mart.healthapp.core.data.health.formatSteps
import ph.mart.healthapp.core.data.food.dailyTotals
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.streak.loggedDays
import ph.mart.healthapp.core.data.streak.streakStats
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.data.water.WaterRepository
import ph.mart.healthapp.core.data.water.waterVolumeLabel
import ph.mart.healthapp.core.designsystem.theme.darkScheme
import ph.mart.healthapp.core.designsystem.theme.lightScheme
import ph.mart.healthapp.reminder.EXTRA_TAB
import ph.mart.healthapp.core.navigation.route.TopLevelDestination

/** Below this the water row is dropped rather than squeezed — a clipped control is worse than an
 * absent one. Matches the 2x1 cell the widget can be resized down to. */
private val WATER_ROW_MIN_HEIGHT = 100.dp

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
            waterRepository.observeToday(),
            // Paired ahead of the combine, which is already at the arity the typed overloads
            // stop at.
            combine(
                exerciseRepository.observeTodayEntries(),
                stepsRepository.observeToday(),
                ::Pair,
            ),
            activeDays,
        ) { profile, entries, glasses, (exercise, steps), days ->
            todayWidgetState(
                profile = profile,
                totals = entries.dailyTotals(),
                glasses = glasses,
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

/**
 * `AppCard`'s chrome, rebuilt with the tokens Glance exposes: 20dp corners, 16dp padding, and
 * `surface` in place of `surfaceContainerLow`, which Glance's `ColorProviders` has no slot for.
 * No hex is written here — every color still comes from the frozen palette by way of the schemes.
 */
@Composable
private fun TodayWidgetContent(state: TodayWidgetState) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(20.dp)
            .padding(16.dp)
            .clickable(actionStartActivity(openAppIntent(LocalContext.current, TopLevelDestination.Home))),
    ) {
        if (state.onboarding) {
            OnboardingContent()
            return@Column
        }
        CaloriesContent(state)
        if (LocalSize.current.height >= WATER_ROW_MIN_HEIGHT) {
            Spacer(GlanceModifier.height(12.dp))
            WaterRow(state)
        }
    }
}

/** Nothing to report until onboarding writes a profile — no targets exist to divide by. */
@Composable
private fun OnboardingContent() {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Set up FitPulse to see today's numbers.",
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
        )
    }
}

@Composable
private fun CaloriesContent(state: TodayWidgetState) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Today",
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            modifier = GlanceModifier.defaultWeight(),
        )
        if (state.streakDays > 0) {
            Text(
                text = "${state.streakDays}-day streak",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
    Spacer(GlanceModifier.height(4.dp))
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionStartActivity(openAppIntent(LocalContext.current, TopLevelDestination.Food))),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = "${state.consumedKcal}",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = " / ${state.budgetKcal} kcal",
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
        )
    }
    Spacer(GlanceModifier.height(8.dp))
    LinearProgressIndicator(
        progress = state.progress,
        color = GlanceTheme.colors.primary,
        backgroundColor = GlanceTheme.colors.surfaceVariant,
        modifier = GlanceModifier.fillMaxWidth(),
    )
    Spacer(GlanceModifier.height(4.dp))
    val remaining = state.remainingKcal
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            // Over budget is stated, not hidden — but as a fact, not a scolding, and in
            // onSurfaceVariant rather than error: a day over target is not a failure state.
            text = if (remaining >= 0) "$remaining kcal left" else "${-remaining} kcal over",
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            modifier = GlanceModifier.defaultWeight(),
        )
        // Omitted rather than zeroed when Google Health isn't connected, same as Home's card.
        if (state.steps > 0) {
            Text(
                text = "${formatSteps(state.steps)} steps",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            )
        }
    }
}

@Composable
private fun WaterRow(state: TodayWidgetState) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Water ${state.glasses} / ${state.goalGlasses} · " +
                waterVolumeLabel(state.glasses, state.unit),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp),
            modifier = GlanceModifier.defaultWeight(),
        )
        if (state.waterGoalReached) {
            Text(
                text = "Goal hit",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        } else {
            FilledButton(
                text = "+1 glass",
                onClick = actionRunCallback<AddGlassAction>(),
                maxLines = 1,
            )
        }
    }
}

/**
 * Reuses the reminder notification's extra, so a widget tap and a notification tap land on a tab
 * the same way — [MainActivity] already reads it.
 *
 * The distinct `data` URI is load-bearing: `Intent.filterEquals` ignores extras, so the two tab
 * intents would otherwise be the same intent, and Glance would conflate their PendingIntents into
 * whichever was created last.
 */
private fun openAppIntent(context: Context, tab: TopLevelDestination) =
    Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        .setData(Uri.parse("fitpulse://widget/${tab.name}"))
        .putExtra(EXTRA_TAB, tab.name)
