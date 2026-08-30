package ph.mart.healthapp.feature.progress.ui.progress

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.health.SleepRepository
import ph.mart.healthapp.core.data.mood.MoodRepository
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.streak.loggedDays
import ph.mart.healthapp.core.data.water.WaterRepository

/** Read-only container — nothing on the Progress tab itself writes data (see [ProgressUiState]),
 * so there's no handleEvent/Event pair here, unlike the other screens in this feature. The water
 * and exercise repositories are read for one thing only: the weekly recap's logged-day count. */
class ProgressViewModel(
    progressRepository: ProgressRepository,
    profileRepository: ProfileRepository,
    foodRepository: FoodRepository,
    waterRepository: WaterRepository,
    exerciseRepository: ExerciseRepository,
    moodRepository: MoodRepository,
    sleepRepository: SleepRepository,
) : ViewModel(), OrbitContainerHost<ProgressUiState, ProgressUiState, Nothing> {

    override val container = orbitContainer<ProgressUiState, Nothing>(ProgressUiState()) {
        observeProgress(
            progressRepository, profileRepository, foodRepository, waterRepository, exerciseRepository,
            moodRepository, sleepRepository,
        )
    }

    private fun observeProgress(
        progressRepository: ProgressRepository,
        profileRepository: ProfileRepository,
        foodRepository: FoodRepository,
        waterRepository: WaterRepository,
        exerciseRepository: ExerciseRepository,
        moodRepository: MoodRepository,
        sleepRepository: SleepRepository,
    ) = intent {
        val progress = combine(
            progressRepository.observeWeightEntries(),
            progressRepository.observeMeasurements(),
            progressRepository.observePhotos(),
            profileRepository.observeProfile(),
            foodRepository.observeDailyNutrition(),
        ) { weightEntries, measurements, photos, profile, dailyNutrition ->
            ProgressUiState(
                weightEntries = weightEntries,
                measurements = measurements,
                photos = photos,
                goalWeightKg = profile?.targetWeightKg,
                goal = profile?.goal,
                preferredUnit = profile?.preferredUnit ?: UnitSystem.Metric,
                dailyNutrition = dailyNutrition,
                // Computed live off the profile, same as every other place targets are shown.
                targets = profile?.dailyTargets(),
            )
        }

        // Chained rather than widened: `combine` only has typed overloads up to five flows, and
        // the logged-day set is independent of the charts' data anyway. Same shape as
        // HomeViewModel.observeHome(), for the same reason.
        val activeDays = combine(
            foodRepository.observeDailyNutrition(),
            waterRepository.observeLoggedDays(),
            progressRepository.observeWeightEntries(),
            exerciseRepository.observeLoggedDays(),
            ::loggedDays,
        )

        combine(
            progress,
            activeDays,
            moodRepository.observeDays(),
            sleepRepository.observeNights(),
        ) { state, days, moodDays, nights ->
            state.copy(activeDays = days, moodDays = moodDays, sleepNights = nights)
        }.collect { newState -> reduce { newState } }
    }
}
