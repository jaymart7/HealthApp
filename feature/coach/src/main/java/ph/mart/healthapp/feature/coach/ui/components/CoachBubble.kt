package ph.mart.healthapp.feature.coach.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.coach.R

/** How wide a bubble may get, as a share of the list. Narrow enough that the eye still has a ragged
 * right edge to track down, which is most of what makes a transcript readable. */
private const val BUBBLE_SHARE = 0.84f

/** And an absolute ceiling on top of it, so a tablet does not get 900px lines. A share alone is a
 * rule about *this* screen's width; a measure is a rule about reading. */
private val BubbleMax = 480.dp

/**
 * The coach's side of a turn.
 *
 * It **stopped being `MascotSpeechBubble`**, which is the app's mascot-dialogue bubble and is still
 * exactly right on Home and in onboarding: centred text in a 280dp box with a tail on its vertical
 * middle, for one cheerful sentence. An answer is prose — several lines of it, with figures — so it
 * wants left-aligned text, a width that tracks the screen, and a tail at the *bottom* corner where
 * the speaker is. Flipping the shared one would have made every caller's bubble worse to make this
 * one right.
 *
 * The corner the tail leaves from is square-ish (8dp against the other three at 20dp), and that is
 * the half doing the work: [UserBubble] is the mirror image, so the two are told apart by shape
 * before colour and a greyscale screenshot still reads as a conversation.
 */
@Composable
internal fun CoachBubble(text: String, modifier: Modifier = Modifier, receipt: String? = null) {
    CoachBubbleShell(modifier = modifier) {
        BubbleText(text = text, color = MaterialTheme.colorScheme.onSurface)
        if (receipt != null) Receipt(line = receipt)
    }
}

/** The coach's bubble with something other than prose in it — the pending turn's placeholder lines.
 * The shell is shared so a bubble that is waiting and a bubble that is talking are the same object
 * on screen, which is what lets the first chunk overwrite in place rather than swapping bubbles. */
@Composable
internal fun CoachBubbleShell(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = BubbleSurface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 8.dp),
    tailAlignment = Alignment.BottomStart,
    tailOffset = (-3).dp,
    modifier = modifier,
    content = content,
)

/** The user's side: the mirror, and deliberately not the coach's bubble flipped — the tail points
 * at whoever is speaking, and there is no second face on this screen to point at. */
@Composable
internal fun UserBubble(text: String, modifier: Modifier = Modifier) {
    BubbleSurface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 8.dp, bottomStart = 20.dp),
        tailAlignment = null,
        modifier = modifier,
    ) {
        BubbleText(text = text, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

/**
 * The shared shell: the width rule, the fill, the corners and the optional tail.
 *
 * `BoxWithConstraints` because the cap is *both* a share and a measure, and the smaller of the two
 * has to win — a percentage alone breaks on a tablet and a fixed `widthIn` alone breaks on a small
 * phone. One subcomposition per bubble is the cost, and a bubble measures once.
 */
@Composable
private fun BubbleSurface(
    color: Color,
    shape: RoundedCornerShape,
    tailAlignment: Alignment?,
    modifier: Modifier = Modifier,
    tailOffset: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val cap = minOf(maxWidth * BUBBLE_SHARE, BubbleMax)
        Box {
            // Under the bubble rather than beside it, so the rounded corner covers its inner half
            // and only the point shows. 2dp of its own radius, or the tip reads as a shard.
            if (tailAlignment != null) {
                Box(
                    modifier = Modifier
                        .align(tailAlignment)
                        .offset(x = tailOffset, y = (-10).dp)
                        .size(10.dp)
                        .rotate(45f)
                        .background(color, RoundedCornerShape(2.dp)),
                )
            }
            Surface(shape = shape, color = color, modifier = Modifier.widthIn(max = cap)) {
                Column(content = content)
            }
        }
    }
}

/** Every figure in an answer is tabular: a streaming reply rewrites its own text several times a
 * second, and digits that change width make the whole line jitter while it does. */
@Composable
private fun BubbleText(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.tabularNums,
        color = color,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

/**
 * What a confirmed draft wrote, inside the answer it came from.
 *
 * Under a rule rather than run into the prose, because it is a different kind of sentence: the rest
 * of the bubble is the coach talking and this is the app reporting. The check is `primary` and the
 * words say "Logged" — colour never carries it alone, which is the rule the draft card's own
 * "nothing logged yet" follows from the other side.
 */
@Composable
private fun Receipt(line: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp)) {
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = AppIcons.CheckCircle,
                contentDescription = stringResource(R.string.coach_receipt_logged),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium.tabularNums,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun CoachBubblePreview() {
    AppTheme {
        Surface {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                UserBubble(text = "Am I getting enough protein?")
                CoachBubble(
                    text = "You're at 62 g of 150 g today, so there's plenty of room. A " +
                        "high-protein dinner would close most of that gap.",
                )
                CoachBubble(
                    text = "Done — two scrambled eggs for breakfast.",
                    receipt = "Logged: Scrambled eggs, 220 kcal.",
                )
            }
        }
    }
}
