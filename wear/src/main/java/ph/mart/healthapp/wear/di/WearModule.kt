package ph.mart.healthapp.wear.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ph.mart.healthapp.wear.data.WearSnapshotRepository
import ph.mart.healthapp.wear.ui.WearTodayViewModel

val wearModule = module {
    single { WearSnapshotRepository(androidContext()) }
    viewModelOf(::WearTodayViewModel)
}
