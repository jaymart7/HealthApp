package ph.mart.healthapp.feature.onboarding.ui.health

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.health.HealthConnection
import ph.mart.healthapp.core.data.health.HealthSyncRepository
import ph.mart.healthapp.feature.onboarding.R

// Resource ids, not text: the screen resolves them, so no Context reaches the ViewModel.
@StringRes private val UNAVAILABLE = R.string.onboarding_health_unavailable

@StringRes private val DECLINED = R.string.onboarding_health_declined

data class OnboardingHealthUiState(
    /**
     * Optimistic until the Play services round trip says otherwise. Starting it false made the
     * step open on a disabled Connect **and** a way out relabelled "Continue" — the screen said
     * the grant was impossible for as long as the check took, then changed its mind.
     */
    val canConnect: Boolean = true,
    /** A Play services round trip, a consent sheet or a sync is in flight. */
    val busy: Boolean = false,
    @StringRes val message: Int? = null,
    val messageIsError: Boolean = false,
    /** The consent sheet was raised and refused. The screen swaps its two actions' weight on
     * this: the question has been answered, so continuing is what the filled button should do. */
    val declined: Boolean = false,
)

sealed interface OnboardingHealthSideEffect {
    data class LaunchConsent(val pendingIntent: PendingIntent) : OnboardingHealthSideEffect
    data object Connected : OnboardingHealthSideEffect
}

/**
 * The onboarding step's own slice of [HealthSyncRepository] — connect or skip, nothing else.
 * Kept separate from Profile's fuller version rather than shared across modules: `:feature:*`
 * modules never reach into each other, and this half is genuinely smaller than that one.
 */
class OnboardingHealthViewModel(
    private val repository: HealthSyncRepository,
) : ViewModel(), OrbitContainerHost<OnboardingHealthUiState, OnboardingHealthUiState, OnboardingHealthSideEffect> {

    override val container =
        orbitContainer<OnboardingHealthUiState, OnboardingHealthSideEffect>(OnboardingHealthUiState()) {
            intent {
                val connection = repository.connection()
                reduce {
                    state.copy(
                        canConnect = connection !is HealthConnection.Unavailable,
                        message = if (connection is HealthConnection.Unavailable) UNAVAILABLE else null,
                        messageIsError = connection is HealthConnection.Unavailable,
                    )
                }
            }
        }

    /**
     * Everything past the tap is a round trip — Play services, then either the consent Activity or
     * a first sync — so `busy` goes up here and only comes down on a path that leaves the user on
     * this screen. The consent branch deliberately leaves it up: the sheet is what happens next,
     * and [onConsentResult] is what lowers it.
     *
     * The re-entrancy guard is not the disabled button's job. A second tap dispatched in the same
     * frame is already in flight before the first recomposition lands.
     */
    fun connect() = intent {
        if (state.busy) return@intent
        reduce { state.copy(busy = true, message = null) }
        when (val connection = repository.connection()) {
            is HealthConnection.Disconnected -> connection.pendingIntent
                ?.let { postSideEffect(OnboardingHealthSideEffect.LaunchConsent(it)) }
                ?: reduce { state.copy(busy = false, message = UNAVAILABLE, messageIsError = true) }

            // The grant already exists — a reinstall, say. Sync and move on.
            is HealthConnection.Connected -> {
                repository.sync()
                postSideEffect(OnboardingHealthSideEffect.Connected)
            }

            // Checking is the screen's own placeholder in Profile and is never returned here;
            // the branch exists because the `when` is exhaustive.
            HealthConnection.Checking, HealthConnection.Unavailable -> reduce {
                state.copy(busy = false, canConnect = false, message = UNAVAILABLE, messageIsError = true)
            }
        }
    }

    fun onConsentResult(data: Intent?) = intent {
        if (repository.completeConsent(data)) {
            repository.sync()
            postSideEffect(OnboardingHealthSideEffect.Connected)
        } else {
            reduce { state.copy(busy = false, message = DECLINED, messageIsError = false, declined = true) }
        }
    }
}
