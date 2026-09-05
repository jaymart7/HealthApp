package ph.mart.healthapp.core.designsystem.component

import androidx.annotation.StringRes
import ph.mart.healthapp.core.designsystem.R

/** What the three Google Health cards say under their switch: on, but nothing to draw yet. */
private val WATCH_NOTE = R.string.ds_card_note_watch

/**
 * Every card on Home the user can move or hide, in the order an untouched install renders them.
 *
 * The declaration order **is** the default layout. It is ordered so that the half-width cards fall
 * into adjacent pairs on an untouched install (`homeRows()` in `:feature:home` does the pairing) —
 * but that is a *default*, not a contract: any order the user drags them into pairs just as well,
 * which is why no card may depend on its position or its neighbour.
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
enum class HomeCard(@StringRes val label: Int, @StringRes val note: Int? = null) {
    Calories(R.string.ds_card_calories),
    Water(R.string.ds_card_water),
    Macros(R.string.ds_card_macros),
    Streak(R.string.ds_card_streak),
    Weight(R.string.ds_card_weight),
    Steps(R.string.ds_card_steps, WATCH_NOTE),
    Sleep(R.string.ds_card_sleep, WATCH_NOTE),
    Heart(R.string.ds_card_heart, WATCH_NOTE),
    BloodPressure(R.string.ds_card_blood_pressure, R.string.ds_card_note_blood_pressure),
    Fasting(R.string.ds_card_fasting),
    Mood(R.string.ds_card_mood),
    Supplements(R.string.ds_card_supplements, R.string.ds_card_note_supplements),
    Cycle(R.string.ds_card_cycle, R.string.ds_card_note_cycle),
    Workout(R.string.ds_card_workout, R.string.ds_card_note_workout),
    ProgressPhoto(R.string.ds_card_progress_photo),
}

/** One card's place in the layout: where it sits, and whether Home draws it at all. */
data class HomeCardSetting(val card: HomeCard, val visible: Boolean = true)

// Stays in Kotlin, with [HomeCard.name]: this is the stored format on `Profile.homeLayout`, and
// a translated token would rewrite a user's Home the first time they changed language.
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
