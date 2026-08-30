package ph.mart.healthapp.reminder

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.water.WaterRepository

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
    private val waterRepository: WaterRepository by inject()
    private val profileRepository: ProfileRepository by inject()

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

        // Same rule for water: don't nudge someone who already hit today's goal.
        if (reminder.checksWater) {
            val goal = profileRepository.observeProfile().first()?.waterGoalGlasses ?: return Result.success()
            if (waterRepository.observeToday().first() >= goal) return Result.success()
        }

        notify(context, reminder.ordinal, reminder.title, reminder.body, reminder.tab)
        return Result.success()
    }
}
