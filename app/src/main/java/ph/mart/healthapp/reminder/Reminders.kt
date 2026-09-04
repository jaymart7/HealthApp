package ph.mart.healthapp.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar
import java.util.TimeZone
import ph.mart.healthapp.EXTRA_ACTION
import ph.mart.healthapp.MainActivity
import ph.mart.healthapp.R
import ph.mart.healthapp.ShortcutAction
import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.fasting.goalReachedMillis
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.navigation.route.TopLevelDestination

/** Intent extra naming the tab a notification tap should land on — read by MainActivity. */
const val EXTRA_TAB = "ph.mart.healthapp.reminder.TAB"

/** Intent extra carrying the notification's own id to [WaterActionReceiver], which is what lets
 * answering the nudge cancel it. */
const val EXTRA_NOTIFICATION_ID = "ph.mart.healthapp.reminder.NOTIFICATION_ID"

/** Input-data key carrying [Reminder.name] into [ReminderWorker]. */
const val KEY_REMINDER = "reminder"

/**
 * The whole reminder schedule, in one table. Times are not invented here — they're the ones the
 * Profile switches already promise in their sublabels (`ReminderKind` in `:feature:profile`):
 * meals 3x daily, weigh-in Monday 8:00, photos every 2 weeks.
 *
 * [mealType] is set only on the three meal reminders, [checksWater] only on the two water ones,
 * [checksSupplements] only on the supplement one, [checksPlan] only on the training one and
 * [checksRecap] only on the weekly recap; all five are what let the worker stay quiet about
 * something already logged today — or, for the recap, about a week with nothing in it.
 */
enum class Reminder(
    val periodDays: Long,
    val hour: Int,
    /** [Calendar.MONDAY] etc. for a weekly reminder; null when any day will do. */
    val dayOfWeek: Int?,
    val title: String,
    val body: String,
    val tab: TopLevelDestination,
    val mealType: MealType?,
    val checksWater: Boolean = false,
    val checksSupplements: Boolean = false,
    /** Silent on a day the training plan doesn't ask for, and on a day already lifted. */
    val checksPlan: Boolean = false,
    /** Silent on a week with nothing logged in it — see [hasRecapToShow]. */
    val checksRecap: Boolean = false,
    /**
     * What the tap should *do* once it lands on [tab], beyond arriving there. Null for every
     * reminder whose tab is the whole answer; [ShortcutAction] for the one that isn't, because a
     * route request delivered by an intent that must re-point an already-running app is exactly
     * what that enum is for — the argument `HealthSync` already made.
     */
    val action: ShortcutAction? = null,
) {
    Breakfast(1, 8, null, "Breakfast logged?", "Add it while you remember the portions.", TopLevelDestination.Food, MealType.Breakfast),
    Lunch(1, 13, null, "Lunch logged?", "A quick entry keeps today's macros honest.", TopLevelDestination.Food, MealType.Lunch),
    Dinner(1, 19, null, "Dinner logged?", "Close out the day's diary.", TopLevelDestination.Food, MealType.Dinner),
    WeighIn(7, 8, Calendar.MONDAY, "Weigh-in day", "Same time, same scale — log this week's weight.", TopLevelDestination.Progress, null),
    Photo(14, 9, null, "Progress photo time", "Two weeks on. Take the next one in the same pose.", TopLevelDestination.Progress, null),
    WaterMidday(1, 11, null, "Water check", "Halfway through the day — how many glasses so far?", TopLevelDestination.Home, null, checksWater = true),
    WaterAfternoon(1, 16, null, "Water check", "Still time to hit today's water goal.", TopLevelDestination.Home, null, checksWater = true),
    // Appended rather than slotted in beside the other daily ones: [ordinal] is the notification
    // id, so inserting mid-list would re-point every notification already pending on a device.
    Supplements(1, 9, null, "Supplements", "Tick off what you've taken today.", TopLevelDestination.Home, null, checksSupplements = true),
    // Appended for [Supplements]' reason — [ordinal] is the notification id. Late afternoon: early
    // enough to still train today, late enough that a morning session has already been logged.
    Workout(1, 17, null, "Training day", "Today's routine is on the plan.", TopLevelDestination.Home, null, checksPlan = true),
    // Appended for [Supplements]' reason once more — [ordinal] is the notification id. Sunday
    // 19:00: late enough that the week is over, early enough to still be read.
    WeeklyRecap(
        7, 19, Calendar.SUNDAY,
        "Your week in review",
        "Seven days done — see how they went.",
        TopLevelDestination.Progress,
        null,
        checksRecap = true,
        action = ShortcutAction.OpenRecap,
    ),
    ;

    val uniqueName: String get() = "reminder-$name"
}

/** Which profile switch owns this reminder. The three meal reminders share one switch, as do the
 * two water ones. */
fun Reminder.enabledIn(profile: Profile): Boolean = when (this) {
    Reminder.Breakfast, Reminder.Lunch, Reminder.Dinner -> profile.mealRemindersOn
    Reminder.WeighIn -> profile.weighInReminderOn
    Reminder.Photo -> profile.photoReminderOn
    Reminder.WaterMidday, Reminder.WaterAfternoon -> profile.waterRemindersOn
    Reminder.Supplements -> profile.supplementRemindersOn
    Reminder.Workout -> profile.workoutRemindersOn
    Reminder.WeeklyRecap -> profile.recapReminderOn
}

