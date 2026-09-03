package ph.mart.healthapp.core.data.food.di

import org.koin.dsl.module
import ph.mart.healthapp.core.data.AppDatabase
import ph.mart.healthapp.core.data.food.BarcodeLookupRepository
import ph.mart.healthapp.core.data.food.BarcodeLookupRepositoryImpl
import ph.mart.healthapp.core.data.food.FoodRecognitionRepository
import ph.mart.healthapp.core.data.food.FoodRecognitionRepositoryImpl
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.FoodRepositoryImpl
import ph.mart.healthapp.core.data.food.FoodSearchRepository
import ph.mart.healthapp.core.data.food.FoodSearchRepositoryImpl
import ph.mart.healthapp.core.data.food.MealIdeaRepository
import ph.mart.healthapp.core.data.food.MealIdeaRepositoryImpl
import ph.mart.healthapp.core.data.food.MealParseRepository
import ph.mart.healthapp.core.data.food.MealParseRepositoryImpl

val foodDataModule = module {
    single { get<AppDatabase>().foodEntryDao() }
    single { get<AppDatabase>().favoriteFoodDao() }
    single { get<AppDatabase>().savedMealDao() }
    single<FoodRepository> { FoodRepositoryImpl(get(), get(), get()) }
    single<FoodRecognitionRepository> { FoodRecognitionRepositoryImpl() }
    single<BarcodeLookupRepository> { BarcodeLookupRepositoryImpl() }
    single<FoodSearchRepository> { FoodSearchRepositoryImpl() }
    single<MealIdeaRepository> { MealIdeaRepositoryImpl() }
    single<MealParseRepository> { MealParseRepositoryImpl() }
}
