package ph.mart.healthapp.feature.progress.ui.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.R

/** The photo sits on this rather than on `surface`, so a light shot has an edge and a dark one has
 * somewhere to end. One step up the container ramp is enough; a black stage would be a theatre. */
@Composable
internal fun photoStageColor() = MaterialTheme.colorScheme.surfaceContainerHighest

/**
 * The ground and the chrome both photo screens share: a full-bleed stage with the picture centred
 * on it and the one deliberate action floating at its top corner.
 *
 * The chrome **floats over** the stage rather than stacking above the photo. Share is all of it
 * now: both callers are routes, so the way out is the toolbar's back arrow, and the corner close
 * button this used to draw would have been a second one an inch below it. Neither route wears the
 * tab's bottom bar or its docked FAB either, so the content column runs to the bottom of the
 * window rather than stopping short of a FAB that is not there.
 *
 * A frame in [content] is expected to take `Modifier.weight(1f, fill = false)` and size itself
 * height-first — a 3:4 photo filling the width of a landscape phone or a tablet is taller than the
 * window it is in, and the controls under it would be off the bottom of the screen.
 */
@Composable
internal fun PhotoOverlayStage(
    modifier: Modifier = Modifier,
    onShare: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(color = photoStageColor(), modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                onShare?.let { SharePill(onClick = it) }
            }
        }
    }
}

@Composable
private fun SharePill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 2.dp,
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(imageVector = AppIcons.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
                text = stringResource(R.string.progress_share),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PhotoOverlayStagePreview() {
    AppTheme {
        PhotoOverlayStage(onShare = {}) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Box(modifier = Modifier.fillMaxWidth().size(240.dp))
            }
        }
    }
}
