package ph.mart.healthapp.feature.onboarding.ui.onboarding.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import ph.mart.healthapp.core.data.profile.ActivityLevel
import ph.mart.healthapp.core.data.profile.CALORIE_FLOOR_WARNING
import ph.mart.healthapp.core.data.profile.CALORIE_TARGET_KCAL
import ph.mart.healthapp.core.data.profile.Goal
import ph.mart.healthapp.core.data.profile.Sex
import ph.mart.healthapp.core.data.profile.belowFloor
import ph.mart.healthapp.core.data.profile.calorieAdjustment
import ph.mart.healthapp.core.data.profile.dailyTargets
import ph.mart.healthapp.core.data.profile.maintenanceKcal
import ph.mart.healthapp.core.designsystem.component.MacroBar
import ph.mart.healthapp.core.designsystem.component.MacroInputGroup
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.StepperButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.onboarding.R
import ph.mart.healthapp.feature.onboarding.ui.onboarding.OnboardingForm
import ph.mart.healthapp.feature.onboarding.ui.shared.components.OnboardingStep

/** How long the macro bar takes to draw itself once the flow is finished. */
private const val CELEBRATION_BAR_MS = 500

private const val CALORIE_STEP_KCAL = 50

/**
 * Onboarding step 6 of 6, and the first thing the app ever tells the user about themselves.
 *
 * The calorie figure leads the screen rather than sitting in a 56dp field at the same weight as
 * "Age", and directly under it is the arithmetic that produced it — which is the part that earns
 * the size. A number with its derivation printed is a calculation the reader can check rather than
 * an assertion, and it is also what makes the stepper read as an adjustment instead of a
 * correction. [ph.mart.healthapp.core.data.profile.maintenanceKcal] already has the figure;
 * printing it costs nothing.
 *
 * Targets are always computed live from [form] — the steppers set an override on top, never a
 * second cached number.
 */
