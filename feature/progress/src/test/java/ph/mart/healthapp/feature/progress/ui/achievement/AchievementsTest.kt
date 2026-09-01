package ph.mart.healthapp.feature.progress.ui.achievement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ph.mart.healthapp.core.data.fasting.FastSession
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.streak.StreakBadge
import ph.mart.healthapp.core.data.streak.StreakStats
import ph.mart.healthapp.feature.progress.ui.achievement.components.captionFor

private const val HOUR = 3_600_000L

private fun fast(hours: Long) = FastSession(startMillis = 0, endMillis = hours * HOUR)

private fun groups(
    streak: StreakStats = StreakStats(current = 0, best = 0, totalDaysLogged = 0),
    weightProgressKg: Double? = null,
    workoutCount: Int = 0,
    fasts: List<FastSession> = emptyList(),
    photoCount: Int = 0,
) = badgeGroups(streak, weightProgressKg, workoutCount, fasts, photoCount)
    .associateBy { it.family }

class AchievementsTest {

    @Test
    fun `the streak family reuses the shipped thresholds rather than a retyped copy`() {
        val streak = groups()[BadgeFamily.Streak]!!
        assertEquals(StreakBadge.entries.map { it.days }, streak.tiers)
    }

    @Test
    fun `a broken streak keeps the badges its best run earned`() {
        // Current run is one day; the best was 31. Four of the five tiers stay lit.
        val streak = groups(StreakStats(current = 1, best = 31, totalDaysLogged = 40))[BadgeFamily.Streak]!!
        assertEquals(31, streak.current)
        assertEquals(4, streak.earnedCount)
        assertEquals(100, streak.next)
    }

    @Test
    fun `the weight family is absent for a Maintain goal and floors everywhere else`() {
        assertNull(groups(weightProgressKg = null)[BadgeFamily.WeightMoved])
        // 4.9 kg has honestly not reached the 5 kg tier.
        assertEquals(4, groups(weightProgressKg = 4.9)[BadgeFamily.WeightMoved]!!.current)
        assertEquals(1, groups(weightProgressKg = 4.9)[BadgeFamily.WeightMoved]!!.earnedCount)
        // Moving the wrong way reads as zero, never a negative count.
        assertEquals(0, groups(weightProgressKg = -3.0)[BadgeFamily.WeightMoved]!!.current)
    }

    @Test
    fun `the longest fast is the max over completed sessions, in whole hours`() {
        val longest = groups(fasts = listOf(fast(14), fast(17), fast(9)))[BadgeFamily.LongestFast]!!
        assertEquals(17, longest.current)
        assertEquals(1, longest.earnedCount)
        assertEquals(24, longest.next)
    }

    @Test
    fun `next is the first unreached tier, and null once every one is earned`() {
        assertEquals(10, groups(photoCount = 3)[BadgeFamily.Photos]!!.next)
        assertNull(groups(photoCount = 99)[BadgeFamily.Photos]!!.next)
    }

    @Test
    fun `captions count up to the next threshold, and never below a tier of one`() {
        val workouts = groups(workoutCount = 9)[BadgeFamily.Workouts]!!
        assertEquals("1 more workout to your 10-workout badge.", captionFor(workouts, UnitSystem.Metric))

        val fasts = groups()[BadgeFamily.Fasts]!!
        assertEquals("Your first fast earns a badge.", captionFor(fasts, UnitSystem.Metric))

        val photos = groups(photoCount = 99)[BadgeFamily.Photos]!!
        assertEquals("Every badge earned.", captionFor(photos, UnitSystem.Metric))

        val weight = groups(weightProgressKg = 3.0)[BadgeFamily.WeightMoved]!!
        assertEquals("Reach 5 kg for the next badge.", captionFor(weight, UnitSystem.Metric))
        assertEquals("Reach 11 lb for the next badge.", captionFor(weight, UnitSystem.Imperial))

        val longest = groups(fasts = listOf(fast(17)))[BadgeFamily.LongestFast]!!
        assertEquals("A 24h fast earns the next badge.", captionFor(longest, UnitSystem.Metric))
    }
}
