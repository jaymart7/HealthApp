package ph.mart.healthapp.core.data

import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ensures a Firebase user is signed in before an AI call. Vertex AI for Firebase requires a
 * signed-in user to generate a token for the backend request; without one, the SDK throws
 * `FirebaseNoSignedInUserException`.
 *
 * FITPULSE is local-first and has no user login, so this uses Anonymous Authentication. A call is
 * made at app start in [FitPulseApplication], but this helper makes a repository call wait if
 * that one has not finished yet (e.g. on first launch with a slow network).
 */
suspend fun ensureAuth() = withContext(Dispatchers.IO) {
    val auth = Firebase.auth
    if (auth.currentUser == null) {
        try {
            Tasks.await(auth.signInAnonymously())
        } catch (_: Exception) {
            // If sign-in fails (e.g. offline), we let it through; the AI call will catch its
            // own exception.
        }
    }
}
