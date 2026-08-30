package ph.mart.healthapp.feature.profile.ui.profile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * The screen owns the picker Uri and the file IO — the ViewModel only ever sees a JSON string.
 * These three are what that ownership costs, kept out of ProfileScreen.kt so the composable file
 * holds composables.
 */
internal fun Context.canPostNotifications(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

internal fun Context.writeText(uri: Uri, text: String): Result<Unit> = runCatching {
    contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
        ?: error("No output stream")
}

internal fun Context.readText(uri: Uri): Result<String> = runCatching {
    contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        ?: error("No input stream")
}
