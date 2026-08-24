package ph.mart.healthapp.core.data.profile.di

import org.koin.dsl.module
import ph.mart.healthapp.core.data.AppDatabase
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.ProfileRepositoryImpl

val profileDataModule = module {
    single { get<AppDatabase>().profileDao() }
    single<ProfileRepository> { ProfileRepositoryImpl(get()) }
}
