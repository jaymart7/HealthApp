package ph.mart.healthapp.wear.ui

import ph.mart.healthapp.core.today.TodaySnapshot

/**
 * [snapshot] null with [loaded] true is the honest empty state: the watch has been asked and the
 * phone has never pushed anything. It is drawn as "Open FitPulse on your phone", never as zeros —
 * a watch showing a 0 kcal day the user has been eating through would be worse than one admitting
 * it doesn't know.
 */
data class WearTodayUiState(
    val loaded: Boolean = false,
    val snapshot: TodaySnapshot? = null,
    /** A tap is in flight to the phone. Both controls are disabled meanwhile — the watch has no
     * Room to write to, so there is nothing to show optimistically. */
    val sending: Boolean = false,
)

/** True once the pushed snapshot describes a day that has since ended. The phone re-pushes at
 * midnight, so this only shows when the phone is out of range — which is exactly when saying so
 * matters. */
fun WearTodayUiState.isStale(todayEpochDay: Long): Boolean =
    snapshot != null && snapshot.dateEpochDay != todayEpochDay

sealed interface WearTodayEvent {
    data object OnAddGlass : WearTodayEvent
    data object OnToggleFast : WearTodayEvent
}

sealed interface WearTodaySideEffect {
    /** The phone didn't take the tap. Nothing was written anywhere, and the watch says so rather
     * than ticking a count it cannot keep. */
    data object PhoneUnreachable : WearTodaySideEffect
}
