package ph.mart.healthapp.core.data.cycle.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One logged cycle day, keyed by local-midnight epoch day — `mood_day`'s shape one domain over.
 *
 * [flow] `0` means "not logged", never a flow of zero, which is what keeps the column non-null and
 * makes clearing a tap an update rather than a delete (soft-delete-only, with no deleted flag).
 * [symptoms] holds enum names comma-joined, the format `Profile.homeLayout` already stores a
 * vocabulary in; an all-zero, no-symptom day is simply not read back.
 *
 * There is deliberately no `cycle` or `period` table beside this one: a period is a run of these
 * days and a cycle is the gap between two runs, both derived — see `Cycle.kt`.
 */
@Entity(tableName = "cycle_day")
internal data class CycleDayEntity(
    @PrimaryKey val dateEpochDay: Long,
    val flow: Int,
    val symptoms: String,
    /** Minutes past local midnight the reading was taken, `0..1439` — null on a row that never
     * carried one: written before the field existed, or brought in by a provider's sync. Not a
     * timestamp, because [dateEpochDay] is the key and a second copy of the day could drift from it. */
    val minuteOfDay: Int? = null,
)
