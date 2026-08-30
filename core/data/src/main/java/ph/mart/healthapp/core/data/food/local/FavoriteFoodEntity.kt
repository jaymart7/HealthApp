package ph.mart.healthapp.core.data.food.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** A food the user starred in the add-entry sheet. Keyed by [name] — the same key the suggestion
 * list dedupes recents against — and carries its own macros so a re-star doesn't depend on the
 * original diary row still being there.
 *
 * Un-starring flips [isFavorite] rather than deleting the row: soft delete only, and it keeps the
 * macros for the next star. */
@Entity(tableName = "favorite_food")
internal data class FavoriteFoodEntity(
    @PrimaryKey val name: String,
    val portionAmount: Double,
    val portionUnit: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val fiberG: Int = 0,
    val sugarG: Int = 0,
    val sodiumMg: Int = 0,
    val isFavorite: Boolean = true,
)
