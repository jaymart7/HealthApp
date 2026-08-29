package ph.mart.healthapp.core.data.health.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ph.mart.healthapp.core.data.AppDatabase
import ph.mart.healthapp.core.data.health.GoogleHealthAuth
import ph.mart.healthapp.core.data.health.GoogleHealthAuthImpl
import ph.mart.healthapp.core.data.health.HealthSyncRepository
import ph.mart.healthapp.core.data.health.HealthSyncRepositoryImpl
import ph.mart.healthapp.core.data.health.SleepRepository
import ph.mart.healthapp.core.data.health.SleepRepositoryImpl

val healthDataModule = module {
    single { get<AppDatabase>().healthLinkDao() }
    single { get<AppDatabase>().sleepDayDao() }
    single<GoogleHealthAuth> { GoogleHealthAuthImpl(androidContext()) }
    single<SleepRepository> { SleepRepositoryImpl(get()) }
    single<HealthSyncRepository> { HealthSyncRepositoryImpl(get(), get(), get(), get(), get(), get(), get(), get()) }
}
