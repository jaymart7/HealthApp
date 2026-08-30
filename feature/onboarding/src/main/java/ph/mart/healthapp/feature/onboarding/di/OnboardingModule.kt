package ph.mart.healthapp.feature.onboarding.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ph.mart.healthapp.feature.onboarding.ui.health.OnboardingHealthViewModel
import ph.mart.healthapp.feature.onboarding.ui.onboarding.OnboardingViewModel

val onboardingModule = module {
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::OnboardingHealthViewModel)
}
