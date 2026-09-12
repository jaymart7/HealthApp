package ph.mart.healthapp.feature.progress.ui.comparison

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import ph.mart.healthapp.core.data.progress.ProgressPhoto

private val PHOTOS = listOf(
    ProgressPhoto(id = 1, dateEpochDay = 100, filePath = "a.jpg", weightKg = 80.0),
    ProgressPhoto(id = 2, dateEpochDay = 130, filePath = "b.jpg", weightKg = 77.5),
    ProgressPhoto(id = 3, dateEpochDay = 160, filePath = "c.jpg", weightKg = null),
)

class ComparisonPairTest {

    @Test
    fun `pairs older first whichever order they were tapped in`() {
        val tappedNewestFirst = comparisonPair(PHOTOS, listOf(2L, 1L))
        assertNotNull(tappedNewestFirst)
        assertEquals(1L, tappedNewestFirst!!.older.id)
        assertEquals(2L, tappedNewestFirst.newer.id)

        val tappedOldestFirst = comparisonPair(PHOTOS, listOf(1L, 2L))
        assertEquals(tappedNewestFirst, tappedOldestFirst)
    }

    @Test
    fun `delta is newer minus older, so a loss reads negative`() {
        assertEquals(-2.5, comparisonPair(PHOTOS, listOf(1L, 2L))!!.weightDeltaKg!!, 0.001)
    }

    @Test
    fun `delta is a gain when the newer shot is the heavier one`() {
        val bulking = listOf(
            ProgressPhoto(id = 1, dateEpochDay = 100, filePath = "a.jpg", weightKg = 70.0),
            ProgressPhoto(id = 2, dateEpochDay = 130, filePath = "b.jpg", weightKg = 72.0),
        )
        assertEquals(2.0, comparisonPair(bulking, listOf(2L, 1L))!!.weightDeltaKg!!, 0.001)
    }

    @Test
    fun `no delta when either shot was taken without a weight`() {
        assertNull(comparisonPair(PHOTOS, listOf(2L, 3L))!!.weightDeltaKg)
    }

    @Test
    fun `no pair below or above two selections`() {
        assertNull(comparisonPair(PHOTOS, emptyList()))
        assertNull(comparisonPair(PHOTOS, listOf(1L)))
        assertNull(comparisonPair(PHOTOS, listOf(1L, 2L, 3L)))
    }

    @Test
    fun `no pair when a selected photo is no longer in the set`() {
        assertNull(comparisonPair(PHOTOS, listOf(1L, 99L)))
        assertNull(comparisonPair(emptyList(), listOf(1L, 2L)))
    }
}
