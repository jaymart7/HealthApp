package ph.mart.healthapp.core.data

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * Words a pure function has chosen and a composable will spell — the split that lets a branch be
 * tested on the JVM while its wording lives in `strings.xml`.
 *
 * Composables resolve; ViewModels name. A function that used to return an English sentence returns
 * one of these, its test asserts *which* phrase with *which* figures, and the screen resolves it
 * with `LocalResources.current`. An argument that is itself a [Phrase] is resolved first, which is
 * how "Last: 60 kg × 8 · 3 sets" nests a load inside a plural inside a sentence.
 *
 * [Raw] is for text that is already final — a unit symbol, a formatted date, a figure — sitting
 * where a sentence could also sit.
 */
sealed interface Phrase {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : Phrase
    data class Plural(@PluralsRes val id: Int, val quantity: Int, val args: List<Any> = emptyList()) : Phrase
    data class Raw(val text: String) : Phrase
}

fun phrase(@StringRes id: Int, vararg args: Any): Phrase = Phrase.Res(id, args.toList())

fun plural(@PluralsRes id: Int, quantity: Int, vararg args: Any): Phrase = Phrase.Plural(id, quantity, args.toList())

/** No-argument resources are read unformatted: `"% taken"` is a literal, and running it through
 * `String.format` would throw. */
fun Phrase.resolve(res: Resources): String = when (this) {
    is Phrase.Res -> if (args.isEmpty()) res.getString(id) else res.getString(id, *args.resolvedIn(res))
    is Phrase.Plural ->
        if (args.isEmpty()) res.getQuantityString(id, quantity) else res.getQuantityString(id, quantity, *args.resolvedIn(res))
    is Phrase.Raw -> text
}

private fun List<Any>.resolvedIn(res: Resources): Array<Any> =
    map { if (it is Phrase) it.resolve(res) else it }.toTypedArray()
