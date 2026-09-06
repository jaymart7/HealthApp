package ph.mart.healthapp.feature.profile.ui.profile

import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.progress.WeightEntry

/** Everything on this screen derives from the one Room-backed profile — targets included, via
 * [ph.mart.healthapp.core.data.profile.dailyTargets]. No second copy of any value lives here.
 *
 * [weightEntries] is not a second copy either: the identity header reports what you weigh *now* and
 * which way it moved, and the profile's own `weightKg` is the onboarding figure, which stops being
 * true the first time anyone steps on a scale. It is the same list Home and Progress read, run
 * through the same [ph.mart.healthapp.core.data.profile.trendVsSevenDaysAgo]. */
data class ProfileUiState(
    val profile: Profile? = null,
    val weightEntries: List<WeightEntry> = emptyList(),
)
