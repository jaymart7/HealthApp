package ph.mart.healthapp.feature.progress.ui.comparison

import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.ProgressPhoto

/**
 * The whole set plus the two ids picked out of it, rather than the pair itself: the grid's
 * selection is what the user changed, and [pair] is what that selection means once the photos it
 * names have actually loaded. Holding the resolved pair in state instead would make a selection
 * arriving before the photos do a silently empty screen.
 */
data class ComparisonUiState(
    val photos: List<ProgressPhoto> = emptyList(),
    val unit: UnitSystem = UnitSystem.Metric,
    val selectedIds: List<Long> = emptyList(),
) {
    val pair: ComparisonPair? get() = comparisonPair(photos, selectedIds)
}

/** Older first, whichever order the two were tapped in — the slider draws [newer] over [older]. */
data class ComparisonPair(val older: ProgressPhoto, val newer: ProgressPhoto) {
    /** Kilograms gained or lost between the two shots, or null unless both carry a weight. */
    val weightDeltaKg: Double?
        get() = newer.weightKg?.let { after -> older.weightKg?.let { before -> after - before } }
}

/**
 * Null unless exactly two ids resolve to photos — one shot is not a before and after, and an id
 * whose photo has since been deleted leaves a selection that no longer names a pair.
 */
fun comparisonPair(photos: List<ProgressPhoto>, selectedIds: List<Long>): ComparisonPair? {
    if (selectedIds.size != 2) return null
    val picked = photos.filter { it.id in selectedIds }.sortedBy { it.dateEpochDay }
    if (picked.size != 2) return null
    return ComparisonPair(older = picked[0], newer = picked[1])
}

sealed interface ComparisonEvent {
    /** The grid's current selection, handed down whole — the container decides what two of them mean. */
    data class OnSelect(val ids: List<Long>) : ComparisonEvent
}
