package ph.mart.healthapp.feature.progress.ui.achievement

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.fasting.FastingRepository
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.streak.loggedDays
import ph.mart.healthapp.core.data.streak.weightProgressKg
import ph.mart.healthapp.core.data.water.WaterRepository

/**
 * The Badges page's container, and the widest of the thirteen: six repositories, against
 * `ProgressViewModel`'s thirteen. An achievement list is a fold over everything the app records, so
 * there is no narrower honest answer.
 *
 * What keeps it a slice rather than a second copy of the tab is what leaves: the outer combine
 * reduces six flows to the five numbers `badgeGroups()` takes plus a unit, so no series reaches
 * [AchievementsUiState]. The two folds it copies — `loggedDays()` and `weightProgressKg()` — are
 * the `:core:data/streak` ones `HomeViewModel` and `ProgressViewModel` already call, so the badge
 * row on the overview and the page behind it cannot disagree about what a logged day is.
 *
 * Chained rather than widened, the shape `ProgressViewModel` uses: `combine` has typed overloads
 * up to five flows.
 */
@Suppress("LongParameterList")
class AchievementsViewModel(
    progressRepository: ProgressRepository,
    profileRepository: ProfileRepository,
    foodRepository: FoodRepository,
    waterRepository: WaterRepository,
    exerciseRepository: ExerciseRepository,
    fastingRepository: FastingRepository,
) : ViewModel(), OrbitContainerHost<AchievementsUiState, AchievementsUiState, Nothing> {

    override val container = orbitContainer<AchievementsUiState, Nothing>(AchievementsUiState()) {
        observeBadges(
            progressRepository, profileRepository, foodRepository,
            waterRepository, exerciseRepository, fastingRepository,
        )
    }

    private fun observeBadges(
        progressRepository: ProgressRepository,
        profileRepository: ProfileRepository,
        foodRepository: FoodRepository,
        waterRepository: WaterRepository,
        exerciseRepository: ExerciseRepository,
        fastingRepository: FastingRepository,
    ) = intent {
        val activeDays = combine(
            foodRepository.observeDailyNutrition(),
            waterRepository.observeLoggedDays(),
            progressRepository.observeWeightEntries(),
            exerciseRepository.observeLoggedDays(),
            ::loggedDays,
        )

        val fromProgress = combine(
            progressRepository.observeWeightEntries(),
            progressRepository.observePhotos(),
            profileRepository.observeProfile(),
        ) { entries, photos, profile ->
            ProgressFacts(
                photoCount = photos.size,
                weightProgressKg = profile?.let { weightProgressKg(entries, it.goal, it.weightKg) },
                unit = profile?.preferredUnit ?: UnitSystem.Metric,
            )
        }

        combine(
            activeDays,
            fromProgress,
            exerciseRepository.observeRecentEntries(),
            fastingRepository.observeSessions(),
        ) { days, facts, entries, fasts ->
            AchievementsUiState(
                activeDays = days,
                weightProgressKg = facts.weightProgressKg,
                workoutCount = entries.size,
                fasts = fasts,
                photoCount = facts.photoCount,
                unit = facts.unit,
            )
        }.collect { newState -> reduce { newState } }
    }
}

/** The three things the progress and profile flows contribute, grouped so the outer combine stays
 * inside the typed overloads' arity. Private and structural — it never leaves this file. */
private data class ProgressFacts(
    val photoCount: Int,
    val weightProgressKg: Double?,
    val unit: UnitSystem,
)
