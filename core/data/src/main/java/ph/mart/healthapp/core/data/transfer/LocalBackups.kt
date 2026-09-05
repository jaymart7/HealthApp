package ph.mart.healthapp.core.data.transfer

import android.content.Context
import java.io.File

/** One backup file on disk. [savedAtMillis] is read out of the name, not off the filesystem. */
data class LocalBackup(val name: String, val savedAtMillis: Long)

/**
 * ponytail: three files — a bad week, a bad fortnight and a bad month. A size budget is the
 * upgrade path if a year of logging ever makes them big enough to matter.
 */
internal const val BACKUP_KEEP = 3

private const val BACKUP_DIR = "backups"
private const val BACKUP_PREFIX = "fitpulse-"
private const val BACKUP_SUFFIX = ".json"

/**
 * The weekly backup's store: app-private, rotated, and readable with no picker.
 *
 * Written to `filesDir` rather than to a user-chosen folder because SAF needs a picker, a picker
 * needs a user, and a user is the thing a background job does not have. `filesDir` is also what
 * Android's own backup covers, so one write serves both mechanisms.
 *
 * A plain class, not a repository interface: there is one implementation and nothing to swap, and
 * the interface rule exists to keep Room types out of `:feature:*` — there are none here.
 */
class LocalBackups(private val context: Context) {

    private val dir: File get() = File(context.filesDir, BACKUP_DIR).apply { mkdirs() }

    /** Newest first. */
    fun list(): List<LocalBackup> = names().sortedDescending().map { LocalBackup(it, savedAtMillis(it)) }

    fun read(name: String): String = File(dir, File(name).name).readText()

    /** Writes one and drops everything past the newest [BACKUP_KEEP]. */
    fun write(json: String) {
        File(dir, "$BACKUP_PREFIX${System.currentTimeMillis()}$BACKUP_SUFFIX").writeText(json)
        staleBackups(names()).forEach { File(dir, it).delete() }
    }

    private fun names(): List<String> =
        dir.list().orEmpty().filter { it.startsWith(BACKUP_PREFIX) && it.endsWith(BACKUP_SUFFIX) }
}

/**
 * The names to delete: everything but the newest [keep].
 *
 * Sorted by *name*, which is the whole reason the file is stamped in epoch millis — those are
 * fixed-width for the next two centuries, so the name orders chronologically and is the single
 * source of both this and the date shown beside the row. `lastModified()` is deliberately not
 * consulted: a device transfer or a restore can reset it.
 */
internal fun staleBackups(names: List<String>, keep: Int = BACKUP_KEEP): List<String> =
    names.sortedDescending().drop(keep)

/** 0 for a name this build didn't write — the row still restores, it just has no date to show. */
internal fun savedAtMillis(name: String): Long =
    name.removePrefix(BACKUP_PREFIX).removeSuffix(BACKUP_SUFFIX).toLongOrNull() ?: 0L
