package ph.mart.healthapp.core.data.food.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert

@Dao
internal interface ScannedProductDao {
    @Query("SELECT * FROM scanned_product WHERE barcode = :barcode")
    suspend fun find(barcode: String): ScannedProductEntity?

    /** Upsert rather than insert: re-scanning a product FDC has since corrected takes the new
     * figures, and the barcode is the identity either way. */
    @Upsert
    suspend fun upsert(entity: ScannedProductEntity)
}
