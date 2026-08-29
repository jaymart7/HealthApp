package ph.mart.healthapp.core.data.mood.di

import org.koin.dsl.module
import ph.mart.healthapp.core.data.AppDatabase
import ph.mart.healthapp.core.data.mood.MoodRepository
import ph.mart.healthapp.core.data.mood.MoodRepositoryImpl

val moodDataModule = module {
    single { get<AppDatabase>().moodDayDao() }
    single<MoodRepository> { MoodRepositoryImpl(get()) }
}
