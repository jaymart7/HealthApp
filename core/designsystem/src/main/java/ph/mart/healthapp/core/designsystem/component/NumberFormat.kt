package ph.mart.healthapp.core.designsystem.component

import java.util.Locale
import kotlin.math.roundToInt

/**
 * The one place a decimal figure becomes a string. Nine copies of [formatOneDecimal] used to sit
 * across five modules under seven names; they are this file now.
 *
 * **Every user-facing decimal in this app is ASCII, and [Locale.US] is what holds that.** The
 * default locale's `"%.1f"` writes `75,5` in de/fr/es/pt/id/ru — but a figure shown in one of
 * these fields is typed back into it, and the two halves of that round trip do not speak the same
 * alphabet: [String.keepDigits] filters to `'.'` and `String.toDoubleOrNull` parses only `'.'`.
 * A comma-formatted seed made the stepper's +/- stop refreshing the display and turned a typed
 * `75,5` into `7552`. Formatter, filter and parser agree on one separator or the field lies.
 *
 * The flip side — `keepDigits` accepting a `','` from a comma-locale keyboard — is in
 * `NumericStepperField.kt`. `NumberFormatTest` and `NumericStepperFieldTest` hold both ends.
 */

/** [decimals] places, fixed. `0` rounds to a whole number rather than printing a bare point. */
fun formatDecimals(value: Double, decimals: Int): String =
    if (decimals == 0) value.roundToInt().toString() else String.format(Locale.US, "%.${decimals}f", value)

/** One decimal, with a whole number left whole — `75`, not `75.0`; `75.5` as itself. What every
 * weight, measurement and body figure in the app is drawn with. */
fun formatOneDecimal(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString() else formatDecimals(value, 1)
