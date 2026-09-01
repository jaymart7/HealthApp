package ph.mart.healthapp.core.data.coach.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ChatMessageDao {
    @Query("SELECT * FROM chat_message WHERE isDeleted = 0 ORDER BY sentAtMillis ASC, id ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    /** Newest first, so `LIMIT` takes the *end* of the conversation; the caller re-reverses it. */
    @Query("SELECT * FROM chat_message WHERE isDeleted = 0 ORDER BY sentAtMillis DESC, id DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ChatMessageEntity>

    @Insert
    suspend fun insert(entity: ChatMessageEntity)

    /** One transaction, so the diary of the conversation can never emit a frame holding a
     * question with no answer under it — `FoodEntryDao.replace`'s reason. */
    @Transaction
    suspend fun addExchange(question: ChatMessageEntity, answer: ChatMessageEntity) {
        insert(question)
        insert(answer)
    }

    @Query("UPDATE chat_message SET isDeleted = 1")
    suspend fun softDeleteAll()
}
