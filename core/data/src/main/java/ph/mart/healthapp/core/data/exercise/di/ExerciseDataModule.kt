package ph.mart.healthapp.core.data.exercise.di

import org.koin.dsl.module
import ph.mart.healthapp.core.data.AppDatabase
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.exercise.ExerciseRepositoryImpl

val exerciseDataModule = module {
    single { get<AppDatabase>().exerciseEntryDao() }
    single<ExerciseRepository> { ExerciseRepositoryImpl(get()) }
}
