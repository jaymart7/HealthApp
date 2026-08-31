package ph.mart.healthapp.feature.profile.ui.supplement

import ph.mart.healthapp.core.data.supplement.Supplement

/**
 * The user's supplement list, in full. Soft-deleted rows never reach here — they stay in Room only
 * so a past `supplement_day` still has a name to render on the Progress tab.
 */
data class SupplementsUiState(
    val supplements: List<Supplement> = emptyList(),
    /** Distinguishes "nothing added" from "not loaded yet", the same guard [
     * ph.mart.healthapp.feature.profile.ui.library.FoodLibraryUiState] uses: both are an empty
     * list on the first frame, and a mascot that flashes before the rows arrive reads as a bug. */
    val loaded: Boolean = false,
)

/** "2000 IU · twice daily", or just one half when the other has nothing to say. */
fun Supplement.summary(): String = listOfNotNull(
    dose.takeIf { it.isNotBlank() },
    when (timesPerDay) {
        1 -> "once daily"
        2 -> "twice daily"
        else -> "${timesPerDay}x daily"
    },
).joinToString(" · ")

sealed interface SupplementsEvent {
    data class OnSave(val supplement: Supplement) : SupplementsEvent
    data class OnDelete(val id: Long) : SupplementsEvent
}
