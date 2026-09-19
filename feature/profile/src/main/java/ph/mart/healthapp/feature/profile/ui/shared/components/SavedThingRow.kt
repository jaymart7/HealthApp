package ph.mart.healthapp.feature.profile.ui.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * One saved thing — a supplement, a food, a saved meal, a recipe, a workout routine — as its own
 * list shows it. Successor to `LibraryRow` and `SupplementListRow`, which were the same row drawn
 * twice and drifting apart: different type for the name, figures rendered as grey captions, and
 * two 44dp icon buttons with delete sitting 4dp from rename.
 *
 * Three slots, always in this order, on all three screens. What changes per screen is the [marker]
 * and what [figures] counts — never the geometry.
 *
 * **[onClick] is the row's only control**, and the row carries nothing on its right. The two icon
 * buttons became one overflow menu and the menu is now gone too: its first item opened the same
 * sheet the row already opens, and its second belongs *inside* that sheet, under the thing it
 * would destroy. No chevron either — see below.
 *
 * **Still no way to use anything.** Logging a meal needs a meal slot and a day, starting a routine
 * needs a workout in progress, and ticking a supplement needs a day — Profile has none of those.
 * [onClick] opens an edit sheet; it never logs, ticks or starts. That is also why there is no
 * trailing chevron: this row does not *go* anywhere, and the card's own ripple is what says it
 * takes a tap.
 *
 * The card sits a step down at `surfaceContainerLow`. `surfaceContainerHighest` sat too close to
 * the figures' own ink; one step down lets a number carry `onSurface` instead of grey, and frees
 * `surfaceContainerHighest` for the markers and the once-daily frequency tile.
 *
 * [detail] is one line and never wraps — the contents on saved meals, recipes and routines, so a
 * row isn't deleted blind. [detailContent] is the same slot for the one caller whose detail is not
 * text: My foods draws its macro triplet there, in the macros' own fixed colours.
 *
 * [footer] is drawn full-bleed under a 1dp rule, and is how a routine's plan zone belongs to the
 * routine rather than hanging off the bottom of it. Null by default.
 *
 * [highlight] is the run a search matched, marked in [name] and [detail] so the reason a row is in
 * a result is visible rather than guessed at. Empty for every row that is not a search result,
 * which is every row on two of the three screens.
 */
@Composable
internal fun SavedThingRow(
    name: String,
    marker: @Composable () -> Unit,
    figures: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    detailContent: (@Composable () -> Unit)? = null,
    highlight: String = "",
    onClick: (() -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                // Symmetric now that nothing sits on the right. The 8dp this used to end on was
                // the overflow menu's touch box hanging off the edge of its glyph.
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                marker()
                Column(
                    // 2dp where there is no third line: a two-line stack wants less air than a
                    // three-line one to read as one block.
                    verticalArrangement = Arrangement.spacedBy(
                        if (detail == null && detailContent == null) 2.dp else 4.dp,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Text(
                        text = name.marking(highlight, boldMatch = false),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    figures()
                    if (detail != null) {
                        Text(
                            // The matched run in a contents line also takes `onSurface` at 500:
                            // the line is grey by default, and a highlight on grey ink is a
                            // background with nothing in it.
                            text = detail.marking(highlight, boldMatch = true),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    detailContent?.invoke()
                }
            }
            if (footer != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                footer()
            }
        }
    }
    val shape = MaterialTheme.shapes.large
    if (onClick == null) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = shape,
            modifier = modifier.fillMaxWidth(),
            content = content,
        )
    } else {
        Surface(
            onClick = onClick,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = shape,
            modifier = modifier.fillMaxWidth(),
            content = content,
        )
    }
}

/** [text] with every occurrence of [run] backed in `secondaryContainer`. Plain text when nothing
 * is being searched for, which is the only path two of the three screens ever take. */
@Composable
private fun String.marking(run: String, boldMatch: Boolean): AnnotatedString {
    if (run.isBlank()) return AnnotatedString(this)
    val highlightStyle = SpanStyle(
        background = MaterialTheme.colorScheme.secondaryContainer,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = if (boldMatch) FontWeight.Medium else null,
    )
    return buildAnnotatedString {
        var cursor = 0
        while (true) {
            val hit = indexOf(run, cursor, ignoreCase = true)
            if (hit < 0) break
            append(substring(cursor, hit))
            withStyle(highlightStyle) { append(substring(hit, hit + run.length)) }
            cursor = hit + run.length
        }
        append(substring(cursor))
    }
}

/**
 * The row family's leading tile: 40dp, 12dp radius, one 20dp glyph.
 *
 * Not [IconTile]. That one is a nav row's affordance — `secondaryContainer`, meaning "this goes
 * somewhere" — and its quiet tone is `surfaceContainer`, one step too close to this card's new
 * `surfaceContainerLow` to read as a tile at all. This says what kind of thing the row holds, and
 * is never a tap target.
 */
@Composable
internal fun RowMarker(icon: ImageVector, contentDescription: String?) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun SavedThingRowPreview() {
    AppTheme {
        Surface {
            SavedThingRow(
                name = "Usual breakfast",
                marker = { RowMarker(icon = AppIcons.Food.outlined, contentDescription = null) },
                figures = { FigureRow(Figure("3", "items"), Figure("540", "kcal")) },
                detail = "Greek yogurt, Oats, Black coffee",
                onClick = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/** A row with a footer — the shape the routine library draws, with its plan zone under the rule. */
@PreviewLightDark
@Composable
private fun SavedThingRowFooterPreview() {
    AppTheme {
        Surface {
            SavedThingRow(
                name = "Push day",
                marker = { RowMarker(icon = AppIcons.Dumbbell, contentDescription = null) },
                figures = { FigureRow(Figure("3", "lifts"), Figure("9", "sets")) },
                detail = "Bench press 3×8, Overhead press 3×8, Dip 2×10",
                onClick = {},
                footer = {
                    Text(
                        text = "Plan zone",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 12.dp),
                    )
                },
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
