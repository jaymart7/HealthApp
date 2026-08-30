package ph.mart.healthapp.core.designsystem.component

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.PreviewLightDark
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
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    // ModalBottomSheet lives in its own dialog window, which @Preview can't host — render a static
    // stand-in so every caller's @PreviewLightDark still shows the sheet.
    if (LocalInspectionMode.current) {
        PreviewSheet(modifier = modifier, content = content)
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            content = content,
        )
    }
}

@Composable
private fun PreviewSheet(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
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
                .padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant),
            )
            Box(modifier = Modifier.size(12.dp))
            content()
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
