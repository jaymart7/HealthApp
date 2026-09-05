package ph.mart.healthapp.core.data.transfer.di

import org.koin.dsl.module
import ph.mart.healthapp.core.data.transfer.DataTransferRepository
import ph.mart.healthapp.core.data.transfer.DataTransferRepositoryImpl

/** No DAOs of its own — this domain has no table. It composes the other domains' repositories and
 * the database it shares with them, which is the whole of what a transaction needs. */
val transferDataModule = module {
    single<DataTransferRepository> {
        DataTransferRepositoryImpl(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
}
