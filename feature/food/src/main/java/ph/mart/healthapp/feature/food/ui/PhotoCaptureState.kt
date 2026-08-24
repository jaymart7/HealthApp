package ph.mart.healthapp.feature.food.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.RecognitionConfidence
import ph.mart.healthapp.core.data.food.RecognizedFood

enum class CaptureFlow { Capture, Analyzing, Confirmation, Retry, NoFood, Offline, PermissionDenied }

@Composable
internal fun rememberPhotoCaptureScreen(): PhotoCaptureScreenState = remember { PhotoCaptureScreenState() }

/**
 * Screen-local flow/UI state. Plain `remember`, not `rememberSaveable` like [FoodScreenState] —
 * a captured [Bitmap] isn't cheaply parcelable, and losing an in-progress capture on process
 * death is an acceptable, flagged simplification: a half-finished photo capture isn't meaningful
 * to restore.
 * ponytail: process death loses the in-progress capture; add a Saver-backed byte[] snapshot if
 * that's ever reported as a real problem.
 */
internal class PhotoCaptureScreenState(
    flow: CaptureFlow = CaptureFlow.Capture,
    photo: Bitmap? = null,
    form: AddEntryForm = AddEntryForm(mealType = defaultMealTypeForNow()),
    originalForm: AddEntryForm = form,
    confidence: RecognitionConfidence = RecognitionConfidence.High,
    searchQuery: String = "",
    showDiscardConfirm: Boolean = false,
) {
    var flow: CaptureFlow by mutableStateOf(flow)
    var photo: Bitmap? by mutableStateOf(photo)
    var form: AddEntryForm by mutableStateOf(form)
    var originalForm: AddEntryForm by mutableStateOf(originalForm)
    var confidence: RecognitionConfidence by mutableStateOf(confidence)
    var searchQuery: String by mutableStateOf(searchQuery)
    var showDiscardConfirm: Boolean by mutableStateOf(showDiscardConfirm)

    val isDirty: Boolean get() = form != originalForm

    fun applyRecognized(food: RecognizedFood) {
        val seeded = food.toAddEntryForm(form.mealType)
        form = seeded
        originalForm = seeded
        confidence = food.confidence
        flow = CaptureFlow.Confirmation
    }

    fun selectMealType(mealType: MealType) {
        form = form.copy(mealType = mealType)
        originalForm = originalForm.copy(mealType = mealType)
    }
}
