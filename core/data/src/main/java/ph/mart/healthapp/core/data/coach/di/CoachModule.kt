package ph.mart.healthapp.core.data.coach.di

import org.koin.dsl.module
import ph.mart.healthapp.core.data.AppDatabase
import ph.mart.healthapp.core.data.coach.CoachRepository
import ph.mart.healthapp.core.data.coach.CoachRepositoryImpl

/**
 * The five repositories are the coach's tools: four that a read tool queries and one whose targets
 * the day block prices against. They are resolved by type from their own domain modules, which is
 * the same reach `insight/` already has — the coach is the second thing in `:core:data` that
 * spans domains, and neither crosses a module boundary to do it.
 */
val coachDataModule = module {
    single { get<AppDatabase>().chatMessageDao() }
    single<CoachRepository> {
        CoachRepositoryImpl(
            dao = get(),
            foodRepository = get(),
            waterRepository = get(),
            progressRepository = get(),
            exerciseRepository = get(),
            profileRepository = get(),
        )
    }
}
