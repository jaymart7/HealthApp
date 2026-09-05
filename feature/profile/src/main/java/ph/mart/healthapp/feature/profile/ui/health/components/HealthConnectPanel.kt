package ph.mart.healthapp.feature.profile.ui.health.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.health.CONNECT_PERMISSIONS
import ph.mart.healthapp.core.data.health.HealthConnectState
import ph.mart.healthapp.core.data.health.HealthMetric
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R

/**
 * Health Connect, drawn *above* the Google Health panel because it is the provider that wins —
 * see `cloudMetrics`. It carries its own rationale copy rather than extending
 * `HealthDisclosurePanel`: that component's bullets are written against `HEALTH_SCOPES` in the
 * order the cloud requests them, and this screen is the target of Health Connect's own "why does
 * this app want my data?" intent, so each provider gets a passage a reviewer can read on its own.
 *
 * Four states, four affordances. [HealthConnectState.Unsupported] draws **nothing at all**: a card
 * explaining an absence the user can do nothing about is the supplements-card rule, one screen over.
 */
@Composable
internal fun HealthConnectPanel(
    state: HealthConnectState,
    /** What FitPulse would ask for now — see `HealthConnectionUiState.connectRequests`. */
    requests: Set<String>,
    busy: Boolean,
    onAllow: () -> Unit,
    onOpenPlayStore: () -> Unit,
) {
    if (state is HealthConnectState.Unsupported || state is HealthConnectState.Checking) return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.profile_connect_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.profile_connect_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when (state) {
            HealthConnectState.UpdateRequired -> {
                Text(
                    text = stringResource(R.string.profile_connect_needs_update),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrimaryButton(
                    label = stringResource(R.string.profile_connect_update),
                    onClick = onOpenPlayStore,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is HealthConnectState.Available -> {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(
                                if (state.granted.isEmpty()) {
                                    R.string.profile_connect_not_allowed
                                } else {
                                    R.string.profile_connect_allowed
                                },
                            ),
                            style = MaterialTheme.typography.bodyMedium
                                .copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        // Every metric we ask for, ticked or not — a partial grant is ordinary
                        // here, and the list is the only place the user can see which of them they
                        // allowed. One already granted is listed whether or not we still ask for
                        // it, so turning cycle tracking off never hides a permission still held.
                        HealthMetric.entries
                            .filter { it.permission in requests || it in state.granted }
                            .forEach { metric ->
                                MetricRow(metric = metric, granted = metric in state.granted)
                            }
                    }
                }
                PrimaryButton(
                    label = stringResource(
                        if (state.granted.isEmpty()) R.string.profile_connect_allow else R.string.profile_connect_change,
                    ),
                    onClick = onAllow,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            HealthConnectState.Checking, HealthConnectState.Unsupported -> Unit
        }
    }
}

@Composable
private fun MetricRow(metric: HealthMetric, granted: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        // A tick or a dash, never colour alone: the difference has to survive being read by
        // someone who can't tell primary from onSurfaceVariant — the dashed-outline rule the
        // Progress cards follow.
        Text(
            text = if (granted) "✓" else "—",
            style = MaterialTheme.typography.bodySmall,
            color = if (granted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Text(
            text = stringResource(metric.label),
            style = MaterialTheme.typography.bodySmall,
            color = if (granted) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/** What each metric is called on screen. The enum is `:core:data`'s and carries no copy. */
@get:StringRes
private val HealthMetric.label: Int
    get() = when (this) {
        HealthMetric.Exercise -> R.string.profile_metric_exercise
        HealthMetric.Weight -> R.string.profile_metric_weight
        HealthMetric.Sleep -> R.string.profile_metric_sleep
        HealthMetric.Steps -> R.string.profile_metric_steps
        HealthMetric.Heart -> R.string.profile_metric_heart
        HealthMetric.BloodPressure -> R.string.profile_metric_blood_pressure
        HealthMetric.Menstruation -> R.string.profile_metric_menstruation
    }

@PreviewLightDark
@Composable
private fun HealthConnectPanelNotAllowedPreview() {
    AppTheme {
        HealthConnectPanel(
            state = HealthConnectState.Available(granted = emptySet()),
            requests = CONNECT_PERMISSIONS,
            busy = false,
            onAllow = {},
            onOpenPlayStore = {},
        )
    }
}

/** The ordinary case: some allowed, some not. */
@PreviewLightDark
@Composable
private fun HealthConnectPanelPartialPreview() {
    AppTheme {
        HealthConnectPanel(
            state = HealthConnectState.Available(
                granted = setOf(HealthMetric.Steps, HealthMetric.Sleep, HealthMetric.Exercise),
            ),
            // Cycle tracking off: the period row is absent, and would be listed if it were granted.
            requests = CONNECT_PERMISSIONS - HealthMetric.Menstruation.permission,
            busy = false,
            onAllow = {},
            onOpenPlayStore = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun HealthConnectPanelUpdateRequiredPreview() {
    AppTheme {
        HealthConnectPanel(
            state = HealthConnectState.UpdateRequired,
            requests = CONNECT_PERMISSIONS,
            busy = false,
            onAllow = {},
            onOpenPlayStore = {},
        )
    }
}
