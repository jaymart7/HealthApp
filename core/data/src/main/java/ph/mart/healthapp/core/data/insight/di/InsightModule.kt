package ph.mart.healthapp.core.data.insight.di

import org.koin.dsl.module
import ph.mart.healthapp.core.data.insight.InsightRepository
import ph.mart.healthapp.core.data.insight.InsightRepositoryImpl

val insightDataModule = module {
    single<InsightRepository> { InsightRepositoryImpl() }
}