@Composable
internal fun ConfirmTargetsScreen(
    form: OnboardingForm,
    isCelebrating: Boolean,
    onFormChange: (OnboardingForm) -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
) {
    val profile = form.toProfileOrNull() ?: return
    val targets = profile.dailyTargets()
    val maintenance = profile.maintenanceKcal()
    // Fully drawn at rest; the celebration re-draws it from the left, which is the only moment
    // the bar is an event rather than a readout.
    val bar = remember { Animatable(1f) }
    LaunchedEffect(isCelebrating) {
        if (isCelebrating) {
            bar.snapTo(0f)
            bar.animateTo(1f, tween(CELEBRATION_BAR_MS))
        }
    }

    OnboardingStep(
        step = 6,
        mascotState = if (isCelebrating) MascotState.Celebrating else MascotState.Idle,
        mascotSize = if (isCelebrating) 48.dp else 32.dp,
        line = stringResource(
            if (isCelebrating) R.string.onboarding_confirm_bubble_done else R.string.onboarding_confirm_bubble,
        ),
        headline = stringResource(R.string.onboarding_confirm_title),
        onBack = onBack,
        bottomBar = {
            PrimaryButton(
                label = stringResource(R.string.onboarding_confirm_cta),
                onClick = onFinish,
                enabled = !isCelebrating,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        CalorieHero(
            calories = targets.calories,
            belowFloor = targets.belowFloor,
            maintenance = maintenance,
            override = form.calorieOverrideKcal,
            goal = profile.goal,
            editable = !isCelebrating,
            onSet = { onFormChange(form.copy(calorieOverrideKcal = it)) },
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_confirm_macro_split),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // The celebration draws the bar left to right rather than revealing it — the same four
            // roles behaving differently, which is the whole budget for this moment.
            MacroBar(
                proteinG = targets.proteinG,
                carbsG = targets.carbsG,
                fatG = targets.fatG,
                modifier = Modifier.drawWithContent {
                    clipRect(right = size.width * bar.value) { this@drawWithContent.drawContent() }
                },
            )
            MacroInputGroup(
                proteinG = targets.proteinG,
                carbsG = targets.carbsG,
                fatG = targets.fatG,
                onProteinChange = { onFormChange(form.copy(proteinOverrideG = it)) },
                onCarbsChange = { onFormChange(form.copy(carbsOverrideG = it)) },
                onFatChange = { onFormChange(form.copy(fatOverrideG = it)) },
                step = 5,
                showPercentages = true,
            )
        }
        Text(
            text = stringResource(R.string.onboarding_confirm_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

/** The number, its two steppers, and the line that shows its working. */
@Composable
private fun CalorieHero(
    calories: Int,
    belowFloor: Boolean,
    maintenance: Int,
    override: Int?,
    goal: Goal,
    editable: Boolean,
    onSet: (Int) -> Unit,
) {
    val label = stringResource(R.string.onboarding_confirm_calories)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .then(
                if (!editable) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
                } else {
                    Modifier
                },
            )
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnimatedVisibility(visible = editable) {
                StepperButton(
                    symbol = "−",
                    label = stringResource(R.string.onboarding_confirm_decrease),
                    onClick = { onSet((calories - CALORIE_STEP_KCAL).coerceAtLeast(CALORIE_TARGET_KCAL.first)) },
                )
            }
            Text(
                text = "%,d".format(calories),
                style = MaterialTheme.typography.displayLarge.tabularNums.copy(
                    fontSize = 57.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-1.5).sp,
                ),
                // Warn, never block: the figure goes `error`, the button stays enabled, and there
                // is no `errorContainer` anywhere near it — being under the floor is a caution the
                // user may walk past, which is the same treatment Profile's calorie card gives it.
                color = if (belowFloor) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            AnimatedVisibility(visible = editable) {
                StepperButton(
                    symbol = "+",
                    label = stringResource(R.string.onboarding_confirm_increase),
                    onClick = { onSet((calories + CALORIE_STEP_KCAL).coerceAtMost(CALORIE_TARGET_KCAL.last)) },
                )
            }
        }
        Text(
            // A unit symbol, not copy — the one thing on this screen that is the same in every
            // language it will ever be read in.
            text = "kcal",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (belowFloor) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = AppIcons.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(CALORIE_FLOOR_WARNING),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = AppIcons.Formula,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(
                    R.string.onboarding_confirm_derivation,
                    maintenance,
                    derivation(maintenance, calories, override, goal),
                ),
                style = MaterialTheme.typography.bodySmall.tabularNums,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The second half of the derivation line. A manual figure says so — "you set yourself" is the
 * honest sentence once the goal's own adjustment is no longer what produced the number, and it is
 * also what makes an override visible rather than silent. */
@Composable
private fun derivation(maintenance: Int, calories: Int, override: Int?, goal: Goal): String {
    val delta = calories - maintenance
    return when {
        override != null && delta < 0 -> stringResource(R.string.onboarding_confirm_adjust_manual_down, abs(delta))
        override != null -> stringResource(R.string.onboarding_confirm_adjust_manual_up, delta)
        goal == Goal.Lose -> stringResource(R.string.onboarding_confirm_adjust_lose, abs(goal.calorieAdjustment()))
        goal == Goal.Build -> stringResource(R.string.onboarding_confirm_adjust_build, goal.calorieAdjustment())
        else -> stringResource(R.string.onboarding_confirm_adjust_maintain)
    }
}

private val PREVIEW_FORM = OnboardingForm(
    goal = Goal.Lose,
    age = 25,
    sex = Sex.Female,
    heightCm = 170.0,
    weightKg = 65.0,
    activityLevel = ActivityLevel.Light,
)

@PreviewLightDark
@Composable
private fun ConfirmTargetsScreenPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ConfirmTargetsScreen(
                form = PREVIEW_FORM,
                isCelebrating = false,
                onFormChange = {},
                onFinish = {},
                onBack = {},
            )
        }
    }
}

/** A manual decrease under the floor: the number goes `error`, the line says who set it, and the
 * button stays filled and enabled. */
@PreviewLightDark
@Composable
private fun ConfirmTargetsScreenBelowFloorPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ConfirmTargetsScreen(
                form = PREVIEW_FORM.copy(calorieOverrideKcal = 1100),
                isCelebrating = false,
                onFormChange = {},
                onFinish = {},
                onBack = {},
            )
        }
    }
}

/** The 900ms the flow takes to finish: Rui grows and celebrates, the steppers go, the hero takes a
 * primary border, and the button waits. No confetti and no new colours. */
@PreviewLightDark
@Composable
private fun ConfirmTargetsScreenCelebratingPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ConfirmTargetsScreen(
                form = PREVIEW_FORM,
                isCelebrating = true,
                onFormChange = {},
                onFinish = {},
                onBack = {},
            )
        }
    }
}
