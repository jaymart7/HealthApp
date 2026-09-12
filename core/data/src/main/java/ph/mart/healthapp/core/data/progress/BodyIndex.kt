package ph.mart.healthapp.core.data.progress

import androidx.annotation.StringRes
import ph.mart.healthapp.core.data.R
import ph.mart.healthapp.core.data.profile.round1

/**
 * The two body-composition figures FitPulse already holds every input for: BMI off the profile's
 * height and the newest weigh-in, waist-to-height off that same height and the newest waist
 * reading. Both derived on read, like [withMovingAverage] and the streak — no table, no column,
 * nothing exported.
 *
 * Neither converts a unit. A ratio is the same number in pounds and inches as in kilos and
 * centimetres, so the display toggle has nothing to say about either one, and the callers hand
 * them stored kg/cm rather than what the screen happens to be showing.
 */

/** Null rather than a throw for an absent or zero input — the profile is nullable upstream and a
 * missing height is an ordinary state, not an error. A divide would give an infinity. */
fun bmiOf(weightKg: Double, heightCm: Double): Double? {
    if (weightKg <= 0 || heightCm <= 0) return null
    val heightM = heightCm / 100
    return round1(weightKg / (heightM * heightM))
}

/**
 * The WHO bands, shown as a plain label and nothing more — no advice copy, no "talk to someone".
 * That is [ph.mart.healthapp.core.data.bloodpressure.BloodPressureCategory]'s rule applied a second
 * time, and for the same reason: 24.1 means nothing to most people without the word beside it, and
 * grading it is not this app's job.
 *
 * Nothing here carries a severity the way that enum does. A band is not a trend, and `error` is
 * reserved for genuinely off-track.
 */
enum class BmiCategory(@StringRes val label: Int) {
    Underweight(R.string.data_bmi_category_underweight),
    Healthy(R.string.data_bmi_category_healthy),
    Overweight(R.string.data_bmi_category_overweight),
    Obese(R.string.data_bmi_category_obese),
}

/** Each boundary lands in the *higher* band — 25.0 is Overweight, not Healthy — which is how the
 * bands are published and the one thing an off-by-one here would silently invert. */
fun bmiCategoryOf(bmi: Double): BmiCategory = when {
    bmi < 18.5 -> BmiCategory.Underweight
    bmi < 25.0 -> BmiCategory.Healthy
    bmi < 30.0 -> BmiCategory.Overweight
    else -> BmiCategory.Obese
}

/** Two decimals, because the whole figure lives between 0.4 and 0.6 and one decimal would round
 * every reading onto the boundary itself. */
fun waistToHeightOf(waistCm: Double, heightCm: Double): Double? {
    if (waistCm <= 0 || heightCm <= 0) return null
    return kotlin.math.round(waistCm / heightCm * 100) / 100
}

/** "Keep your waist to less than half your height" — reported as a boundary, never as a band. One
 * published number is a fact the app can stand behind; a four-step scale derived from it would be
 * the app grading a body, which is the line the cycle tab's absent fertile window also draws. */
const val WAIST_TO_HEIGHT_HEALTHY_MAX = 0.5

/**
 * The third and fourth figures, once a body fat reading exists: what of the newest weigh-in is fat
 * and what is everything else. Same shape as the two above — derived on read, no column, no export
 * field — and the same null-rather-than-throw rule, because a percentage at or past 100 is not a
 * body, it is a typo that would report a negative mass.
 *
 * Unlike BMI and waist-to-height these two **do** convert on the way out: a ratio is the same
 * number in pounds as in kilos, a mass is not. They still take stored kg, and the screen applies
 * [kgToDisplayUnit][ph.mart.healthapp.core.data.profile.kgToDisplayUnit] the way it does to every
 * other weight it draws.
 */
fun fatMassKgOf(weightKg: Double, bodyFatPercent: Double): Double? {
    if (weightKg <= 0 || bodyFatPercent <= 0 || bodyFatPercent >= 100) return null
    return round1(weightKg * bodyFatPercent / 100)
}

fun leanMassKgOf(weightKg: Double, bodyFatPercent: Double): Double? {
    if (weightKg <= 0 || bodyFatPercent <= 0 || bodyFatPercent >= 100) return null
    return round1(weightKg * (1 - bodyFatPercent / 100))
}
