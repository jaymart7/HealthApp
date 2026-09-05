package ph.mart.healthapp.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureRepository
import ph.mart.healthapp.core.data.cycle.CycleRepository
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.fasting.FastingRepository
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.mood.MoodRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.supplement.SupplementRepository
import ph.mart.healthapp.core.data.transfer.LocalBackups
import ph.mart.healthapp.core.data.transfer.exportJson
import ph.mart.healthapp.core.data.water.WaterRepository

private const val BACKUP_WORK = "weekly-backup"

/**
 * Writes the export file the Data screen writes, once a week, with nobody watching.
 *
 * An offline-first app with no account has exactly one answer to a lost phone, and until this
 * existed that answer was "you remembered to tap Export". It lives in `:app` for the reason the
 * reminders and the widget do — a background job is a system surface, not a screen — and it posts
 * nothing: it is deliberately not a [ph.mart.healthapp.reminder.Reminder], every entry of which is
 * a nudge whose `ordinal` is a notification id.
 *
 * Koin's global context is already started (Application.onCreate runs before any worker), so
 * [KoinComponent] is enough — no custom `WorkerFactory`, the call `ReminderWorker` makes.
 */
class BackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val profileRepository: ProfileRepository by inject()
    private val foodRepository: FoodRepository by inject()
    private val progressRepository: ProgressRepository by inject()
    private val waterRepository: WaterRepository by inject()
    private val exerciseRepository: ExerciseRepository by inject()
    private val moodRepository: MoodRepository by inject()
    private val cycleRepository: CycleRepository by inject()
    private val fastingRepository: FastingRepository by inject()
    private val supplementRepository: SupplementRepository by inject()
    private val bloodPressureRepository: BloodPressureRepository by inject()
    private val localBackups: LocalBackups by inject()

    override suspend fun doWork(): Result {
        // No profile means onboarding hasn't finished, so there is nothing yet worth a file.
        profileRepository.observeProfile().first() ?: return Result.success()
        localBackups.write(
            exportJson(
                profileRepository, foodRepository, progressRepository, waterRepository,
                exerciseRepository, moodRepository, cycleRepository, fastingRepository,
                supplementRepository, bloodPressureRepository,
            ),
        )
        return Result.success()
    }
}

/**
 * KEEP, not UPDATE, for [ph.mart.healthapp.reminder.ReminderScheduler.reconcile]'s reason: this
 * runs on every app start, and UPDATE would reset the initial delay each time — the backup would
 * never reach its first run on someone who opens the app most days.
 */
fun enqueueWeeklyBackup(context: Context) {
    WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
        BACKUP_WORK,
        ExistingPeriodicWorkPolicy.KEEP,
        PeriodicWorkRequestBuilder<BackupWorker>(7, TimeUnit.DAYS).build(),
    )
}
