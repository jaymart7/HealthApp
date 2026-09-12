package ph.mart.healthapp.feature.progress.ui.comparison

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.TrendDirection
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.goalRelativeTrend
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.designsystem.component.SegmentedToggle
import ph.mart.healthapp.core.designsystem.component.formatDayMonth
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.progress.R
import ph.mart.healthapp.feature.progress.ui.comparison.components.ComparisonSideBySide
import ph.mart.healthapp.feature.progress.ui.comparison.components.ComparisonSlider
import ph.mart.healthapp.feature.progress.ui.shared.components.PhotoOverlayStage
import ph.mart.healthapp.feature.progress.ui.shared.components.SharePhotoStripSheet
import ph.mart.healthapp.feature.progress.ui.shared.components.sampleBetween

/** A minus sign, not a hyphen: `-3.2` at headline size sets the dash at a third of the digit
 * height and half its weight, and the figure reads as a stray mark rather than a loss. */
private const val MINUS = "−"

/** The toggle names two framings; it should not stretch to whatever the delta beside it leaves. */
private val ModeToggleWidth = 168.dp

/**
 * Two shots under a divider you drag, or side by side — the question a grid of thumbnails can't
 * answer, with the answer stated rather than implied.
 *
 * A route rather than an overlay drawn over the Progress tab, so it wears the toolbar's back arrow
 * and none of the tab's chrome — no bottom bar and no FAB over a full-screen viewer, and no back
 * handler of its own. [selectedIds] is the pair `PhotoComparisonRoute` carries; the container turns
 * it into an ordered pair, and nothing is drawn until exactly two of them resolve.
 */
@Composable
internal fun PhotoComparisonScreen(
    selectedIds: List<Long>,
    modifier: Modifier = Modifier,
    viewModel: ComparisonViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    LaunchedEffect(selectedIds) { viewModel.handleEvent(ComparisonEvent.OnSelect(selectedIds)) }
    val pair = uiState.pair ?: return
    PhotoComparisonContent(
        pair = pair,
        photos = uiState.photos,
        unit = uiState.unit,
        goal = uiState.goal,
        modifier = modifier,
    )
}

@Composable
private fun PhotoComparisonContent(
    pair: ComparisonPair,
    photos: List<ProgressPhoto>,
    unit: UnitSystem,
    goal: Goal?,
    modifier: Modifier = Modifier,
    state: ComparisonState = rememberComparisonState(),
) {
    PhotoOverlayStage(onShare = { state.sharing = true }, modifier = modifier) {
        // `fill = false`: the frame takes what it needs of the free height and leaves the rest,
        // rather than stretching a photo to fill a tall window.
        val frame = Modifier.weight(1f, fill = false)
        when (state.mode) {
            CompareMode.Slider -> ComparisonSlider(pair = pair, state = state, modifier = frame)
            CompareMode.SideBySide -> ComparisonSideBySide(pair = pair, modifier = frame)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ComparisonHeadline(pair = pair, unit = unit, goal = goal, modifier = Modifier.weight(1f))
            SegmentedToggle(
                options = listOf(
                    stringResource(R.string.progress_compare_mode_slider),
                    stringResource(R.string.progress_compare_mode_side),
                ),
                selectedIndex = state.mode.ordinal,
                onSelect = { state.mode = CompareMode.entries[it] },
                modifier = Modifier.width(ModeToggleWidth),
            )
        }
    }

    if (state.sharing) {
        // A before/after shares as a strip spanning the pair, so it goes through the same sheet
        // the timelapse does — see `sampleBetween`.
        SharePhotoStripSheet(
            photos = sampleBetween(photos, pair.older, pair.newer),
            unit = unit,
            onDismiss = { state.sharing = false },
        )
    }
}

/**
 * The result, said out loud. The weight delta is what the two photos are being read for, so it is
 * the largest thing on the screen and it carries the goal-relative colour — a kilo gained is the
 * point for someone building and the opposite for someone cutting, which is why
 * [goalRelativeTrend] and not a sign test decides it.
 *
 * A pair with a weight missing at either end still has a span, and the span becomes the headline
 * rather than the screen losing its answer.
 */
@Composable
private fun ComparisonHeadline(
    pair: ComparisonPair,
    unit: UnitSystem,
    goal: Goal?,
    modifier: Modifier = Modifier,
) {
    val days = (pair.newer.dateEpochDay - pair.older.dateEpochDay).toInt()
    val span = pluralStringResource(R.plurals.progress_strip_days, days, days)
    val delta = pair.weightDeltaKg

    val headline: String
    val headlineColor: Color
    val caption: String
    if (delta == null) {
        headline = span
        headlineColor = MaterialTheme.colorScheme.onSurface
        caption = stringResource(
            R.string.progress_compare_between,
            formatEpochDay(pair.older.dateEpochDay),
            formatEpochDay(pair.newer.dateEpochDay),
        )
    } else {
        val display = delta.kgToDisplayUnit(unit)
        val sign = if (display > 0) "+" else if (display < 0) MINUS else ""
        headline = stringResource(
            R.string.progress_weight_value,
            "$sign${"%.1f".format(abs(display))}",
            unit.weightUnitLabel(),
        )
        headlineColor = when (goalRelativeTrend(goal, delta)) {
            TrendDirection.OnTrack -> MaterialTheme.colorScheme.primary
            TrendDirection.OffTrack -> MaterialTheme.colorScheme.error
            TrendDirection.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        caption = stringResource(
            R.string.progress_compare_over,
            span,
            formatDayMonth(pair.older.dateEpochDay),
            formatDayMonth(pair.newer.dateEpochDay),
        )
    }

    Column(modifier = modifier) {
        Text(
            text = headline,
            style = MaterialTheme.typography.displaySmall.tabularNums,
            color = headlineColor,
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall.tabularNums,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@PreviewLightDark
@Composable
private fun PhotoComparisonScreenPreview() {
    val pair = ComparisonPair(
        older = ProgressPhoto(id = 1, dateEpochDay = 0, filePath = "", weightKg = 80.0),
        newer = ProgressPhoto(id = 2, dateEpochDay = 92, filePath = "", weightKg = 76.8),
    )
    AppTheme {
        PhotoComparisonContent(
            pair = pair,
            photos = listOf(pair.older, pair.newer),
            unit = UnitSystem.Metric,
            goal = Goal.Lose,
        )
    }
}
