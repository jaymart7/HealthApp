package ph.mart.healthapp.reminder

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import ph.mart.healthapp.R

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

    /**
     * The fasting goal is the one one-shot here: its hour comes from when the user stopped eating,
     * not from a clock, so there is nothing to repeat. [delayMillis] null cancels — no fast open,
     * the switch off, or a target already in the past.
     *
     * REPLACE rather than KEEP, the opposite of [reconcile]'s policy and for the opposite reason:
     * starting a new fast *must* move the pending notification, since the old one's delay was
     * measured from a fast that no longer exists.
     */
    fun scheduleFastingGoal(delayMillis: Long?) {
        if (delayMillis == null) {
            workManager.cancelUniqueWork(FASTING_GOAL_WORK)
            return
        }
        workManager.enqueueUniqueWork(
            FASTING_GOAL_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<FastingGoalWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build(),
        )
    }

    private fun Reminder.request() = PeriodicWorkRequestBuilder<ReminderWorker>(periodDays, TimeUnit.DAYS)
        .setInitialDelay(
            System.currentTimeMillis().let { now -> nextRunMillis(hour, dayOfWeek, now) - now },
            TimeUnit.MILLISECONDS,
        )
        .setInputData(workDataOf(KEY_REMINDER to name))
        .build()

    companion object {
        /** One channel for every notification this app posts. Split per kind only if someone asks
         * to mute meal nudges without losing weigh-in. */
        fun createChannel(context: Context) {
            // NotificationChannelCompat, not the platform class: minSdk is 24 and channels only
            // exist from 26 — the compat builder no-ops below that instead of crashing.
            NotificationManagerCompat.from(context).createNotificationChannel(
                NotificationChannelCompat.Builder(
                    REMINDER_CHANNEL_ID,
                    NotificationManagerCompat.IMPORTANCE_DEFAULT,
                ).setName(context.getString(R.string.app_notification_channel)).build(),
            )
        }
    }
}
