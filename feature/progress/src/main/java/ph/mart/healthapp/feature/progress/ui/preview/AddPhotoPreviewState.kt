package ph.mart.healthapp.feature.progress.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
internal fun rememberAddPhotoPreviewState(): AddPhotoPreviewState =
    rememberSaveable(saver = AddPhotoPreviewState.Saver) { AddPhotoPreviewState() }

/**
 * `rememberSaveable`, where the flow this replaced could only manage `remember`: the non-parcelable
 * [android.graphics.Bitmap] that forced that is out of here now, riding the route as a path, and
 * everything left is a Long, a Double and a Boolean. A rotation on the form keeps the date and the
 * weight, which it did not before.
 */
internal class AddPhotoPreviewState(
    form: AddPhotoPreviewForm = AddPhotoPreviewForm(),
    showingCalendar: Boolean = false,
) {
    var form: AddPhotoPreviewForm by mutableStateOf(form)
    var showingCalendar: Boolean by mutableStateOf(showingCalendar)

    companion object {
        val Saver: Saver<AddPhotoPreviewState, Any> = listSaver(
            save = { listOf(it.form.dateEpochDay, it.form.weightKg, it.showingCalendar) },
            restore = {
                AddPhotoPreviewState(
                    form = AddPhotoPreviewForm(
                        dateEpochDay = it[0] as Long,
                        weightKg = it[1] as Double?,
                    ),
                    showingCalendar = it[2] as Boolean,
                )
            },
        )
    }
}
