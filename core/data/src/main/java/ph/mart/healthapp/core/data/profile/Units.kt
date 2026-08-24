package ph.mart.healthapp.core.data.profile

const val CM_PER_IN = 2.54
const val KG_PER_LB = 0.453592

fun round1(value: Double): Double = kotlin.math.round(value * 10) / 10

fun UnitSystem.weightUnitLabel(): String = if (this == UnitSystem.Imperial) "lb" else "kg"
fun UnitSystem.lengthUnitLabel(): String = if (this == UnitSystem.Imperial) "in" else "cm"

fun Double.kgToDisplayUnit(unit: UnitSystem): Double =
    round1(if (unit == UnitSystem.Imperial) this / KG_PER_LB else this)

fun Double.displayUnitToKg(unit: UnitSystem): Double =
    if (unit == UnitSystem.Imperial) this * KG_PER_LB else this

fun Double.cmToDisplayUnit(unit: UnitSystem): Double =
    round1(if (unit == UnitSystem.Imperial) this / CM_PER_IN else this)

fun Double.displayUnitToCm(unit: UnitSystem): Double =
    if (unit == UnitSystem.Imperial) this * CM_PER_IN else this
