package ph.mart.healthapp.reminder

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ph.mart.healthapp.R
import ph.mart.healthapp.core.data.fasting.FastingRepository
import ph.mart.healthapp.core.data.fasting.formatElapsed
import ph.mart.healthapp.core.data.fasting.goalReachedMillis
import ph.mart.healthapp.core.navigation.route.TopLevelDestination

/** Distinct from every [Reminder.ordinal] so the two kinds can't overwrite each other's row in
 * the shade. */
const val FASTING_GOAL_NOTIFICATION_ID = 1000

/** Unique work name — one pending goal notification at a time, because there is one fast at a time. */
const val FASTING_GOAL_WORK = "fasting-goal"

/**
 * Says a fast reached its target. Enqueued as a one-shot when the fast starts, unlike every
 * [Reminder], which is periodic — a fast's target lands at an hour derived from when the user
 * happened to stop eating, not at one on a clock.
 *
 * It re-reads the fast at fire time rather than trusting its own input data: WorkManager will
 * happily run work whose cancellation raced it, and the one thing this notification must never do
 * is congratulate someone on a fast they ended two hours ago.
 */
class FastingGoalWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val fastingRepository: FastingRepository by inject()

    override suspend fun doWork(): Result {
        // Revoked after the work was enqueued — stay quiet rather than posting into the void.
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return Result.success()

        // Ended, discarded, or replaced by a later fast that hasn't got there yet.
        val fast = fastingRepository.observeActive().first() ?: return Result.success()
        val now = System.currentTimeMillis()
        if (now < fast.goalReachedMillis) return Result.success()

        notify(
            context = context,
            id = FASTING_GOAL_NOTIFICATION_ID,
            title = context.getString(R.string.app_fasting_goal_title, fast.goalHours),
            body = context.getString(
                R.string.app_fasting_goal_body,
                formatElapsed(now - fast.startMillis),
            ),
            tab = TopLevelDestination.Home,
        )
        return Result.success()
    }
}
