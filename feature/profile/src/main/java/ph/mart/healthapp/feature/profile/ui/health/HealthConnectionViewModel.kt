package ph.mart.healthapp.feature.profile.ui.health

import android.content.Intent
import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.health.HealthConnection
import ph.mart.healthapp.core.data.health.HealthSyncRepository
import ph.mart.healthapp.core.data.health.HealthSyncResult

private const val UNAVAILABLE =
    "Google Health needs Google Play services and a Google account on this device."

private const val CONSENT_DECLINED = "Not connected — nothing was shared."

private const val SYNC_FAILED = "Couldn't reach Google Health. Try again."

private const val OFFLINE = "You're offline. Connect and try again."

/** What one sync produced, already turned into something the screen can render. */
private data class SyncOutcome(
    val connection: HealthConnection,
    val message: String,
    val isError: Boolean,
)

class HealthConnectionViewModel(
    private val repository: HealthSyncRepository,
) : ViewModel(), OrbitContainerHost<HealthConnectionUiState, HealthConnectionUiState, HealthConnectionSideEffect> {

    override val container =
        orbitContainer<HealthConnectionUiState, HealthConnectionSideEffect>(HealthConnectionUiState()) {
            refresh()
        }

    /** Silent: [HealthSyncRepository.connection] never raises UI, it only reports what Google says. */
    fun refresh() = intent {
        val connection = repository.connection()
        reduce {
            state.copy(
                connection = connection,
                busy = false,
                message = if (connection is HealthConnection.Unavailable) UNAVAILABLE else state.message,
                messageIsError = connection is HealthConnection.Unavailable,
            )
        }
    }

    /**
     * Reachable only from the disclosure screen's own button. The disclosure has to be what the
     * user reads before Google's consent screen appears, so nothing else in the app calls this.
     */
    fun connect() = intent {
        when (val connection = state.connection) {
            is HealthConnection.Disconnected -> {
                val pendingIntent = connection.pendingIntent
                if (pendingIntent == null) {
                    reduce { state.copy(message = UNAVAILABLE, messageIsError = true) }
                } else {
                    reduce { state.copy(busy = true, message = null) }
                    postSideEffect(HealthConnectionSideEffect.LaunchConsent(pendingIntent))
                }
            }

            // Already granted — a reconnect after a local disconnect. Go straight to the data.
            is HealthConnection.Connected -> {
                reduce { state.copy(busy = true, message = null) }
                val outcome = runSync()
                reduce { state.applied(outcome) }
            }

            HealthConnection.Checking, HealthConnection.Unavailable ->
                reduce { state.copy(message = UNAVAILABLE, messageIsError = true) }
        }
    }

    fun onConsentResult(data: Intent?) = intent {
        if (repository.completeConsent(data)) {
            val outcome = runSync()
            reduce { state.applied(outcome) }
        } else {
            val connection = repository.connection()
            reduce {
                state.copy(
                    connection = connection,
                    busy = false,
                    message = CONSENT_DECLINED,
                    messageIsError = false,
                )
            }
        }
    }

    fun sync() = intent {
        reduce { state.copy(busy = true, message = null) }
        val outcome = runSync()
        reduce { state.applied(outcome) }
    }

    fun confirmDisconnect(show: Boolean) = intent {
        reduce { state.copy(confirmingDisconnect = show) }
    }

    fun disconnect(deleteImported: Boolean, deleteSent: Boolean) = intent {
        reduce { state.copy(busy = true, confirmingDisconnect = false, message = null) }
        repository.disconnect(deleteImported = deleteImported, deleteSent = deleteSent)
        val connection = repository.connection()
        reduce {
            state.copy(
                connection = connection,
                busy = false,
                message = buildString {
                    append("Disconnected.")
                    if (deleteImported) append(" Imported data was deleted from this device.")
                    if (deleteSent) append(" What FitPulse sent was deleted from Google Health.")
                },
                messageIsError = false,
            )
        }
    }

    private suspend fun runSync(): SyncOutcome {
        val result = repository.sync()
        return SyncOutcome(
            // Re-read after the write so the imported count on screen matches what just landed.
            connection = repository.connection(),
            message = when (result) {
                is HealthSyncResult.Imported ->
                    if (result.items == 0) "Up to date." else "Imported ${result.items} workouts."

                HealthSyncResult.Offline -> OFFLINE
                is HealthSyncResult.NeedsConsent -> CONSENT_DECLINED
                HealthSyncResult.Failed -> SYNC_FAILED
            },
            isError = result !is HealthSyncResult.Imported,
        )
    }
}

/** Keeps the three intents that end in a sync from spelling out the same `copy`. */
private fun HealthConnectionUiState.applied(outcome: SyncOutcome) = copy(
    connection = outcome.connection,
    busy = false,
    message = outcome.message,
    messageIsError = outcome.isError,
)
