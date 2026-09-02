package ph.mart.healthapp.feature.progress.ui.progress.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Picture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import ph.mart.healthapp.core.data.food.NutritionAverages
import ph.mart.healthapp.core.data.mood.MoodAverages
import ph.mart.healthapp.core.data.profile.DailyTargets
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.WeightTrendDisplay
import ph.mart.healthapp.core.data.progress.GoalProjection
import ph.mart.healthapp.core.designsystem.component.AppBottomSheet
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.progress.ui.progress.BestDay
import ph.mart.healthapp.feature.progress.ui.progress.WeeklyRecap

/**
 * Preview-then-share for the weekly recap: the sheet shows exactly the PNG that leaves the app,
 * which is why the branding can exist here without ever appearing on the Progress screen.
 *
 * [WeeklyRecapCard] is rendered verbatim — every figure, and every colour rule behind it, stays
 * the card's. This only adds the opaque ground a shared image needs (a captured layer is
 * transparent wherever nothing painted) and the footer that says which app drew it.
 *
 * No `NavigationEventHandler`: [AppBottomSheet] delegates to `ModalBottomSheet`, which already
 * takes back, and this is a leaf with no sub-level of its own.
 */
@Composable
internal fun ShareRecapSheet(
    recap: WeeklyRecap,
    goal: Goal?,
    unit: UnitSystem,
    projection: GoalProjection?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picture = remember { Picture() }

    AppBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Redirect this subtree's draw into a Picture and then play it back into the real
                // canvas: the API-24-safe capture. GraphicsLayer.toImageBitmap() is the shorter
                // call but only pays off above the app's minSdk.
                .drawWithCache {
                    val width = size.width.toInt()
                    val height = size.height.toInt()
                    onDrawWithContent {
                        val pictureCanvas = Canvas(picture.beginRecording(width, height))
                        draw(this, layoutDirection, pictureCanvas, size) {
                            this@onDrawWithContent.drawContent()
                        }
                        picture.endRecording()
                        drawIntoCanvas { it.nativeCanvas.drawPicture(picture) }
                    }
                }
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 8.dp),
        ) {
            WeeklyRecapCard(recap = recap, goal = goal, unit = unit, projection = projection)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                MascotAvatar(state = MascotState.Happy, size = 24.dp)
                Text(
                    text = "FitPulse",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        PrimaryButton(
            label = "Share",
            onClick = {
                scope.launch {
                    // Zero until the sheet has drawn a frame — a tap that fast would otherwise
                    // hand the chooser an empty file.
                    if (picture.width > 0) {
                        shareRecapPng(context, picture)
                        onDismiss()
                    }
                }
            },
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

/**
 * One file, overwritten: the last card shared is the only one worth keeping, and a fixed name
 * means nothing accumulates in the cache. The grant is read-only and scoped to `cacheDir/share`
 * by `@xml/file_paths` — nothing in `filesDir` (the progress photos, the database) is reachable
 * through the provider.
 */
private suspend fun shareRecapPng(context: Context, picture: Picture) {
    val uri = withContext(Dispatchers.IO) {
        // Software bitmap on every API: Bitmap.createBitmap(picture) is shorter above API 28 but
        // yields a hardware bitmap, and compressing one of those is its own compatibility story.
        val bitmap = Bitmap.createBitmap(picture.width, picture.height, Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(bitmap).drawPicture(picture)
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "fitpulse-recap.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, null))
}

@PreviewLightDark
@Composable
private fun ShareRecapSheetPreview() {
    AppTheme {
        ShareRecapSheet(
            recap = WeeklyRecap(
                daysLogged = 6,
                averages = NutritionAverages(1940, 141, 196, 68, daysLogged = 5),
                targets = DailyTargets(calories = 2000, proteinG = 150, carbsG = 200, fatG = 67, floor = 1500),
                weightTrend = WeightTrendDisplay(currentKg = 76.0, deltaKg = -0.8, hasPrior = true),
                moodAverages = MoodAverages(mood = 3.6, energy = 3.1, daysLogged = 6),
                bestDay = BestDay(dateEpochDay = 20_690, calories = 1985),
            ),
            goal = Goal.Lose,
            unit = UnitSystem.Metric,
            projection = GoalProjection(
                goalWeightKg = 72.0,
                kgPerWeek = -0.4,
                targetEpochDay = 20_760,
                reached = false,
            ),
            onDismiss = {},
        )
    }
}
