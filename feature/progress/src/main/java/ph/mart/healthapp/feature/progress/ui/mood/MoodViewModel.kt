package ph.mart.healthapp.feature.progress.ui.mood

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ph.mart.healthapp.core.data.mood.MoodRepository

/**
 * The Mood page's container — `SleepViewModel`'s shape and its reasoning. It writes nothing; the
 * two-tap reflection that fills this series is Home's card, under its own container.
 */
class MoodViewModel(
    moodRepository: MoodRepository,
) : ViewModel(), OrbitContainerHost<MoodUiState, MoodUiState, Nothing> {

    override val container = orbitContainer<MoodUiState, Nothing>(MoodUiState()) {
        observeMood(moodRepository)
    }

    private fun observeMood(moodRepository: MoodRepository) = intent {
        moodRepository.observeDays().collect { days -> reduce { MoodUiState(days = days) } }
    }
}
