package ph.mart.healthapp.feature.progress.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.profile.kgToDisplayUnit
import ph.mart.healthapp.core.data.profile.weightUnitLabel
import ph.mart.healthapp.core.data.progress.ProgressPhoto
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.component.formatEpochDay
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/** Static side-by-side only — the draggable slider stays deferred per BUILD_PLAN.md. */
@Composable
fun PhotoComparisonScreen(photoA: ProgressPhoto, photoB: ProgressPhoto, unit: UnitSystem, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = "Compare photos", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ComparisonTile(photoA, modifier = Modifier.weight(1f))
                ComparisonTile(photoB, modifier = Modifier.weight(1f))
            }
            val delta = photoB.weightKg?.let { b -> photoA.weightKg?.let { a -> b - a } }
            if (delta != null) {
                Text(
                    text = "${if (delta > 0) "+" else ""}${"%.1f".format(delta.kgToDisplayUnit(unit))} ${unit.weightUnitLabel()}",
                    style = MaterialTheme.typography.titleMedium.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            SecondaryButton(label = "Close", onClick = onClose)
        }
    }
}

@Composable
private fun ComparisonTile(photo: ProgressPhoto, modifier: Modifier = Modifier) {
    val imageBitmap = rememberBitmapFromFile(photo.filePath)
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            contentAlignment = Alignment.Center,
        ) {
            imageBitmap?.let {
                Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Text(
            text = formatEpochDay(photo.dateEpochDay),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun PhotoComparisonScreenPreview() {
    AppTheme {
        PhotoComparisonScreen(
            photoA = ProgressPhoto(id = 1, dateEpochDay = 0, filePath = "", weightKg = 80.0),
            photoB = ProgressPhoto(id = 2, dateEpochDay = 30, filePath = "", weightKg = 77.5),
            unit = UnitSystem.Metric,
            onClose = {},
        )
    }
}
