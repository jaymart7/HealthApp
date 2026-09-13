package ph.mart.healthapp.feature.progress.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ph.mart.healthapp.feature.progress.ui.activity.ActivityViewModel
import ph.mart.healthapp.feature.progress.ui.addphoto.AddPhotoViewModel
import ph.mart.healthapp.feature.progress.ui.comparison.ComparisonViewModel
import ph.mart.healthapp.feature.progress.ui.cycle.CycleViewModel
import ph.mart.healthapp.feature.progress.ui.cycle.LogCycleViewModel
import ph.mart.healthapp.feature.progress.ui.energy.EnergyCheckInViewModel
import ph.mart.healthapp.feature.progress.ui.fasting.FastingViewModel
import ph.mart.healthapp.feature.progress.ui.heart.HeartViewModel
import ph.mart.healthapp.feature.progress.ui.measurement.AddMeasurementViewModel
import ph.mart.healthapp.feature.progress.ui.mood.MoodViewModel
import ph.mart.healthapp.feature.progress.ui.photo.PhotosViewModel
import ph.mart.healthapp.feature.progress.ui.pressure.BloodPressureViewModel
import ph.mart.healthapp.feature.progress.ui.progress.ProgressViewModel
import ph.mart.healthapp.feature.progress.ui.recap.RecapViewModel
import ph.mart.healthapp.feature.progress.ui.sleep.SleepViewModel
import ph.mart.healthapp.feature.progress.ui.strength.StrengthViewModel
import ph.mart.healthapp.feature.progress.ui.supplement.SupplementsViewModel
import ph.mart.healthapp.feature.progress.ui.timelapse.TimelapseViewModel
import ph.mart.healthapp.feature.progress.ui.weight.LogWeightViewModel

val progressModule = module {
    viewModelOf(::ProgressViewModel)
    viewModelOf(::LogWeightViewModel)
    viewModelOf(::AddPhotoViewModel)
    viewModelOf(::AddMeasurementViewModel)
    viewModelOf(::BloodPressureViewModel)
    viewModelOf(::LogCycleViewModel)
    viewModelOf(::EnergyCheckInViewModel)
    // The read-only routes — each owns the data it draws rather than a slice of ProgressUiState.
    // The Photos page and the three photo/recap screens came first (DECISIONS.md -> Progress
    // photos & timelapse); the subject pages are following, one per commit.
    viewModelOf(::PhotosViewModel)
    viewModelOf(::SleepViewModel)
    viewModelOf(::MoodViewModel)
    viewModelOf(::HeartViewModel)
    viewModelOf(::SupplementsViewModel)
    viewModelOf(::StrengthViewModel)
    viewModelOf(::FastingViewModel)
    viewModelOf(::ActivityViewModel)
    viewModelOf(::CycleViewModel)
    viewModelOf(::ComparisonViewModel)
    viewModelOf(::TimelapseViewModel)
    viewModelOf(::RecapViewModel)
}
