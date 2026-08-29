package ph.mart.healthapp.core.data.health

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Every Google Health scope FitPulse asks for, each tied to one shipping feature. Requesting more
 * than the app uses is what gets a Restricted-scope verification rejected, so this list is the
 * single source of truth — the disclosure screen and the Cloud Console justifications are written
 * against it.
 */
private const val SCOPE_PREFIX = "https://www.googleapis.com/auth/googlehealth."

internal val HEALTH_SCOPES = listOf(
    "${SCOPE_PREFIX}activity_and_fitness.readonly",
    "${SCOPE_PREFIX}health_metrics_and_measurements.readonly",
    "${SCOPE_PREFIX}sleep.readonly",
    "${SCOPE_PREFIX}nutrition.writeonly",
)

/** What one authorization attempt produced. */
sealed interface HealthAuthResult {
    /** Granted. The token is good for about an hour and is never written to disk. */
    data class Granted(val accessToken: String) : HealthAuthResult

    /** The user has to see Google's consent screen. Launch [pendingIntent] from an Activity. */
    data class NeedsConsent(val pendingIntent: PendingIntent) : HealthAuthResult

    /** No Play services, no signed-in account, or the call failed. */
    data object Unavailable : HealthAuthResult
}

/**
 * The only file in FitPulse that touches `play-services-auth`. Everything above it sees a
 * [String] token and a framework [PendingIntent], which is what keeps `:feature:profile` free of
 * the dependency.
 *
 * There is deliberately **no refresh token and no stored credential**: Google holds the grant, and
 * [authorize] returns a fresh access token silently on every later call for as long as the user
 * has not revoked it. That also makes "are we connected?" a question with one honest answer —
 * ask Google, rather than trusting a local flag that a revocation from myaccount.google.com would
 * silently turn into a lie.
 */
interface GoogleHealthAuth {
    /**
     * Silent when the grant already exists. Safe to call on screen load: it never shows UI on its
     * own, it only hands back the [PendingIntent] that would.
     */
    suspend fun authorize(): HealthAuthResult

    /** The token out of the consent Activity's result. Null if the user backed out. */
    fun tokenFromConsentResult(data: Intent?): String?

    /** Drops the grant on Google's side. Best-effort — a failure still disconnects locally. */
    suspend fun revoke(accessToken: String)
}

internal class GoogleHealthAuthImpl(private val context: Context) : GoogleHealthAuth {

    /**
     * `Tasks.await` rather than `kotlinx-coroutines-play-services` — one blocking call on
     * [Dispatchers.IO] is not worth a dependency. It must never run on the main thread, which the
     * `withContext` guarantees.
     *
     * `include_granted_scopes` is deliberately absent: Google's migration guide calls out mixing
     * these with legacy Fit scopes as a source of authorization failures.
     */
    override suspend fun authorize(): HealthAuthResult = withContext(Dispatchers.IO) {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(HEALTH_SCOPES.map(::Scope))
            .build()
        runCatching { Tasks.await(Identity.getAuthorizationClient(context).authorize(request)) }
            .fold(
                onSuccess = { result ->
                    val pendingIntent = result.pendingIntent
                    when {
                        result.hasResolution() && pendingIntent != null ->
                            HealthAuthResult.NeedsConsent(pendingIntent)
                        // A granted result without a token is not usable; treat it as unavailable
                        // rather than reporting a connection that cannot make a call.
                        else -> result.accessToken?.let(HealthAuthResult::Granted)
                            ?: HealthAuthResult.Unavailable
                    }
                },
                onFailure = { HealthAuthResult.Unavailable },
            )
    }

    override fun tokenFromConsentResult(data: Intent?): String? = runCatching {
        Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data).accessToken
    }.getOrNull()

    /** The Authorization API has no revoke of its own, so this is the OAuth 2.0 endpoint. */
    override suspend fun revoke(accessToken: String) {
        withContext(Dispatchers.IO) {
            healthPost("https://oauth2.googleapis.com/revoke?token=$accessToken", token = null, body = "")
        }
    }
}
