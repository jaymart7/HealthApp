package ph.mart.healthapp.core.data.coach.di

import org.koin.dsl.module
import ph.mart.healthapp.core.data.AppDatabase
import ph.mart.healthapp.core.data.coach.CoachRepository
import ph.mart.healthapp.core.data.coach.CoachRepositoryImpl
import ph.mart.healthapp.core.data.coach.CoachToolbox
import ph.mart.healthapp.core.data.debugCoach

/**
 * The eight repositories are the coach's tools: seven that a read tool queries and one whose
 * targets the day block prices against. They are resolved by type from their own domain modules,
 * which is the same reach `insight/` already has — the coach is the second thing in `:core:data` that
 * spans domains, and neither crosses a module boundary to do it.
 *
 * The toolbox is built here rather than inside the repository so that both the real coach and the
 * debug build's fake one hold the same instance. `debugCoach` is a source-set pair and returns
 * null in release, so the whole fake is absent from a release build rather than merely unreachable
 * — `seedDebugData`'s shape.
 */
val coachDataModule = module {
    single { get<AppDatabase>().chatMessageDao() }
    single {
        CoachToolbox(
            foodRepository = get(),
            progressRepository = get(),
            waterRepository = get(),
            exerciseRepository = get(),
            profileRepository = get(),
            sleepRepository = get(),
            moodRepository = get(),
            fastingRepository = get(),
        )
    }
    single<CoachRepository> {
        val real = CoachRepositoryImpl(
            dao = get(),
            foodRepository = get(),
            waterRepository = get(),
            exerciseRepository = get(),
            toolbox = get(),
        )
        debugCoach(real, get()) ?: real
    }
}
