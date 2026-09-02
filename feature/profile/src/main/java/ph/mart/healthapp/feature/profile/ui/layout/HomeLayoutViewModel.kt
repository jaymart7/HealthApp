package ph.mart.healthapp.feature.profile.ui.layout

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.designsystem.component.HomeCardSetting
import ph.mart.healthapp.core.designsystem.component.encodeHomeCardLayout
import ph.mart.healthapp.core.designsystem.component.homeCardLayout

/**
 * Reads and writes one field of the profile. Its own ViewModel rather than `ProfileViewModel`:
 * this screen is a second [androidx.lifecycle.ViewModelStoreOwner], so sharing that one would
 * spin up a second instance of all ten of its repositories to write a single column.
 */
class HomeLayoutViewModel(
    private val profileRepository: ProfileRepository,
) : ViewModel(), OrbitContainerHost<HomeLayoutUiState, HomeLayoutUiState, Nothing> {

    override val container = orbitContainer<HomeLayoutUiState, Nothing>(HomeLayoutUiState()) {
        observeLayout()
    }

    private fun observeLayout() = intent {
        profileRepository.observeProfile().collect { profile ->
            reduce { state.copy(layout = homeCardLayout(profile?.homeLayout), loaded = true) }
        }
    }

    /** The whole layout, written on every change — it is one string, so there is nothing smaller
     * to write and no partial state to reconcile. */
    fun save(layout: List<HomeCardSetting>) = intent {
        val profile = profileRepository.observeProfile().first() ?: return@intent
        profileRepository.saveProfile(profile.copy(homeLayout = encodeHomeCardLayout(layout)))
    }

    /**
     * Writes null, not the default order encoded — the "Reset to calculated" call the target
     * overrides make. A stored default is a *pin*: it would freeze this install's order against
     * any card a later build adds, which is exactly what null is there to avoid.
     */
    fun reset() = intent {
        val profile = profileRepository.observeProfile().first() ?: return@intent
        profileRepository.saveProfile(profile.copy(homeLayout = null))
    }
}
