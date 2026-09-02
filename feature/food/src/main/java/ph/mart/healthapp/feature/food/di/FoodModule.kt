package ph.mart.healthapp.feature.food.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ph.mart.healthapp.feature.food.ui.barcode.BarcodeScanViewModel
import ph.mart.healthapp.feature.food.ui.diary.FoodViewModel
import ph.mart.healthapp.feature.food.ui.exercise.LogExerciseViewModel
import ph.mart.healthapp.feature.food.ui.ideas.MealIdeasViewModel
import ph.mart.healthapp.feature.food.ui.photo.PhotoCaptureViewModel
import ph.mart.healthapp.feature.food.ui.recipe.RecipeBuilderViewModel
import ph.mart.healthapp.feature.food.ui.search.FoodSearchViewModel

val foodModule = module {
    viewModelOf(::FoodViewModel)
    viewModelOf(::FoodSearchViewModel)
    viewModelOf(::PhotoCaptureViewModel)
    viewModelOf(::BarcodeScanViewModel)
    viewModelOf(::LogExerciseViewModel)
    viewModelOf(::RecipeBuilderViewModel)
    viewModelOf(::MealIdeasViewModel)
}
