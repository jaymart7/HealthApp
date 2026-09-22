package ph.mart.healthapp.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.ktx.AppUpdateResult
import com.google.android.play.core.ktx.requestUpdateFlow
import kotlinx.coroutines.flow.catch
import ph.mart.healthapp.R

/** Play hands the flow's result back through `onActivityResult`, which nothing here implements:
 * the flow re-emits the state either way, so the code is arbitrary and exists only because
 * `startFlexibleUpdate` demands one. */
private const val UPDATE_REQUEST_CODE = 1801

/**
 * One Play check per launch, and the restart it eventually earns.
 *
 * Flexible rather than immediate: the download runs behind the app the user is already using, and
 * nothing is blocked until they tap Restart. Play draws the "Update available" dialog itself, so
 * the only copy this app owns is the snackbar at the end.
 *
 * Silent on every failure, which is most installs: a debug build, a sideload, a device with no
 * Play Store and an offline one all raise `InstallException`, and there is nothing to offer in any
 * of those cases. The same degrade rule the AI surfaces follow — an unavailable service says
 * nothing rather than apologising.
 *
 * Draws nothing of its own, which is why it takes the shell's [SnackbarHostState] rather than
 * raising a host, and why it has no preview.
 */
@Composable
internal fun AppUpdatePrompt(snackbarHostState: SnackbarHostState) {
    // Null only in a preview or a test context, neither of which has a Play install to update.
    val activity = LocalActivity.current ?: return
    // Resolved here rather than inside the coroutine: a suspend body cannot read a resource.
    val ready = stringResource(R.string.app_update_ready)
    val restart = stringResource(R.string.app_update_restart)
    // Keyed on the Activity, so the dialog is offered once per launch and not once per
    // recomposition.
    LaunchedEffect(activity) {
        AppUpdateManagerFactory.create(activity).requestUpdateFlow()
            .catch { }
            .collect { result ->
                when (result) {
                    is AppUpdateResult.Available ->
                        result.startFlexibleUpdate(activity, UPDATE_REQUEST_CODE)

                    is AppUpdateResult.Downloaded -> {
                        // Indefinite with a dismiss: an update the user has already paid the
                        // download for should not slide away while they are mid-scroll, and a
                        // dismissed one comes back on the next launch.
                        val tapped = snackbarHostState.showSnackbar(
                            message = ready,
                            actionLabel = restart,
                            withDismissAction = true,
                            duration = SnackbarDuration.Indefinite,
                        ) == SnackbarResult.ActionPerformed
                        if (tapped) result.completeUpdate()
                    }

                    // NotAvailable and InProgress: nothing to say.
                    else -> Unit
                }
            }
    }
}
