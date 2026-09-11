package ph.mart.healthapp.core.data.food.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ph.mart.healthapp.core.data.AppDatabase
import ph.mart.healthapp.core.data.food.BarcodeLookupRepository
import ph.mart.healthapp.core.data.food.BarcodeLookupRepositoryImpl
import ph.mart.healthapp.core.data.food.FoodRecognitionRepository
import ph.mart.healthapp.core.data.food.FoodRecognitionRepositoryImpl
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.food.FoodRepositoryImpl
import ph.mart.healthapp.core.data.food.MealIdeaRepository
import ph.mart.healthapp.core.data.food.MealIdeaRepositoryImpl
import ph.mart.healthapp.core.data.food.MealParseRepository
import ph.mart.healthapp.core.data.food.MealParseRepositoryImpl
import ph.mart.healthapp.core.data.food.ProductSearchRepository
import ph.mart.healthapp.core.data.food.ProductSearchRepositoryImpl

val foodDataModule = module {
    single { get<AppDatabase>().foodEntryDao() }
    single { get<AppDatabase>().favoriteFoodDao() }
    single { get<AppDatabase>().savedMealDao() }
    single { get<AppDatabase>().scannedProductDao() }
    single<FoodRepository> { FoodRepositoryImpl(androidContext(), get(), get(), get()) }
    single<FoodRecognitionRepository> { FoodRecognitionRepositoryImpl() }
    single<BarcodeLookupRepository> { BarcodeLookupRepositoryImpl(get()) }
    single<MealIdeaRepository> { MealIdeaRepositoryImpl() }
    single<MealParseRepository> { MealParseRepositoryImpl() }
    single<ProductSearchRepository> { ProductSearchRepositoryImpl() }
}
