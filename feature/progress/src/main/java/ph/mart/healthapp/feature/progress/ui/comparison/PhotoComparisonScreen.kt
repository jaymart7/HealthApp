package ph.mart.healthapp.feature.progress.ui.comparison

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.comparison.components.ComparisonSlider
import ph.mart.healthapp.feature.progress.ui.shared.components.SharePhotoStripSheet

/**
 * Two shots side by side under a divider you drag — the question a grid of thumbnails can't answer.
 *
 * A full-screen overlay inside the Progress tab rather than a route, which is why it wires its own
 * `NavigationBackHandler`: without one, back out of a comparison left the tab entirely instead of
 * clearing the selection. [selectedIds] is the grid's selection handed down; the container turns it
 * into a pair, and nothing is drawn until exactly two of them resolve.
 */
@Composable
internal fun PhotoComparisonScreen(
    selectedIds: List<Long>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ComparisonViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    LaunchedEffect(selectedIds) { viewModel.handleEvent(ComparisonEvent.OnSelect(selectedIds)) }
    val pair = uiState.pair ?: return
    PhotoComparisonContent(pair = pair, unit = uiState.unit, onClose = onClose, modifier = modifier)
}

@Composable
private fun PhotoComparisonContent(
    pair: ComparisonPair,
    unit: UnitSystem,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    state: ComparisonState = rememberComparisonState(),
) {
    // A full-screen overlay, not a route: back has to clear the selection rather than leave the
    // Progress tab entirely.
    val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(state = navigationState, onBackCompleted = onClose)

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = stringResource(R.string.progress_compare_title), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            ComparisonSlider(pair = pair, state = state)
            pair.weightDeltaKg?.let { delta ->
                Text(
                    text = stringResource(
                        R.string.progress_weight_value,
                        "${if (delta > 0) "+" else ""}${"%.1f".format(delta.kgToDisplayUnit(unit))}",
                        unit.weightUnitLabel(),
                    ),
                    style = MaterialTheme.typography.titleMedium.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(label = stringResource(R.string.progress_share), onClick = { state.sharing = true }, modifier = Modifier.weight(1f))
                SecondaryButton(label = stringResource(R.string.progress_close), onClick = onClose, modifier = Modifier.weight(1f))
            }
        }
    }

    if (state.sharing) {
        // A before/after is a two-frame strip, so it shares through the same sheet the timelapse does.
        SharePhotoStripSheet(
            photos = listOf(pair.older, pair.newer),
            unit = unit,
            onDismiss = { state.sharing = false },
        )
    }
}

@PreviewLightDark
@Composable
private fun PhotoComparisonScreenPreview() {
    AppTheme {
        PhotoComparisonContent(
            pair = ComparisonPair(
                older = ProgressPhoto(id = 1, dateEpochDay = 0, filePath = "", weightKg = 80.0),
                newer = ProgressPhoto(id = 2, dateEpochDay = 30, filePath = "", weightKg = 77.5),
            ),
            unit = UnitSystem.Metric,
            onClose = {},
        )
    }
}
