package ph.mart.healthapp.feature.home.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ph.mart.healthapp.feature.home.ui.HomeViewModel

val homeModule = module {
    viewModelOf(::HomeViewModel)
}
