package ph.mart.healthapp.feature.food.ui.voice.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R

/**
 * The sentence, quoted back, on every screen that is *about* it rather than asking for it.
 *
 * Two screens draw it and for the same reason. The parse screen shows it during the wait, because
 * a mis-dictated word is cheapest to catch while the call is still in flight — the screen used to
 * be a bare spinner over words the user could no longer see. The dead ends show it because "no food
 * in that one" and "that didn't work" are both statements about a specific sentence, and a screen
 * that makes a claim about words should have the words on it.
 *
 * The sentence is `titleLarge`, not body: it is being proofread, not read. The eyebrow names the
 * slot beside it, which is the other thing the user chose and the other thing they might want to
 * change before spending a second parse.
 *
 * [onEdit] null draws no footer at all — the offline state deliberately shows the sentence without
 * offering to fix it, because the words are not what is wrong.
 */
@Composable
internal fun SentenceCard(
    sentence: String,
    mealType: MealType,
    modifier: Modifier = Modifier,
    caveat: String? = null,
    onEdit: (() -> Unit)? = null,
) {
    AppCard(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.food_voice_you_said, stringResource(mealType.labelRes)),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.9.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = sentence,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (caveat != null || onEdit != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = caveat.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f),
                    )
                    if (onEdit != null) {
                        TextButton(
                            label = stringResource(R.string.food_voice_edit_short),
                            onClick = onEdit,
                            icon = AppIcons.Edit,
                        )
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SentenceCardPreview() {
    AppTheme {
        Surface {
            SentenceCard(
                sentence = "two scrambled eggs, a slice of toast and a black coffee",
                mealType = MealType.Breakfast,
                caveat = "Wrong word? Fix it now — this doesn't have to finish.",
                onEdit = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** The dead-end shape: the words, the slot, and nothing offered — the buttons are the screen's. */
@PreviewLightDark
@Composable
private fun SentenceCardBarePreview() {
    AppTheme {
        Surface {
            SentenceCard(
                sentence = "a cup of tea",
                mealType = MealType.Snacks,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
