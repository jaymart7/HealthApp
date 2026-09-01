package ph.mart.healthapp.feature.coach.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ph.mart.healthapp.feature.coach.ui.CoachViewModel

val coachModule = module {
    viewModelOf(::CoachViewModel)
}
