package ph.mart.healthapp.core.data.supplement.di

import org.koin.dsl.module
import ph.mart.healthapp.core.data.AppDatabase
import ph.mart.healthapp.core.data.supplement.SupplementRepository
import ph.mart.healthapp.core.data.supplement.SupplementRepositoryImpl

val supplementDataModule = module {
    single { get<AppDatabase>().supplementDao() }
    single<SupplementRepository> { SupplementRepositoryImpl(get()) }
}
