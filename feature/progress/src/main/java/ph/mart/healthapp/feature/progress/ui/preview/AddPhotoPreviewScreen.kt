package ph.mart.healthapp.feature.progress.ui.preview

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.displayUnitToKg
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.designsystem.component.AppTopBar
import ph.mart.healthapp.core.designsystem.component.NumericStepperField
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.component.SheetDatePicker
import ph.mart.healthapp.core.designsystem.component.formatOneDecimal
import ph.mart.healthapp.core.designsystem.component.rememberBitmapFromFile
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R

/** The 3:4 the whole set is displayed at, from the grid tile to the timelapse frame. */
private const val PHOTO_ASPECT = 3f / 4f

/**
 * The shot, and the date and weight it gets filed under — the second half of adding a progress
 * photo, and a route of its own because the first half is a full-window viewfinder with nothing in
 * common with a scrolling form.
 *
 * [cachePath] is the staging file the capture route wrote. It is read back through
 * [rememberBitmapFromFile], the app's one decoder for a stored photo, which also means the shot
 * survives a rotation and a process death — neither of which the single-route flow managed.
 *
 * No back handler: [onRetake] *is* back, and popping this entry lands on the viewfinder by
 * construction. Leaving altogether is the pop after it, which is what [onExitFlow] does.
 */
@Composable
fun AddPhotoPreviewScreen(
    cachePath: String,
    onRetake: () -> Unit,
    onExitFlow: () -> Unit,
    viewModel: AddPhotoPreviewViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    val state = rememberAddPhotoPreviewState()
    viewModel.collectSideEffect { effect ->
        when (effect) {
            AddPhotoPreviewSideEffect.Saved -> onExitFlow()
        }
    }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        AddPhotoPreviewContent(
            uiState = uiState,
            state = state,
            photo = rememberBitmapFromFile(cachePath),
            onRetake = onRetake,
            onCancel = onExitFlow,
            onEvent = viewModel::handleEvent,
        )
    }
}

/**
 * [photo] is null while the decode is in flight — and stays null if the staging file has been
 * reclaimed. The bar and the fields draw either way, because a screen that renders nothing until
 * the decode lands is a screen with no reachable way back out of it; only the frame and Save wait
 * for the picture.
 */
@Composable
private fun AddPhotoPreviewContent(
    uiState: AddPhotoPreviewUiState,
    state: AddPhotoPreviewState,
    photo: ImageBitmap?,
    onRetake: () -> Unit,
    onCancel: () -> Unit,
    onEvent: (AddPhotoPreviewEvent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Zero insets: the Scaffold has already cleared the system bars for this route, and the
        // default would apply them a second time — every Progress subject page's reason, same shape.
        AppTopBar(
            title = stringResource(R.string.progress_photo_add),
            onBack = onRetake,
            windowInsets = WindowInsets(0),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                // The Scaffold's insets do not union the IME; the weight stepper's keyboard
                // avoidance is this line.
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            if (photo != null) {
                Image(
                    bitmap = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(PHOTO_ASPECT).clip(RoundedCornerShape(12.dp)),
                )
            }
            SheetDatePicker(
                showingCalendar = state.showingCalendar,
                onShowCalendar = { state.showingCalendar = true },
                onBackToFields = { state.showingCalendar = false },
                selectedDate = state.form.dateEpochDay,
                markedDates = uiState.photos.map { it.dateEpochDay }.toSet(),
                onSelectDate = { date ->
                    state.form = state.form.copy(dateEpochDay = date)
                    state.showingCalendar = false
                },
                selectedMinuteOfDay = state.form.minuteOfDay,
                onSelectTime = { state.form = state.form.copy(minuteOfDay = it) },
            ) {
                WeightField(
                    form = state.form,
                    unit = uiState.preferredUnit,
                    onFormChange = { state.form = it },
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            if (!state.showingCalendar) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryButton(label = stringResource(R.string.progress_cancel), onClick = onCancel, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        label = stringResource(R.string.progress_save),
                        onClick = { photo?.let { onEvent(AddPhotoPreviewEvent.OnSave(it.asAndroidBitmap(), state.form)) } },
                        enabled = photo != null,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeightField(form: AddPhotoPreviewForm, unit: UnitSystem, onFormChange: (AddPhotoPreviewForm) -> Unit, modifier: Modifier = Modifier) {
    val step = 0.5
    NumericStepperField(
        label = stringResource(R.string.progress_photo_weight),
        value = form.weightKg?.let { formatOneDecimal(it.kgToDisplayUnit(unit)) } ?: stringResource(R.string.progress_none),
        unitSuffix = unit.weightUnitLabel(),
        onIncrement = { onFormChange(form.copy(weightKg = ((form.weightKg ?: 0.0) + step.displayUnitToKg(unit)))) },
        onDecrement = { onFormChange(form.copy(weightKg = (((form.weightKg ?: step) - step.displayUnitToKg(unit)).coerceAtLeast(20.0)))) },
        // An emptied field is no weight recorded, not a weight of zero — this one is optional.
        onValueChange = { onFormChange(form.copy(weightKg = it.toDoubleOrNull()?.displayUnitToKg(unit))) },
        decimal = true,
        modifier = modifier,
    )
}

@PreviewLightDark
@Composable
private fun AddPhotoPreviewContentPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            AddPhotoPreviewContent(
                uiState = AddPhotoPreviewUiState(),
                state = AddPhotoPreviewState(),
                photo = previewPhoto(),
                onRetake = {},
                onCancel = {},
                onEvent = {},
            )
        }
    }
}

/** A stand-in for the shot, so the preview shows the frame the fields sit under. */
private fun previewPhoto(): ImageBitmap =
    Bitmap.createBitmap(3, 4, Bitmap.Config.ARGB_8888)
        .apply { eraseColor(android.graphics.Color.DKGRAY) }
        .asImageBitmap()
