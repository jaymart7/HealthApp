package ph.mart.healthapp.feature.food.ui.photo

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.RecognitionConfidence
import ph.mart.healthapp.core.data.food.RecognizedFood
import ph.mart.healthapp.core.data.food.ScannedProduct
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.defaultMealTypeForNow
import ph.mart.healthapp.feature.food.ui.shared.toAddEntryForm

enum class CaptureFlow {
    Capture, Analyzing, Confirmation, SearchConfirmation, Retry, NoFood, Offline, PermissionDenied
}

@Composable
internal fun rememberPhotoCaptureScreen(): PhotoCaptureScreenState = remember { PhotoCaptureScreenState() }

/**
 * Screen-local flow/UI state. Plain `remember`, not `rememberSaveable` like [FoodScreenState][ph.mart.healthapp.feature.food.ui.diary.FoodScreenState] —
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
) {
    var flow: CaptureFlow by mutableStateOf(flow)
    var photo: Bitmap? by mutableStateOf(photo)
    var form: AddEntryForm by mutableStateOf(form)
    var originalForm: AddEntryForm by mutableStateOf(originalForm)
    var confidence: RecognitionConfidence by mutableStateOf(confidence)

    /** What a confirmed discard does — go back to the camera, back to the search, or leave the
     * flow. Non-null exactly while the dialog is up. A lambda rather than a [CaptureFlow] because
     * the Discard *button* leaves the flow entirely, and it has to ask the same question the back
     * gesture does rather than acting without one. */
    var pendingDiscard: (() -> Unit)? by mutableStateOf(null)

    val isDirty: Boolean get() = form != originalForm

    fun applyRecognized(food: RecognizedFood) {
        val seeded = food.toAddEntryForm(form.mealType)
        form = seeded
        originalForm = seeded
        confidence = food.confidence
        flow = CaptureFlow.Confirmation
    }

    /** A hit picked from the food search: no photo, no AI estimate, so no confidence notice. */
    fun applyProduct(product: ScannedProduct) {
        val seeded = product.toAddEntryForm(form.mealType)
        form = seeded
        originalForm = seeded
        flow = CaptureFlow.SearchConfirmation
    }

    /** The "enter it manually" path out of the search: keep the chosen meal, clear the rest. */
    fun startManualEntry() {
        val blank = AddEntryForm(mealType = form.mealType)
        form = blank
        originalForm = blank
        flow = CaptureFlow.SearchConfirmation
    }

    fun selectMealType(mealType: MealType) {
        form = form.copy(mealType = mealType)
        originalForm = originalForm.copy(mealType = mealType)
    }
}
