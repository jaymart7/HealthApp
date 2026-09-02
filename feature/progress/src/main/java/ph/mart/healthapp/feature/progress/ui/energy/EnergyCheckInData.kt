package ph.mart.healthapp.feature.progress.ui.energy

import ph.mart.healthapp.core.data.profile.Profile

/**
 * The profile itself, not a copy of anything derived from it: the check-in's numbers are folded
 * from `ProgressUiState`'s series, and holding a second copy of them here would give the card and
 * the overlay two sources that could disagree. What this container is for is the *write* — and
 * [Profile.addExerciseToBudget], which the overlay warns about and nothing else on Progress reads.
 */
data class EnergyCheckInUiState(val profile: Profile? = null)

sealed interface EnergyCheckInEvent {
    /** Pins the measured target, exactly as the Goals stepper pins a typed one. */
    data class OnApply(val kcal: Int) : EnergyCheckInEvent
}
