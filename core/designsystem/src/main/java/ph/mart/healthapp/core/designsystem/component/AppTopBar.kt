package ph.mart.healthapp.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import ph.mart.healthapp.core.designsystem.R
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme

/**
 * The toolbar every non-top-level destination wears — a title, a back arrow, and an [actions] slot
 * that is empty unless a screen fills it. The counterpart to [BottomNavBar]: a tab gets the bar and
 * the FAB, anything a level above gets this.
 *
 * [actions] is empty by default because `AppScaffold` draws this bar for most routes from a `NavKey`
 * and nothing else, and a trailing control generally needs data only that screen's own container
 * holds — the Photos page's share needs the photo set. A screen with one therefore draws its own
 * bar and `AppScaffold` stands down, the way the camera flows already do.
 *
 * [onBack] is wired to the back *dispatcher* rather than a direct pop (see `AppScaffold`), so a
 * screen holding its own `NavigationBackHandler` still gets to ask before it loses anything.
 *
 * [titleStyle] exists for the one screen whose bar changes what it says: the barcode flow's review
 * step hands over to the food's own name once the subject card has scrolled past, at a smaller
 * size. Deliberately **not** an M3 `TopAppBarScrollBehavior` — that type is still experimental, and
 * putting it in this signature would push an `@OptIn` onto every screen in the app that wears a
 * toolbar, to answer a question the review screen's own `ScrollState` already answers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = titleStyle,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = AppIcons.Back,
                    contentDescription = stringResource(R.string.ds_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier,
    )
}

@PreviewLightDark
@Composable
private fun AppTopBarPreview() {
    AppTheme {
        Surface {
            AppTopBar(title = "Food library", onBack = {})
        }
    }
}
