package ph.mart.healthapp.feature.food.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ph.mart.healthapp.feature.food.ui.BarcodeScanViewModel
import ph.mart.healthapp.feature.food.ui.FoodSearchViewModel
import ph.mart.healthapp.feature.food.ui.FoodViewModel
import ph.mart.healthapp.feature.food.ui.LogExerciseViewModel
import ph.mart.healthapp.feature.food.ui.PhotoCaptureViewModel

val foodModule = module {
    viewModelOf(::FoodViewModel)
    viewModelOf(::FoodSearchViewModel)
    viewModelOf(::PhotoCaptureViewModel)
    viewModelOf(::BarcodeScanViewModel)
    viewModelOf(::LogExerciseViewModel)
}
