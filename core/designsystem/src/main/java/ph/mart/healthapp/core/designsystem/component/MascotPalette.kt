package ph.mart.healthapp.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import ph.mart.healthapp.core.designsystem.R

internal data class MascotColors(val body: Color, val feature: Color)

/**
 * The colour the user picks in Profile → Settings → Appearance, applied to whichever buddy is
 * picked. Two kinds of entry, and the difference is [hue].
 *
 * The **first five** resolve from the colour scheme, and each was one character's fill before the
 * colour became a choice of its own — so each is already proven against light, dark and all three
 * contrast schemes. [Soft] is the default because [MascotCharacter.Rui] is, so an untouched install
 * renders exactly as it did. Three roles are deliberately absent from them: **tertiary** and
 * **tertiaryContainer** are the AI accent and the carbs colour, **error** means genuinely
 * off-track, and **secondaryContainer** is what the picker fills its selected cell with — a mascot
 * that vanished the moment it was chosen is the one thing a picker must not do. [Contrast] is the
 * one pair that *inverts* with the theme; [Neutral] is the one whose *features* carry the accent
 * rather than its fill.
 *
 * The **thirty after them** are fixed hue angles, and they exist because the scheme cannot supply a
 * pink or a red — the roles that would are the ones listed above as spoken for. They are the app's
 * only colours not read from `MaterialTheme`, which is safe exactly because they take part in no
 * scheme: a mascot fill is decorative, and nothing else is drawn from them. Declaration order walks
 * the wheel once, and that order is the order the picker's grid draws.
 *
 * Every entry's [name] is a persisted token — `Profile.mascotPaletteName` is that string — which is
 * why the display name is [labelRes] and why none of the five may be renamed or removed.
 */
enum class MascotPalette(
    /** Public where [hue] is internal: the picker lives in `:feature:profile` and has to print
     * it, while the hue is only ever read by [mascotColors] two declarations down. */
    @StringRes val labelRes: Int,
    /** Degrees on the colour wheel. Null means the pair is resolved from the theme instead. */
    internal val hue: Float? = null,
) {
    Soft(R.string.ds_mascot_colour_soft),
    Bold(R.string.ds_mascot_colour_bold),
    Muted(R.string.ds_mascot_colour_muted),
    Contrast(R.string.ds_mascot_colour_contrast),
    Neutral(R.string.ds_mascot_colour_neutral),

    Red(R.string.ds_mascot_colour_red, 2f),
    Scarlet(R.string.ds_mascot_colour_scarlet, 10f),
    Coral(R.string.ds_mascot_colour_coral, 16f),
    Orange(R.string.ds_mascot_colour_orange, 26f),
    Tangerine(R.string.ds_mascot_colour_tangerine, 34f),
    Amber(R.string.ds_mascot_colour_amber, 42f),
    Gold(R.string.ds_mascot_colour_gold, 48f),
    Yellow(R.string.ds_mascot_colour_yellow, 54f),
    Olive(R.string.ds_mascot_colour_olive, 68f),
    Lime(R.string.ds_mascot_colour_lime, 84f),
    Grass(R.string.ds_mascot_colour_grass, 104f),
    Green(R.string.ds_mascot_colour_green, 130f),
    Emerald(R.string.ds_mascot_colour_emerald, 152f),
    Mint(R.string.ds_mascot_colour_mint, 166f),
    Teal(R.string.ds_mascot_colour_teal, 176f),
    Aqua(R.string.ds_mascot_colour_aqua, 186f),
    Cyan(R.string.ds_mascot_colour_cyan, 194f),
    Sky(R.string.ds_mascot_colour_sky, 202f),
    Azure(R.string.ds_mascot_colour_azure, 212f),
    Blue(R.string.ds_mascot_colour_blue, 224f),
    Cobalt(R.string.ds_mascot_colour_cobalt, 234f),
    Indigo(R.string.ds_mascot_colour_indigo, 250f),
    Violet(R.string.ds_mascot_colour_violet, 266f),
    Purple(R.string.ds_mascot_colour_purple, 282f),
    Orchid(R.string.ds_mascot_colour_orchid, 296f),
    Magenta(R.string.ds_mascot_colour_magenta, 310f),
    Fuchsia(R.string.ds_mascot_colour_fuchsia, 320f),
    Pink(R.string.ds_mascot_colour_pink, 330f),
    Rose(R.string.ds_mascot_colour_rose, 342f),
    Crimson(R.string.ds_mascot_colour_crimson, 350f),
}

