package ph.mart.healthapp.feature.profile.ui.supplement

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.shared.components.Figure

/**
 * The user's supplement list, in full and **sorted A→Z at read time** rather than stored sorted.
 * Soft-deleted rows never reach here — they stay in Room only so a past `supplement_day` still
 * has a name to render on the Progress tab.
 *
 * There are no sections. The list is typically three to ten rows, the app holds no time-of-day
 * data, so grouping by morning/evening would be information nobody entered; insertion order,
 * which is what shipped, looks random after a year.
 */
data class SupplementsUiState(
    val supplements: List<Supplement> = emptyList(),
    /** Distinguishes "nothing added" from "not loaded yet", the same guard [
     * ph.mart.healthapp.feature.profile.ui.library.FoodLibraryUiState] uses: both are an empty
     * list on the first frame, and a mascot that flashes before the rows arrive reads as a bug. */
    val loaded: Boolean = false,
)

/**
 * "500 mg / twice a day" — the dose and the schedule, as two figures rather than one grey
 * caption. The dose is free text the app never does math on, so it rides the figure slot whole.
 *
 * A supplement with no dose has no figure at all, which is [noDoseLine]'s case: this returns an
 * empty list rather than inventing a figure out of the schedule alone.
 */
@Composable
internal fun Supplement.figures(): List<Figure> =
    if (dose.isBlank()) emptyList() else listOf(Figure(dose), Figure(scheduleWords()))

/** "No dose set · once a day" — the whole line quiet, because there is no number on it to be the
 * thing that is read. The frequency is on the marker either way. */
@Composable
internal fun Supplement.noDoseLine(): String =
    "${stringResource(R.string.profile_supplements_no_dose)} · ${scheduleWords()}"

@Composable
private fun Supplement.scheduleWords(): String = when (timesPerDay) {
    1 -> stringResource(R.string.profile_supplements_once_daily)
    2 -> stringResource(R.string.profile_supplements_twice_daily)
    else -> stringResource(R.string.profile_supplements_times_daily, timesPerDay)
}

sealed interface SupplementsEvent {
    data class OnSave(val supplement: Supplement) : SupplementsEvent
    data class OnDelete(val id: Long) : SupplementsEvent
}
