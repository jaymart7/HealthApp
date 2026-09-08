package ph.mart.healthapp.core.data.profile

import kotlin.math.roundToInt
import ph.mart.healthapp.core.data.food.Nutrients

/**
 * A daily target for each of the seven nutrients, derived from the profile — never stored, never
 * edited, exactly as [calculateDailyTargets] derives calories.
 *
 * This reverses the app's long-standing "reported, never graded" position on fiber, sugar and
 * sodium, and the reason it can is that the objection was specific: *"there is nothing on the
 * profile to derive a fiber goal from."* There is. The Dietary Reference Intakes are published as
 * a function of **sex and age band**, and the profile has carried both since onboarding — they are
 * two of the six Mifflin–St Jeor inputs. Fiber and sugar go one better and ride the *calorie*
 * target itself, so editing a calorie target moves them the way it already moves the macro split.
 *
 * Figures are the IOM/NASEM reference values for adults. Deliberately not conditioned on pregnancy
 * or lactation: the profile records neither, and inventing the state to change the iron figure
 * would be a claim this app cannot stand behind — the same reasoning that keeps a fertile window
 * out of cycle tracking.
 */
fun nutrientTargets(profile: Profile, targets: DailyTargets): Nutrients = Nutrients(
    // IOM Adequate Intake: 14 g per 1000 kcal.
    fiberG = (targets.calories * FIBER_G_PER_KCAL).roundToInt(),
    // WHO: free sugars under 10% of energy, at 4 kcal per gram.
    sugarG = (targets.calories * SUGAR_ENERGY_FRACTION / KCAL_PER_CARB_G).roundToInt(),
    sodiumMg = SODIUM_LIMIT_MG,
    vitaminDUg = if (profile.age >= 71) VITAMIN_D_ELDER_UG else VITAMIN_D_ADULT_UG,
    calciumMg = calciumFor(profile),
    ironUg = ironFor(profile),
    potassiumMg = if (profile.sex == Sex.Male) POTASSIUM_MALE_MG else POTASSIUM_FEMALE_MG,
)

private const val FIBER_G_PER_KCAL = 14.0 / 1000
private const val SUGAR_ENERGY_FRACTION = 0.10
private const val KCAL_PER_CARB_G = 4.0

/** The CDRR, identical for both sexes across adulthood — the one figure here with no band. */
private const val SODIUM_LIMIT_MG = 2300

private const val VITAMIN_D_ADULT_UG = 15
private const val VITAMIN_D_ELDER_UG = 20

private const val CALCIUM_BASE_MG = 1000
private const val CALCIUM_RAISED_MG = 1200

/** Micrograms, matching [Nutrients.ironUg]: 18 mg for menstruating-age women, 8 mg otherwise. */
private const val IRON_HIGH_UG = 18_000
private const val IRON_BASE_UG = 8_000

private const val POTASSIUM_MALE_MG = 3400
private const val POTASSIUM_FEMALE_MG = 2600

/** Rises at 51 for women and at 71 for men — the one target whose band differs by sex *and* age. */
private fun calciumFor(profile: Profile): Int {
    val raised = when (profile.sex) {
        Sex.Female -> profile.age >= 51
        Sex.Male -> profile.age >= 71
    }
    return if (raised) CALCIUM_RAISED_MG else CALCIUM_BASE_MG
}

/** The 19–50 band for women is the only place iron differs, and it differs by more than double. */
private fun ironFor(profile: Profile): Int =
    if (profile.sex == Sex.Female && profile.age in 19..50) IRON_HIGH_UG else IRON_BASE_UG
