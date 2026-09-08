package ph.mart.healthapp.core.data.food.local

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import ph.mart.healthapp.core.data.food.Nutrients

/**
 * A product FoodData Central has already resolved for a scanned barcode, kept so the second scan of
 * the same package costs nothing. It makes the scanner work offline and stops a rescan re-spending
 * the app-wide FDC key budget.
 *
 * Keyed by the normalised barcode — digits, leading zeros stripped — which is what
 * `barcodeKey` produces and the only identity the two widths of the same GTIN share.
 *
 * Fields below the key mirror `ScannedProduct` one for one, as [FavoriteFoodEntity] does.
 *
 * ponytail: no timestamp, no TTL and no eviction — a GTIN's nutrition panel doesn't change, and a
 * row is ~100 bytes. Add a `scannedAt` and a trim if a scanner-heavy install ever makes the table
 * worth measuring.
 */
@Entity(tableName = "scanned_product")
internal data class ScannedProductEntity(
    @PrimaryKey val barcode: String,
    val name: String,
    val portionAmount: Double,
    val portionUnit: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    @Embedded val nutrients: Nutrients = Nutrients(),
)
