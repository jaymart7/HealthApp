package ph.mart.healthapp.core.data.note.di

import org.koin.dsl.module
import ph.mart.healthapp.core.data.AppDatabase
import ph.mart.healthapp.core.data.note.NoteRepository
import ph.mart.healthapp.core.data.note.NoteRepositoryImpl

val noteDataModule = module {
    single { get<AppDatabase>().noteDayDao() }
    single<NoteRepository> { NoteRepositoryImpl(get()) }
}
