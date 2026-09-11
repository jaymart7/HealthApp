package ph.mart.healthapp.core.data

import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ensures a Firebase user is signed in before an AI call.
 *
 * Firebase AI Logic does not document Authentication as a requirement — App Check is what the
 * backend actually checks — but the SDK attaches an Auth token when `firebase-auth` is on the
 * classpath, and did throw `FirebaseNoSignedInUserException` without one. FITPULSE is local-first
 * and has no user login, so this uses Anonymous Authentication. A call is made at app start in
 * [FitPulseApplication]; this helper makes a repository call wait if that one has not finished
 * yet (e.g. on first launch with a slow network).
 *
 * A sign-in failure is deliberately **not** rethrown. Throwing here pre-empts the AI call with an
 * exception of our own making, which the caller swallows identically — so a console
 * misconfiguration (Anonymous provider disabled) would be indistinguishable from being offline.
 * Letting it through means the SDK's own error is what reaches [logAiFailure].
 */
suspend fun ensureAuth() = withContext(Dispatchers.IO) {
    val auth = Firebase.auth
    // Silences "Ignoring header X-Firebase-Locale because its value was null" — a benign GMS log,
    // not an error, but one that otherwise sits in logcat looking like the cause of every failure.
    auth.useAppLanguage()
    if (auth.currentUser == null) {
        try {
            Tasks.await(auth.signInAnonymously())
        } catch (e: Exception) {
            logAiFailure("anonymous sign-in", e)
        }
    }
}
