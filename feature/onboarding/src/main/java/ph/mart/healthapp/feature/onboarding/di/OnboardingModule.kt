package ph.mart.healthapp.feature.onboarding.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ph.mart.healthapp.feature.onboarding.ui.OnboardingHealthViewModel
import ph.mart.healthapp.feature.onboarding.ui.OnboardingViewModel

val onboardingModule = module {
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::OnboardingHealthViewModel)
}
