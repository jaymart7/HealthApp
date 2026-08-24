package ph.mart.healthapp.feature.progress.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
internal fun rememberAddPhotoState(): AddPhotoState = remember { AddPhotoState() }

/** Plain `remember`, not `rememberSaveable` — same justified exception
 * [ph.mart.healthapp.feature.food.ui.PhotoCaptureScreenState] already established for holding a
 * non-parcelable [Bitmap]; losing an in-progress photo pick on process death is acceptable.
 * ponytail: process death loses the in-progress capture; add a Saver-backed byte[] snapshot if
 * that's ever reported as a real problem. */
internal class AddPhotoState(
    step: AddPhotoStep = AddPhotoStep.Pick,
    photo: Bitmap? = null,
    form: AddPhotoForm = AddPhotoForm(),
    showingCalendar: Boolean = false,
) {
    var step: AddPhotoStep by mutableStateOf(step)
    var photo: Bitmap? by mutableStateOf(photo)
    var form: AddPhotoForm by mutableStateOf(form)
    var showingCalendar: Boolean by mutableStateOf(showingCalendar)
}
