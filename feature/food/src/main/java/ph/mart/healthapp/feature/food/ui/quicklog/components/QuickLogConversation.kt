package ph.mart.healthapp.feature.food.ui.quicklog.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.exercise.ExerciseEntry
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.QuickLogTurn
import ph.mart.healthapp.core.data.food.RecognitionConfidence
import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.Motion
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.shared.AddEntryForm

/**
 * Everything above the bar: the line that says what the rows answer, the rows as two cards, and the
 * conversation still going on under them.
 *
 * **The conversation is a thread while it lasts** (the handoff's A1–A3). Every question and every
 * answer stays on screen, the user's end-aligned in the field's own colour — so they read as having
 * left the field — and the model's in the AI accent, `tertiaryContainer` under
 * `onTertiaryContainer`, the app's one way of saying the model is talking. While a call runs, the
 * model's turn is a bubble of three dots rather than a spinner somewhere else: the words that were
 * sent stay on screen, which is what the old sheet lost. When rows arrive the thread collapses into
 * "You said · …" over them, and a correction typed on the review is a thread of its own under the
 * rows it corrects. [thread] and [threadStart] come from `QuickLogState`, where that split is tested.
 *
 * Every block carries its own 12dp below it rather than the column spacing them: a block leaving
 * would otherwise leave its gap behind it.
 */
