package ph.mart.healthapp.reminder

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

const val REMINDER_CHANNEL_ID = "reminders"

/**
 * Reconciles WorkManager against whichever reminders the profile has switched on. Nothing commands
 * a schedule — [ph.mart.healthapp.FitPulseApplication] watches the profile row and calls
 * [reconcile] whenever the enabled set moves, so the Profile toggle stays a plain Room write.
 */
class ReminderScheduler(context: Context) {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun reconcile(enabled: Set<Reminder>) {
        Reminder.entries.forEach { reminder ->
            if (reminder in enabled) {
                workManager.enqueueUniquePeriodicWork(
                    reminder.uniqueName,
                    // KEEP, not UPDATE: reconcile runs on every app start, and UPDATE would reset
                    // the initial delay each time — a daily reminder would never reach its first
                    // run on someone who opens the app most mornings.
                    // ponytail: the flip side is that changing an hour in a later release won't
                    // reach existing installs. Rename the unique work if that ever matters.
                    ExistingPeriodicWorkPolicy.KEEP,
                    reminder.request(),
                )
            } else {
                workManager.cancelUniqueWork(reminder.uniqueName)
            }
        }
    }

    private fun Reminder.request() = PeriodicWorkRequestBuilder<ReminderWorker>(periodDays, TimeUnit.DAYS)
        .setInitialDelay(
            System.currentTimeMillis().let { now -> nextRunMillis(hour, dayOfWeek, now) - now },
            TimeUnit.MILLISECONDS,
        )
        .setInputData(workDataOf(KEY_REMINDER to name))
        .build()

    companion object {
        /** One channel for all five reminders. Split per kind only if someone asks to mute meal
         * nudges without losing weigh-in. */
        fun createChannel(context: Context) {
            // NotificationChannelCompat, not the platform class: minSdk is 24 and channels only
            // exist from 26 — the compat builder no-ops below that instead of crashing.
            NotificationManagerCompat.from(context).createNotificationChannel(
                NotificationChannelCompat.Builder(
                    REMINDER_CHANNEL_ID,
                    NotificationManagerCompat.IMPORTANCE_DEFAULT,
                ).setName("Reminders").build(),
            )
        }
    }
}
