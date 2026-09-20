package ph.mart.healthapp.feature.profile.ui.supplement

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ph.mart.healthapp.core.data.supplement.EVERY_DAY
import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.core.data.supplement.SupplementLabelReading
import ph.mart.healthapp.core.data.supplement.dayLabel
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
    /** A name lookup is in flight. State rather than a side effect because it is one: the sheet's
     * sparkle is a spinner for as long as it holds, and a second tap must not spend a second call. */
    val lookingUp: Boolean = false,
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

/**
 * "twice a day · Mon · Wed · Fri" — how often, then on which days. The second half is absent on a
 * supplement due daily rather than spelled out: "once a day" already says every day, and seven
 * abbreviations in a row would be the row's longest line saying the least.
 */
@Composable
private fun Supplement.scheduleWords(): String {
    val often = when (timesPerDay) {
        1 -> stringResource(R.string.profile_supplements_once_daily)
        2 -> stringResource(R.string.profile_supplements_twice_daily)
        else -> stringResource(R.string.profile_supplements_times_daily, timesPerDay)
    }
    return if (days == EVERY_DAY) often else "$often · ${dayLabel()}"
}

sealed interface SupplementsEvent {
    data class OnSave(val supplement: Supplement) : SupplementsEvent
    data class OnDelete(val id: Long) : SupplementsEvent

    /** The name as typed in the sheet — bounded by [ph.mart.healthapp.core.data.supplement.SUPPLEMENT_NAME_MAX]
     * at the field, which is the only cap the call needs. */
    data class OnLookUp(val name: String) : SupplementsEvent
}

/**
 * The lookup's two answers. The screen's other three writes report themselves through the list
 * they change; this one lands in a sheet that is already open, which is the skill's
 * "loading a record to edit" shape — the one `SupplementScanViewModel` cites for the same reading.
 *
 * [LookupFailed] carries an id and not words: a ViewModel names a string and a composable resolves
 * it. Offline, unrecognised and failed are three different sentences and one type, because the
 * screen does the same thing with all three — puts them under the field that was typed in.
 */
sealed interface SupplementsSideEffect {
    data class LookedUp(val reading: SupplementLabelReading) : SupplementsSideEffect
    data class LookupFailed(@StringRes val messageRes: Int) : SupplementsSideEffect
}
