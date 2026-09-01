package ph.mart.healthapp.core.data.coach.di

import org.koin.dsl.module
import ph.mart.healthapp.core.data.AppDatabase
import ph.mart.healthapp.core.data.coach.CoachRepository
import ph.mart.healthapp.core.data.coach.CoachRepositoryImpl

val coachDataModule = module {
    single { get<AppDatabase>().chatMessageDao() }
    single<CoachRepository> { CoachRepositoryImpl(get()) }
}
