package ph.mart.healthapp.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import ph.mart.healthapp.core.designsystem.theme.backgroundDark
import ph.mart.healthapp.core.designsystem.theme.errorDark
import ph.mart.healthapp.core.designsystem.theme.onBackgroundDark
import ph.mart.healthapp.core.designsystem.theme.onErrorDark
import ph.mart.healthapp.core.designsystem.theme.onPrimaryContainerDark
import ph.mart.healthapp.core.designsystem.theme.onPrimaryDark
import ph.mart.healthapp.core.designsystem.theme.onSecondaryContainerDark
import ph.mart.healthapp.core.designsystem.theme.onSecondaryDark
import ph.mart.healthapp.core.designsystem.theme.onSurfaceDark
import ph.mart.healthapp.core.designsystem.theme.onSurfaceVariantDark
import ph.mart.healthapp.core.designsystem.theme.onTertiaryContainerDark
import ph.mart.healthapp.core.designsystem.theme.onTertiaryDark
import ph.mart.healthapp.core.designsystem.theme.outlineDark
import ph.mart.healthapp.core.designsystem.theme.outlineVariantDark
import ph.mart.healthapp.core.designsystem.theme.primaryContainerDark
import ph.mart.healthapp.core.designsystem.theme.primaryDark
import ph.mart.healthapp.core.designsystem.theme.secondaryContainerDark
import ph.mart.healthapp.core.designsystem.theme.secondaryDark
import ph.mart.healthapp.core.designsystem.theme.surfaceContainerDark
import ph.mart.healthapp.core.designsystem.theme.surfaceContainerHighDark
import ph.mart.healthapp.core.designsystem.theme.surfaceContainerLowDark
import ph.mart.healthapp.core.designsystem.theme.tertiaryContainerDark
import ph.mart.healthapp.core.designsystem.theme.tertiaryDark

/**
 * The app's frozen palette, on a wrist.
 *
 * Only the *dark* half of it. `Profile.darkThemeOn` reaches the widget because a home screen can
 * be light; a watch face is black by convention and by battery, and Wear Material 3 is designed
 * against a black background — a light watch app would be the odd one out on the device, not on
 * the phone. Contrast schemes are absent for the reason the widget's are: there is no
 * `UiModeManager.getContrast()` equivalent to swap them on.
 *
 * The colours come from `:core:designsystem`'s Color.kt, so no hex is written here and the
 * wrist can never drift from the phone. Typography is Wear's own: its type scale is drawn for a
 * round display, and Poppins/Inter would need the downloadable-font provider on the watch for no
 * legibility gain — the same call the widget made for Glance.
 */
private val wearColorScheme = ColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    onSurface = onSurfaceDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    error = errorDark,
    onError = onErrorDark,
)

@Composable
fun FitPulseWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = wearColorScheme, content = content)
}
