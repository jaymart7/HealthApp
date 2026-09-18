package ph.mart.healthapp.core.data.note

import kotlinx.coroutines.flow.Flow

/**
 * What the user wrote about one day, in their own words.
 *
 * **Blank means "nothing written", never an empty note.** Clearing the field is how a note is
 * deleted — the same reading [ph.mart.healthapp.core.data.mood.MoodDay]'s zero has, and the reason
 * neither domain needs a deleted flag to stay inside the soft-delete-only rule.
 */
data class DayNote(val dateEpochDay: Long, val text: String)

/**
 * Long enough for a paragraph about a day, short enough that a note never becomes a journal the
 * diary has to scroll past. The cap is applied on the way into the table rather than by the field,
 * so no caller can write a longer one.
 */
const val NOTE_MAX_CHARS = 500

/** Trimmed and capped. A note of nothing but whitespace is a blank one, which is a cleared one. */
fun String.toNoteText(): String = trim().take(NOTE_MAX_CHARS)

/**
 * One row per day holding one string — the shape [ph.mart.healthapp.core.data.mood.MoodRepository]
 * already has, and for the same reasons: correcting what you wrote is an update rather than a
 * delete, and a day nobody has written about simply reads back blank.
 *
 * Dated rather than today-only, unlike the mood check-in: the diary shows any past day, and a note
 * belongs to the day being reviewed rather than to the day it was typed on.
 *
 * Deliberately **not** a streak domain, a chart or a recap line. A note is not a number, so there
 * is nothing here that observes logged days.
 */
interface NoteRepository {
    /** Blank-filled, never null — the diary never has to special-case a missing row. */
    fun observeForDate(dateEpochDay: Long): Flow<DayNote>

    /** Trimmed and capped through [toNoteText]; blank clears that day's note. */
    suspend fun setNote(dateEpochDay: Long, text: String)

    /** Days actually written about, oldest first — for data export. */
    suspend fun allNotes(): List<DayNote>

    /** Blanks every day, for import's replace-in-full semantics. */
    suspend fun clearAllNotes()
}
