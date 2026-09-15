package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * Shared bottom-sheet chrome: Material 3 [ModalBottomSheet] with the app's
 * [MaterialTheme.colorScheme.surfaceContainerLow] container and the standard drag handle.
 * Scrim, swipe-to-dismiss, window insets (nav bar + IME) and predictive-back dismissal all come
 * from [ModalBottomSheet]. A sub-level inside the sheet — the [SheetDatePicker] calendar — can
 * still take back first: its handler registers after the sheet's, so it wins while showing.
 *
 * The content column scrolls, because a sheet is only ever as tall as the screen and the tallest
 * of these (the food diary's add-entry sheet: recipes, saved meals, recents, search, then the form
 * itself) runs past that on a small phone — without this its Add button is simply out of reach.
 * The sheet's own drag still wins while the scroll sits at the top. Nothing inside a sheet may be
 * a lazy list: this hands its children unbounded height.
 *
 * [horizontalPadding] is the content column's gutter and is 16dp for every sheet but one. The
 * quick-action sheet passes `0.dp` and pads each of its rows instead, so a row's pressed state
 * layer runs the sheet's full width rather than stopping short of it — a list row's ripple that
 * leaves a 16dp margin either side reads as a button, not a row.
 *
 * [bottomBar] is drawn **outside** the scrolling column, pinned under it: a sheet whose action is
 * the last thing in a scroll makes committing cost a scroll past everything the user has already
 * decided about. It is null for every sheet whose content is short enough that the two are the same
 * thing, and the add-entry sheet is the one that passes it. It carries the sheet's own bottom
 * gutter, so the content column gives its 24dp up when a bar is present.
 *
 * [scrollable] is false for the one sheet whose content scrolls *itself* — the add-entry sheet's
 * search state hands its whole height to one list with its own scroller, and a scroll inside a
 * scroll is the thing that redesign existed to remove. A non-scrolling column is given the height
 * rather than sized to its content, so the child can fill it.
 *
 * [scrollState] is passed in only where the caller has to *read* the scroll — the add-entry form's
 * top bar takes over the food's name once the card has gone past it.
 *
 * [expanded] asks the sheet for the whole screen and keeps it there — the add-entry sheet's search
 * state, which hands its full height to one list. A `Boolean` rather than a hoisted `SheetState`
 * because `SheetState` is an experimental Material type: putting it in this signature would push an
 * `@OptIn` onto every sheet in the app to answer a question one caller asks. The state stays inside
 * this file, where the opt-in already is.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    expanded: Boolean = false,
    scrollable: Boolean = true,
    scrollState: ScrollState = rememberScrollState(),
    bottomBar: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // ModalBottomSheet lives in its own dialog window, which @Preview can't host — render a static
    // stand-in so every caller's @PreviewLightDark still shows the sheet.
    if (LocalInspectionMode.current) {
        PreviewSheet(
            modifier = modifier,
            horizontalPadding = horizontalPadding,
            bottomBar = bottomBar,
            content = content,
        )
        return
    }

    val sheetState = rememberModalBottomSheetState()
    // Only ever asked to grow. Coming back down is the content shrinking, not the sheet being
    // dragged — a settle() to partial would fight a user who had already pulled it up themselves.
    LaunchedEffect(expanded) { if (expanded) sheetState.expand() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        SheetBody(
            horizontalPadding = horizontalPadding,
            scrollable = scrollable,
            scrollState = scrollState,
            bottomBar = bottomBar,
            content = content,
        )
    }
}

/**
 * The content column and, under it, the docked bar.
 *
 * `weight(1f, fill = false)` is what lets one shape serve both: with no bar the column is as tall as
 * its content and the sheet sizes itself to it, exactly as before; with one, the column gives up
 * whatever the bar needs and scrolls the rest. Plain `weight(1f)` would stretch every short sheet to
 * the full screen.
 */
@Composable
private fun ColumnScope.SheetBody(
    horizontalPadding: Dp,
    scrollable: Boolean,
    scrollState: ScrollState,
    bottomBar: @Composable (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // fill = false is what lets one shape serve every sheet: a short sheet stays the height
            // of its content. The search state is the one that wants the height handed to it.
            .weight(1f, fill = !scrollable)
            .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier)
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                // The bar carries the bottom gutter when there is one, so the scroll can run right
                // up to the rule above it — that half-cut last row is the "more below" signal.
                bottom = if (bottomBar == null) 24.dp else 0.dp,
            ),
        content = content,
    )
    bottomBar?.invoke()
}

@Composable
private fun PreviewSheet(
    modifier: Modifier,
    horizontalPadding: Dp,
    bottomBar: @Composable (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(top = 12.dp, bottom = if (bottomBar == null) 24.dp else 0.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant),
            )
            Box(modifier = Modifier.size(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = horizontalPadding, end = horizontalPadding),
                content = content,
            )
            bottomBar?.invoke()
        }
    }
}

@PreviewLightDark
@Composable
private fun AppBottomSheetPreview() {
    AppTheme {
        AppBottomSheet(onDismiss = {}) {
            Text(
                text = "Log food",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
    }
}
