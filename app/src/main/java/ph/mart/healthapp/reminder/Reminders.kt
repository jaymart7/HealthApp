package ph.mart.healthapp.reminder

import java.util.Calendar
import java.util.TimeZone
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.navigation.route.TopLevelDestination

/** Intent extra naming the tab a notification tap should land on — read by MainActivity. */
const val EXTRA_TAB = "ph.mart.healthapp.reminder.TAB"

/** Input-data key carrying [Reminder.name] into [ReminderWorker]. */
const val KEY_REMINDER = "reminder"

/**
 * The whole reminder schedule, in one table. Times are not invented here — they're the ones the
 * Profile switches already promise in their sublabels (`ReminderKind` in `:feature:profile`):
 * meals 3x daily, weigh-in Monday 8:00, photos every 2 weeks.
 *
 * [mealType] is set only on the three meal reminders, and is what lets the worker stay quiet when
 * that meal is already in today's diary.
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
) {
    Breakfast(1, 8, null, "Breakfast logged?", "Add it while you remember the portions.", TopLevelDestination.Food, MealType.Breakfast),
    Lunch(1, 13, null, "Lunch logged?", "A quick entry keeps today's macros honest.", TopLevelDestination.Food, MealType.Lunch),
    Dinner(1, 19, null, "Dinner logged?", "Close out the day's diary.", TopLevelDestination.Food, MealType.Dinner),
    WeighIn(7, 8, Calendar.MONDAY, "Weigh-in day", "Same time, same scale — log this week's weight.", TopLevelDestination.Progress, null),
    Photo(14, 9, null, "Progress photo time", "Two weeks on. Take the next one in the same pose.", TopLevelDestination.Progress, null),
    ;

    val uniqueName: String get() = "reminder-$name"
}

/** Which profile switch owns this reminder. The three meal reminders share one switch. */
fun Reminder.enabledIn(profile: Profile): Boolean = when (this) {
    Reminder.Breakfast, Reminder.Lunch, Reminder.Dinner -> profile.mealRemindersOn
    Reminder.WeighIn -> profile.weighInReminderOn
    Reminder.Photo -> profile.photoReminderOn
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
