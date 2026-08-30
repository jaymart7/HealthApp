package ph.mart.healthapp.feature.onboarding.ui.health

import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.health.HealthConnection
import ph.mart.healthapp.core.data.health.HealthSyncRepository

private const val UNAVAILABLE =
    "Google Health needs Google Play services and a Google account on this device."

private const val DECLINED = "Not connected — nothing was shared. You can do this later in Profile."

data class OnboardingHealthUiState(
    val canConnect: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false,
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

    fun connect() = intent {
        when (val connection = repository.connection()) {
            is HealthConnection.Disconnected -> connection.pendingIntent
                ?.let { postSideEffect(OnboardingHealthSideEffect.LaunchConsent(it)) }
                ?: reduce { state.copy(message = UNAVAILABLE, messageIsError = true) }

            // The grant already exists — a reinstall, say. Sync and move on.
            is HealthConnection.Connected -> {
                repository.sync()
                postSideEffect(OnboardingHealthSideEffect.Connected)
            }

            HealthConnection.Checking, HealthConnection.Unavailable ->
                reduce { state.copy(canConnect = false, message = UNAVAILABLE, messageIsError = true) }
        }
    }

    fun onConsentResult(data: Intent?) = intent {
        if (repository.completeConsent(data)) {
            repository.sync()
            postSideEffect(OnboardingHealthSideEffect.Connected)
        } else {
            reduce { state.copy(message = DECLINED, messageIsError = false) }
        }
    }
}
