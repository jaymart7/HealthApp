package ph.mart.healthapp.feature.profile.ui.health

import android.app.PendingIntent
import ph.mart.healthapp.core.data.health.HealthConnection

data class HealthConnectionUiState(
    val connection: HealthConnection = HealthConnection.Checking,
    /** A network call or a Play services round trip is in flight; the buttons are disabled. */
    val busy: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false,
    /** The disconnect confirmation sheet is showing. */
    val confirmingDisconnect: Boolean = false,
)

/** Google's consent screen is an Activity, so only the composable can launch it. */
sealed interface HealthConnectionSideEffect {
    data class LaunchConsent(val pendingIntent: PendingIntent) : HealthConnectionSideEffect
}
