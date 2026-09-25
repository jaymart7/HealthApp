package ph.mart.healthapp.core.designsystem.component

import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * The two lines every mic in this app needs, in one place because three screens now draw one: the
 * coach's composer, talk-to-log's input, and the log-exercise sheet's describe field.
 *
 * Speech is **the system's own dialog** ([RecognizerIntent.ACTION_RECOGNIZE_SPEECH]) rather than an
 * in-app [SpeechRecognizer]: it needs no `RECORD_AUDIO` permission, so there is no permission
 * screen to write and nothing to deny, and the transcript lands in a field that stays editable.
 * Typing is the same path either way — a mic only fills a field in.
 *
 * What is *not* here is what each caller decides for itself: whether a transcript appends to what
 * is already in the field or replaces it, and whether a control that cannot answer is hidden or
 * merely inert. Both are argued at their call sites.
 */
@Composable
fun rememberSpeechAvailable(): Boolean {
    val context = LocalContext.current
    // Both, because the mic launches the dialog, not the service: a phone can carry a recognizer
    // with nothing answering the intent (an OEM's service, the Google app disabled), and there a
    // tap was an ActivityNotFoundException. The manifest's <queries> makes the intent visible.
    return remember(context) {
        SpeechRecognizer.isRecognitionAvailable(context) &&
            speechIntent("").resolveActivity(context.packageManager) != null
    }
}

/**
 * [prompt] is the words the system dialog shows, and it is passed in rather than read here: it is
 * each screen's own heading, so the dialog reads as part of the screen that opened it. Not a
 * composable, for that reason — there is nothing to remember.
 */
fun speechIntent(prompt: String): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
}

/**
 * What came back, or null. The dialog returns a ranked list and every caller here wants the first
 * of it; blank is the same as nothing, which is what a cancelled dialog and a silent one both are.
 *
 * Here rather than at each call site so no screen has to import [RecognizerIntent] merely to read
 * its own result — the whole of the API surface a mic needs is these three functions.
 */
fun spokenPhrase(data: Intent?): String? = data
    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
    ?.firstOrNull()
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
