package ph.mart.healthapp.core.data.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalBackupsTest {

    private fun name(millis: Long) = "fitpulse-$millis.json"

    @Test
    fun `rotation keeps the newest three`() {
        val names = listOf(name(1_000), name(4_000), name(2_000), name(3_000))
        assertEquals(listOf(name(1_000)), staleBackups(names))
    }

    @Test
    fun `nothing is dropped below the limit`() {
        assertEquals(emptyList<String>(), staleBackups(listOf(name(1), name(2))))
        assertEquals(emptyList<String>(), staleBackups(emptyList()))
    }

    /**
     * A backup is written to `<name>.tmp` and renamed into place, so a run killed mid-write cannot
     * leave a truncated file that sorts newest and evicts a good one. That only holds while the
     * staging name fails this filter — move `.tmp` in front of `.json` and the half-written file
     * becomes both listable and rotatable, with nothing to say so.
     */
    @Test
    fun `a staging file is not a backup`() {
        assertTrue(isBackupName(name(1_000)))
        assertFalse(isBackupName(name(1_000) + ".tmp"))
        assertFalse(isBackupName("notes-1000.json"))

        // And so it is invisible to the rotation: four finished files beside one staging file
        // still drop exactly one.
        val onDisk = listOf(name(1), name(2), name(3), name(4), name(5) + ".tmp")
        assertEquals(listOf(name(1)), staleBackups(onDisk.filter(::isBackupName)))
    }

    /** The name is the only clock — sorting it must order the same way the numbers do. */
    @Test
    fun `the stamp round-trips out of the name`() {
        assertEquals(1_757_000_000_000L, savedAtMillis(name(1_757_000_000_000L)))
        assertEquals(0L, savedAtMillis("fitpulse-backup.json"))
    }
}
