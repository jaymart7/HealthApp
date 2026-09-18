package ph.mart.healthapp.core.data.exercise.di

import org.koin.dsl.module
import ph.mart.healthapp.core.data.AppDatabase
import ph.mart.healthapp.core.data.debugExerciseParse
import ph.mart.healthapp.core.data.exercise.ExerciseParseRepository
import ph.mart.healthapp.core.data.exercise.ExerciseParseRepositoryImpl
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.exercise.ExerciseRepositoryImpl
import ph.mart.healthapp.core.data.exercise.RoutineRepository
import ph.mart.healthapp.core.data.exercise.RoutineRepositoryImpl

val exerciseDataModule = module {
    single { get<AppDatabase>().exerciseEntryDao() }
    single { get<AppDatabase>().routineDao() }
    single<ExerciseRepository> { ExerciseRepositoryImpl(get()) }
    single<RoutineRepository> { RoutineRepositoryImpl(get()) }
    // No DAO and no database: the only AI binding in this domain, and the only one here that
    // reads nothing local. `FoodDataModule`'s three sit the same way.
    single<ExerciseParseRepository> { debugExerciseParse() ?: ExerciseParseRepositoryImpl() }
}
