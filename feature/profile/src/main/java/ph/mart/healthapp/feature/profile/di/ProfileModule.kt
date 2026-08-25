package ph.mart.healthapp.feature.profile.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ph.mart.healthapp.feature.profile.ui.ProfileViewModel

val profileModule = module {
    viewModelOf(::ProfileViewModel)
}
