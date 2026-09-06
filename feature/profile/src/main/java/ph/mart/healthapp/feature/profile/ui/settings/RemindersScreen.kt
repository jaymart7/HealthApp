package ph.mart.healthapp.feature.profile.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.settings.components.RemindersGroupCard
import ph.mart.healthapp.feature.profile.ui.settings.components.RemindersPermissionBanner

/**
 * The eight reminder switches, on their own screen rather than in one card under a caption — eight
 * switches behind one heading was a screen wearing a card's clothes, and the permission refusal had
 * nowhere to explain itself but as red text under a wall of them.
 *
 * It shares [SettingsViewModel] with the screen that opens it: these are the same eight profile
 * fields, plus one piece of permission state the screen owns itself, and a host of its own would be
 * a second collector on the same row for nothing.
 */
@Composable
fun RemindersScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val uiState by viewModel.collectAsState()
    val context = LocalContext.current

    // Held in state and refreshed on resume, not read during composition. The permission can be
    // granted from system Settings — which the banner's own button opens — and a bare call in the
    // composable body is not a snapshot read, so the banner would go on claiming a block until
    // something unrelated happened to recompose this screen.
    var canPostNotifications by remember { mutableStateOf(context.canPostNotifications()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) canPostNotifications = context.canPostNotifications()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var pendingReminder by remember { mutableStateOf<ReminderKind?>(null) }

    // A reminder that can't post a notification is a lie, so the switch only goes on once the
    // permission is actually granted — a refusal leaves it off and re-shows the banner.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val kind = pendingReminder
        pendingReminder = null
        canPostNotifications = granted
        if (granted && kind != null) viewModel.setReminder(kind, true)
    }

    RemindersContent(
        profile = uiState.profile,
        blocked = !canPostNotifications,
        onToggle = { kind, enabled ->
            if (!enabled || canPostNotifications) {
                viewModel.setReminder(kind, enabled)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Always true once we're here — `canPostNotifications` is only ever false above
                // TIRAMISU — but stated at the point of use so the API-33 permission constant is
                // visibly guarded, which reading it out of a state variable no longer showed.
                pendingReminder = kind
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onOpenSystemSettings = {
            // Not the permission dialog: once it has been refused twice Android stops showing it,
            // and this app's notification page is the only remaining way back.
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        },
    )
}

@Composable
private fun RemindersContent(
    profile: Profile?,
    blocked: Boolean,
    onToggle: (ReminderKind, Boolean) -> Unit,
    onOpenSystemSettings: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        if (profile == null) return@Surface
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 32.dp),
        ) {
            if (blocked) RemindersPermissionBanner(onOpenSystemSettings = onOpenSystemSettings)
            ReminderGroup.entries.forEach { group ->
                RemindersGroupCard(
                    group = group,
                    enabled = profile::reminderEnabled,
                    onToggle = onToggle,
                )
            }
            Text(
                text = stringResource(R.string.profile_reminders_footer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
    }
}

private fun previewProfile() = Profile(
    sex = Sex.Female,
    age = 31,
    heightCm = 165.0,
    weightKg = 62.0,
    activityLevel = ActivityLevel.Moderate,
    goal = Goal.Lose,
)

@PreviewLightDark
@PreviewScreenSizes
@Composable
private fun RemindersScreenPreview() {
    AppTheme {
        RemindersContent(
            profile = previewProfile(),
            blocked = false,
            onToggle = { _, _ -> },
            onOpenSystemSettings = {},
        )
    }
}

/** The state the banner exists for — every switch below it is inert until Android is asked again. */
@PreviewLightDark
@Composable
private fun RemindersScreenBlockedPreview() {
    AppTheme {
        RemindersContent(
            profile = previewProfile(),
            blocked = true,
            onToggle = { _, _ -> },
            onOpenSystemSettings = {},
        )
    }
}
