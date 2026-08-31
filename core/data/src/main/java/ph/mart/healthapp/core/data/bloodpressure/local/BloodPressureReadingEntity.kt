package ph.mart.healthapp.core.data.bloodpressure.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One cuff reading, keyed by nothing but its own id.
 *
 * [takenAtMillis] is the only time this row carries — no denormalised `date` column, unlike
 * [ph.mart.healthapp.core.data.exercise.local.ExerciseEntryEntity], which needs one because the
 * diary reads a day at a time. Here the day is derived on read, so the two can't drift.
 *
 * [pulseBpm] of `0` means the figure wasn't entered, not a pulse of zero.
 */
@Entity(tableName = "blood_pressure_reading")
internal data class BloodPressureReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val takenAtMillis: Long,
    val systolic: Int,
    val diastolic: Int,
    val pulseBpm: Int = 0,
    val isDeleted: Boolean = false,
)
