package ph.mart.healthapp.core.data.exercise

import ph.mart.healthapp.core.data.weekdayIndex

/*
 * The weekly training plan: which routines are planned for which weekdays, and how the current
 * week is going against that.
 *
 * Pure, beside [Routine] and `Strength.kt`, for the reason those are — nothing here touches Room,
 * so all of it is testable without a database. It is the `streak/` and `goalProjection()` shape
 * applied one domain over: **no table of its own**, no repository, nothing to notify off. The plan
 * is [Routine.days], and everything below is a fold over the routines and the workouts that were
 * already being read.
 *
 * The one judgement call is what counts as trained: a day holding any workout **with sets**, not
 * "the planned routine was performed". Nothing links a logged workout back to the routine that
 * seeded it, and this feature deliberately does not add that link.
 *
 * ponytail: so a freestyle session on a Push day ticks the Push day. A `routineId` on
 * `exercise_entry` is the upgrade path if that ever misleads someone.
 */
const val DAYS_IN_WEEK = 7

/** What the picker prints in its seven cells, Monday first. */
val WEEKDAY_INITIALS = listOf("M", "T", "W", "T", "F", "S", "S")

/** What the card prints, Monday first — one vocabulary, so the picker and the card can never
 * disagree about which cell is which day. */
val WEEKDAY_SHORT = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

val WEEKDAY_NAMES = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

/** The mask with [index]'s bit flipped — the picker's whole write. */
fun Int.toggleWeekday(index: Int): Int = this xor (1 shl index)

fun Int.hasWeekday(index: Int): Boolean = this and (1 shl index) != 0

fun Routine.isPlannedOn(epochDay: Long): Boolean = days.hasWeekday(weekdayIndex(epochDay))

/** "Mon · Wed · Fri", and empty for an unscheduled routine — the row and the card both say so in
 * their own words rather than printing this blank. */
fun Routine.dayLabel(): String =
    WEEKDAY_SHORT.filterIndexed { index, _ -> days.hasWeekday(index) }.joinToString(" · ")

fun List<Routine>.plannedOn(epochDay: Long): List<Routine> = filter { it.isPlannedOn(epochDay) }

/** Whether any routine is on the plan at all. Home's card is hidden until this is true —
 * `SupplementsCard`'s rule: nothing is missing, nothing has been authored yet. */
fun List<Routine>.anyScheduled(): Boolean = any { it.days != 0 }

/** Monday of the week [epochDay] falls in. */
fun weekStart(epochDay: Long): Long = epochDay - weekdayIndex(epochDay)

/** One day of the plan strip: what was intended, what happened, and whether it is today. */
data class PlanDay(
    val epochDay: Long,
    val planned: Boolean,
    val trained: Boolean,
    val isToday: Boolean,
)

/**
 * The current week, Monday to Sunday, scored against the plan — the only place adherence is
 * defined, so the card's dots and its "2 of 3" line can't tell different stories.
 *
 * The week is the one containing [todayEpochDay], not a rolling seven days: a plan is written in
 * weekdays, so a strip that started on a Thursday would put Monday's dot in two different places
 * on two different days. (The weekly *recap* on Progress is rolling for the opposite reason — it
 * has no weekday to anchor to.)
 */
fun trainingWeek(
    routines: List<Routine>,
    entries: List<ExerciseEntry>,
    todayEpochDay: Long,
): List<PlanDay> {
    val trainedDays = entries.withSets().mapTo(mutableSetOf()) { it.dateEpochDay }
    val monday = weekStart(todayEpochDay)
    return (0 until DAYS_IN_WEEK).map { offset ->
        val day = monday + offset
        PlanDay(
            epochDay = day,
            planned = routines.any { it.days.hasWeekday(offset) },
            trained = day in trainedDays,
            isToday = day == todayEpochDay,
        )
    }
}

/** The week up to and including today. A Monday must not report Friday as a session missed. */
private fun List<PlanDay>.elapsed(): List<PlanDay> {
    val todayIndex = indexOfFirst { it.isToday }
    return if (todayIndex < 0) this else take(todayIndex + 1)
}

/** The two halves of "2 of 3 done this week", counted the same way so they always agree. */
fun List<PlanDay>.plannedSoFar(): Int = elapsed().count { it.planned }

fun List<PlanDay>.trainedSoFar(): Int = elapsed().count { it.planned && it.trained }
