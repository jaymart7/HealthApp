package ph.mart.healthapp

import android.app.Application
import androidx.glance.appwidget.updateAll
import com.google.firebase.FirebaseApp
import ph.mart.healthapp.appcheck.initAppCheck
import ph.mart.healthapp.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.context.startKoin
import org.koin.dsl.module
import ph.mart.healthapp.core.data.di.databaseModule
import ph.mart.healthapp.core.data.di.networkModule
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.exercise.di.exerciseDataModule
import ph.mart.healthapp.core.data.fasting.FastingRepository
import ph.mart.healthapp.core.data.fasting.di.fastingDataModule
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.di.foodDataModule
import ph.mart.healthapp.core.data.health.StepsRepository
import ph.mart.healthapp.core.data.health.di.healthDataModule
import ph.mart.healthapp.core.data.insight.di.insightDataModule
import ph.mart.healthapp.core.data.bloodpressure.di.bloodPressureDataModule
import ph.mart.healthapp.core.data.coach.di.coachDataModule
import ph.mart.healthapp.core.data.cycle.di.cycleDataModule
import ph.mart.healthapp.core.data.mood.di.moodDataModule
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.di.profileDataModule
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.progress.di.progressDataModule
import ph.mart.healthapp.core.data.supplement.di.supplementDataModule
import ph.mart.healthapp.core.data.transfer.di.transferDataModule
import ph.mart.healthapp.core.data.water.WaterRepository
import ph.mart.healthapp.core.data.water.di.waterDataModule
import ph.mart.healthapp.debug.seedDebugData
import ph.mart.healthapp.feature.coach.di.coachModule
import ph.mart.healthapp.feature.food.di.foodModule
import ph.mart.healthapp.feature.home.di.homeModule
import ph.mart.healthapp.feature.onboarding.di.onboardingModule
import ph.mart.healthapp.feature.profile.di.profileModule
import ph.mart.healthapp.feature.progress.di.progressModule
import ph.mart.healthapp.reminder.Reminder
import ph.mart.healthapp.reminder.ReminderScheduler
import ph.mart.healthapp.reminder.enabledIn
import ph.mart.healthapp.reminder.fastingGoalDelayMillis
import ph.mart.healthapp.reminder.fastingGoalTargetMillis
import ph.mart.healthapp.today.todaySnapshotFlow
import ph.mart.healthapp.ui.AppRootViewModel
import ph.mart.healthapp.wear.pushTodayToWear
import ph.mart.healthapp.widget.TodayWidget

class FitPulseApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        initAppCheck()

        val koinApp = startKoin {
            androidContext(this@FitPulseApplication)
            modules(
                databaseModule,
                networkModule,
                profileDataModule,
                foodDataModule,
                insightDataModule,
                coachDataModule,
                progressDataModule,
                waterDataModule,
                exerciseDataModule,
                moodDataModule,
                cycleDataModule,
                fastingDataModule,
                supplementDataModule,
                bloodPressureDataModule,
                healthDataModule,
                transferDataModule,
                onboardingModule,
                foodModule,
                homeModule,
                coachModule,
                progressModule,
                profileModule,
                module { viewModelOf(::AppRootViewModel) },
            )
        }
        // No-op in release — see the release source set's counterpart.
        seedDebugData(koinApp.koin)

        scheduleReminders(koinApp.koin.get())
        scheduleFastingGoal(koinApp.koin.get(), koinApp.koin.get())
        updateGlanceables(
            koinApp.koin.get(),
            koinApp.koin.get(),
            koinApp.koin.get(),
            koinApp.koin.get(),
            koinApp.koin.get(),
            koinApp.koin.get(),
            koinApp.koin.get(),
        )
    }

    /**
     * Reminder scheduling is derived from the profile row, not commanded: the Profile switches
     * only write to Room, and this collector reconciles WorkManager behind them. Same reactive
     * shape as [ph.mart.healthapp.ui.AppRootViewModel]'s onboarding gate.
     *
     * The `map` to the enabled set is what keeps this quiet — the profile re-emits on every weight
     * edit and unit change, and none of those should touch a schedule.
     */
    private fun scheduleReminders(profileRepository: ProfileRepository) {
        ReminderScheduler.createChannel(this)
        val scheduler = ReminderScheduler(this)
        applicationScope.launch {
            profileRepository.observeProfile()
                .map { profile -> Reminder.entries.filter { profile != null && it.enabledIn(profile) }.toSet() }
                .distinctUntilChanged()
                .collect(scheduler::reconcile)
        }
    }

    /**
     * The fasting goal notification, in the same derived-not-commanded shape as
     * [scheduleReminders] — but derived off the *running fast* rather than off a repeating clock,
     * because a fast's target lands at an hour the user chose by stopping eating.
     *
     * The map to an absolute target instant is what keeps this quiet: the profile row re-emits on
     * every weight edit, and a delay recomputed against a moving `now` would differ every time and
     * churn the queue.
     */
    private fun scheduleFastingGoal(
        fastingRepository: FastingRepository,
        profileRepository: ProfileRepository,
    ) {
        val scheduler = ReminderScheduler(this)
        applicationScope.launch {
            combine(
                fastingRepository.observeActive(),
                profileRepository.observeProfile(),
            ) { fast, profile -> fastingGoalTargetMillis(fast, profile?.fastingRemindersOn == true) }
                .distinctUntilChanged()
                .collect { target ->
                    scheduler.scheduleFastingGoal(fastingGoalDelayMillis(target, System.currentTimeMillis()))
                }
        }
    }

    /**
     * Keeps the glanceable surfaces honest, in the same derived-not-commanded shape as
     * [scheduleReminders]: nothing in the app tells the widget to redraw or the watch to update,
     * this reconciles both off the same Room flows the widget itself reads.
     *
     * There is one flow, not one per surface — [todaySnapshotFlow] is what the widget's Glance
     * session collects too, so the home screen and the wrist cannot disagree about today. Two
     * near-identical `combine` chains lived here and in `TodayWidget` before the watch arrived,
     * which is exactly how that disagreement starts.
     *
     * `distinctUntilChanged` now compares the snapshot itself — a data class — rather than the
     * bag of raw flow values it used to, so a Room emission that changes nothing the surfaces
     * draw no longer wakes either of them.
     */
    private fun updateGlanceables(
        foodRepository: FoodRepository,
        waterRepository: WaterRepository,
        profileRepository: ProfileRepository,
        exerciseRepository: ExerciseRepository,
        stepsRepository: StepsRepository,
        fastingRepository: FastingRepository,
        progressRepository: ProgressRepository,
    ) {
        applicationScope.launch {
            todaySnapshotFlow(
                profileRepository = profileRepository,
                foodRepository = foodRepository,
                waterRepository = waterRepository,
                exerciseRepository = exerciseRepository,
                progressRepository = progressRepository,
                stepsRepository = stepsRepository,
                fastingRepository = fastingRepository,
            )
                .distinctUntilChanged()
                .collect { snapshot ->
                    TodayWidget().updateAll(this@FitPulseApplication)
                    pushTodayToWear(this@FitPulseApplication, snapshot)
                }
        }
    }
}
