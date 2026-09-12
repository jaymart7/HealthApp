package ph.mart.healthapp.core.data.progress

import ph.mart.healthapp.core.data.profile.CM_PER_IN
import ph.mart.healthapp.core.data.profile.KG_PER_LB
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BodyIndexTest {

    @Test
    fun `bmi is weight over height in metres squared`() {
        assertEquals(22.9, bmiOf(weightKg = 70.0, heightCm = 175.0)!!, 0.0)
    }

    @Test
    fun `a missing height is null rather than an infinity`() {
        assertNull(bmiOf(weightKg = 70.0, heightCm = 0.0))
        assertNull(waistToHeightOf(waistCm = 84.0, heightCm = 0.0))
        assertNull(waistToHeightOf(waistCm = 0.0, heightCm = 175.0))
    }

    /** The one thing an off-by-one here would silently invert: every boundary belongs to the band
     * above it, so 25.0 is Overweight and never Healthy. */
    @Test
    fun `each band boundary lands in the higher band`() {
        assertEquals(BmiCategory.Underweight, bmiCategoryOf(18.4))
        assertEquals(BmiCategory.Healthy, bmiCategoryOf(18.5))
        assertEquals(BmiCategory.Healthy, bmiCategoryOf(24.9))
        assertEquals(BmiCategory.Overweight, bmiCategoryOf(25.0))
        assertEquals(BmiCategory.Overweight, bmiCategoryOf(29.9))
        assertEquals(BmiCategory.Obese, bmiCategoryOf(30.0))
    }

    /** Why neither function converts: the same body in pounds and inches is the same figure, so
     * the display toggle has nothing to say about either one. */
    @Test
    fun `both figures are unit-free`() {
        val metricBmi = bmiOf(weightKg = 70.0, heightCm = 175.0)!!
        val imperialBmi = 703 * (70.0 / KG_PER_LB) / ((175.0 / CM_PER_IN) * (175.0 / CM_PER_IN))
        assertEquals(metricBmi, imperialBmi, 0.05)

        assertEquals(
            waistToHeightOf(waistCm = 84.0, heightCm = 175.0)!!,
            waistToHeightOf(waistCm = 84.0 / CM_PER_IN, heightCm = 175.0 / CM_PER_IN)!!,
            0.0,
        )
    }

    @Test
    fun `fat and lean mass split the weigh-in between them`() {
        assertEquals(16.4, fatMassKgOf(weightKg = 82.0, bodyFatPercent = 20.0)!!, 0.0)
        assertEquals(65.6, leanMassKgOf(weightKg = 82.0, bodyFatPercent = 20.0)!!, 0.0)
        assertEquals(
            82.0,
            fatMassKgOf(82.0, 18.5)!! + leanMassKgOf(82.0, 18.5)!!,
            0.1,
        )
    }

    /** A percentage at or past 100 reports a negative lean mass if it is let through, and a body
     * fat of zero is nobody — both are a slipped finger, not a reading. */
    @Test
    fun `an impossible percentage is null rather than a negative mass`() {
        assertNull(leanMassKgOf(weightKg = 82.0, bodyFatPercent = 100.0))
        assertNull(leanMassKgOf(weightKg = 82.0, bodyFatPercent = 0.0))
        assertNull(leanMassKgOf(weightKg = 0.0, bodyFatPercent = 20.0))
        assertNull(fatMassKgOf(weightKg = 82.0, bodyFatPercent = 120.0))
    }

    /** Unlike the two ratios, these take stored kg and hand back stored kg — the screen converts.
     * Reaching for `kgToDisplayUnit` on the way *in* would report a lean mass of 144 to somebody
     * who weighs 82. */
    @Test
    fun `fat and lean mass are stored kilos in and stored kilos out`() {
        val lean = leanMassKgOf(weightKg = 82.0, bodyFatPercent = 20.0)!!
        assertEquals(65.6, lean, 0.0)
        assertEquals(lean / KG_PER_LB, lean.kgToDisplayUnit(UnitSystem.Imperial), 0.05)
    }

    @Test
    fun `waist to height keeps two decimals so a reading never rounds onto the boundary`() {
        assertEquals(0.48, waistToHeightOf(waistCm = 84.0, heightCm = 175.0)!!, 0.0)
        assertEquals(0.49, waistToHeightOf(waistCm = 86.0, heightCm = 175.0)!!, 0.0)
        assertEquals(WAIST_TO_HEIGHT_HEALTHY_MAX, waistToHeightOf(waistCm = 87.5, heightCm = 175.0)!!, 0.0)
    }
}
