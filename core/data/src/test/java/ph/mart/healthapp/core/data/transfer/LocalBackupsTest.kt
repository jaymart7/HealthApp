package ph.mart.healthapp.core.data.transfer

import org.junit.Assert.assertEquals
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

    /** The name is the only clock — sorting it must order the same way the numbers do. */
    @Test
    fun `the stamp round-trips out of the name`() {
        assertEquals(1_757_000_000_000L, savedAtMillis(name(1_757_000_000_000L)))
        assertEquals(0L, savedAtMillis("fitpulse-backup.json"))
    }
}
