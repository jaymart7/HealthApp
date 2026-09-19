package ph.mart.healthapp.core.data.supplement.local

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import ph.mart.healthapp.core.data.food.Nutrients
import ph.mart.healthapp.core.data.supplement.EVERY_DAY

/** The user's own list. [deleted] is the soft delete — the row stays so past `supplement_day`
 * rows keep a name to render. */
@Entity(tableName = "supplement")
internal data class SupplementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dose: String,
    val timesPerDay: Int,
    val deleted: Boolean,
    val createdAt: Long,
    /** Per dose, flattened into seven columns — `food_entry`'s shape and for its reason: one
     * value type rather than seven fields repeated across every carrier. */
    @Embedded val nutrients: Nutrients = Nutrients(),
    /** The panel as printed. Empty for every supplement nobody scanned. */
    val panel: String = "",
    /** The Monday-first weekday mask — see [ph.mart.healthapp.core.data.supplement.Supplement.days].
     * Defaulted so the migration's `DEFAULT 127` and a fresh row agree. */
    val days: Int = EVERY_DAY,
)
