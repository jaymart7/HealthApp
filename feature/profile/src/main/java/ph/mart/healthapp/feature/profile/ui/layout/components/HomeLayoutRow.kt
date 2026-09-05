package ph.mart.healthapp.feature.profile.ui.layout.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ph.mart.healthapp.core.designsystem.component.HomeCard
import ph.mart.healthapp.core.designsystem.component.HomeCardSetting
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R

/**
 * The fixed height every row in this list wears. Uniform rows are what let the drag work off one
 * constant instead of measuring each item.
 *
 * ponytail: measure per row via `LazyListState.layoutInfo` if these ever gain variable height.
 */
internal val HomeLayoutRowHeight = 64.dp

/**
 * One card in the Home layout editor: a drag handle, its name, and a switch.
 *
 * The handle is the only thing that starts a drag — a long-press on the whole row would both
 * delay the gesture and put it in competition with the list's own scroll. That makes the reorder
 * pointer-only, which is why every row also carries **Move up / Move down** accessibility
 * actions: a drag nobody using TalkBack can perform is not a control.
 */
@Composable
internal fun HomeLayoutRow(
    setting: HomeCardSetting,
    dragging: Boolean,
    offsetY: Float,
    onToggle: () -> Unit,
    onMove: (Int) -> Boolean,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = setting.card
    // A drag nobody using TalkBack can perform is not a control, so the row carries these two.
    // Resolved here, not in the semantics lambda, which cannot read a resource.
    val moveUp = stringResource(R.string.profile_layout_move_up)
    val moveDown = stringResource(R.string.profile_layout_move_down)
    Surface(
        color = if (dragging) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        shape = MaterialTheme.shapes.medium,
        // The dragged row lifts over its neighbours, and its offset is read inside the
        // graphicsLayer lambda so the whole drag settles in the Draw phase.
        tonalElevation = if (dragging) 4.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(HomeLayoutRowHeight)
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer { translationY = offsetY }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(moveUp) { onMove(-1) },
                    CustomAccessibilityAction(moveDown) { onMove(1) },
                )
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, end = 12.dp),
        ) {
            Icon(
                imageVector = AppIcons.DragHandle,
                // Null: the row's Move up/Move down actions are the accessible path, and a
                // focusable handle that can't be dragged would be a dead stop in the traversal.
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(44.dp)
                    .padding(10.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { onDragStart() },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                            onDrag = { change, amount ->
                                change.consume()
                                onDrag(amount.y)
                            },
                        )
                    },
            )
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    text = stringResource(card.label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                // Only the data-gated cards carry one: a switch that is on and still shows
                // nothing has to say why.
                card.note?.let { note ->
                    Text(
                        text = stringResource(note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(checked = setting.visible, onCheckedChange = { onToggle() })
        }
    }
}

@PreviewLightDark
@Composable
private fun HomeLayoutRowPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                HomeLayoutRow(
                    setting = HomeCardSetting(HomeCard.Calories),
                    dragging = false,
                    offsetY = 0f,
                    onToggle = {},
                    onMove = { true },
                    onDragStart = {},
                    onDrag = {},
                    onDragEnd = {},
                )
                HomeLayoutRow(
                    setting = HomeCardSetting(HomeCard.Sleep, visible = false),
                    dragging = true,
                    offsetY = 0f,
                    onToggle = {},
                    onMove = { true },
                    onDragStart = {},
                    onDrag = {},
                    onDragEnd = {},
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
