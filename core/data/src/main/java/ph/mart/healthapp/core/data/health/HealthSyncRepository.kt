package ph.mart.healthapp.core.data.health

import android.app.PendingIntent
import android.content.Intent

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
 * FitPulse's side of the Google Health API. Reads the user's workouts into the same
 * `exercise_entry` table the diary writes to, so an imported workout is an ordinary entry from
 * every other screen's point of view — including `budgetKcal()` and the logging streak.
 *
 * Sync is on-connect and on-demand only. There is no background job: a periodic network read of
 * health data is a thing that has to be justified at verification, and "when the user asks"
 * covers the need.
 */
interface HealthSyncRepository {
    /** Silent — never shows UI. Safe to call whenever a screen that displays state appears. */
    suspend fun connection(): HealthConnection

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
