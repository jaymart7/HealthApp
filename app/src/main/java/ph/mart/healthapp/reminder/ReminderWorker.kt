package ph.mart.healthapp.reminder

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
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
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.streak.loggedDays
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
    private val progressRepository: ProgressRepository by inject()

    /**
     * Posts if there is anything to say, and then — on every path, including every quiet one —
     * books the next firing. The chain *is* the schedule (see [ReminderScheduler.schedule]), so a
     * run that stayed silent because breakfast was already logged must still put tomorrow's in the
     * queue, or that reminder stops for good.
     *
     * The re-enqueue is the last statement rather than the first because REPLACE on a unique name
     * cancels whatever is running under it — which is this worker. By here the notification is
     * already posted and there is nothing left to lose; `enqueueUniqueWork` hands off to
     * WorkManager's own executor and returns, so the next run is booked whether or not this
     * coroutine survives the line.
     */
    override suspend fun doWork(): Result {
        val reminder = inputData.getString(KEY_REMINDER)
            ?.let { name -> Reminder.entries.firstOrNull { it.name == name } }
            ?: return Result.success()

        if (shouldNotify(reminder)) {
            notify(
                context,
                reminder.ordinal,
                reminder.title,
                reminder.body,
                reminder.tab,
                waterAction = reminder.checksWater,
                action = reminder.action,
            )
        }

        ReminderScheduler(context).schedule(reminder, ExistingWorkPolicy.REPLACE)
        return Result.success()
    }

    /** Every reason to stay quiet, in one predicate — extracted from [doWork] so the reschedule
     * below it cannot be skipped by an early return, which is exactly how a chain dies. */
    private suspend fun shouldNotify(reminder: Reminder): Boolean {
        // Revoked after the work was enqueued — stay quiet rather than posting into the void.
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false

        // Don't nudge someone about a meal they've already logged today.
        val mealType = reminder.mealType
        if (mealType != null && foodRepository.observeTodayEntries().first().any { it.mealType == mealType }) {
            return false
        }

        // Same rule for water: don't nudge someone who already hit today's goal.
        if (reminder.checksWater) {
            val goal = profileRepository.observeProfile().first()?.waterGoalGlasses ?: return false
            if (waterRepository.observeToday().first() >= goal) return false
        }

        // And for supplements — including the case where there are none at all, which is a
        // reminder about an empty list.
        if (reminder.checksSupplements) {
            val supplements = supplementRepository.observeToday().first()
            if (supplements.isEmpty() || supplements.all { it.isComplete }) return false
        }

        // And for the training plan: quiet on a rest day, and quiet once something has been
        // lifted today. "Lifted" is a workout with sets — the same discriminator `trainingWeek()`
        // scores the week's dots with, so the notification and the card can't disagree.
        if (reminder.checksPlan) {
            val today = todayEpochDay()
            if (routineRepository.observeRoutines().first().plannedOn(today).isEmpty()) return false
            if (exerciseRepository.observeTodayEntries().first().withSets().isNotEmpty()) return false
        }

        // And for the weekly recap: quiet on a week with nothing logged in it. The fold is the
        // streak's own four domains, the same combine `ProgressViewModel` and
        // `observeInsightRequest` already make — so the notification and the card it opens can
        // never disagree about whether there is a week to report.
        if (reminder.checksRecap) {
            val days = loggedDays(
                foodRepository.observeDailyNutrition().first(),
                waterRepository.observeLoggedDays().first(),
                progressRepository.observeWeightEntries().first(),
                exerciseRepository.observeLoggedDays().first(),
            )
            if (!hasRecapToShow(days, todayEpochDay())) return false
        }

        return true
    }
}
