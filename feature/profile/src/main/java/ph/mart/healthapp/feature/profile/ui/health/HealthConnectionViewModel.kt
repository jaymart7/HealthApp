package ph.mart.healthapp.feature.profile.ui.health

import android.content.Intent
import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import androidx.activity.result.contract.ActivityResultContract
import ph.mart.healthapp.core.data.health.CONNECT_PERMISSIONS
import ph.mart.healthapp.core.data.health.HealthConnectState
import ph.mart.healthapp.core.data.health.HealthConnection
import ph.mart.healthapp.core.data.health.HealthSyncRepository
import ph.mart.healthapp.core.data.health.HealthSyncResult

private const val UNAVAILABLE =
    "Google Health needs Google Play services and a Google account on this device."

private const val CONSENT_DECLINED = "Not connected — nothing was shared."

private const val SYNC_FAILED = "Couldn't reach Google Health. Try again."

private const val OFFLINE = "You're offline. Connect and try again."

private const val CONNECT_DECLINED = "Health Connect isn't sharing anything yet."

/** What one sync produced, already turned into something the screen can render. */
private data class SyncOutcome(
    val connection: HealthConnection,
    val connect: HealthConnectState,
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

    /**
     * The contract the screen's launcher is built from. It comes off the repository so
     * `androidx.health.connect` never reaches this module — see
     * [HealthSyncRepository.connectPermissionContract].
     */
    fun permissionContract(): ActivityResultContract<Set<String>, Set<String>> =
        repository.connectPermissionContract()

    /**
     * Silent on both sides: neither [HealthSyncRepository.connection] nor
     * [HealthSyncRepository.connectState] raises UI, they only report what each provider says right
     * now — which is the whole reason neither is a stored flag. Run once per entry to the screen:
     * this route owns its own ViewModelStoreOwner, so leaving and returning re-asks, and a
     * permission revoked from the Health Connect app in between is seen on the way back in.
     */
    fun refresh() = intent {
        val connection = repository.connection()
        val connect = repository.connectState()
        reduce {
            state.copy(
                connection = connection,
                connect = connect,
                busy = false,
                // Unavailable is only worth saying when Health Connect isn't covering things
                // anyway: a phone with no Play services still syncs locally, and reporting that
                // as an error would contradict the panel above saying it works.
                message = if (connection is HealthConnection.Unavailable && connect !is HealthConnectState.Available) {
                    UNAVAILABLE
                } else {
                    state.message
                },
                messageIsError = connection is HealthConnection.Unavailable &&
                    connect !is HealthConnectState.Available,
            )
        }
    }

    /**
     * Health Connect's own sheet decides this, not FitPulse — so every permission is asked for
     * every time, including ones already granted. Health Connect shows the user what they have and
     * lets them take one away, which is the screen a "Change what FitPulse reads" button should
     * open.
     */
    fun requestConnectPermissions() = intent {
        when (state.connect) {
            is HealthConnectState.Available ->
                postSideEffect(HealthConnectionSideEffect.RequestConnectPermissions(CONNECT_PERMISSIONS))

            HealthConnectState.UpdateRequired ->
                postSideEffect(HealthConnectionSideEffect.OpenHealthConnectListing)

            HealthConnectState.Checking, HealthConnectState.Unsupported -> Unit
        }
    }

    /**
     * The sheet hands back what is granted *now*, whichever way the user moved the switches. A
     * grant is worth syncing immediately — it is the whole reason they tapped — and a decline is
     * reported rather than left silent, so the panel's unticked rows have an explanation.
     */
    fun onConnectPermissionsResult(granted: Set<String>) = intent {
        if (granted.isEmpty()) {
            val connect = repository.connectState()
            reduce {
                state.copy(
                    connect = connect,
                    busy = false,
                    message = CONNECT_DECLINED,
                    messageIsError = false,
                )
            }
        } else {
            reduce { state.copy(busy = true, message = null) }
            val outcome = runSync()
            reduce { state.applied(outcome) }
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
            connect = repository.connectState(),
            message = when (result) {
                is HealthSyncResult.Imported ->
                    if (result.items == 0) "Up to date." else "Imported ${result.items} items."

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
    connect = outcome.connect,
    busy = false,
    message = outcome.message,
    messageIsError = outcome.isError,
)
