package ph.mart.healthapp.feature.progress.ui.progress.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding

/**
 * The shared scroll container the five Column-based tabs sit in — Photos is excluded because
 * its LazyVerticalGrid scrolls itself, and nesting it here would measure it with infinite height.
 *
 * No `@PreviewLightDark`: it draws nothing of its own, so a preview would render whatever sample
 * content the preview itself supplied.
 */
@Composable
internal fun ScrollingTab(scrollState: ScrollState, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(top = 16.dp, bottom = DockedFabContentPadding),
        content = content,
    )
}
