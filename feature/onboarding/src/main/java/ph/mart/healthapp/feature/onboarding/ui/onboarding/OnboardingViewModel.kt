package ph.mart.healthapp.feature.onboarding.ui.onboarding

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.nowMinuteOfDay
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.progress.WeightEntry
import ph.mart.healthapp.core.data.todayEpochDay

/**
 * One container for the whole 6-step flow, not one per screen — later steps' math depends on
 * earlier answers, and only the final step persists anything. Steps 0-4 never touch this
 * ViewModel at all (no repository access, no event needed); the only event is [OnboardingEvent.OnFinish].
 */
class OnboardingViewModel(
    private val profileRepository: ProfileRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel(), OrbitContainerHost<OnboardingUiState, OnboardingUiState, OnboardingSideEffect> {

    override val container = orbitContainer<OnboardingUiState, OnboardingSideEffect>(OnboardingUiState())

    fun handleEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.OnFinish -> onFinish(event.form)
        }
    }

    private fun onFinish(form: OnboardingForm) = intent {
        val profile = form.toProfileOrNull() ?: return@intent
        celebrateThenSave(profile)
    }

    private suspend fun Syntax<OnboardingUiState, OnboardingSideEffect>.celebrateThenSave(profile: Profile) {
        reduce { state.copy(isCelebrating = true) }
        // The celebration's own length: the macro bar draws for 500ms and the button is held
        // until the whole thing has landed.
        delay(900)
        // The weight just typed is the journey's first point, so the Weight chart and "lost since
        // you started" have somewhere to start from on day one. Only into an empty log — a
        // reinstall that kept its data, or a Google Health sync on step 5, already has its own —
        // and before the profile, whose write is what swaps this screen out from under us.
        if (progressRepository.observeWeightEntries().first().isEmpty()) {
            progressRepository.upsertWeightEntry(
                WeightEntry(dateEpochDay = todayEpochDay(), weightKg = profile.weightKg, minuteOfDay = nowMinuteOfDay()),
            )
        }
        profileRepository.saveProfile(profile)
        postSideEffect(OnboardingSideEffect.Finished)
    }
}
