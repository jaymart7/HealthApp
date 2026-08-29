package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * The Google Health in-app disclosure, shown full-screen *before* the OAuth consent prompt is
 * raised — both from the onboarding step and from Profile, which is why it lives here rather than
 * in either feature.
 *
 * The wording is not decoration. A Restricted-scope app has to name what it collects and what
 * each category is for, in the normal flow of the app, on a screen that carries nothing else —
 * not buried in a settings menu, a privacy policy, or bundled with unrelated permissions. One
 * bullet per requested scope, in the same order `HEALTH_SCOPES` requests them, so a reviewer can
 * put the two side by side.
 */
private data class Disclosure(val title: String, val body: String)

private val DISCLOSURES = listOf(
    Disclosure(
        "Workouts and activity",
        "Imported so logged exercise raises your daily calorie budget.",
    ),
    Disclosure(
        "Weight and body measurements",
        "Imported so your weight trend and goal date use your real weigh-ins.",
    ),
    Disclosure(
        "Sleep",
        "Imported to show last night's sleep on your home screen.",
    ),
    Disclosure(
        "Nutrition and hydration",
        "The meals and water you log in FitPulse are sent to Google Health.",
    ),
)

@Composable
fun HealthDisclosurePanel(
    onConnect: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String,
    modifier: Modifier = Modifier,
    connectEnabled: Boolean = true,
    message: String? = null,
    messageIsError: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = modifier.fillMaxWidth()) {
        Text(
            text = "FitPulse and Google Health",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "FitPulse collects health and fitness data from Google Health to show your " +
                "workouts and the calories they burned, track your weight trend, and show how " +
                "you slept. It also sends the meals and water you log to Google Health, so your " +
                "nutrition stays in one place.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DISCLOSURES.forEach { disclosure ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = disclosure.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = disclosure.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = "Your health data is stored on this device. FitPulse never sells or shares it, " +
                "and doesn't upload it anywhere except Google Health. You can disconnect and " +
                "delete it at any time from Profile.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = if (messageIsError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        PrimaryButton(
            label = "Connect Google Health",
            onClick = onConnect,
            enabled = connectEnabled,
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(label = dismissLabel, onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}

@PreviewLightDark
@Composable
private fun HealthDisclosurePanelPreview() {
    AppTheme {
        Surface {
            HealthDisclosurePanel(
                onConnect = {},
                onDismiss = {},
                dismissLabel = "Not now",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun HealthDisclosurePanelUnavailablePreview() {
    AppTheme {
        Surface {
            HealthDisclosurePanel(
                onConnect = {},
                onDismiss = {},
                dismissLabel = "Back",
                connectEnabled = false,
                message = "Google Health needs Google Play services and a signed-in Google account.",
                messageIsError = true,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
