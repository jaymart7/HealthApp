package ph.mart.healthapp.feature.progress.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ph.mart.healthapp.feature.progress.ui.addphoto.AddPhotoViewModel
import ph.mart.healthapp.feature.progress.ui.comparison.ComparisonViewModel
import ph.mart.healthapp.feature.progress.ui.cycle.CycleViewModel
import ph.mart.healthapp.feature.progress.ui.energy.EnergyCheckInViewModel
import ph.mart.healthapp.feature.progress.ui.measurement.AddMeasurementViewModel
import ph.mart.healthapp.feature.progress.ui.photo.PhotosViewModel
import ph.mart.healthapp.feature.progress.ui.pressure.BloodPressureViewModel
import ph.mart.healthapp.feature.progress.ui.progress.ProgressViewModel
import ph.mart.healthapp.feature.progress.ui.recap.RecapViewModel
import ph.mart.healthapp.feature.progress.ui.timelapse.TimelapseViewModel
import ph.mart.healthapp.feature.progress.ui.weight.LogWeightViewModel

val progressModule = module {
    viewModelOf(::ProgressViewModel)
    viewModelOf(::LogWeightViewModel)
    viewModelOf(::AddPhotoViewModel)
    viewModelOf(::AddMeasurementViewModel)
    viewModelOf(::BloodPressureViewModel)
    viewModelOf(::CycleViewModel)
    viewModelOf(::EnergyCheckInViewModel)
    // The four read-only routes — each owns the data it draws rather than a slice of
    // ProgressUiState; see DECISIONS.md -> Progress photos & timelapse.
    viewModelOf(::PhotosViewModel)
    viewModelOf(::ComparisonViewModel)
    viewModelOf(::TimelapseViewModel)
    viewModelOf(::RecapViewModel)
}
