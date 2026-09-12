package ph.mart.healthapp.feature.progress.ui.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
internal fun rememberPhotosState(): PhotosState =
    rememberSaveable(saver = PhotosState.Saver()) { PhotosState() }

/** The two photos a comparison reads. Also the ceiling [PhotosState.toggle] holds the selection to. */
internal const val COMPARISON_PICKS = 2

/**
 * UI-only: which tiles are picked and whether the share sheet is up. It lived in
 * `ProgressScreenState` while the page was a swap-in inside the Progress tab; now that the page is
 * a route it holds its own, which is also what stops a selection surviving into a screen that has
 * nothing to do with it.
 */
internal class PhotosState(
    selectedIds: List<Long> = emptyList(),
    sharing: Boolean = false,
) {
    var selectedIds: List<Long> by mutableStateOf(selectedIds)
    var sharing: Boolean by mutableStateOf(sharing)

    /**
     * Tap to pick, tap again to drop. At the ceiling the **oldest** pick leaves, so a third tap
     * reads as swapping one end of the pair rather than as a tap that did nothing — and the two
     * that remain stay in pick order, which is the order the tile badges number them in.
     */
    fun toggle(id: Long) {
        selectedIds = when {
            id in selectedIds -> selectedIds - id
            selectedIds.size >= COMPARISON_PICKS -> selectedIds.drop(1) + id
            else -> selectedIds + id
        }
    }

    fun clear() {
        selectedIds = emptyList()
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun Saver(): Saver<PhotosState, Any> = listSaver(
            // Appended, never renumbered — the rule `ProgressScreenState`'s saver keeps.
            save = { listOf(it.selectedIds, it.sharing) },
            restore = { saved -> PhotosState(saved[0] as List<Long>, saved[1] as Boolean) },
        )
    }
}
