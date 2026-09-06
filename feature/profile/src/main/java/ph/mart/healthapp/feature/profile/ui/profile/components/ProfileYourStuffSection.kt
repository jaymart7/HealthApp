package ph.mart.healthapp.feature.profile.ui.profile.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.AppCard
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.shared.components.AppListRow
import ph.mart.healthapp.feature.profile.ui.shared.components.IconTile
import ph.mart.healthapp.feature.profile.ui.shared.components.NavChevron

/**
 * Everything the user has saved and can go and edit: their supplement list, their food library,
 * their workout routines. Three rows in one card, where they used to be two sections and three
 * cards — they are the same kind of thing (a door out of this screen) and now look it.
 *
 * The `secondaryContainer` tile is what says so. It is the app's "this goes somewhere" tone, and
 * it is exactly what the Day-targets rows above deliberately do *not* wear: those stay put.
 *
 * **No counts, no cached state.** A number here is one more thing that can go stale, and the screen
 * it opens is where counting is honest.
 */
@Composable
internal fun ProfileYourStuffSection(
    onOpenSupplements: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenRoutines: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
        NavRow(
            label = stringResource(R.string.profile_stuff_supplements),
            sublabel = stringResource(R.string.profile_supplements_row_sub),
            icon = AppIcons.Supplement,
            onClick = onOpenSupplements,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavRow(
            label = stringResource(R.string.profile_library_food),
            sublabel = stringResource(R.string.profile_library_food_sub),
            icon = AppIcons.Book,
            onClick = onOpenLibrary,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavRow(
            label = stringResource(R.string.profile_library_routines),
            sublabel = stringResource(R.string.profile_library_routines_sub),
            icon = AppIcons.Dumbbell,
            onClick = onOpenRoutines,
        )
    }
}

/** The row is the tap target rather than the card, which is the difference between one card holding
 * three doors and three cards that are each one. */
@Composable
private fun NavRow(label: String, sublabel: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        AppListRow(
            label = label,
            sublabel = sublabel,
            leading = { IconTile(icon = icon, contentDescription = null) },
            trailing = { NavChevron() },
        )
    }
}

@PreviewLightDark
@Composable
private fun ProfileYourStuffSectionPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ProfileYourStuffSection(
                onOpenSupplements = {},
                onOpenLibrary = {},
                onOpenRoutines = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
