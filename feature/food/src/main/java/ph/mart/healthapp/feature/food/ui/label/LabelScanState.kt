package ph.mart.healthapp.feature.food.ui.label

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ph.mart.healthapp.core.data.food.LabelBasis
import ph.mart.healthapp.core.data.food.LabelReading
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.feature.food.ui.barcode.BarcodeScanScreenState
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm
import ph.mart.healthapp.feature.food.ui.shared.defaultMealTypeForNow
import ph.mart.healthapp.feature.food.ui.shared.toAddEntryForm

/**
 * [Unreadable] is the photo with no panel in it and [Retry] is the call that didn't work — the two
 * are separate for the reason `LabelScanResult` keeps them apart, and they say different things.
 */
enum class LabelFlow { Capture, Reading, Confirmation, Unreadable, Retry, Offline, PermissionDenied }

@Composable
internal fun rememberLabelScanScreen(): LabelScanScreenState = remember { LabelScanScreenState() }

/** Screen-local flow/UI state, same shape and same plain-`remember` reasoning as
 * [BarcodeScanScreenState] — a half-finished scan isn't meaningful to restore across process
 * death. */
internal class LabelScanScreenState(
    flow: LabelFlow = LabelFlow.Capture,
    form: AddEntryForm = AddEntryForm(mealType = defaultMealTypeForNow()),
    originalForm: AddEntryForm = form,
) {
    var flow: LabelFlow by mutableStateOf(flow)
    var form: AddEntryForm by mutableStateOf(form)
    var originalForm: AddEntryForm by mutableStateOf(originalForm)

    /**
     * Which column of the panel the form was seeded from, and null when nobody read a panel at all.
     *
     * One field rather than a boolean beside an enum: **non-null is exactly "read from the label"**,
     * which is what puts the AI chip on the confirmation and the readout under the macros, while the
     * value itself is what the caveat names. Kept here rather than re-derived from the form's
     * portion, because the user is free to change that portion and the panel still said what it
     * said.
     */
    var readBasis: LabelBasis? by mutableStateOf(null)

    /** What the seeded figures are figures for, so the portion control's "×1.5" stays true when the
     * panel declared a serving rather than 100 g. */
    var seedAmount: Double by mutableStateOf(100.0)

    /** Off until asked for, like the add-entry sheet's. It survives an edit to the form, which is
     * the point: the user turns it on while checking the figures, not after. */
    var saveMyFood: Boolean by mutableStateOf(false)

    /** What a confirmed discard does — retake, or leave the flow. Non-null exactly while the dialog
     * is up, a lambda for the same reason [BarcodeScanScreenState.pendingDiscard] is one. */
    var pendingDiscard: (() -> Unit)? by mutableStateOf(null)

    val isDirty: Boolean get() = form != originalForm

    fun applyReading(reading: LabelReading) {
        val seeded = reading.toAddEntryForm(form.mealType)
        form = seeded
        originalForm = seeded
        readBasis = reading.basis
        seedAmount = seeded.portionAmount
        flow = LabelFlow.Confirmation
    }

    /** Every dead end's way forward: keep the chosen meal, claim nothing else. */
    fun startManualEntry() {
        val blank = AddEntryForm(mealType = form.mealType)
        form = blank
        originalForm = blank
        readBasis = null
        seedAmount = blank.portionAmount
        flow = LabelFlow.Confirmation
    }

    fun recapture() {
        val blank = AddEntryForm(mealType = form.mealType)
        form = blank
        originalForm = blank
        readBasis = null
        seedAmount = blank.portionAmount
        flow = LabelFlow.Capture
    }

    fun selectMealType(mealType: MealType) {
        form = form.copy(mealType = mealType)
        originalForm = originalForm.copy(mealType = mealType)
    }
}
