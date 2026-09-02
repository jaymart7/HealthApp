package ph.mart.healthapp.feature.profile.ui.layout

import ph.mart.healthapp.core.designsystem.component.HomeCardSetting

/**
 * The Home layout as the profile holds it, already resolved from its stored string — a card
 * added in this build is in the list even if the saved string predates it.
 */
data class HomeLayoutUiState(
    val layout: List<HomeCardSetting> = emptyList(),
    /** Distinguishes "not read yet" from a real layout, the same guard [
     * ph.mart.healthapp.feature.profile.ui.supplement.SupplementsUiState] uses. The screen seeds
     * its working copy off the first loaded emission and never again — re-seeding mid-drag would
     * fight the finger. */
    val loaded: Boolean = false,
)
