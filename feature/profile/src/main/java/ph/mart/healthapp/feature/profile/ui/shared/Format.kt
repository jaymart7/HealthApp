package ph.mart.healthapp.feature.profile.ui.shared

/** A body figure, printed the way it was measured: 62 rather than 62.0, but 62.4 kept whole.
 * Not copy — a number format, and the same one Home, onboarding and both weight sheets already
 * keep their own private copy of. Shared across this feature's flows because the Profile header
 * and About you must never disagree about the digit they print for the same kilo. */
internal fun formatBodyValue(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString() else "%.1f".format(value)
