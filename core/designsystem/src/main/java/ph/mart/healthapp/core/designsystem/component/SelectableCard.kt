package ph.mart.healthapp.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums

/** How a card carries its glyph. [Circle] is the answer-sized treatment onboarding's goal and
 * activity steps use, where the card is most of the screen and the icon is the thing the eye lands
 * on; [Inline] is a glyph beside one line of text, for a card that is only as tall as its label. */
enum class CardIcon { Inline, Circle }

/**
 * Large tappable card. Selected = [MaterialTheme.colorScheme.primaryContainer] fill + 2dp
 * [MaterialTheme.colorScheme.primary] border, and a trailing check, because on a card that is
 * already the size of a paragraph the fill alone is a colour change rather than an answer.
 *
 * [outlined] drops the fill for a 1dp outline. It is how a step says it is optional: three steps
 * of filled cards have taught the reader what "required" looks like, so dropping one level is
 * legible without a badge or a footnote. Selected still fills — *optional* applies to the
 * question, not to the answer.
 *
 * [supporting] is a third line under the subtitle, for the consequence of the choice rather than
 * its description — onboarding's activity step prints what the level does to the maintenance
 * figure there.
 */
@Composable
fun SelectableCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    supporting: String? = null,
    leadingIcon: ImageVector? = null,
    icon: CardIcon = CardIcon.Inline,
    outlined: Boolean = false,
) {
    val border = when {
        selected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        outlined -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        else -> null
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = when {
            selected -> MaterialTheme.colorScheme.primaryContainer
            outlined -> Color.Transparent
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        border = border,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            },
    ) {
        Row(
            // A selected card takes its 2dp border out of its own padding, so the inner geometry
            // is identical either way and nothing shifts by two pixels on selection.
            modifier = Modifier.padding(if (selected) 14.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                CardGlyph(icon = leadingIcon, style = icon, selected = selected)
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                AnimatedVisibility(visible = supporting != null && selected) {
                    Text(
                        text = supporting.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.tabularNums,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            AnimatedVisibility(
                visible = selected,
                enter = scaleIn(initialScale = 0.8f, animationSpec = tween(150)) + fadeIn(tween(150)),
                exit = fadeOut(),
            ) {
                Icon(
                    imageVector = AppIcons.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/** The glyph, in whichever of the two treatments the caller asked for. A selected circle flips to
 * [MaterialTheme.colorScheme.primary] — for the 400ms a card is held before the step advances it
 * is the loudest thing on the screen, which is what makes the hold read as confirmation. */
@Composable
private fun CardGlyph(icon: ImageVector, style: CardIcon, selected: Boolean) {
    when (style) {
        CardIcon.Inline -> Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
        }

        CardIcon.Circle -> Surface(
            shape = CircleShape,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxHeight()) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SelectableCardPreview() {
    AppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                SelectableCard(
                    title = "Lose weight",
                    subtitle = "Gradual, sustainable calorie deficit",
                    leadingIcon = AppIcons.TrendDown,
                    icon = CardIcon.Circle,
                    selected = true,
                    onClick = {},
                    modifier = Modifier.height(140.dp),
                )
                SelectableCard(
                    title = "Maintain",
                    subtitle = "Keep your current weight steady",
                    leadingIcon = AppIcons.Balance,
                    icon = CardIcon.Circle,
                    selected = false,
                    onClick = {},
                    modifier = Modifier.height(140.dp),
                )
                SelectableCard(title = "Vegetarian", leadingIcon = AppIcons.Egg, selected = false, outlined = true, onClick = {})
            }
        }
    }
}

/** The third line, which only a selected card carries — the consequence of the answer, not its
 * description. */
@PreviewLightDark
@Composable
private fun SelectableCardSupportingPreview() {
    AppTheme {
        Surface {
            SelectableCard(
                title = "Light",
                subtitle = "Light exercise 1–3 days/week",
                supporting = "× 1.375 on your maintenance",
                leadingIcon = AppIcons.Steps,
                icon = CardIcon.Circle,
                selected = true,
                onClick = {},
                modifier = Modifier.padding(16.dp).height(144.dp),
            )
        }
    }
}
