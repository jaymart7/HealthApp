package ph.mart.healthapp.core.data.mood

import kotlinx.coroutines.flow.Flow
import ph.mart.healthapp.core.data.progress.ChartRange

/**
 * One day's reflection: how the user felt and how much energy they had, each on a 1–5 scale.
 *
 * **0 means "not set", never a zero score.** A day where only the mood face was tapped is a
 * valid row (`mood = 4, energy = 0`) — that is what keeps both columns non-null and stops the
 * card from demanding two taps to record one.
 */
data class MoodDay(val dateEpochDay: Long, val mood: Int, val energy: Int)

val MOOD_SCALE = 1..5

/** Five steps, worst → best. [label] is what the control announces to TalkBack. */
enum class MoodLevel(val value: Int, val label: String) {
    VeryLow(1, "Very low"),
    Low(2, "Low"),
    Okay(3, "Okay"),
    Good(4, "Good"),
    Great(5, "Great"),
}

/** [daysLogged] counts days with *either* value set, so it can exceed both denominators below. */
data class MoodAverages(val mood: Double?, val energy: Double?, val daysLogged: Int)

/**
 * Zeros are skipped rather than averaged in, and each series keeps its own denominator — a week
 * of mood-only days reports a mood average and a null energy one, instead of quietly halving
 * the energy score.
 */
fun List<MoodDay>.moodAverages(): MoodAverages {
    val moods = filter { it.mood > 0 }
    val energies = filter { it.energy > 0 }
    return MoodAverages(
        mood = moods.takeIf { it.isNotEmpty() }?.let { days -> days.sumOf { it.mood }.toDouble() / days.size },
        energy = energies.takeIf { it.isNotEmpty() }?.let { days -> days.sumOf { it.energy }.toDouble() / days.size },
        daysLogged = count { it.mood > 0 || it.energy > 0 },
    )
}

/**
 * Anchored to today, unlike [ph.mart.healthapp.core.data.progress.inRange] which anchors to the
 * latest weigh-in: a mood chart headed "1M" must show the last 30 days with their gaps intact,
 * not the 30 days around whenever the user last opened the app.
 */
fun List<MoodDay>.inRange(range: ChartRange, todayEpochDay: Long): List<MoodDay> {
    val days = range.days ?: return this
    return filter { it.dateEpochDay >= todayEpochDay - days }
}

/**
 * Stored one row per day holding two small ints — the same shape as
 * [ph.mart.healthapp.core.data.water.WaterRepository], and for the same reason: correcting a tap
 * is an update rather than a delete, which keeps this domain inside the soft-delete-only rule
 * without a deleted flag.
 *
 * Deliberately **not** part of the logging streak. The streak's four domains (food, water,
 * weigh-in, exercise) are things the user *did*; a two-tap reflection holding a 40-day run would
 * cheapen it. That is why there is no `observeLoggedDays()` here.
 */
interface MoodRepository {
    /** Zero-filled, never null — the card never has to special-case a missing row. */
    fun observeToday(): Flow<MoodDay>

    /** [level] in [MOOD_SCALE], or 0 to clear. Leaves that day's energy untouched. */
    suspend fun setTodayMood(level: Int)

    /** [level] in [MOOD_SCALE], or 0 to clear. Leaves that day's mood untouched. */
    suspend fun setTodayEnergy(level: Int)

    /** Logged days only, oldest first — the Progress tab and the weekly recap. */
    fun observeDays(): Flow<List<MoodDay>>

    /** Dated write — an import or the debug seed. */
    suspend fun upsertDay(day: MoodDay)

    /** Every logged day, oldest first — for data export. */
    suspend fun allDays(): List<MoodDay>

    /** Zeroes every day, for import's replace-in-full semantics. */
    suspend fun clearAllDays()
}
