package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * The shared card chrome — `surfaceContainerLow`, 20dp corners, 16dp padding. Started life as
 * Home's private card; promoted here in Phase 8 once Profile needed the identical chrome, per
 * CLAUDE.md's "used in ≥2 screens → :core:designsystem, never duplicated" rule.
 *
 * [shape], [border] and [contentPadding] all default to that chrome, so every caller that predates
 * them is unchanged. They exist for the food diary's section cards, which are the one place in the
 * app where a card is a *container for rows* rather than a padded block: 16dp corners, zero
 * padding (the rows carry their own indent), and — for the exercise block — a transparent fill with
 * a 1dp `outlineVariant` border, the inverse of the meal cards' filled-no-border. That inversion is
 * what tells a credit apart from a meal without spending a colour on it.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    shape: Shape = RoundedCornerShape(20.dp),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick == null) {
        Surface(shape = shape, color = color, border = border, modifier = modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    } else {
        // Surface's own onClick overload, so the ripple is clipped to the card's corners.
        Surface(onClick = onClick, shape = shape, color = color, border = border, modifier = modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}

@PreviewLightDark
@Composable
private fun AppCardPreview() {
    AppTheme {
        Surface {
            AppCard(modifier = Modifier.padding(16.dp)) {
                Text(text = "Card content", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

/** The diary's two section containers: filled-no-border, and its inverse. They have to be
 * distinguishable at a glance in both schemes without either one reading as disabled. */
@PreviewLightDark
@Composable
private fun AppCardSectionPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                AppCard(shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(16.dp)) {
                    Text(text = "Breakfast", color = MaterialTheme.colorScheme.onSurface)
                }
                AppCard(
                    modifier = Modifier.padding(top = 12.dp),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Text(text = "Exercise", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
