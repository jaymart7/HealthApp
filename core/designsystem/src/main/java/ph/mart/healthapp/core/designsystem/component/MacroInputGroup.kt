package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/**
 * Three labeled numeric rows (Protein/Carbs/Fat) using the fixed semantic colors — protein =
 * `primary`, carbs = `tertiary`, fat = `secondary` — identical across every macro display in the
 * app.
 */
@Composable
fun MacroInputGroup(
    proteinG: Int,
    carbsG: Int,
    fatG: Int,
    onProteinChange: (Int) -> Unit,
    onCarbsChange: (Int) -> Unit,
    onFatChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MacroRow("Protein", proteinG, MaterialTheme.colorScheme.primary, onProteinChange)
        MacroRow("Carbs", carbsG, MaterialTheme.colorScheme.tertiary, onCarbsChange)
        MacroRow("Fat", fatG, MaterialTheme.colorScheme.secondary, onFatChange)
    }
}

@Composable
private fun MacroRow(label: String, grams: Int, dotColor: Color, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(dotColor),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
        Text(
            text = "${grams}g",
            style = MaterialTheme.typography.titleSmall.tabularNums,
            color = MaterialTheme.colorScheme.onSurface,
        )
        StepperButton(symbol = "−", onClick = { onChange(grams - 1) })
        StepperButton(symbol = "+", onClick = { onChange(grams + 1) })
    }
}

@PreviewLightDark
@Composable
private fun MacroInputGroupPreview() {
    AppTheme {
        Surface {
            MacroInputGroup(
                proteinG = 32,
                carbsG = 48,
                fatG = 14,
                onProteinChange = {},
                onCarbsChange = {},
                onFatChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
