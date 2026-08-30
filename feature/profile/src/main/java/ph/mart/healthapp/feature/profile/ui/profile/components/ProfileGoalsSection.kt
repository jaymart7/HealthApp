package ph.mart.healthapp.feature.profile.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Profile
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.MacroBar
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

private fun ActivityLevel.label(): String = when (this) {
    ActivityLevel.Sedentary -> "Sedentary"
    ActivityLevel.Light -> "Light"
    ActivityLevel.Moderate -> "Moderate"
    ActivityLevel.Very -> "Very active"
}

/**
 * Read-only, by design — the numbers come straight from [dailyTargets], the same call Home and
 * onboarding's Confirm step make. Editing targets lives in onboarding, not here.
 */
@Composable
internal fun ProfileGoalsSection(profile: Profile, modifier: Modifier = Modifier) {
    val targets = profile.dailyTargets()
    SettingsSection(label = "Goals", modifier = modifier) {
        AppCard {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                Text(
                    text = "Calorie target",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${targets.calories} kcal",
                    style = MaterialTheme.typography.titleSmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            MacroBar(proteinG = targets.proteinG, carbsG = targets.carbsG, fatG = targets.fatG)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                MacroLegend("Protein ${targets.proteinG}g", MaterialTheme.colorScheme.primary)
                MacroLegend("Carbs ${targets.carbsG}g", MaterialTheme.colorScheme.tertiary)
                MacroLegend("Fat ${targets.fatG}g", MaterialTheme.colorScheme.secondary)
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            SettingsRow(
                label = "Activity level",
                trailing = {
                    Text(
                        text = profile.activityLevel.label(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
    }
}

@Composable
private fun MacroLegend(text: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.tabularNums,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@PreviewLightDark
@Composable
private fun ProfileGoalsSectionPreview() {
    AppTheme {
        Surface {
            ProfileGoalsSection(
                profile = Profile(
                    sex = Sex.Male,
                    age = 26,
                    heightCm = 170.0,
                    weightKg = 75.5,
                    activityLevel = ActivityLevel.Sedentary,
                    goal = Goal.Maintain,
                ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
