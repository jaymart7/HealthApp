package ph.mart.healthapp.reminder

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.exercise.RoutineRepository
import ph.mart.healthapp.core.data.exercise.plannedOn
import ph.mart.healthapp.core.data.exercise.withSets
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.supplement.SupplementRepository
import ph.mart.healthapp.core.data.water.WaterRepository

/**
 * Posts one reminder notification. Which one is carried in the input data, so every schedule
 * shares this single worker.
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
    private val supplementRepository: SupplementRepository by inject()
    private val routineRepository: RoutineRepository by inject()
    private val exerciseRepository: ExerciseRepository by inject()

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

        // And for supplements — including the case where there are none at all, which is a
        // reminder about an empty list.
        if (reminder.checksSupplements) {
            val supplements = supplementRepository.observeToday().first()
            if (supplements.isEmpty() || supplements.all { it.isComplete }) return Result.success()
        }

        // And for the training plan: quiet on a rest day, and quiet once something has been
        // lifted today. "Lifted" is a workout with sets — the same discriminator `trainingWeek()`
        // scores the week's dots with, so the notification and the card can't disagree.
        if (reminder.checksPlan) {
            val today = todayEpochDay()
            if (routineRepository.observeRoutines().first().plannedOn(today).isEmpty()) {
                return Result.success()
            }
            if (exerciseRepository.observeTodayEntries().first().withSets().isNotEmpty()) {
                return Result.success()
            }
        }

        notify(
            context,
            reminder.ordinal,
            reminder.title,
            reminder.body,
            reminder.tab,
            waterAction = reminder.checksWater,
        )
        return Result.success()
    }
}
