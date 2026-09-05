package ph.mart.healthapp.core.designsystem.component

import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.R
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
private data class Disclosure(@StringRes val title: Int, @StringRes val body: Int)

private val DISCLOSURES = listOf(
    Disclosure(R.string.ds_health_scope_activity, R.string.ds_health_scope_activity_body),
    Disclosure(R.string.ds_health_scope_body, R.string.ds_health_scope_body_body),
    Disclosure(R.string.ds_health_scope_sleep, R.string.ds_health_scope_sleep_body),
    Disclosure(R.string.ds_health_scope_nutrition, R.string.ds_health_scope_nutrition_body),
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
            text = stringResource(R.string.ds_health_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.ds_health_intro),
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
                                text = stringResource(disclosure.title),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(disclosure.body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.ds_health_storage),
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
            label = stringResource(R.string.ds_health_connect),
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
