package ph.mart.healthapp.feature.progress.ui.photo

import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.camera.rememberCameraCaptureController
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.displayUnitToKg
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.NumericStepperField
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.component.SheetDatePicker
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme

@Composable
fun AddPhotoSheet(onDismiss: () -> Unit, viewModel: AddPhotoViewModel = koinViewModel()) {
    val uiState by viewModel.collectAsState()
    val state = rememberAddPhotoState()
    viewModel.collectSideEffect { effect ->
        when (effect) {
            AddPhotoSideEffect.Saved -> onDismiss()
        }
    }

    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            @Suppress("DEPRECATION")
            state.photo = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            state.step = AddPhotoStep.Preview
        }
    }

    when (state.step) {
        AddPhotoStep.Capture -> AddPhotoCaptureScreen(
            onClose = { state.step = AddPhotoStep.Pick },
            onCaptured = { bitmap -> state.photo = bitmap; state.step = AddPhotoStep.Preview },
        )
        AddPhotoStep.Pick, AddPhotoStep.Preview -> AddPhotoContent(
            uiState = uiState,
            state = state,
            onDismiss = onDismiss,
            onTakePhoto = { state.step = AddPhotoStep.Capture },
            onChooseGallery = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onEvent = viewModel::handleEvent,
        )
    }
}

@Composable
private fun AddPhotoCaptureScreen(onClose: () -> Unit, onCaptured: (Bitmap) -> Unit) {
    val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(state = navigationState, onBackCompleted = onClose)

    val controller = rememberCameraCaptureController()
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        controller.Preview(modifier = Modifier.fillMaxSize())

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape),
        ) {
            Icon(imageVector = AppIcons.Close, contentDescription = "Close", tint = Color.White)
        }

        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
            Surface(
                onClick = { scope.launch { onCaptured(controller.capture()) } },
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(72.dp),
            ) {}
        }
    }
}

@Composable
private fun AddPhotoContent(
    uiState: AddPhotoUiState,
    state: AddPhotoState,
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseGallery: () -> Unit,
    onEvent: (AddPhotoEvent) -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss) {
        Text(
            text = "Add photo",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        val photo = state.photo
        if (state.step == AddPhotoStep.Pick || photo == null) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(label = "Take photo", onClick = onTakePhoto, modifier = Modifier.fillMaxWidth())
                SecondaryButton(label = "Choose from gallery", onClick = onChooseGallery, modifier = Modifier.fillMaxWidth())
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(
                    bitmap = photo.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)),
                )
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
                        SecondaryButton(label = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                        PrimaryButton(
                            label = "Save",
                            onClick = { onEvent(AddPhotoEvent.OnSave(photo, state.form)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightField(form: AddPhotoForm, unit: UnitSystem, onFormChange: (AddPhotoForm) -> Unit, modifier: Modifier = Modifier) {
    val step = 0.5
    NumericStepperField(
        label = "Weight (optional)",
        value = form.weightKg?.let { formatWeight(it.kgToDisplayUnit(unit)) } ?: "—",
        unitSuffix = unit.weightUnitLabel(),
        onIncrement = { onFormChange(form.copy(weightKg = ((form.weightKg ?: 0.0) + step.displayUnitToKg(unit)))) },
        onDecrement = { onFormChange(form.copy(weightKg = (((form.weightKg ?: step) - step.displayUnitToKg(unit)).coerceAtLeast(20.0)))) },
        modifier = modifier,
    )
}

private fun formatWeight(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString() else "%.1f".format(value)

@PreviewLightDark
@Composable
private fun AddPhotoSheetPickPreview() {
    AppTheme {
        AddPhotoContent(
            uiState = AddPhotoUiState(),
            state = AddPhotoState(),
            onDismiss = {},
            onTakePhoto = {},
            onChooseGallery = {},
            onEvent = {},
        )
    }
}
