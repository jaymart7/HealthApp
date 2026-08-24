package ph.mart.healthapp

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.context.startKoin
import org.koin.dsl.module
import ph.mart.healthapp.core.data.di.databaseModule
import ph.mart.healthapp.core.data.di.networkModule
import ph.mart.healthapp.core.data.food.di.foodDataModule
import ph.mart.healthapp.core.data.profile.di.profileDataModule
import ph.mart.healthapp.core.data.progress.di.progressDataModule
import ph.mart.healthapp.feature.food.di.foodModule
import ph.mart.healthapp.feature.onboarding.di.onboardingModule
import ph.mart.healthapp.feature.progress.di.progressModule
import ph.mart.healthapp.ui.AppRootViewModel

class HealthTrackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@HealthTrackApplication)
            modules(
                databaseModule,
                networkModule,
                profileDataModule,
                foodDataModule,
                progressDataModule,
                onboardingModule,
                foodModule,
                progressModule,
                module { viewModelOf(::AppRootViewModel) },
            )
        }
    }
}
