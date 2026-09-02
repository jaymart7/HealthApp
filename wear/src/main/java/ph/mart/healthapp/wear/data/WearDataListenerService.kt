package ph.mart.healthapp.wear.data

import androidx.wear.tiles.TileService
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import ph.mart.healthapp.core.today.TODAY_PATH
import ph.mart.healthapp.wear.tile.TodayTileService

/**
 * Exists for the tile alone.
 *
 * The app collects the data item itself while it is open, but a tile has no lifecycle and cannot
 * observe anything — it is asked to render and answers once. A phone cannot poke a tile either,
 * so the push has to be turned into a refresh request on this side. The tile's own freshness
 * interval is the fallback for a push that never arrives, not the mechanism.
 */
class WearDataListenerService : WearableListenerService() {

    override fun onDataChanged(events: DataEventBuffer) {
        val touchedToday = events.any { it.dataItem.uri.path == TODAY_PATH }
        events.release()
        if (touchedToday) {
            TileService.getUpdater(this).requestUpdate(TodayTileService::class.java)
        }
    }
}