/**
 * Body and feature are the same hue at two lightnesses, so thirty colours are thirty numbers rather
 * than sixty hand-tuned hexes and one change here retunes all of them.
 *
 * One pair, not a light table and a dark one: lightness `0.72` is a bright figure on a dark surface
 * and a saturated one on a light surface, and the feature colour rides the *body*, not the surface
 * — so nothing here reads the scheme and nothing here can disagree with it. The swatch's
 * `outlineVariant` ring is what keeps a pale hue findable on a pale card.
 *
 * [FEATURE_LIGHTNESS] is `0.20` rather than `0.24` because of yellow: around 54° the body is at its
 * brightest and a `0.24` feature clears only ~4.2:1 against it. `MascotPaletteTest` sweeps every
 * hue for ≥ 4.5:1, which is what stops the next tune here from quietly blinding one buddy.
 */
private const val BODY_SATURATION = 0.55f
private const val BODY_LIGHTNESS = 0.72f
private const val FEATURE_SATURATION = 0.70f
private const val FEATURE_LIGHTNESS = 0.20f

internal fun hueColors(hue: Float): MascotColors = MascotColors(
    body = Color.hsl(hue, BODY_SATURATION, BODY_LIGHTNESS),
    feature = Color.hsl(hue, FEATURE_SATURATION, FEATURE_LIGHTNESS),
)

@Composable
internal fun mascotColors(palette: MascotPalette): MascotColors {
    palette.hue?.let { return hueColors(it) }
    val scheme = MaterialTheme.colorScheme
    return when (palette) {
        MascotPalette.Bold -> MascotColors(scheme.primary, scheme.onPrimary)
        MascotPalette.Muted -> MascotColors(scheme.secondary, scheme.onSecondary)
        MascotPalette.Contrast -> MascotColors(scheme.inverseSurface, scheme.inverseOnSurface)
        MascotPalette.Neutral -> MascotColors(scheme.surfaceContainerHighest, scheme.primary)
        // Soft, and anything a hue was forgotten on — the default is the safe read either way.
        else -> MascotColors(scheme.primaryContainer, scheme.onPrimaryContainer)
    }
}

/** The palette's fill on its own, for the swatches the picker draws. Public where [mascotColors] is
 * internal because a plain circle needs the body colour and nothing else — the feature colour has
 * no meaning without a face to put it on. */
@Composable
fun mascotSwatchColor(palette: MascotPalette): Color = mascotColors(palette).body

/** What the swatch may draw *on* itself — the check the picker marks the chosen cell with. The
 * theme's own `onSecondaryContainer` would be wrong here: the cell is one of thirty-five arbitrary
 * fills, and only the palette's own feature colour is guaranteed against it (`MascotPaletteTest`
 * holds every hue's pair at ≥ 4.5:1, and the five theme pairs are `on*` roles by construction). */
@Composable
fun mascotFeatureColor(palette: MascotPalette): Color = mascotColors(palette).feature

/** [mascotCharacterOf] for the colour, and it degrades the same way and for the same reasons. */
fun mascotPaletteOf(name: String?): MascotPalette =
    MascotPalette.entries.firstOrNull { it.name == name } ?: MascotPalette.Soft

/** Provided by `AppTheme` beside [LocalMascot], off the same profile row. The colour is an
 * appearance choice like the buddy and the scheme, so it is resolved where those are. */
val LocalMascotPalette = staticCompositionLocalOf { MascotPalette.Soft }
