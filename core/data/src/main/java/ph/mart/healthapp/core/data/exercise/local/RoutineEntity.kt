package ph.mart.healthapp.core.data.exercise.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * A named workout template — "Push day" is the lifts it was saved with, not a session that
 * happened. Shaped like `SavedMealEntity`, which is the same idea for food: a thing the user
 * authored once and re-uses, soft-deleted here only, with its children ([RoutineLiftEntity])
 * simply never grouped in for a deleted parent.
 *
 * Nothing links a logged workout back to the routine it came from. Starting one seeds the set
 * list and then gets out of the way, exactly as re-logging a saved meal does — which is what lets
 * a routine be renamed or deleted without touching a single row of history.
 */
@Entity(tableName = "routine")
internal data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val isDeleted: Boolean = false,
)

/**
 * One planned lift: what to do, how many sets, at how many reps. **No load column** — a programme
 * is "Squat 3×5" and the weight moves every week, so a stored target would go stale within days
 * and turn the routine into something to maintain. The load comes from what was actually lifted
 * last time; see `Routine.toSets`.
 *
 * [routineId] is a plain column, not a foreign key — the parent/child join is a Kotlin fold over
 * two small lists, the call `SavedMealItemEntity` and `StrengthSetEntity` both make.
 */
@Entity(tableName = "routine_lift")
internal data class RoutineLiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val exerciseName: String,
    val sets: Int,
    val reps: Int,
)
