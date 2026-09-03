package ph.mart.healthapp.feature.food.ui.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * What a model call looks like while it is in flight: the mascot, an indeterminate bar and one
 * honest line. `AnalyzingScreen`'s rule — nothing rotates, and nothing promises progress the call
 * cannot report.
 *
 * In `shared/` because the ideas flow and the voice flow both draw it. `AnalyzingScreen` itself is
 * not this: it renders over the photo it is analysing, which is the whole of its layout.
 */
@Composable
internal fun ThinkingState(line: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        MascotAvatar(state = MascotState.Thinking, size = 88.dp)
        LinearProgressIndicator(modifier = Modifier.size(width = 200.dp, height = 4.dp))
        Text(
            text = line,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@PreviewLightDark
@Composable
private fun ThinkingStatePreview() {
    AppTheme {
        Surface { ThinkingState(line = "Working out what that adds up to…") }
    }
}
