package ph.mart.healthapp.core.designsystem.component

/** What the three Google Health cards say under their switch: on, but nothing to draw yet. */
private const val WATCH_NOTE = "Shows once Google Health has synced"

/**
 * Every card on Home the user can move or hide, in the order an untouched install renders them.
 *
 * The declaration order **is** the default layout, and it matches `HomeCards.kt` — so a profile
 * that has never opened the editor draws exactly what it drew before the editor existed.
 *
 * The mascot greeting and the AI insight are deliberately absent: the greeting is the app's only
 * door to the coach, and the insight owns an expand/collapse whose exit is what stops the cards
 * below it jumping. Both stay pinned above the customizable block.
 *
 * This lives in `:core:designsystem` for the reason [MascotCharacter] does — it is an appearance
 * vocabulary shared by the screen that renders it (`:feature:home`) and the picker that sets it
 * (`:feature:profile`), and `:feature:*` modules never import each other. `:core:data` therefore
 * stores the *name*, not this type, exactly as it does for the mascot.
 *
 * [note] is what the editor row prints under a card whose visibility is *also* gated on data
 * arriving. A switch that is on and still shows nothing has to say why.
 */
enum class HomeCard(val label: String, val note: String? = null) {
    Calories("Calories"),
    Streak("Streak"),
    Water("Water"),
    Fasting("Fasting"),
    Workout("Today's workout", "Shows once a routine has days set"),
    Sleep("Sleep", WATCH_NOTE),
    Steps("Steps", WATCH_NOTE),
    Heart("Heart rate", WATCH_NOTE),
    BloodPressure("Blood pressure", "Shows once you log a reading"),
    Mood("Mood"),
    Supplements("Supplements", "Shows once your list has something in it"),
    Weight("Weight"),
    Macros("Macros"),
    ProgressPhoto("Progress photo"),
}

/** One card's place in the layout: where it sits, and whether Home draws it at all. */
data class HomeCardSetting(val card: HomeCard, val visible: Boolean = true)

private const val HIDDEN_PREFIX = "-"

/**
 * Resolves the layout string stored on the profile. Null — and anything that parses to nothing —
 * is the default: every card, in declaration order, visible. The same reading `mascotName`'s null
 * has.
 *
 * Two rules the format exists to keep, in opposite directions:
 * - a name this build doesn't recognise is **dropped**, so a card retired in a later version
 *   can't leave a hole in a saved layout;
 * - a card the string never mentions is **appended, visible**, so a card *added* in a later
 *   version shows up for everyone who had already saved a layout. Silently hiding a new feature
 *   from exactly the users who customised their Home is the failure mode here.
 */
fun homeCardLayout(stored: String?): List<HomeCardSetting> {
    val saved = stored.orEmpty().split(',').mapNotNull { token ->
        val trimmed = token.trim()
        val hidden = trimmed.startsWith(HIDDEN_PREFIX)
        val name = if (hidden) trimmed.removePrefix(HIDDEN_PREFIX) else trimmed
        HomeCard.entries.firstOrNull { it.name == name }?.let { HomeCardSetting(it, visible = !hidden) }
    }.distinctBy { it.card }
    val mentioned = saved.mapTo(mutableSetOf()) { it.card }
    return saved + HomeCard.entries.filterNot { it in mentioned }.map { HomeCardSetting(it) }
}

/** The inverse of [homeCardLayout]. "Reset to default" writes null rather than this. */
fun encodeHomeCardLayout(layout: List<HomeCardSetting>): String =
    layout.joinToString(",") { if (it.visible) it.card.name else HIDDEN_PREFIX + it.card.name }
