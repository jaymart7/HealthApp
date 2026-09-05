package ph.mart.healthapp.wear.tile

import android.content.ComponentName
import androidx.compose.ui.graphics.toArgb
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material3.CardDefaults.filledTonalCardColors
import androidx.wear.protolayout.material3.CircularProgressIndicatorDefaults.filledTonalProgressIndicatorColors
import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.material3.GraphicDataCardStyle.Companion.largeGraphicDataCardStyle
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.circularProgressIndicator
import androidx.wear.protolayout.material3.graphicDataCard
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.types.argb
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.Material3TileService
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
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
import ph.mart.healthapp.core.today.TodaySnapshot
import ph.mart.healthapp.core.today.progress
import ph.mart.healthapp.core.today.remainingKcal
import ph.mart.healthapp.wear.MainActivity
import ph.mart.healthapp.wear.R
import ph.mart.healthapp.wear.data.latestSnapshot

/** Half an hour, matching the widget's `updatePeriodMillis` — and for the widget's reason. The
 * push is what keeps a tile current within seconds of a log; this only has to catch the day
 * turning over while the phone is out of range. */
private const val FRESHNESS_MILLIS = 30 * 60 * 1000L

/**
 * Today, one swipe from the watch face.
 *
 * The tile earns its place for the reason the phone has a widget: an app you have to open every
 * time is an app nobody opens. It renders the same [TodaySnapshot] the watch app does — read
 * straight from the Data Layer's stored item, with no ViewModel, because a tile has no lifecycle
 * to hold one. The same call the widget and `ReminderWorker` make.
 *
 * It shows and never writes. A tile's tap targets are coarse, "+1 glass" here would be a glass
 * logged by a sleeve, and the app it opens is one tap away with the real control on it.
 *
 * `allowDynamicTheme = false` is not a stylistic choice: dynamic color is disabled app-wide, and a
 * tile following the watch face's wallpaper colours would be the one FitPulse surface that did.
 */
class TodayTileService : Material3TileService(
    allowDynamicTheme = false,
    defaultColorScheme = fitPulseTileColors,
) {

    override suspend fun MaterialScope.tileResponse(requestParams: TileRequest): Tile {
        val snapshot = latestSnapshot(this@TodayTileService)
        val openApp = Clickable.Builder()
            .setId("open")
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setPackageName(packageName)
                            .setClassName(MainActivity::class.java.name)
                            .build(),
                    )
                    .build(),
            )
            .build()

        val layout = when {
            snapshot == null || snapshot.onboarding -> emptyLayout(openApp)
            else -> todayLayout(snapshot, openApp)
        }

        return Tile.Builder()
            .setTileTimeline(Timeline.fromLayoutElement(layout))
            .setFreshnessIntervalMillis(FRESHNESS_MILLIS)
            .build()
    }

    /**
     * The ring carries the day and the number inside it is what's *left* — the app's own reading,
     * so a glance at the tile and a glance at the watch app never differ. Water rides the edge
     * button because it is the one line worth a second figure; the streak and steps are the app's,
     * not the tile's, which is what keeps this readable at arm's length.
     *
     * Tonal card, tonal ring: a `surfaceContainer` card with a `primary` ring is both the pairing
     * protolayout documents and what every FitPulse card already looks like. The filled default
     * would make the tile a green slab — and pairing it with the *variant* ring colours, as this
     * first did, draws the arc in a colour picked for a different background.
     */
    private fun MaterialScope.todayLayout(snapshot: TodaySnapshot, openApp: Clickable) =
        primaryLayout(
            titleSlot = { text(getString(R.string.wear_tile_title).layoutString, typography = Typography.LABEL_SMALL) },
            mainSlot = {
                graphicDataCard(
                    onClick = openApp,
                    height = expand(),
                    style = largeGraphicDataCardStyle(),
                    colors = filledTonalCardColors(),
                    graphic = {
                        circularProgressIndicator(
                            staticProgress = snapshot.progress,
                            colors = filledTonalProgressIndicatorColors(),
                        )
                    },
                    title = {
                        val remaining = snapshot.remainingKcal
                        text(
                            "${if (remaining >= 0) remaining else -remaining}".layoutString,
                            typography = Typography.NUMERAL_MEDIUM,
                        )
                    },
                    content = {
                        text(
                            getString(
                                if (snapshot.remainingKcal >= 0) R.string.wear_kcal_left else R.string.wear_kcal_over,
                            ).layoutString,
                            typography = Typography.LABEL_SMALL,
                        )
                    },
                )
            },
            bottomSlot = {
                textEdgeButton(onClick = openApp) {
                    text(getString(R.string.wear_tile_water, snapshot.glasses, snapshot.goalGlasses).layoutString)
                }
            },
        )

    /** Nothing has ever been pushed to this watch. The tile says where the fix is rather than
     * drawing a zero day — the watch app's rule, for the same reason. */
    private fun MaterialScope.emptyLayout(openApp: Clickable) =
        primaryLayout(
            mainSlot = {
                text(
                    getString(R.string.wear_tile_no_data).layoutString,
                    typography = Typography.BODY_MEDIUM,
                    maxLines = 3,
                )
            },
            bottomSlot = { textEdgeButton(onClick = openApp) { text(getString(R.string.wear_tile_open).layoutString) } },
        )
}

/**
 * The frozen palette again, in protolayout's own colour type. Kept beside the tile rather than
 * shared with `FitPulseWearTheme`: the two `ColorScheme` classes have identical field names and
 * no common supertype, so "sharing" them would mean a mapping function longer than either.
 */
private val fitPulseTileColors = ColorScheme(
    primary = primaryDark.toArgb().argb,
    onPrimary = onPrimaryDark.toArgb().argb,
    primaryContainer = primaryContainerDark.toArgb().argb,
    onPrimaryContainer = onPrimaryContainerDark.toArgb().argb,
    secondary = secondaryDark.toArgb().argb,
    onSecondary = onSecondaryDark.toArgb().argb,
    secondaryContainer = secondaryContainerDark.toArgb().argb,
    onSecondaryContainer = onSecondaryContainerDark.toArgb().argb,
    tertiary = tertiaryDark.toArgb().argb,
    onTertiary = onTertiaryDark.toArgb().argb,
    tertiaryContainer = tertiaryContainerDark.toArgb().argb,
    onTertiaryContainer = onTertiaryContainerDark.toArgb().argb,
    surfaceContainerLow = surfaceContainerLowDark.toArgb().argb,
    surfaceContainer = surfaceContainerDark.toArgb().argb,
    surfaceContainerHigh = surfaceContainerHighDark.toArgb().argb,
    onSurface = onSurfaceDark.toArgb().argb,
    onSurfaceVariant = onSurfaceVariantDark.toArgb().argb,
    outline = outlineDark.toArgb().argb,
    outlineVariant = outlineVariantDark.toArgb().argb,
    background = backgroundDark.toArgb().argb,
    onBackground = onBackgroundDark.toArgb().argb,
    error = errorDark.toArgb().argb,
    onError = onErrorDark.toArgb().argb,
)
