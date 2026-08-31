package ph.mart.healthapp.core.data.supplement.local

import androidx.room3.Entity

/** One supplement on one local day. [dueTimes] is snapshotted at write time and never re-read
 * from the supplement — see [ph.mart.healthapp.core.data.supplement.SupplementDay]. */
@Entity(tableName = "supplement_day", primaryKeys = ["dateEpochDay", "supplementId"])
internal data class SupplementDayEntity(
    val dateEpochDay: Long,
    val supplementId: Long,
    val taken: Int,
    val dueTimes: Int,
)
