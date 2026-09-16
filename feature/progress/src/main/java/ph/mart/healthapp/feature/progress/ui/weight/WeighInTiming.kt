package ph.mart.healthapp.feature.progress.ui.weight

import ph.mart.healthapp.core.data.progress.WeightEntry

/**
 * Whether this user weighs in at two different times of day, and what the scale says about it.
 *
 * **Why weight is allowed on both sides here, when
 * [patterns][ph.mart.healthapp.feature.progress.ui.progress.patterns] refuses it on either.** That
 * file's reason for the refusal is that a day-level weight is mostly water — which is precisely
 * what this reports. It is a statement about the *measurement*, not about the body: a scale reads
 * differently depending on the hour you stand on it, and a trend drawn from readings taken at
 * scattered hours is measuring the clock as much as the person. Methodological, not clinical, and
 * the only conclusion it invites is "weigh in at the same time".
 *
 * Same shape as a `Pattern` otherwise: a median split of the user's own log, two averages and the
 * day count behind each, no "because", and null wherever there is nothing honest to say. The
 * floors below are its own rather than `Patterns.kt`'s, because a weigh-in series is sparser than
 * a food log and the two questions do not need the same bar.
 */

/** Weigh-ins carrying a time, before a split is worth drawing. Lower than `MIN_PAIRED_DAYS`: a
 * weigh-in is a weekly-or-so act for most people, where a food day is a daily one. */
const val MIN_TIMED_WEIGH_INS = 12

/** Per side, so neither average is one heavy evening. `MIN_SIDE_DAYS`' figure, same reason. */
const val MIN_WEIGH_IN_SIDE = 5

/** The floor on the two sides' mean *times*. Two weigh-ins ninety minutes apart are one habit
 * with some slack in it, not a morning group and an evening one. */
const val MIN_WEIGH_IN_SPREAD_MINUTES = 120.0

/** The floor on the weight gap. Below this the split is reporting scale noise and daily water. */
const val MIN_WEIGH_IN_DELTA_KG = 0.3

/**
 * [lateFromMinute] is the earliest minute on the late side — the exact boundary, so the screen can
 * say "from 1:40 PM on" and mean it, rather than rounding the median into a claim.
 */
data class WeighInTimeSplit(
    val earlyDays: Int,
    val lateDays: Int,
    val earlyAvgKg: Double,
    val lateAvgKg: Double,
    val lateFromMinute: Int,
) {
    /** Positive when the later readings are the heavier ones, which is the usual direction. */
    val deltaKg: Double get() = lateAvgKg - earlyAvgKg
}

/**
 * Null unless the log really splits: too few timed weigh-ins, a side too thin, times that didn't
 * separate, or a gap small enough to be water. The caller draws nothing on null — the recap card's
 * and the patterns card's rule.
 *
 * Imported weigh-ins count. A provider's record carries the instant the scale reported, which is
 * when the user stood on it; it is `note` that marks provenance, and provenance is not the
 * question here.
 *
 * The split is at the **median** rather than a fixed "morning is before 10" cutoff, because a
 * cutoff would be this app deciding when a weigh-in ought to happen. The median only says "your
 * earlier half against your later half". Ties go to the early side, unless that would empty the
 * late one, in which case they go late — `Patterns.kt`'s handling, and for its reason: either way
 * the two groups are the two real ones.
 */
fun List<WeightEntry>.weighInTimeSplit(): WeighInTimeSplit? {
    val timed = mapNotNull { entry -> entry.minuteOfDay?.let { it to entry.weightKg } }
    if (timed.size < MIN_TIMED_WEIGH_INS) return null

    val median = timed.map { (minute, _) -> minute.toDouble() }.sorted().median()
    var early = timed.filter { (minute, _) -> minute <= median }
    var late = timed.filter { (minute, _) -> minute > median }
    if (early.isEmpty() || late.isEmpty()) {
        early = timed.filter { (minute, _) -> minute < median }
        late = timed.filter { (minute, _) -> minute >= median }
    }
    if (early.size < MIN_WEIGH_IN_SIDE || late.size < MIN_WEIGH_IN_SIDE) return null

    val spread = late.map { (minute, _) -> minute.toDouble() }.average() -
        early.map { (minute, _) -> minute.toDouble() }.average()
    if (spread < MIN_WEIGH_IN_SPREAD_MINUTES) return null

    val earlyAvgKg = early.map { (_, kg) -> kg }.average()
    val lateAvgKg = late.map { (_, kg) -> kg }.average()
    if (kotlin.math.abs(lateAvgKg - earlyAvgKg) < MIN_WEIGH_IN_DELTA_KG) return null

    return WeighInTimeSplit(
        earlyDays = early.size,
        lateDays = late.size,
        earlyAvgKg = earlyAvgKg,
        lateAvgKg = lateAvgKg,
        lateFromMinute = late.minOf { (minute, _) -> minute },
    )
}

/** Called on a sorted, non-empty list — `MIN_TIMED_WEIGH_INS` is checked first. */
private fun List<Double>.median(): Double =
    if (size % 2 == 1) this[size / 2] else (this[size / 2 - 1] + this[size / 2]) / 2
