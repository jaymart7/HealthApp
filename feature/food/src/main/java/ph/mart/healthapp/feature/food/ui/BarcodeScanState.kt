package ph.mart.healthapp.feature.food.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.ScannedProduct

enum class ScanFlow { Scanning, LookingUp, Confirmation, NotFound, Offline, PermissionDenied }

@Composable
internal fun rememberBarcodeScanScreen(): BarcodeScanScreenState = remember { BarcodeScanScreenState() }

/** Screen-local flow/UI state, same shape and same plain-`remember` reasoning as
 * [PhotoCaptureScreenState] — a half-finished scan isn't meaningful to restore across process
 * death. */
internal class BarcodeScanScreenState(
    flow: ScanFlow = ScanFlow.Scanning,
    form: AddEntryForm = AddEntryForm(mealType = defaultMealTypeForNow()),
    originalForm: AddEntryForm = form,
    showDiscardConfirm: Boolean = false,
) {
    var flow: ScanFlow by mutableStateOf(flow)
    var form: AddEntryForm by mutableStateOf(form)
    var originalForm: AddEntryForm by mutableStateOf(originalForm)
    var showDiscardConfirm: Boolean by mutableStateOf(showDiscardConfirm)

    val isDirty: Boolean get() = form != originalForm

    fun applyProduct(product: ScannedProduct) {
        val seeded = product.toAddEntryForm(form.mealType)
        form = seeded
        originalForm = seeded
        flow = ScanFlow.Confirmation
    }

    /** The not-found path: keep the chosen meal, clear anything a previous scan left behind. */
    fun startManualEntry() {
        val blank = AddEntryForm(mealType = form.mealType)
        form = blank
        originalForm = blank
        flow = ScanFlow.Confirmation
    }

    fun rescan() {
        val blank = AddEntryForm(mealType = form.mealType)
        form = blank
        originalForm = blank
        flow = ScanFlow.Scanning
    }

    fun selectMealType(mealType: MealType) {
        form = form.copy(mealType = mealType)
        originalForm = originalForm.copy(mealType = mealType)
    }
}
