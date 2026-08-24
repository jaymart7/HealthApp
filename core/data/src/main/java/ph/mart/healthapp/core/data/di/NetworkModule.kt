package ph.mart.healthapp.core.data.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ph.mart.healthapp.core.data.network.NetworkMonitor
import ph.mart.healthapp.core.data.network.NetworkMonitorImpl

val networkModule = module {
    single<NetworkMonitor> { NetworkMonitorImpl(androidContext()) }
}
