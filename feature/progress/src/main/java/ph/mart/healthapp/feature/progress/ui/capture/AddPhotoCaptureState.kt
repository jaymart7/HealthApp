package ph.mart.healthapp.feature.progress.ui.capture

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** Reads the grant once, on the way in — the launcher below is what changes it afterwards. */
@Composable
internal fun rememberAddPhotoCaptureState(): AddPhotoCaptureState {
    val context = LocalContext.current
    return remember {
        AddPhotoCaptureState(
            hasCameraPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
}

/**
 * Two flags, and they are not the same question. [hasCameraPermission] is what the sensor will
 * actually do; [permissionRefused] is whether the user has said no *yet*, and it starts false even
 * with the grant missing so the prompt goes up over the viewfinder rather than over an explanation
 * nobody has earned. Only a refusal swaps the screen.
 *
 * Plain `remember` is enough: both flags are re-derived from the system on the way back in, so
 * there is nothing here a restore could get wrong.
 */
internal class AddPhotoCaptureState(hasCameraPermission: Boolean = false) {
    var hasCameraPermission: Boolean by mutableStateOf(hasCameraPermission)
    var permissionRefused: Boolean by mutableStateOf(false)
}
