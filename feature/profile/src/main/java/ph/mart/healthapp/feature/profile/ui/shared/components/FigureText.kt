package ph.mart.healthapp.feature.profile.ui.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/** Not copy — the separator between two figures, and a glyph rather than a word for the same
 * reason a middle dot was wrong here: it has to read as thinner than either figure it parts. */
private const val FIGURE_SEPARATOR = "/"

/**
 * One figure and the unit it is measured in. [value] is the number — always tabular — and [unit]
 * is the word after it, which may be empty for a figure that is already a phrase ("twice a day").
 */
internal data class Figure(val value: String, val unit: String = "")

/**
 * The row's figure line: the numbers the user saved, drawn as data rather than as a grey caption.
 * This is the one place they are audited, so the figure carries `onSurface` and only the unit
 * stays quiet.
 *
 * Every figure on all three screens goes through here, which is what stops tabular figures being
 * a per-screen decision — a kcal count that jitters as a row updates is the one thing all of them
 * would otherwise be free to get wrong separately.
 *
 * The separator is an `outlineVariant` slash rather than a middle dot in the text colour: a dot at
 * full ink reads as a third figure, and the whole point of the line is that the figures are the
 * only things on it carrying weight.
 */
@Composable
internal fun FigureRow(vararg figures: Figure, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        figures.forEachIndexed { index, figure ->
            if (index > 0) {
                Text(
                    text = FIGURE_SEPARATOR,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            FigureText(figure)
        }
    }
}

@Composable
private fun FigureText(figure: Figure) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = figure.value,
            style = MaterialTheme.typography.bodyMedium.tabularNums,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (figure.unit.isNotEmpty()) {
            Text(
                text = figure.unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A figure line with nothing to quote — a supplement with no dose, a section with no figure.
 * All quiet, because there is no number on it to be the thing that is read. */
@Composable
internal fun QuietFigureLine(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@PreviewLightDark
@Composable
private fun FigureRowPreview() {
    AppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                FigureRow(Figure("420", "kcal"), Figure("1", "serving"))
                FigureRow(Figure("500", "mg"), Figure("twice a day"))
                FigureRow(Figure("3", "lifts"), Figure("9", "sets"))
                QuietFigureLine(text = "No dose set · once a day")
            }
        }
    }
}
