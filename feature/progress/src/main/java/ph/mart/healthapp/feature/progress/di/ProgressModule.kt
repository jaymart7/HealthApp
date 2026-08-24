package ph.mart.healthapp.feature.progress.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ph.mart.healthapp.feature.progress.ui.AddMeasurementViewModel
import ph.mart.healthapp.feature.progress.ui.AddPhotoViewModel
import ph.mart.healthapp.feature.progress.ui.LogWeightViewModel
import ph.mart.healthapp.feature.progress.ui.ProgressViewModel

val progressModule = module {
    viewModelOf(::ProgressViewModel)
    viewModelOf(::LogWeightViewModel)
    viewModelOf(::AddPhotoViewModel)
    viewModelOf(::AddMeasurementViewModel)
}
