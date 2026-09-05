package ph.mart.healthapp.core.data.cycle.di

import org.koin.dsl.module
import ph.mart.healthapp.core.data.AppDatabase
import ph.mart.healthapp.core.data.cycle.CycleRepository
import ph.mart.healthapp.core.data.cycle.CycleRepositoryImpl

val cycleDataModule = module {
    single { get<AppDatabase>().cycleDayDao() }
    single<CycleRepository> { CycleRepositoryImpl(get()) }
}
