package ph.mart.healthapp.core.data.fasting

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ph.mart.healthapp.core.data.fasting.local.FastSessionDao
import ph.mart.healthapp.core.data.fasting.local.FastSessionEntity

internal class FastingRepositoryImpl(private val dao: FastSessionDao) : FastingRepository {

    override fun observeActive(): Flow<FastSession?> = dao.observeActive().map { it?.toDomain() }

    override fun observeSessions(): Flow<List<FastSession>> =
        dao.observeCompleted().map { entities -> entities.map { it.toDomain() } }

    /** The guard is a read-then-write rather than a unique index: SQLite treats every NULL as
     * distinct, so `endMillis` can't carry a uniqueness constraint that means anything. */
    override suspend fun start(goalHours: Int) {
        if (dao.activeNow() != null) return
        dao.insertActive(System.currentTimeMillis(), goalHours.coerceIn(FAST_GOAL_HOURS))
    }

    override suspend fun stop() {
        dao.stopActive(System.currentTimeMillis())
    }

    override suspend fun discardActive() {
        dao.deleteActive()
    }

    override suspend fun upsertSession(session: FastSession) {
        dao.upsert(
            FastSessionEntity(
                id = session.id,
                startMillis = session.startMillis,
                endMillis = session.endMillis,
                goalHours = session.goalHours.coerceIn(FAST_GOAL_HOURS),
            ),
        )
    }

    override suspend fun allSessions(): List<FastSession> = dao.allCompleted().map { it.toDomain() }

    override suspend fun clearAllSessions() {
        dao.clearAll()
    }
}

private fun FastSessionEntity.toDomain() = FastSession(
    id = id,
    startMillis = startMillis,
    endMillis = endMillis,
    goalHours = goalHours,
)
