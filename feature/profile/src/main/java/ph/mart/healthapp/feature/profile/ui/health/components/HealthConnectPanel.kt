package ph.mart.healthapp.feature.profile.ui.health.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.health.HealthConnectState
import ph.mart.healthapp.core.data.health.HealthMetric
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme

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
    busy: Boolean,
    onAllow: () -> Unit,
    onOpenPlayStore: () -> Unit,
) {
    if (state is HealthConnectState.Unsupported || state is HealthConnectState.Checking) return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Health Connect",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "FitPulse reads your workouts, weight, sleep, steps, heart rate and blood " +
                "pressure from Health Connect, on your phone — no account and no internet " +
                "connection. It is used to raise your calorie budget, track your weight trend and " +
                "show your sleep, heart rate and readings. FitPulse writes nothing back.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when (state) {
            HealthConnectState.UpdateRequired -> {
                Text(
                    text = "Health Connect needs updating before FitPulse can read from it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrimaryButton(
                    label = "Update Health Connect",
                    onClick = onOpenPlayStore,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is HealthConnectState.Available -> {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (state.granted.isEmpty()) "Not allowed yet" else "Allowed",
                            style = MaterialTheme.typography.bodyMedium
                                .copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        // Every metric, ticked or not — a partial grant is ordinary here, and the
                        // list is the only place the user can see which five of six they allowed.
                        HealthMetric.entries.forEach { metric ->
                            MetricRow(metric = metric, granted = metric in state.granted)
                        }
                    }
                }
                PrimaryButton(
                    label = if (state.granted.isEmpty()) "Allow" else "Change what FitPulse reads",
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
            text = metric.label,
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
private val HealthMetric.label: String
    get() = when (this) {
        HealthMetric.Exercise -> "Workouts"
        HealthMetric.Weight -> "Weight"
        HealthMetric.Sleep -> "Sleep"
        HealthMetric.Steps -> "Steps"
        HealthMetric.Heart -> "Heart rate"
        HealthMetric.BloodPressure -> "Blood pressure"
    }

@PreviewLightDark
@Composable
private fun HealthConnectPanelNotAllowedPreview() {
    AppTheme {
        HealthConnectPanel(
            state = HealthConnectState.Available(granted = emptySet()),
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
            busy = false,
            onAllow = {},
            onOpenPlayStore = {},
        )
    }
}
