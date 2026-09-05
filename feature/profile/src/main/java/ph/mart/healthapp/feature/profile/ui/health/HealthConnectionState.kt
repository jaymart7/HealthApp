package ph.mart.healthapp.feature.profile.ui.health

import android.app.PendingIntent
import ph.mart.healthapp.core.data.health.CONNECT_PERMISSIONS
import ph.mart.healthapp.core.data.health.HealthConnectState
import ph.mart.healthapp.core.data.health.HealthConnection

data class HealthConnectionUiState(
    val connection: HealthConnection = HealthConnection.Checking,
    /** Health Connect's own state — the local provider, drawn above the cloud one. */
    val connect: HealthConnectState = HealthConnectState.Checking,
    /**
     * What FitPulse would ask Health Connect for right now — `connectPermissions()`, which drops
     * menstruation while cycle tracking is off. The panel lists these rows plus anything already
     * granted, so it never shows a row nobody can tick and never hides one that is ticked.
     */
    val connectRequests: Set<String> = CONNECT_PERMISSIONS,
    /** A network call or a Play services round trip is in flight; the buttons are disabled. */
    val busy: Boolean = false,
    val message: HealthMessage? = null,
    val messageIsError: Boolean = false,
    /** The disconnect confirmation sheet is showing. */
    val confirmingDisconnect: Boolean = false,
)

/** Both consent flows are Activities, so only the composable can launch either. */
sealed interface HealthConnectionSideEffect {
    data class LaunchConsent(val pendingIntent: PendingIntent) : HealthConnectionSideEffect

    /** Health Connect's own permission sheet. The set is plain permission strings, which is what
     *  keeps this module free of `androidx.health.connect` — see `connectPermissionContract`. */
    data class RequestConnectPermissions(val permissions: Set<String>) : HealthConnectionSideEffect

    /** Health Connect is installed but too old. Only the Activity can leave for the Play Store. */
    data object OpenHealthConnectListing : HealthConnectionSideEffect
}
