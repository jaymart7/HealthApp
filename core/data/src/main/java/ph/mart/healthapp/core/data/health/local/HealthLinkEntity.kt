package ph.mart.healthapp.core.data.health.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One row per data point that has crossed the boundary, in either direction. It is the whole of
 * FitPulse's sync bookkeeping — there is no cursor table and no "connected" flag anywhere.
 *
 * - **Import dedup:** [remoteName] is the primary key, so re-syncing an overlapping window can
 *   never duplicate a workout.
 * - **The sync cursor:** `MAX(remoteTimeMillis)` per [dataType] is where the next window starts.
 *   Derived rather than stored, so a failed sync can't advance a cursor past data it never wrote.
 * - **Deletion:** [pushed] separates "delete what we imported" (soft-delete [localId] in
 *   [localTable]) from "delete what we sent" (`dataPoints:batchDelete` on [remoteName]).
 *
 * Deliberately *not* exported by `ProfileExport` — a restored backup on another device has no
 * relationship to these remote names, the same reasoning that keeps saved meals out.
 */
@Entity(tableName = "health_link")
internal data class HealthLinkEntity(
    /** `users/me/dataTypes/exercise/dataPoints/abc123` — the API's own resource name. */
    @PrimaryKey val remoteName: String,
    val dataType: String,
    /** Empty for a pushed row: what we sent lives in the table it was logged in already. */
    val localTable: String,
    val localId: Long,
    /** The data point's own time, not when we synced — that is what makes it a usable cursor. */
    val remoteTimeMillis: Long,
    /** true = FitPulse wrote it to Google Health; false = FitPulse imported it. */
    val pushed: Boolean,
)
