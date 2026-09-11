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
 * scheme: a mascot fill is decorative, and nothing else is drawn from them.
 *
 * They are **fifteen families of two**, not thirty points on one wheel. Thirty evenly-spaced hues
 * sit 12° apart, and 12° of a pale fill is a colour nobody can tell from the one beside it — which
 * is exactly how this went wrong the first time. So the wheel carries fifteen hues at 15–35°, the
 * wide gaps going to the greens and blues where hue moves slowest to the eye, and each hue appears
 * twice: once pale, once [vivid]. Declaration order is the picker's grid order and it alternates
 * pale, vivid, pale, vivid — so every neighbour in the grid differs by a whole tier or by a full
 * family's worth of hue, and `MascotPaletteTest` asserts precisely that.
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
    /** The second axis. Two entries share every [hue] — a pale one and a saturated one — and they
     * alternate down the list, so no two neighbours in the picker's grid are a small hue step
     * apart. */
    internal val vivid: Boolean = false,
) {
    Soft(R.string.ds_mascot_colour_soft),
    Bold(R.string.ds_mascot_colour_bold),
    Muted(R.string.ds_mascot_colour_muted),
    Contrast(R.string.ds_mascot_colour_contrast),
    Neutral(R.string.ds_mascot_colour_neutral),

    Blush(R.string.ds_mascot_colour_blush, 0f),
    Red(R.string.ds_mascot_colour_red, 0f, vivid = true),
    Peach(R.string.ds_mascot_colour_peach, 25f),
    Orange(R.string.ds_mascot_colour_orange, 25f, vivid = true),
    Butter(R.string.ds_mascot_colour_butter, 48f),
    Yellow(R.string.ds_mascot_colour_yellow, 48f, vivid = true),
    Pear(R.string.ds_mascot_colour_pear, 75f),
    Lime(R.string.ds_mascot_colour_lime, 75f, vivid = true),
    Sage(R.string.ds_mascot_colour_sage, 105f),
    Green(R.string.ds_mascot_colour_green, 105f, vivid = true),
    Jade(R.string.ds_mascot_colour_jade, 140f),
    Emerald(R.string.ds_mascot_colour_emerald, 140f, vivid = true),
    Seafoam(R.string.ds_mascot_colour_seafoam, 172f),
    Teal(R.string.ds_mascot_colour_teal, 172f, vivid = true),
    Sky(R.string.ds_mascot_colour_sky, 195f),
    Cyan(R.string.ds_mascot_colour_cyan, 195f, vivid = true),
    Powder(R.string.ds_mascot_colour_powder, 218f),
    Azure(R.string.ds_mascot_colour_azure, 218f, vivid = true),
    Periwinkle(R.string.ds_mascot_colour_periwinkle, 240f),
    Blue(R.string.ds_mascot_colour_blue, 240f, vivid = true),
    Lavender(R.string.ds_mascot_colour_lavender, 265f),
    Indigo(R.string.ds_mascot_colour_indigo, 265f, vivid = true),
    Lilac(R.string.ds_mascot_colour_lilac, 285f),
    Violet(R.string.ds_mascot_colour_violet, 285f, vivid = true),
    Mauve(R.string.ds_mascot_colour_mauve, 305f),
    Magenta(R.string.ds_mascot_colour_magenta, 305f, vivid = true),
    Orchid(R.string.ds_mascot_colour_orchid, 325f),
    Pink(R.string.ds_mascot_colour_pink, 325f, vivid = true),
    Rose(R.string.ds_mascot_colour_rose, 345f),
    Crimson(R.string.ds_mascot_colour_crimson, 345f, vivid = true),
}

/**
 * Both tiers of a family are the same hue; what separates them is **chroma**, which is the axis a
 * pale fill has none of — `0.26` against `0.52`, so a pale buddy beside its vivid twin is not a
 * near-miss of it. Lightness stays high in *both*, and that is the constraint doing the real work:
 * a mascot is drawn on `surface` in either scheme, so neither tier may go dark, and because neither
 * goes dark every one of the thirty takes the same near-black face.
 *
 * [VIVID_LIGHTNESS] is `0.72` and not the `0.64` this first shipped with because of the blues. A
 * saturated blue is *dark* — at `0.64` its body lands at luminance `0.14`, which is too dark to
 * carry a dark face and still too dark to carry a light one, so `Red`, `Blue` and `Indigo` all fell
 * under 4.5:1 whichever way the face went. Lifting the tier put the darkest body back above `0.23`,
 * where one face colour serves all thirty. `MascotPaletteTest` sweeps every one of them, which is
 * what stops the next tune of these five numbers from quietly flattening a face into its head.
 */
private const val PALE_SATURATION = 0.58f
private const val PALE_LIGHTNESS = 0.78f
private const val VIVID_SATURATION = 0.92f
private const val VIVID_LIGHTNESS = 0.72f
private const val FACE_SATURATION = 0.80f
private const val FACE_LIGHTNESS = 0.15f

internal fun hueColors(hue: Float, vivid: Boolean): MascotColors = MascotColors(
    body = if (vivid) {
        Color.hsl(hue, VIVID_SATURATION, VIVID_LIGHTNESS)
    } else {
        Color.hsl(hue, PALE_SATURATION, PALE_LIGHTNESS)
    },
    feature = Color.hsl(hue, FACE_SATURATION, FACE_LIGHTNESS),
)

@Composable
internal fun mascotColors(palette: MascotPalette): MascotColors {
    palette.hue?.let { return hueColors(it, palette.vivid) }
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
