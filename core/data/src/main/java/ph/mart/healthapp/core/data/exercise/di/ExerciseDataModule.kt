package ph.mart.healthapp.core.data.exercise.di

import org.koin.dsl.module
import ph.mart.healthapp.core.data.AppDatabase
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.exercise.ExerciseRepositoryImpl
import ph.mart.healthapp.core.data.exercise.RoutineRepository
import ph.mart.healthapp.core.data.exercise.RoutineRepositoryImpl

val exerciseDataModule = module {
    single { get<AppDatabase>().exerciseEntryDao() }
    single { get<AppDatabase>().routineDao() }
    single<ExerciseRepository> { ExerciseRepositoryImpl(get()) }
    single<RoutineRepository> { RoutineRepositoryImpl(get()) }
}
