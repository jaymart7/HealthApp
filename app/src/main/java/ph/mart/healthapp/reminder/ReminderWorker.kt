package ph.mart.healthapp.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ph.mart.healthapp.MainActivity
import ph.mart.healthapp.R
import ph.mart.healthapp.core.data.food.FoodRepository

/**
 * Posts one reminder notification. Which one is carried in the input data, so all five schedules
 * share this single worker.
 *
 * Koin's global context is already started (Application.onCreate runs before any worker), so
 * [KoinComponent] is enough — no custom `WorkerFactory`.
 */
class ReminderWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val foodRepository: FoodRepository by inject()

    override suspend fun doWork(): Result {
        val reminder = inputData.getString(KEY_REMINDER)
            ?.let { name -> Reminder.entries.firstOrNull { it.name == name } }
            ?: return Result.success()

        // Revoked after the work was enqueued — stay quiet rather than posting into the void.
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return Result.success()

        // Don't nudge someone about a meal they've already logged today.
        val mealType = reminder.mealType
        if (mealType != null && foodRepository.observeTodayEntries().first().any { it.mealType == mealType }) {
            return Result.success()
        }

        notify(reminder)
        return Result.success()
    }

    private fun notify(reminder: Reminder) {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_TAB, reminder.tab.name)
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminder.ordinal,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.title)
            .setContentText(reminder.body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        // areNotificationsEnabled() above already covers the permission; this catch is only for the
        // race where it's revoked between that check and here.
        runCatching { NotificationManagerCompat.from(context).notify(reminder.ordinal, notification) }
    }
}
