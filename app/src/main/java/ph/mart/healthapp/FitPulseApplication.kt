package ph.mart.healthapp

import android.app.Application
import com.google.firebase.FirebaseApp
import ph.mart.healthapp.appcheck.initAppCheck
import ph.mart.healthapp.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.context.startKoin
import org.koin.dsl.module
import ph.mart.healthapp.core.data.di.databaseModule
import ph.mart.healthapp.core.data.di.networkModule
import ph.mart.healthapp.core.data.food.di.foodDataModule
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.di.profileDataModule
import ph.mart.healthapp.core.data.progress.di.progressDataModule
import ph.mart.healthapp.core.data.water.di.waterDataModule
import ph.mart.healthapp.debug.seedDebugData
import ph.mart.healthapp.feature.food.di.foodModule
import ph.mart.healthapp.feature.home.di.homeModule
import ph.mart.healthapp.feature.onboarding.di.onboardingModule
import ph.mart.healthapp.feature.profile.di.profileModule
import ph.mart.healthapp.feature.progress.di.progressModule
import ph.mart.healthapp.reminder.Reminder
import ph.mart.healthapp.reminder.ReminderScheduler
import ph.mart.healthapp.reminder.enabledIn
import ph.mart.healthapp.ui.AppRootViewModel

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
                progressDataModule,
                waterDataModule,
                onboardingModule,
                foodModule,
                homeModule,
                progressModule,
                profileModule,
                module { viewModelOf(::AppRootViewModel) },
            )
        }
        // No-op in release — see the release source set's counterpart.
        seedDebugData(koinApp.koin)

        scheduleReminders(koinApp.koin.get())
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
}
