package ph.mart.healthapp.core.data.progress.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface WeightEntryDao {
    @Query("SELECT * FROM weight_entry ORDER BY date ASC")
    fun observeAll(): Flow<List<WeightEntryEntity>>

    /** The newest weigh-in by date — what [ph.mart.healthapp.core.data.profile.Profile.latestWeighInKg]
     * joins in, so the calorie target re-prices off the scale rather than the onboarding figure. */
    @Query("SELECT weightKg FROM weight_entry ORDER BY date DESC LIMIT 1")
    fun observeLatestKg(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WeightEntryEntity)

    /** A real DELETE, not a soft one: the table is keyed by date and has no tombstone column, and
     * the only caller is a user asking to remove a weigh-in that Google Health put here. */
    @Query("DELETE FROM weight_entry WHERE date = :date")
    suspend fun delete(date: Long)
}
