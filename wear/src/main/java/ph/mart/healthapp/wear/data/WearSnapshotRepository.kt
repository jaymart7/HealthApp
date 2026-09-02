package ph.mart.healthapp.wear.data

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import ph.mart.healthapp.core.today.KEY_SNAPSHOT
import ph.mart.healthapp.core.today.MSG_ADD_GLASS
import ph.mart.healthapp.core.today.MSG_TOGGLE_FAST
import ph.mart.healthapp.core.today.TODAY_PATH
import ph.mart.healthapp.core.today.TodaySnapshot
import ph.mart.healthapp.core.today.decodeSnapshot

private const val TAG = "WearSnapshot"

/**
 * Everything the watch knows about today, and the only two things it can change.
 *
 * There is no database here and there is not going to be one: Room stays on the phone, and the
 * Data Layer's own persisted data item *is* the cache — the last snapshot pushed is readable on
 * the watch with the phone switched off, so a cold start with no connection still draws the
 * morning's numbers rather than a spinner. Null means nothing has ever arrived.
 *
 * `Tasks.await` on [Dispatchers.IO] rather than `kotlinx-coroutines-play-services`, the call
 * `GoogleHealthAuth` already made on the phone: one blocking call is not worth a dependency.
 */
class WearSnapshotRepository(private val context: Context) {

    /**
     * Seeded from the stored item, then live. The seed is what makes the app usable out of
     * range; the listener is what makes a glass logged on the phone show up on the wrist without
     * a refresh.
     */
    val snapshots: Flow<TodaySnapshot?> = flow {
        // Seeded before the listener is attached, not after: the read does IO, and a push landing
        // while it was in flight would otherwise be overwritten by the older stored value and stay
        // wrong until the next one.
        emit(latestSnapshot(context))
        emitAll(dataChanges())
    }

    private fun dataChanges(): Flow<TodaySnapshot?> = callbackFlow {
        val client = Wearable.getDataClient(context)
        val listener = DataClient.OnDataChangedListener { events ->
            events.forEach { event ->
                if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == TODAY_PATH) {
                    trySend(event.dataItem.snapshot())
                }
            }
            events.release()
        }
        client.addListener(listener)
        awaitClose { client.removeListener(listener) }
    }

    /** True once the phone has taken the tap. See [sendMessage] for why a false is shown rather
     * than swallowed. */
    suspend fun addGlass(): Boolean = sendMessage(MSG_ADD_GLASS)

    suspend fun toggleFast(): Boolean = sendMessage(MSG_TOGGLE_FAST)

    /**
     * Sent to every connected node rather than to one resolved by capability. A phone with
     * FitPulse installed is the only node that can answer, an extra node cannot be harmed by a
     * path it doesn't listen for, and a capability declaration would be a second file to keep in
     * step for no behaviour.
     *
     * A failure is returned, never faked: the watch has no Room to write to, so an optimistic
     * tick that evaporated on the next push would be worse than a refused one.
     *
     * ponytail: no offline queue — the tap is lost when the phone is unreachable. A data item
     * outbox keyed by id, deleted by the phone once applied, is the upgrade path if wrist logging
     * out of range turns out to matter.
     */
    private suspend fun sendMessage(path: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
            nodes.count { node ->
                runCatching {
                    Tasks.await(Wearable.getMessageClient(context).sendMessage(node.id, path, null))
                }.isSuccess
            } > 0
        }.getOrElse {
            Log.d(TAG, "$path not delivered: ${it.message}")
            false
        }
    }
}

/**
 * The stored snapshot, or null. Shared with the tile, which has no lifecycle to hold a flow and
 * reads this once per render.
 */
suspend fun latestSnapshot(context: Context): TodaySnapshot? = withContext(Dispatchers.IO) {
    runCatching {
        val items = Tasks.await(Wearable.getDataClient(context).dataItems)
        val snapshot = items.firstOrNull { it.uri.path == TODAY_PATH }?.snapshot()
        items.release()
        snapshot
    }.getOrElse {
        Log.d(TAG, "No snapshot on this watch yet: ${it.message}")
        null
    }
}

private fun com.google.android.gms.wearable.DataItem.snapshot(): TodaySnapshot? =
    DataMapItem.fromDataItem(this).dataMap.getString(KEY_SNAPSHOT)?.let(::decodeSnapshot)
