package ph.mart.healthapp.feature.progress.ui.pressure

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.data.bloodpressure.DIASTOLIC_RANGE
import ph.mart.healthapp.core.data.bloodpressure.PULSE_RANGE
import ph.mart.healthapp.core.data.bloodpressure.SYSTOLIC_RANGE
import ph.mart.healthapp.core.data.bloodpressure.categoryOf
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.NumericStepperField
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R

@Composable
fun LogBloodPressureSheet(
    onDismiss: () -> Unit,
    viewModel: BloodPressureViewModel = koinViewModel(),
) {
    val state = rememberLogBloodPressureState()
    viewModel.collectSideEffect { effect ->
        when (effect) {
            BloodPressureSideEffect.Saved -> onDismiss()
        }
    }
    LogBloodPressureContent(state = state, onDismiss = onDismiss, onEvent = viewModel::handleEvent)
}

/**
 * No date or time picker: a reading is stamped with the moment Save is tapped. Transcribing a
 * paper log is not the workflow a backdated weigh-in is — add a `SheetDatePicker` here if it ever
 * becomes one.
 *
 * All three figures are typable, not just steppable. A stepper-only 128 costs 128 taps, which is
 * the reason `StepperValueField` exists at all; the +/- buttons are for nudging a figure that is
 * already about right.
 */
@Composable
private fun LogBloodPressureContent(
    state: LogBloodPressureState,
    onDismiss: () -> Unit,
    onEvent: (BloodPressureEvent) -> Unit,
) {
    val form = state.form
    AppBottomSheet(onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.progress_bp_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NumericStepperField(
                label = stringResource(R.string.progress_bp_systolic),
                value = form.systolic.toString(),
                unitSuffix = stringResource(R.string.progress_bp_mmhg),
                onIncrement = { state.form = form.copy(systolic = (form.systolic + 1).coerceIn(SYSTOLIC_RANGE)) },
                onDecrement = { state.form = form.copy(systolic = (form.systolic - 1).coerceIn(SYSTOLIC_RANGE)) },
                onValueChange = { text -> state.form = form.copy(systolic = text.toIntOrNull() ?: 0) },
                error = stringResource(R.string.progress_bp_error).takeIf { !form.isValid },
            )
            NumericStepperField(
                label = stringResource(R.string.progress_bp_diastolic),
                value = form.diastolic.toString(),
                unitSuffix = stringResource(R.string.progress_bp_mmhg),
                onIncrement = { state.form = form.copy(diastolic = (form.diastolic + 1).coerceIn(DIASTOLIC_RANGE)) },
                onDecrement = { state.form = form.copy(diastolic = (form.diastolic - 1).coerceIn(DIASTOLIC_RANGE)) },
                onValueChange = { text -> state.form = form.copy(diastolic = text.toIntOrNull() ?: 0) },
            )
            // Optional: 0 is "the cuff didn't show one, or I didn't type it", never a pulse of zero.
            NumericStepperField(
                label = stringResource(R.string.progress_bp_pulse),
                value = if (form.pulseBpm > 0) form.pulseBpm.toString() else "",
                unitSuffix = stringResource(R.string.progress_bp_bpm),
                onIncrement = { state.form = form.copy(pulseBpm = (form.pulseBpm + 1).coerceIn(PULSE_RANGE)) },
                onDecrement = { state.form = form.copy(pulseBpm = (form.pulseBpm - 1).coerceAtLeast(0)) },
                onValueChange = { text -> state.form = form.copy(pulseBpm = text.toIntOrNull() ?: 0) },
            )

            if (form.isValid) {
                Text(
                    text = stringResource(categoryOf(form.systolic, form.diastolic).label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryButton(label = stringResource(R.string.progress_cancel), onClick = onDismiss, modifier = Modifier.weight(1f))
                PrimaryButton(
                    label = stringResource(R.string.progress_save),
                    onClick = { onEvent(BloodPressureEvent.OnSave(form)) },
                    enabled = form.isValid,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** A sheet renders invisible in isolation, so the preview supplies its own scrim. */
@PreviewLightDark
@Composable
private fun LogBloodPressureSheetPreview() {
    AppTheme {
        Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.32f)).padding(top = 48.dp)) {
            LogBloodPressureContent(
                state = LogBloodPressureState(BloodPressureForm(systolic = 128, diastolic = 82, pulseBpm = 71)),
                onDismiss = {},
                onEvent = {},
            )
        }
    }
}

/** Transposed numbers: Save is off and the systolic field says why. */
@PreviewLightDark
@Composable
private fun LogBloodPressureSheetInvalidPreview() {
    AppTheme {
        Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.32f)).padding(top = 48.dp)) {
            LogBloodPressureContent(
                state = LogBloodPressureState(BloodPressureForm(systolic = 82, diastolic = 128)),
                onDismiss = {},
                onEvent = {},
            )
        }
    }
}
