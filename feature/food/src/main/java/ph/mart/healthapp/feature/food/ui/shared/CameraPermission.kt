package ph.mart.healthapp.feature.food.ui.shared

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat

/**
 * Whether Android will still raise the system prompt for [permission], or whether the only way
 * back is Settings.
 *
 * Only meaningful *after* a denial, which is the only place either camera flow asks: before the
 * first request `shouldShowRequestPermissionRationale` is false too, and reading it then would
 * send a first-time user to Settings for a prompt they were never shown.
 */
internal fun Context.permissionPermanentlyDenied(permission: String): Boolean {
    val activity = findActivity() ?: return false
    return !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}

/** The app's own page in system Settings — the only remaining path once the prompt is spent.
 * Without this, "Grant access" re-launches a prompt Android silently refuses to show, and the
 * screen is a dead end. */
internal fun Context.openAppSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
