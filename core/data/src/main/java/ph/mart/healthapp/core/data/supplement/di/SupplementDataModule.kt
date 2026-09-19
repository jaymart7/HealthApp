package ph.mart.healthapp.core.data.supplement.di

import org.koin.dsl.module
import ph.mart.healthapp.core.data.AppDatabase
import ph.mart.healthapp.core.data.debugSupplementScan
import ph.mart.healthapp.core.data.supplement.SupplementRepository
import ph.mart.healthapp.core.data.supplement.SupplementRepositoryImpl
import ph.mart.healthapp.core.data.supplement.SupplementScanRepository
import ph.mart.healthapp.core.data.supplement.SupplementScanRepositoryImpl

/** `debugSupplementScan()` is a source-set pair and is null in release — see `DebugAi.kt`. */
val supplementDataModule = module {
    single { get<AppDatabase>().supplementDao() }
    single<SupplementRepository> { SupplementRepositoryImpl(get()) }
    single<SupplementScanRepository> { debugSupplementScan() ?: SupplementScanRepositoryImpl() }
}
