package ph.mart.healthapp.core.data.coach.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One chat bubble. [sentAtMillis] is the only ordering — a conversation is a sequence, not a
 * series of days, so there is no `date` column and nothing derives one.
 *
 * Both rows of an exchange are written together, so a `pending` flag would have no state to
 * describe: an unanswered question is never stored at all.
 */
@Entity(tableName = "chat_message")
internal data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromUser: Boolean,
    val text: String,
    val sentAtMillis: Long,
    val isDeleted: Boolean = false,
)