@Composable
internal fun QuickLogConversation(
    said: String?,
    turns: List<QuickLogTurn>,
    threadStart: Int,
    thinking: Boolean,
    foods: List<AddEntryForm>,
    exercises: List<ExerciseEntry>,
    mealType: MealType,
    expandedIndex: Int?,
    onToggleFood: (Int) -> Unit,
    onFoodChange: (Int, AddEntryForm) -> Unit,
    onRemoveFood: (Int) -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onMealTypeSelect: (MealType) -> Unit,
    modifier: Modifier = Modifier,
    waterGlasses: Int? = null,
    weightKg: Double? = null,
    unit: UnitSystem = UnitSystem.Metric,
    offline: Boolean = false,
    onRemoveWater: () -> Unit = {},
    onRemoveWeight: () -> Unit = {},
) {
    val rise = with(LocalDensity.current) { 16.dp.roundToPx() }
    val others = otherRows(
        exercises = exercises,
        waterGlasses = waterGlasses,
        weightKg = weightKg,
        unit = unit,
        unsure = offline,
        onRemoveExercise = onRemoveExercise,
        onRemoveWater = onRemoveWater,
        onRemoveWeight = onRemoveWeight,
    )
    Column(modifier = modifier.fillMaxWidth()) {
        val shownSaid = retainLast(said)
        AnimatedVisibility(
            visible = said != null,
            enter = fadeIn(tween(QuickLogMotion.Enter, easing = Motion.EmphasizedDecelerate)) +
                expandVertically(tween(QuickLogMotion.Enter, easing = Motion.EmphasizedDecelerate)),
            exit = fadeOut(tween(QuickLogMotion.Exit, easing = Motion.EmphasizedAccelerate)) +
                shrinkVertically(tween(QuickLogMotion.Exit, easing = Motion.EmphasizedAccelerate)),
        ) {
            Text(
                text = stringResource(R.string.food_voice_you_said, shownSaid.orEmpty()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        // Kept while it leaves: a card fading out has to still have its rows to fade.
        val shownFoods = retainLast(foods.takeIf { it.isNotEmpty() }).orEmpty()
        AnimatedVisibility(
            visible = foods.isNotEmpty(),
            enter = fadeIn(tween(QuickLogMotion.Enter, easing = Motion.EmphasizedDecelerate)) +
                slideInVertically(tween(QuickLogMotion.Enter, easing = Motion.EmphasizedDecelerate)) { rise },
            exit = fadeOut(tween(QuickLogMotion.Exit, easing = Motion.EmphasizedAccelerate)) +
                shrinkVertically(tween(QuickLogMotion.Exit, easing = Motion.EmphasizedAccelerate)),
        ) {
            FoodCard(
                foods = shownFoods,
                mealType = mealType,
                expandedIndex = expandedIndex,
                onMealTypeSelect = onMealTypeSelect,
                onToggle = onToggleFood,
                onChange = onFoodChange,
                onRemove = onRemoveFood,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        val shownOthers = retainLast(others.takeIf { it.isNotEmpty() }).orEmpty()
        AnimatedVisibility(
            visible = others.isNotEmpty(),
            enter = fadeIn(
                tween(QuickLogMotion.Enter, QuickLogMotion.CardStagger, Motion.EmphasizedDecelerate),
            ) + slideInVertically(
                tween(QuickLogMotion.Enter, QuickLogMotion.CardStagger, Motion.EmphasizedDecelerate),
            ) { rise },
            exit = fadeOut(tween(QuickLogMotion.Exit, easing = Motion.EmphasizedAccelerate)) +
                shrinkVertically(tween(QuickLogMotion.Exit, easing = Motion.EmphasizedAccelerate)),
        ) {
            OtherCard(rows = shownOthers, modifier = Modifier.padding(bottom = 12.dp))
        }

        Thread(turns = turns, threadStart = threadStart, thinking = thinking)
    }
}

/** One place in the thread: a user turn, or the model's — whose [text] is null while it thinks. */
private data class Slot(val fromUser: Boolean, val text: String?)

/**
 * The bubbles, one slot per turn index, **kept after their turn is gone** so a slot can animate out:
 * a cancel, a dead end or a start-over removes turns from the state in one step, and a bubble that
 * simply vanished could not travel back into the field or fold into the "You said" line.
 *
 * The model's slot at a given index is the thinking bubble first and the question after, one bubble
 * that changes its mind — which is how the dots can grow into the question's size (A2).
 */
@Composable
private fun Thread(turns: List<QuickLogTurn>, threadStart: Int, thinking: Boolean) {
    val live = buildMap {
        turns.forEachIndexed { index, turn -> put(index, Slot(turn.fromUser, turn.text)) }
        if (thinking) put(turns.size, Slot(fromUser = false, text = null))
    }
    // Plain rather than snapshot state: it only has to outlive recomposition, and it is rewritten
    // from [live] on every pass — `rememberFillDirection`'s trick, for its reason.
    val known = remember { mutableListOf<Slot>() }
    live.forEach { (index, slot) -> if (index < known.size) known[index] = slot else known.add(slot) }
    val inspection = LocalInspectionMode.current

    known.forEachIndexed { index, remembered ->
        key(index) {
            val slot = live[index] ?: remembered
            // A photo sent with no words said nothing to show.
            val visible = index >= threadStart && index in live && !(slot.fromUser && slot.text.isNullOrBlank())
            // Starts hidden, so a slot's first appearance is an entrance — an `AnimatedVisibility`
            // composed already visible has nothing to animate, and the flight would never leave the
            // field. A preview has no second frame, so it starts where it ends.
            val shown = remember { MutableTransitionState(inspection) }.apply { targetState = visible }
            AnimatedVisibility(
                visibleState = shown,
                enter = if (slot.fromUser) {
                    fadeIn(tween(QuickLogMotion.Swap))
                } else {
                    // After the flight has started, from the corner it speaks from.
                    fadeIn(tween(QuickLogMotion.Swap, QuickLogMotion.Fade, Motion.EmphasizedDecelerate)) +
                        scaleIn(
                            animationSpec = tween(QuickLogMotion.Swap, QuickLogMotion.Fade, Motion.EmphasizedDecelerate),
                            initialScale = 0.9f,
                            transformOrigin = TransformOrigin(0f, 0f),
                        )
                },
                exit = fadeOut(tween(QuickLogMotion.Enter, easing = Motion.EmphasizedAccelerate)) +
                    shrinkVertically(tween(QuickLogMotion.Enter, easing = Motion.EmphasizedAccelerate)),
            ) {
                if (slot.fromUser) {
                    UserBubble(
                        text = slot.text.orEmpty(),
                        modifier = Modifier.padding(bottom = 12.dp),
                        bubbleModifier = Modifier.quickLogShared(turnKey(index), this),
                    )
                } else {
                    AiBubble(text = slot.text, modifier = Modifier.padding(bottom = 12.dp))
                }
            }
        }
    }
}

/**
 * What the user said, end-aligned and filled in the field's own `surfaceContainerHighest`, so it
 * reads as having come out of the field — which, in the sheet, it visibly does. The corner nearest
 * the field is the one that is not rounded. [bubbleModifier] lands on the bubble itself rather than
 * the full-width row, because that is the shape that flies.
 */
@Composable
internal fun UserBubble(text: String, modifier: Modifier = Modifier, bubbleModifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.TopEnd, modifier = modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 4.dp, bottomStart = 16.dp),
            modifier = Modifier
                .padding(start = 48.dp)
                .then(bubbleModifier),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

/**
 * The model's turn, in the AI accent: three dots while it reads, the question once it asks. One
 * bubble for both, so the dots resize into the question rather than one bubble replacing another —
 * the dots fade first and the words arrive over the end of the resize.
 */
@Composable
internal fun AiBubble(text: String?, modifier: Modifier = Modifier) {
    val reading = stringResource(R.string.food_quick_reading)
    Box(contentAlignment = Alignment.TopStart, modifier = modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
            modifier = Modifier.padding(end = 48.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Icon(
                    imageVector = AppIcons.AiSparkle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    // 20dp on a 24dp line: centred on the first line, however many follow.
                    modifier = Modifier.padding(vertical = 2.dp).size(20.dp),
                )
                AnimatedContent(
                    targetState = text,
                    transitionSpec = {
                        fadeIn(
                            tween(QuickLogMotion.Swap, QuickLogMotion.Travel - QuickLogMotion.Swap, Motion.Standard),
                        ) togetherWith fadeOut(tween(QuickLogMotion.Fade, easing = Motion.Standard)) using
                            SizeTransform(clip = false) { _, _ ->
                                tween(QuickLogMotion.Travel, easing = Motion.EmphasizedDecelerate)
                            }
                    },
                    label = "aiTurn",
                ) { shown ->
                    if (shown == null) {
                        ThinkingDots(modifier = Modifier.clearAndSetSemantics { contentDescription = reading })
                    } else {
                        Text(text = shown, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

/** Three dots pulsing in turn — the model reading. Alpha only, read in the draw layer, so the
 * pulse repaints and never recomposes. */
@Composable
private fun ThinkingDots(modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "reading")
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        // One line of bodyLarge tall, so the bubble is the height the question will be.
        modifier = modifier.height(24.dp),
    ) {
        repeat(3) { dot ->
            val alpha = pulse.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(QuickLogMotion.DotsLoop / 2, easing = Motion.Standard),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(dot * QuickLogMotion.DotStagger),
                ),
                label = "dot$dot",
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer { this.alpha = alpha.value }
                    .background(MaterialTheme.colorScheme.onTertiaryContainer, CircleShape),
            )
        }
    }
}

private val PREVIEW_FOODS = listOf(
    AddEntryForm(MealType.Breakfast, "Scrambled eggs", 2.0, "egg", 180, 12, 1, 14),
    AddEntryForm(MealType.Breakfast, "Wholemeal toast", 1.0, "slice", 80, 4, 14, 1)
        .copy(confidence = RecognitionConfidence.Low, uncertainAbout = "a slice"),
)

private val PREVIEW_EXERCISES = listOf(
    ExerciseEntry(type = ExerciseType.Run, name = "along the river", minutes = 30, burnedKcal = 343),
)

@PreviewLightDark
@Composable
private fun BubblesPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(modifier = Modifier.padding(16.dp)) {
                UserBubble(text = "rice and chicken adobo", modifier = Modifier.padding(bottom = 12.dp))
                AiBubble(text = "About how much rice — one cup or two?", modifier = Modifier.padding(bottom = 12.dp))
                UserBubble(text = "two cups", modifier = Modifier.padding(bottom = 12.dp))
                AiBubble(text = null)
            }
        }
    }
}

/** A question waiting: the whole thread, nothing collapsed. */
@PreviewLightDark
@Composable
private fun QuickLogConversationQuestionPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            QuickLogConversation(
                said = null,
                turns = listOf(
                    QuickLogTurn(fromUser = true, text = "rice and chicken adobo"),
                    QuickLogTurn(fromUser = false, text = "About how much rice — one cup or two?"),
                ),
                threadStart = 0,
                thinking = false,
                foods = emptyList(),
                exercises = emptyList(),
                mealType = MealType.Lunch,
                expandedIndex = null,
                onToggleFood = {},
                onFoodChange = { _, _ -> },
                onRemoveFood = {},
                onRemoveExercise = {},
                onMealTypeSelect = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** A meal and a run from one sentence — the thread collapsed into the line over both cards. */
@PreviewLightDark
@Composable
private fun QuickLogConversationReviewPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            QuickLogConversation(
                said = "two eggs and toast, then a 30 min run along the river",
                turns = listOf(QuickLogTurn(fromUser = true, text = "two eggs and toast, then a 30 min run along the river")),
                threadStart = 1,
                thinking = false,
                foods = PREVIEW_FOODS,
                exercises = PREVIEW_EXERCISES,
                mealType = MealType.Breakfast,
                expandedIndex = null,
                onToggleFood = {},
                onFoodChange = { _, _ -> },
                onRemoveFood = {},
                onRemoveExercise = {},
                onMealTypeSelect = {},
                waterGlasses = 3,
                weightKg = 72.4,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** A correction in flight: the rows stay, and the correction and the dots sit under them. */
@PreviewLightDark
@Composable
private fun QuickLogConversationCorrectingPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            QuickLogConversation(
                said = "two eggs and toast",
                turns = listOf(
                    QuickLogTurn(fromUser = true, text = "two eggs and toast"),
                    QuickLogTurn(fromUser = true, text = "make it two slices"),
                ),
                threadStart = 1,
                thinking = true,
                foods = PREVIEW_FOODS,
                exercises = emptyList(),
                mealType = MealType.Breakfast,
                expandedIndex = null,
                onToggleFood = {},
                onFoodChange = { _, _ -> },
                onRemoveFood = {},
                onRemoveExercise = {},
                onMealTypeSelect = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
