package ph.mart.healthapp.core.data

import java.text.DateFormatSymbols
import java.util.Calendar

/**
 * The weekday bitmask and the names that print it — Monday first, the bit positions
 * [weekdayIndex] returns.
 *
 * These live here rather than in `exercise/TrainingPlan.kt`, where they started, because they are
 * domain-neutral and now have three callers across three domains: the training plan
 * ([ph.mart.healthapp.core.data.exercise.Routine.days]), the week budget, and a supplement's
 * schedule. [weekdayIndex] is in `EpochDay.kt` next door, which is the other half of the same
 * vocabulary.
 */
const val DAYS_IN_WEEK = 7

/** What a picker prints in its seven cells, Monday first. */
fun weekdayInitials(): List<String> = weekdayShort().map { it.take(1) }

/** What a card prints, Monday first — one vocabulary, so a picker and a card can never disagree
 * about which cell is which day. */
fun weekdayShort(): List<String> = mondayFirst(DateFormatSymbols.getInstance().shortWeekdays)

fun weekdayNames(): List<String> = mondayFirst(DateFormatSymbols.getInstance().weekdays)

/** [DateFormatSymbols] indexes by [Calendar.SUNDAY]..[Calendar.SATURDAY] with a blank at 0; this
 * app counts from Monday, which is what every weekday bitmask means. */
private fun mondayFirst(names: Array<String>): List<String> = listOf(
    Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
    Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY,
).map { names[it] }

/** The mask with [index]'s bit flipped — a picker's whole write. */
fun Int.toggleWeekday(index: Int): Int = this xor (1 shl index)

fun Int.hasWeekday(index: Int): Boolean = this and (1 shl index) != 0

/** "Mon · Wed · Fri", and empty for a mask of 0 — a caller with a meaning for "none" says so in
 * its own words rather than printing this blank. */
fun Int.weekdayLabel(): String =
    weekdayShort().filterIndexed { index, _ -> hasWeekday(index) }.joinToString(" · ")