/**
 * The instant a pending fasting-goal notification should fire, or null when there shouldn't be one.
 * Split from [fastingGoalDelayMillis] so the collector in `FitPulseApplication` can
 * `distinctUntilChanged` on an *absolute* target — the profile row re-emits on every weight edit,
 * and a delay recomputed against a moving `now` would look different every time and re-enqueue the
 * work for no reason.
 */
internal fun fastingGoalTargetMillis(fast: FastSession?, fastingRemindersOn: Boolean): Long? =
    fast?.takeIf { fastingRemindersOn }?.goalReachedMillis

/**
 * Null for a target already in the past: the work enqueued when the fast started has already fired
 * by then, and re-firing it on the next profile emission would be a duplicate.
 */
internal fun fastingGoalDelayMillis(targetMillis: Long?, nowMillis: Long): Long? =
    targetMillis?.takeIf { it > nowMillis }?.minus(nowMillis)

/**
 * True when the last seven days hold at least one logged day — the four-domain definition, so
 * [ph.mart.healthapp.core.data.streak.loggedDays] is what the caller folds.
 *
 * This is deliberately the *same* predicate `recap()` returns null on, which is what makes the
 * notification unable to open an empty overlay: a week with nothing in it has no recap to show, and
 * the card on the Progress tab is hidden in exactly that case. The 6 is `RecapPeriod.Week.days - 1`
 * spelled out — that enum is a `:feature:progress` UI type, and `:app/reminder` has no business
 * importing one. If the two ever drift, `RecapScreen` already degrades to its empty state.
 *
 * Pure, like [fastingGoalTargetMillis], so the unit test can drive it over a plain day set.
 */
internal fun hasRecapToShow(loggedDays: Set<Long>, todayEpochDay: Long): Boolean =
    (todayEpochDay - 6..todayEpochDay).any { it in loggedDays }

/**
 * Posts one notification, tapping through to [tab]. Shared by [ReminderWorker] and
 * [FastingGoalWorker] so the two don't hold two copies of the same builder — and so a change to
 * how FitPulse notifies reaches both.
 *
 * [id] must be stable per notification kind: it is what lets a repeat replace its predecessor
 * rather than stack another row in the shade.
 *
 * [action] rides [EXTRA_ACTION] beside the tab, for the reminder whose tap has to *do* something
 * once it lands rather than merely arrive — `MainActivity` resolves it exactly as it resolves a
 * launcher shortcut, so nothing new parses it.
 *
 * [waterAction] adds the one action button in the app. Only water gets one: [addGlass] is a single
 * unambiguous write, already shared with the widget and the watch so all three cap at the same
 * goal. "Log breakfast" has no single write — it needs the sheet, and a button that opened a sheet
 * would be the tap the notification body already is.
 */
internal fun notify(
    context: Context,
    id: Int,
    title: String,
    body: String,
    tab: TopLevelDestination,
    waterAction: Boolean = false,
    action: ShortcutAction? = null,
) {
    val intent = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        .putExtra(EXTRA_TAB, tab.name)
        .apply { action?.let { putExtra(EXTRA_ACTION, it.name) } }
    val pendingIntent = PendingIntent.getActivity(
        context,
        id,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(body)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
    if (waterAction) {
        val actionIntent = Intent(context, WaterActionReceiver::class.java)
            .putExtra(EXTRA_NOTIFICATION_ID, id)
        // Request code [id] rather than 0: the two water reminders differ only in their extra,
        // and FLAG_UPDATE_CURRENT on a shared code would let one rewrite the other's intent.
        val actionPendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            actionIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        // The icon is required by the signature and ignored by every phone launcher since API 24;
        // reusing the small icon beats a second drawable nothing renders.
        builder.addAction(R.drawable.ic_notification, "+1 glass", actionPendingIntent)
    }
    val notification = builder.build()
    // The caller's areNotificationsEnabled() check already covers the permission; this catch is
    // only for the race where it's revoked between that check and here.
    runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
}

/**
 * Absolute millis of the next [hour]:00 local — strictly in the future, and on [dayOfWeek] when one
 * is given. Takes primitives rather than a [Reminder] so it stays a plain JVM function the unit
 * test can drive at a fixed instant.
 *
 * ponytail: `java.util.Calendar`, not `java.time` — same reason as the comment in
 * `core/data/food/FoodRepositoryImpl.kt`: this project has no core-library desugaring configured.
 */
internal fun nextRunMillis(
    hour: Int,
    dayOfWeek: Int?,
    nowMillis: Long,
    zone: TimeZone = TimeZone.getDefault(),
): Long {
    val calendar = Calendar.getInstance(zone).apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (calendar.timeInMillis <= nowMillis) calendar.add(Calendar.DAY_OF_MONTH, 1)
    // Stepping a day at a time rather than set(DAY_OF_WEEK, …), which resolves against the locale's
    // first-day-of-week and can land in the past.
    while (dayOfWeek != null && calendar.get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
        calendar.add(Calendar.DAY_OF_MONTH, 1)
    }
    return calendar.timeInMillis
}
