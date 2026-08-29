package ph.mart.healthapp.core.data.exercise.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** [burnedKcal] is stored as logged, not recomputed on read — the user can overwrite the MET
 * estimate, and a later weigh-in must not silently rewrite what a past workout burned. */
@Entity(tableName = "exercise_entry")
internal data class ExerciseEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val name: String,
    val date: Long,
    val loggedAt: Long,
    val minutes: Int,
    val burnedKcal: Int,
    val isDeleted: Boolean = false,
)
