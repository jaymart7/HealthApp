package ph.mart.healthapp.feature.progress.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ph.mart.healthapp.feature.progress.ui.measurement.AddMeasurementViewModel
import ph.mart.healthapp.feature.progress.ui.photo.AddPhotoViewModel
import ph.mart.healthapp.feature.progress.ui.pressure.BloodPressureViewModel
import ph.mart.healthapp.feature.progress.ui.progress.ProgressViewModel
import ph.mart.healthapp.feature.progress.ui.weight.LogWeightViewModel

val progressModule = module {
    viewModelOf(::ProgressViewModel)
    viewModelOf(::LogWeightViewModel)
    viewModelOf(::AddPhotoViewModel)
    viewModelOf(::AddMeasurementViewModel)
    viewModelOf(::BloodPressureViewModel)
}
