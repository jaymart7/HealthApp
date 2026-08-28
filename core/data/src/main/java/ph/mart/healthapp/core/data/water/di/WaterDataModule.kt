package ph.mart.healthapp.core.data.water.di

import org.koin.dsl.module
import ph.mart.healthapp.core.data.AppDatabase
import ph.mart.healthapp.core.data.water.WaterRepository
import ph.mart.healthapp.core.data.water.WaterRepositoryImpl

val waterDataModule = module {
    single { get<AppDatabase>().waterDayDao() }
    single<WaterRepository> { WaterRepositoryImpl(get()) }
}
