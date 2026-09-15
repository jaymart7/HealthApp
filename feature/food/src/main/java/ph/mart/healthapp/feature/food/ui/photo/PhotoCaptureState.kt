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
    /** The search/manual path's single form, edited on [CaptureFlow.SearchConfirmation]. The
     * recognized path uses [items] instead — a plate is several foods, a search hit is one. */
    var form: AddEntryForm by mutableStateOf(form)
    var originalForm: AddEntryForm by mutableStateOf(originalForm)
    var confidence: RecognitionConfidence by mutableStateOf(confidence)

    /** The recognized rows as they stand — edited, repriced, some removed. */
    var items: List<AddEntryForm> by mutableStateOf(emptyList())

    /** The estimate as it arrived, so [itemsDirty] can tell an untouched plate from a corrected
     * one. */
    private var recognized: List<AddEntryForm> by mutableStateOf(emptyList())

    /** Which row is open for editing — one at a time, so the list stays scannable. */
    var expandedIndex: Int? by mutableStateOf(null)

    /** The captured plate open full-screen over the review form. A flag inside
     * [CaptureFlow.Confirmation] rather than a ninth [CaptureFlow]: nothing about the flow changes
     * while it's up — the form is still there underneath, still dirty or not — only what's drawn
     * over it, and a ninth state would have to answer what logging and discarding mean from it. */
    var viewingPhoto: Boolean by mutableStateOf(false)

    /** What a confirmed discard does — go back to the camera, back to the search, or leave the
     * flow. Non-null exactly while the dialog is up. A lambda rather than a [CaptureFlow] because
     * the Discard *button* leaves the flow entirely, and it has to ask the same question the back
     * gesture does rather than acting without one. */
    var pendingDiscard: (() -> Unit)? by mutableStateOf(null)

    /** [CaptureFlow.SearchConfirmation]'s unsaved-edits question. */
    val isDirty: Boolean get() = form != originalForm

    /** [CaptureFlow.Confirmation]'s. Separate because the two states edit different things and the
     * back handler already dispatches per flow. */
    val itemsDirty: Boolean get() = items != recognized

    /**
     * What the camera saw, as rows to review.
     *
     * A single-food plate opens with its one row **expanded**: the list is there so a plate of
     * several foods is scannable, and collapsing the only row there is would put a tap in front of
     * the form this screen has always opened on.
     */
    fun applyRecognized(foods: List<RecognizedFood>) {
        val seeded = foods.map { it.toAddEntryForm(form.mealType) }
        items = seeded
        recognized = seeded
        expandedIndex = 0.takeIf { seeded.size == 1 }
        // The notice is about the plate, not one row: one uncertain portion is a reason to read
        // all of them.
        confidence = if (foods.any { it.confidence == RecognitionConfidence.Low }) {
            RecognitionConfidence.Low
        } else {
            RecognitionConfidence.High
        }
        flow = CaptureFlow.Confirmation
    }

    fun updateItem(index: Int, form: AddEntryForm) {
        items = items.mapIndexed { i, existing -> if (i == index) form else existing }
    }

    fun removeItem(index: Int) {
        items = items.filterIndexed { i, _ -> i != index }
        expandedIndex = null
    }

    fun toggleExpanded(index: Int) {
        expandedIndex = if (expandedIndex == index) null else index
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

    /** Everything moves together, so changing the slot is never an edit to discard. */
    fun selectMealType(mealType: MealType) {
        form = form.copy(mealType = mealType)
        originalForm = originalForm.copy(mealType = mealType)
        items = items.map { it.copy(mealType = mealType) }
        recognized = recognized.map { it.copy(mealType = mealType) }
    }
}
