package ph.mart.healthapp.core.data.coach.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One chat bubble. [sentAtMillis] is the only ordering — a conversation is a sequence, not a
 * series of days, so there is no `date` column and nothing derives one.
 *
 * Both rows of an exchange are written together, so a `pending` flag would have no state to
 * describe: an unanswered question is never stored at all.
 *
 * [receipt] is what a confirmed draft *wrote*, and it is its own column rather than a line joined
 * onto [text]. Joined, the screen could not tell the coach's prose from the app's report once the
 * turn came back out of Room — it drew as one paragraph on reload while the live turn drew it under
 * a rule with a check beside it, so the same answer looked different depending on when you read it.
 * Null on every message that logged nothing, which is nearly all of them.
 *
 * [report] is the *window* of the report card that turn drew, in days, and it is a column for
 * [receipt]'s reason — the card is the app reporting, not the coach talking. Only the window is
 * stored: every figure on the card is re-folded from Room at render, so a report read next week
 * is folded against the rows as they are then rather than showing a number that has moved.
 */
@Entity(tableName = "chat_message")
internal data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromUser: Boolean,
    val text: String,
    val sentAtMillis: Long,
    val isDeleted: Boolean = false,
    val receipt: String? = null,
    val report: Int? = null,
)
