package ph.mart.healthapp.core.data.food

/**
 * One naming rule for every product that arrives from a food database — FoodData Central's
 * `foods/search` rows and Open Food Facts' products both — so a scanned package reads the same
 * however it was resolved.
 *
 * The brand leads, since two brands' version of the same product are otherwise indistinguishable in
 * the diary, and it is dropped when the name already opens with it ("Nutella" branded "Nutella").
 *
 * Null when there is no name: a product with none is unusable in the diary, and a half-filled row
 * is worse than none.
 */
internal fun brandedName(brand: String?, description: String?): String? {
    val name = description?.normalizeCase()?.takeIf { it.isNotEmpty() } ?: return null
    val brandName = brand?.normalizeCase().orEmpty()
    return when {
        brandName.isEmpty() -> name
        name.startsWith(brandName, ignoreCase = true) -> name
        else -> "$brandName · $name"
    }
}

/**
 * All-caps in, title case out; anything already carrying lowercase is left as its source wrote it.
 *
 * FDC shouts its branded descriptions ("SPICY SWEET CHILI FLAVORED TORTILLA CHIPS") while its
 * Foundation rows are ordinary prose ("Broccoli, raw"), so only the all-caps ones are recased —
 * title-casing everything would turn "Broccoli, raw" into "Broccoli, Raw".
 */
private fun String.normalizeCase(): String {
    val trimmed = trim()
    if (trimmed != trimmed.uppercase()) return trimmed
    return trimmed.split(' ').joinToString(" ") { word ->
        word.lowercase().replaceFirstChar(Char::uppercaseChar)
    }
}
