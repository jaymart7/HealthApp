package ph.mart.healthapp.core.data.insight.di

import org.koin.dsl.module
import ph.mart.healthapp.core.data.debugInsight
import ph.mart.healthapp.core.data.insight.InsightRepository
import ph.mart.healthapp.core.data.insight.InsightRepositoryImpl

/** `debugInsight()` is a source-set pair and is null in release — see `DebugAi.kt`. */
val insightDataModule = module {
    single<InsightRepository> { debugInsight() ?: InsightRepositoryImpl() }
}
