package ph.mart.healthapp.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import ph.mart.healthapp.MainActivity
import ph.mart.healthapp.R
import ph.mart.healthapp.core.data.fasting.formatClockTime
import ph.mart.healthapp.core.data.health.formatSteps
import ph.mart.healthapp.core.navigation.route.TopLevelDestination
import ph.mart.healthapp.core.today.TodaySnapshot
import ph.mart.healthapp.core.today.progress
import ph.mart.healthapp.core.today.remainingKcal
import ph.mart.healthapp.core.today.waterGoalReached
import ph.mart.healthapp.reminder.EXTRA_TAB

private val WATER_ROW_MIN_HEIGHT = 100.dp

/**
 * `AppCard`'s chrome, rebuilt with the tokens Glance exposes: 20dp corners, 16dp padding, and
 * `surface` in place of `surfaceContainerLow`, which Glance's `ColorProviders` has no slot for.
 * No hex is written here — every color still comes from the frozen palette by way of the schemes.
 */
@Composable
internal fun TodayWidgetContent(state: TodaySnapshot) {
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
            FastingRow(state)
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
            text = LocalContext.current.getString(R.string.widget_onboarding),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
        )
    }
}

@Composable
private fun CaloriesContent(state: TodaySnapshot) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = LocalContext.current.getString(R.string.widget_today),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            modifier = GlanceModifier.defaultWeight(),
        )
        if (state.streakDays > 0) {
            Text(
                text = LocalContext.current.getString(R.string.widget_streak, state.streakDays),
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
            text = LocalContext.current.getString(R.string.widget_budget, state.budgetKcal),
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
            text = LocalContext.current.getString(
                if (remaining >= 0) R.string.widget_kcal_left else R.string.widget_kcal_over,
                if (remaining >= 0) remaining else -remaining,
            ),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            modifier = GlanceModifier.defaultWeight(),
        )
        // Omitted rather than zeroed when Google Health isn't connected, same as Home's card.
        if (state.steps > 0) {
            Text(
                text = LocalContext.current.getString(R.string.widget_steps, formatSteps(state.steps)),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            )
        }
    }
}

@Composable
private fun WaterRow(state: TodaySnapshot) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = LocalContext.current.getString(
                R.string.widget_water,
                state.glasses,
                state.goalGlasses,
                state.waterLabel,
            ),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp),
            modifier = GlanceModifier.defaultWeight(),
        )
        if (state.waterGoalReached) {
            Text(
                text = LocalContext.current.getString(R.string.widget_water_goal_hit),
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        } else {
            FilledButton(
                text = LocalContext.current.getString(R.string.widget_add_glass),
                onClick = actionRunCallback<AddGlassAction>(),
                maxLines = 1,
            )
        }
    }
}

/** Omitted entirely when nothing is running, like the steps line — a fast you haven't started is
 * not a zero. */
@Composable
private fun FastingRow(state: TodaySnapshot) {
    val until = state.fastingUntilMillis ?: return
    Spacer(GlanceModifier.height(4.dp))
    Text(
        text = if (state.fastingGoalReached) {
            LocalContext.current.getString(R.string.widget_fast_complete)
        } else {
            LocalContext.current.getString(R.string.widget_fasting_until, formatClockTime(until))
        },
        style = TextStyle(
            color = if (state.fastingGoalReached) {
                GlanceTheme.colors.primary
            } else {
                GlanceTheme.colors.onSurfaceVariant
            },
            fontSize = 13.sp,
        ),
    )
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
