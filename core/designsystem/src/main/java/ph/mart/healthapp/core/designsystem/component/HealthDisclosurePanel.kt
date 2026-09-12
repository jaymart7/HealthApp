package ph.mart.healthapp.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.R
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * The Google Health in-app disclosure, shown full-screen *before* the OAuth consent prompt is
 * raised — both from the onboarding step and from Profile, which is why it lives here rather than
 * in either feature.
 *
 * The wording is not decoration. A Restricted-scope app has to name what it collects and what each
 * category is for, in the normal flow of the app, on a screen that carries nothing else — not
 * buried in a settings menu, a privacy policy, or bundled with unrelated permissions. **One row
 * per requested scope, in the same order `HEALTH_SCOPES` requests them**, so a reviewer can put
 * the two side by side. The fourth is `nutrition.writeonly` and is the one that goes the other
 * way, which is why the assurances say "only the meals and water you log" rather than "read-only":
 * this app does write back, and a disclosure that claimed otherwise would be false.
 *
 * A glyph and five words per scope rather than a paragraph and four two-line bullets: a wall of
 * grey prose above two buttons is what a consent form looks like, and consent forms teach people
 * to press the fast button without reading.
 */
private data class Scope(val icon: ImageVector, @StringRes val title: Int, @StringRes val line: Int)

private val SCOPES = listOf(
    Scope(AppIcons.Run, R.string.ds_health_scope_activity, R.string.ds_health_scope_activity_line),
    Scope(AppIcons.Weight, R.string.ds_health_scope_body, R.string.ds_health_scope_body_line),
    Scope(AppIcons.Bedtime, R.string.ds_health_scope_sleep, R.string.ds_health_scope_sleep_line),
    Scope(AppIcons.Water, R.string.ds_health_scope_nutrition, R.string.ds_health_scope_nutrition_line),
)

/** The three promises the old small print made in one 40-word sentence, one line each so they can
 * be read in the four seconds someone actually gives this screen. */
private val ASSURANCES = listOf(
    AppIcons.Lock to R.string.ds_health_assure_write,
    AppIcons.Smartphone to R.string.ds_health_assure_device,
    AppIcons.Undo to R.string.ds_health_assure_undo,
)

/**
 * [title] is null where the screen around the panel already carries one — onboarding's step
 * chrome draws its own headline, Profile's does not.
 *
 * [declined] is the user having seen Google's consent sheet and said no. It swaps the two
 * actions' weight: continuing becomes the filled button and retrying drops to a text button,
 * because the question has been answered and the screen's job is now to get out of the way
 * without implying they got it wrong. [connectEnabled] false is the other half of that — the
 * device cannot offer the grant at all, which is not the user's mistake either, so neither state
 * uses an error colour anywhere.
 */
@Composable
fun HealthDisclosurePanel(
    onConnect: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String,
    modifier: Modifier = Modifier,
    title: String? = stringResource(R.string.ds_health_title),
    connectEnabled: Boolean = true,
    declined: Boolean = false,
    message: String? = null,
    messageIsError: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.ds_health_reads),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SCOPES.forEach { scope ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = scope.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp),
                    )
                    Column {
                        Text(
                            text = stringResource(scope.title),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(scope.line),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(16.dp),
        ) {
            ASSURANCES.forEach { (icon, text) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (message != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(12.dp),
            ) {
                Icon(
                    // The device lacking Play services is a fact about the device; a decline is an
                    // answer. Neither is a failure, so neither gets `error` or `errorContainer`.
                    imageVector = if (messageIsError) AppIcons.Info else AppIcons.LinkOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // "Skip for now" implies a choice the user still has. Once the grant is impossible or
        // already refused, the only honest label on the way out is "Continue" — the panel
        // substitutes it rather than making both callers work it out.
        val dismiss = if (connectEnabled && !declined) {
            dismissLabel
        } else {
            stringResource(R.string.ds_health_continue)
        }
        val connect = stringResource(R.string.ds_health_connect)
        if (declined) {
            PrimaryButton(label = dismiss, onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            TextButton(label = connect, onClick = onConnect, modifier = Modifier.fillMaxWidth())
        } else {
            PrimaryButton(
                label = connect,
                onClick = onConnect,
                enabled = connectEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(label = dismiss, onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
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

/** The two actions swapped: the user has already answered, so continuing is the filled button. */
@PreviewLightDark
@Composable
private fun HealthDisclosurePanelDeclinedPreview() {
    AppTheme {
        Surface {
            HealthDisclosurePanel(
                onConnect = {},
                onDismiss = {},
                dismissLabel = "Skip for now",
                declined = true,
                message = "Not connected — nothing was shared. You can do this later in Profile.",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
