package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * Mascot/icon + heading + optional body + optional [content] + up to two actions. Used by every
 * empty/status state: Home day-one, empty diary, empty progress photos, and the photo flow's
 * Retry/NoFood/Offline states (which pass a photo or search field in [icon]/[actions] instead of
 * the mascot default).
 *
 * [content] is a full-width slot between the body and the actions, for a state that has something
 * to *show* as well as something to say — talk-to-log's dead ends, which quote the sentence back so
 * the words the screen is about are the words on it. A caller that passes none gets exactly the
 * column it always had.
 */
@Composable
fun FullScreenState(
    icon: @Composable () -> Unit,
    heading: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(if (actions != null) 24.dp else 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        icon()
        Text(
            text = heading,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (body != null) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (content != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
        if (actions != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = actions,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FullScreenStatePreview() {
    AppTheme {
        Surface {
            FullScreenState(
                icon = { MascotAvatar(state = MascotState.Sleepy, size = 64.dp) },
                heading = "No progress photos yet",
                body = "Add your first photo to start tracking changes over time.",
                actions = {
                    PrimaryButton(label = "Add photo", onClick = {}, modifier = Modifier.fillMaxWidth())
                    SecondaryButton(label = "Not now", onClick = {}, modifier = Modifier.fillMaxWidth())
                },
            )
        }
    }
}
