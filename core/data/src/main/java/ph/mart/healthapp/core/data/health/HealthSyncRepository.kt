package ph.mart.healthapp.core.data.health

import android.app.PendingIntent
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

/** What the Profile card and the onboarding step render. */
sealed interface HealthConnection {
    /** Asked but not answered yet — Play services is being consulted. */
    data object Checking : HealthConnection

    /** The grant exists. [importedItems] is how many data points FitPulse currently holds. */
    data class Connected(val importedItems: Int) : HealthConnection

    /** Not granted yet. [pendingIntent] shows Google's consent screen when the user asks for it. */
    data class Disconnected(val pendingIntent: PendingIntent?) : HealthConnection

    /** No Play services, or no Google account on the device. */
    data object Unavailable : HealthConnection
}

sealed interface HealthSyncResult {
    data class Imported(val items: Int) : HealthSyncResult
    data object Offline : HealthSyncResult

    /** The grant went away mid-session — the user revoked it from their Google account. */
    data class NeedsConsent(val pendingIntent: PendingIntent?) : HealthSyncResult
    data object Failed : HealthSyncResult
}

/**
 * FitPulse's health integration — **two providers, one entry point.**
 *
 * Health Connect reads the same six types on-device (see [HealthConnectSource]); the Google Health
 * API reads whatever is left and is the only leg that pushes. Everything imported lands in the
 * table the diary already writes to, so an imported workout is an ordinary entry from every other
 * screen's point of view — including `budgetKcal()` and the logging streak — whichever provider
 * fetched it.
 *
 * [sync] is the single entry point precisely so precedence is decided in one place: two
 * orchestrators is how two surfaces come to disagree about which provider owns a table. The
 * Connections screen and the onboarding step both call it and neither knows the difference.
 *
 * Sync is on-connect and on-demand only. There is no background job: a periodic network read of
 * health data is a thing that has to be justified at verification, and "when the user asks"
 * covers the need. Health Connect makes that cheap rather than unnecessary — it still polls, it
 * just polls a local provider.
 */
interface HealthSyncRepository {
    /** Silent — never shows UI. Safe to call whenever a screen that displays state appears. */
    suspend fun connection(): HealthConnection

    /** Health Connect's side of the same question, and just as silent. */
    suspend fun connectState(): HealthConnectState

    /**
     * The contract the Connections screen hands `rememberLauncherForActivityResult`. It comes from
     * here rather than being built in `:feature:profile` so that module never imports
     * `androidx.health.connect` — the rule [GoogleHealthAuth] follows by handing back a
     * `PendingIntent` instead of a Play services type.
     */
    fun connectPermissionContract(): ActivityResultContract<Set<String>, Set<String>>

    /** Hands back the result of the consent Activity. True when a token came back. */
    suspend fun completeConsent(data: Intent?): Boolean

    suspend fun sync(): HealthSyncResult

    /**
     * Revokes the grant and, when asked, deletes what came of it — in both directions:
     * [deleteImported] removes the workouts, weigh-ins and sleep FitPulse pulled in, and
     * [deleteSent] removes the meals and water it sent out. Both are the user's call, and both
     * default to on in the UI: an integration that leaves data behind on either side after being
     * switched off is exactly what the security assessment looks for.
     */
    suspend fun disconnect(deleteImported: Boolean, deleteSent: Boolean)
}
