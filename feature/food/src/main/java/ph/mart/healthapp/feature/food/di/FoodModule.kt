package ph.mart.healthapp.feature.food.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ph.mart.healthapp.feature.food.ui.barcode.BarcodeScanViewModel
import ph.mart.healthapp.feature.food.ui.diary.FoodViewModel
import ph.mart.healthapp.feature.food.ui.history.FoodHistoryViewModel
import ph.mart.healthapp.feature.food.ui.ideas.MealIdeasViewModel
import ph.mart.healthapp.feature.food.ui.label.LabelScanViewModel
import ph.mart.healthapp.feature.food.ui.myfood.NewFoodViewModel
import ph.mart.healthapp.feature.food.ui.photo.PhotoCaptureViewModel
import ph.mart.healthapp.feature.food.ui.quicklog.QuickLogViewModel
import ph.mart.healthapp.feature.food.ui.recipe.RecipeBuilderViewModel
import ph.mart.healthapp.feature.food.ui.search.FoodSearchViewModel
import ph.mart.healthapp.feature.food.ui.voice.VoiceLogViewModel

val foodModule = module {
    viewModelOf(::FoodViewModel)
    viewModelOf(::FoodSearchViewModel)
    viewModelOf(::PhotoCaptureViewModel)
    viewModelOf(::BarcodeScanViewModel)
    viewModelOf(::LabelScanViewModel)
    viewModelOf(::RecipeBuilderViewModel)
    viewModelOf(::NewFoodViewModel)
    viewModelOf(::MealIdeasViewModel)
    viewModelOf(::VoiceLogViewModel)
    viewModelOf(::FoodHistoryViewModel)
    viewModelOf(::QuickLogViewModel)
}
