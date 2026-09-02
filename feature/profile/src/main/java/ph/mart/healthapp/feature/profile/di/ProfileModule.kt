package ph.mart.healthapp.feature.profile.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ph.mart.healthapp.feature.profile.ui.health.HealthConnectionViewModel
import ph.mart.healthapp.feature.profile.ui.layout.HomeLayoutViewModel
import ph.mart.healthapp.feature.profile.ui.library.FoodLibraryViewModel
import ph.mart.healthapp.feature.profile.ui.profile.ProfileViewModel
import ph.mart.healthapp.feature.profile.ui.supplement.SupplementsViewModel

val profileModule = module {
    viewModelOf(::ProfileViewModel)
    viewModelOf(::HealthConnectionViewModel)
    viewModelOf(::FoodLibraryViewModel)
    viewModelOf(::SupplementsViewModel)
    viewModelOf(::HomeLayoutViewModel)
}
