package ph.mart.healthapp.core.data.exercise.local

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * One set of one lift inside an [ExerciseEntryEntity]. [entryId] is a plain column, not a foreign
 * key — the parent/child join is a Kotlin fold over two small lists, the same call
 * `SavedMealItemEntity` makes with its `mealId`.
 *
 * There is no `isDeleted` here: the parent's soft delete is the only one, and every read joins
 * back to `exercise_entry` filtering on it, so a deleted workout's sets are simply never grouped
 * in. The indexed [entryId] is what keeps that join cheap.
 *
 * A [weightKg] of 0 is bodyweight — see `StrengthSet`, where the reading is recorded.
 */
@Entity(tableName = "strength_set", indices = [Index("entryId")])
internal data class StrengthSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val exerciseName: String,
    val reps: Int,
    val weightKg: Double,
)
