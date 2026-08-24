package ph.mart.healthapp.core.data.progress.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ph.mart.healthapp.core.data.AppDatabase
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.progress.ProgressRepositoryImpl

val progressDataModule = module {
    single { get<AppDatabase>().weightEntryDao() }
    single { get<AppDatabase>().measurementEntryDao() }
    single { get<AppDatabase>().progressPhotoDao() }
    single<ProgressRepository> { ProgressRepositoryImpl(androidContext(), get(), get(), get()) }
}
