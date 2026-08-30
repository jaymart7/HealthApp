package ph.mart.healthapp.core.data.fasting.di

import org.koin.dsl.module
import ph.mart.healthapp.core.data.AppDatabase
import ph.mart.healthapp.core.data.fasting.FastingRepository
import ph.mart.healthapp.core.data.fasting.FastingRepositoryImpl

val fastingDataModule = module {
    single { get<AppDatabase>().fastSessionDao() }
    single<FastingRepository> { FastingRepositoryImpl(get()) }
}
