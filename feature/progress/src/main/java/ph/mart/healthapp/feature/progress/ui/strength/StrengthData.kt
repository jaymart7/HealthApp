package ph.mart.healthapp.feature.progress.ui.strength

import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.profile.UnitSystem

/**
 * The last year's **lifting** entries, oldest first — already through `withSets()` in the
 * container, so an empty list here is "nothing lifted" rather than "nothing logged", which is the
 * question the page's empty state actually asks.
 *
 * [unit] is the profile's, for the volume figures and the records. No `cycleTrackingOn` beside it:
 * Strength sits in Training, so its switcher never has to decide whether to draw Cycle.
 */
data class StrengthUiState(
    val entries: List<ExerciseEntry> = emptyList(),
    val unit: UnitSystem = UnitSystem.Metric,
)
