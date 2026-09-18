package ph.mart.healthapp.core.data.note

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ph.mart.healthapp.core.data.note.local.NoteDayDao
import ph.mart.healthapp.core.data.note.local.NoteDayEntity

internal class NoteRepositoryImpl(private val dao: NoteDayDao) : NoteRepository {

    override fun observeForDate(dateEpochDay: Long): Flow<DayNote> =
        dao.observeForDate(dateEpochDay).map { it?.toDomain() ?: DayNote(dateEpochDay, text = "") }

    /** The trim and the cap happen here, not in the field: an import and the diary's sheet are two
     * callers, and a rule enforced at one of them is a rule the other can break. */
    override suspend fun setNote(dateEpochDay: Long, text: String) {
        dao.upsert(NoteDayEntity(dateEpochDay = dateEpochDay, text = text.toNoteText()))
    }

    override suspend fun allNotes(): List<DayNote> = dao.allWritten().map { it.toDomain() }

    override suspend fun clearAllNotes() {
        dao.clearAll()
    }
}

private fun NoteDayEntity.toDomain() = DayNote(dateEpochDay = dateEpochDay, text = text)
