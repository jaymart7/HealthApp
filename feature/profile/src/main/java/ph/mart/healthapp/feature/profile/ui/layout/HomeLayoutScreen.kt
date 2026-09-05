package ph.mart.healthapp.feature.profile.ui.layout

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.designsystem.component.HomeCardSetting
import ph.mart.healthapp.core.designsystem.component.homeCardLayout
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.layout.components.HomeLayoutRow
import ph.mart.healthapp.feature.profile.ui.layout.components.HomeLayoutRowHeight



/** How close to an edge the dragged row has to get before the list starts following it. */
private val AutoScrollEdge = 48.dp

/**
 * Profile → Home layout: which cards Home draws, and in what order.
 *
 * One Nav3 level above Profile, the shape `SupplementsScreen` and `FoodLibraryScreen` use — a
 * list that outgrows a sheet, with NavDisplay's own back returning to Profile. Authoring lives
 * here and rendering lives on Home, the same division the supplement list draws.
 */
@Composable
fun HomeLayoutScreen(viewModel: HomeLayoutViewModel = koinViewModel()) {
    val uiState by viewModel.collectAsState()
    HomeLayoutContent(uiState = uiState, onSave = viewModel::save, onReset = viewModel::reset)
}

@Composable
private fun HomeLayoutContent(
    uiState: HomeLayoutUiState,
    onSave: (List<HomeCardSetting>) -> Unit,
    onReset: () -> Unit,
) {
    // The working copy. Seeded off the first *loaded* emission and never re-seeded: this screen's
    // own writes come back through Room, and re-seeding on one mid-drag would yank the row out
    // from under the finger. Same guard `DiarySheets` applies to its seed.
    val cards = remember { mutableStateListOf<HomeCardSetting>() }
    var seeded by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.loaded) {
        if (uiState.loaded && !seeded) {
            cards.addAll(uiState.layout)
            seeded = true
        }
    }

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    // One constant instead of measuring: every row is [HomeLayoutRowHeight] tall.
    val step = with(density) { (HomeLayoutRowHeight + 8.dp).toPx() }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    // The list follows a row dragged to either edge. Re-runs on every offset change and then
    // keeps scrolling on its own, so holding still at the edge still travels.
    LaunchedEffect(draggingIndex, dragOffset) {
        if (draggingIndex < 0) return@LaunchedEffect
        while (true) {
            val amount = autoScrollAmount(listState, draggingIndex, dragOffset, density)
            if (amount == 0f) break
            // Nothing left to scroll — stop rather than spin a frame loop at the end of the list.
            if (listState.scrollBy(amount) == 0f) break
        }
    }

    fun commit() = onSave(cards.toList())

    /** Moves the row [from] by [delta] slots, for the accessibility actions. False when it can't. */
    fun move(from: Int, delta: Int): Boolean {
        val to = from + delta
        if (to !in cards.indices) return false
        cards.add(to, cards.removeAt(from))
        commit()
        return true
    }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Text(
                    text = stringResource(R.string.profile_layout_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            itemsIndexed(cards, key = { _, setting -> setting.card.name }) { index, setting ->
                HomeLayoutRow(
                    setting = setting,
                    dragging = index == draggingIndex,
                    offsetY = if (index == draggingIndex) dragOffset else 0f,
                    onToggle = {
                        cards[index] = setting.copy(visible = !setting.visible)
                        commit()
                    },
                    onMove = { delta -> move(index, delta) },
                    onDragStart = {
                        draggingIndex = index
                        dragOffset = 0f
                    },
                    onDrag = { dy ->
                        dragOffset += dy
                        // Swapping and subtracting a whole row leaves the dragged row exactly
                        // where the finger left it, whatever threshold triggered the swap.
                        while (dragOffset > step / 2 && draggingIndex < cards.lastIndex) {
                            cards.add(draggingIndex + 1, cards.removeAt(draggingIndex))
                            draggingIndex += 1
                            dragOffset -= step
                        }
                        while (dragOffset < -step / 2 && draggingIndex > 0) {
                            cards.add(draggingIndex - 1, cards.removeAt(draggingIndex))
                            draggingIndex -= 1
                            dragOffset += step
                        }
                    },
                    onDragEnd = {
                        draggingIndex = -1
                        dragOffset = 0f
                        commit()
                    },
                    // The dragged row is finger-driven; letting Compose animate its placement too
                    // would have the two fighting over the same pixels.
                    modifier = if (index == draggingIndex) Modifier else Modifier.animateItem(),
                )
            }
            item {
                TextButton(
                    onClick = {
                        cards.clear()
                        cards.addAll(homeCardLayout(null))
                        onReset()
                    },
                ) {
                    Text(stringResource(R.string.profile_layout_reset))
                }
            }
        }
    }
}

/**
 * How far to scroll this frame so a row dragged to an edge keeps travelling — zero when the row
 * is comfortably inside the viewport.
 */
private fun autoScrollAmount(
    listState: LazyListState,
    draggingIndex: Int,
    dragOffset: Float,
    density: Density,
): Float {
    val info = listState.layoutInfo
    // `draggingIndex` indexes the cards; the intro paragraph is item 0.
    val item = info.visibleItemsInfo.firstOrNull { it.index == draggingIndex + 1 } ?: return 0f
    val edge = with(density) { AutoScrollEdge.toPx() }
    val top = item.offset + dragOffset
    val bottom = top + item.size
    return when {
        top < info.viewportStartOffset + edge -> -edge / 4f
        bottom > info.viewportEndOffset - edge -> edge / 4f
        else -> 0f
    }
}

@PreviewLightDark
@Composable
private fun HomeLayoutScreenPreview() {
    AppTheme {
        HomeLayoutContent(
            uiState = HomeLayoutUiState(layout = homeCardLayout("Macros,-Sleep"), loaded = true),
            onSave = {},
            onReset = {},
        )
    }
}
