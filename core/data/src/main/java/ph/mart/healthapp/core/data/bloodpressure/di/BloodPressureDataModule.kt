package ph.mart.healthapp.core.data.bloodpressure.di

import org.koin.dsl.module
import ph.mart.healthapp.core.data.AppDatabase
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureRepository
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureRepositoryImpl

val bloodPressureDataModule = module {
    single { get<AppDatabase>().bloodPressureReadingDao() }
    single<BloodPressureRepository> { BloodPressureRepositoryImpl(get()) }
}
