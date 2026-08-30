package ph.mart.healthapp.feature.onboarding.ui.onboarding

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.ProfileRepository

/**
 * One container for the whole 6-step flow, not one per screen — later steps' math depends on
 * earlier answers, and only the final step persists anything. Steps 0-4 never touch this
 * ViewModel at all (no repository access, no event needed); the only event is [OnboardingEvent.OnFinish].
 */
class OnboardingViewModel(
    private val profileRepository: ProfileRepository,
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
        delay(700)
        profileRepository.saveProfile(profile)
        postSideEffect(OnboardingSideEffect.Finished)
    }
}
