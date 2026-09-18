package ph.mart.healthapp.core.data.note.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface NoteDayDao {
    @Query("SELECT * FROM note_day WHERE dateEpochDay = :date")
    fun observeForDate(date: Long): Flow<NoteDayEntity?>

    @Query("SELECT * FROM note_day WHERE text != '' ORDER BY dateEpochDay ASC")
    suspend fun allWritten(): List<NoteDayEntity>

    @Upsert
    suspend fun upsert(entity: NoteDayEntity)

    /** Blanked rather than deleted, the call [ph.mart.healthapp.core.data.mood.local.MoodDayDao]
     * makes: an emptied note is the row saying nothing, not a row that was never there. */
    @Query("UPDATE note_day SET text = ''")
    suspend fun clearAll()
}
