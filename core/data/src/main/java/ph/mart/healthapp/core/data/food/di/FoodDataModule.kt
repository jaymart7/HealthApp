package ph.mart.healthapp.core.data.food.di

import org.koin.dsl.module
import ph.mart.healthapp.core.data.AppDatabase
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.FoodRepositoryImpl

val foodDataModule = module {
    single { get<AppDatabase>().foodEntryDao() }
    single<FoodRepository> { FoodRepositoryImpl(get()) }
}
