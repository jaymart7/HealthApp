package ph.mart.healthapp.reminder

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
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
            // KEEP, not REPLACE: reconcile runs on every app start, and REPLACE would push the
            // pending firing back each time — a daily reminder would never reach its first run on
            // someone who opens the app most mornings. Once a run has finished, the unique name is
            // no longer held, so this also re-forms a chain that was somehow broken.
            if (reminder in enabled) schedule(reminder, ExistingWorkPolicy.KEEP)
            else workManager.cancelUniqueWork(reminder.uniqueName)
        }
    }

    /**
     * The **next single firing** of [reminder], and only that one. [ReminderWorker] books the one
     * after it as its last act, so the schedule is a chain of one-shots rather than a
     * `PeriodicWorkRequest`.
     *
     * That is the whole point. A periodic request takes its initial delay once and then re-anchors
     * every subsequent period to the end of the previous window — Doze deferrals accumulate, and a
     * DST change or a move between timezones shifted every later firing permanently with no way
     * back. A chain re-reads [nextRunMillis] against the current clock and zone on every run, so an
     * 08:00 reminder is at 08:00 the next morning whatever happened to the clock overnight.
     *
     * It also retires the old `ponytail:` caveat here: changing an hour in a later release now
     * reaches existing installs at their next firing, with no unique-work rename.
     */
    fun schedule(reminder: Reminder, policy: ExistingWorkPolicy) {
        workManager.enqueueUniqueWork(reminder.uniqueName, policy, reminder.request())
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

    private fun Reminder.request() = OneTimeWorkRequestBuilder<ReminderWorker>()
        .setInitialDelay(
            System.currentTimeMillis().let { now ->
                nextRunMillis(hour, dayOfWeek, now, periodDays) - now
            },
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
