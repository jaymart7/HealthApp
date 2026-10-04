package ph.mart.healthapp.reminder

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
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
     * Posts if there is anything to say, and then — on every path, including every quiet one and
     * every one that threw — books the next firing. The chain *is* the schedule (see
     * [ReminderScheduler.schedule]), so a run that stayed silent because breakfast was already
     * logged must still put tomorrow's in the queue, or that reminder stops for good. The two
     * exceptions are the two ways the user stops it: the switch is off, or `reconcile` cancelled
     * this run.
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

        // Switched off since this run was booked: the chain ends here, unbooked. `reconcile` only
        // cancels when the enabled set *moves*, so a chain that slipped past its one cancel would
        // otherwise keep firing a reminder the user turned off until the next cold start.
        if (isSwitchedOff(reminder)) return Result.success()

        // Caught, because the re-enqueue below is only unskippable by an *early return* —
        // shouldNotify does seven repository reads, and a throw from any of them leaves doWork
        // via Result.failure(), which drops the unique work. The chain IS the schedule, so that
        // is the same death the predicate was extracted to prevent, one path over. A day this
        // worker cannot read is a day it stays quiet about, and tomorrow still gets booked.
        //
        // A cancel is rethrown, never caught with the rest: it is `reconcile` switching this
        // reminder off, and swallowing it walked straight into the REPLACE below and re-booked the
        // chain it had just cancelled. A system stop needs no re-book either — WorkManager
        // reschedules stopped work itself.
        val notify = try {
            shouldNotify(reminder)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
        if (notify) {
            notify(
                context,
                reminder.ordinal,
                context.getString(reminder.title),
                context.getString(reminder.body),
                reminder.tab,
                waterAction = reminder.checksWater,
                action = reminder.action,
            )
        }

        ReminderScheduler(context).schedule(reminder, ExistingWorkPolicy.REPLACE)
        return Result.success()
    }

    /** Off by the profile — null counts as off, the reading `reconcile` makes. A read that throws
     * is *not* off: an unreadable profile must not end a chain the user never touched. */
    private suspend fun isSwitchedOff(reminder: Reminder): Boolean = try {
        profileRepository.observeProfile().first()?.let { !reminder.enabledIn(it) } ?: true
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        false
    }

    /** Every reason to stay quiet, in one predicate — extracted from [doWork] so the reschedule
     * below it cannot be skipped by an early return, which is exactly how a chain dies. It may
     * still *throw*, which is the same death by another door; [doWork] catches it there. */
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
