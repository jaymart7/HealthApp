package ph.mart.healthapp.feature.profile.ui.supplement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.core.designsystem.component.DockedActionBar
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.component.SecondaryButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.core.designsystem.theme.tabularNums
import ph.mart.healthapp.feature.profile.R
import ph.mart.healthapp.feature.profile.ui.shared.components.DeleteConfirmDialog
import ph.mart.healthapp.feature.profile.ui.shared.components.FigureRow
import ph.mart.healthapp.feature.profile.ui.shared.components.QuietFigureLine
import ph.mart.healthapp.feature.profile.ui.shared.components.SavedThingRow
import ph.mart.healthapp.feature.profile.ui.supplement.components.FrequencyMarker
import ph.mart.healthapp.feature.profile.ui.supplement.components.SupplementEditSheet

/**
 * The supplement list, one Nav3 level above Profile — the only place it can be authored. Home's
 * card renders it and ticks it; nothing there can add, rename or remove one, which is why this
 * screen exists at all.
 *
 * Same row family as `FoodLibraryScreen`, and the same refusal: no tick here, because a tick
 * belongs to a day and Profile has none.
 *
 * **Add is docked, not the last thing in the scroll.** The shipped button was a full-width
 * `PrimaryButton` at the bottom of the content, which scrolled away the moment the list got long
 * enough to need it. A screen-level FAB would land beside the app's own at ≥840dp and a top-bar
 * action would put the pane's primary action in the chrome above it; a bar docked to the bottom
 * of the *pane* is the only one of the three that stays inside its own column at both widths.
 */
@Composable
fun SupplementsScreen(
    onOpenScan: () -> Unit,
    viewModel: SupplementsViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    SupplementsContent(uiState = uiState, onEvent = viewModel::handleEvent, onOpenScan = onOpenScan)
}

@Composable
private fun SupplementsContent(
    uiState: SupplementsUiState,
    onEvent: (SupplementsEvent) -> Unit,
    onOpenScan: () -> Unit,
) {
    // Local rather than in a saveable holder, for the same reason `FoodLibraryScreen` keeps its
    // own: a sheet that survived process death would reopen on a row the user has stopped looking
    // at.
    var editing by remember { mutableStateOf<Supplement?>(null) }
    var pendingDelete by remember { mutableStateOf<Supplement?>(null) }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.loaded && uiState.supplements.isEmpty()) {
                    // Centred rather than top-hugged, which the shipped inline version had to be:
                    // the docked bar already guarantees Add is on screen, so the state can read as
                    // an invitation instead of a truncated screen. Idle, not Sleepy — nothing has
                    // failed here, there is simply nothing yet.
                    FullScreenState(
                        icon = { MascotAvatar(state = MascotState.Idle, size = 88.dp) },
                        heading = stringResource(R.string.profile_supplements_empty_heading),
                        body = stringResource(R.string.profile_supplements_empty_body),
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (uiState.supplements.isNotEmpty()) {
                            // A label, not a control, so it scrolls away with the list.
                            item(key = "count") {
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.profile_supplements_count,
                                        uiState.supplements.size,
                                        uiState.supplements.size,
                                    ),
                                    style = MaterialTheme.typography.bodySmall.tabularNums,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                                )
                            }
                        }
                        items(
                            count = uiState.supplements.size,
                            key = { index -> uiState.supplements[index].id },
                        ) { index ->
                            val supplement = uiState.supplements[index]
                            val figures = supplement.figures()
                            SavedThingRow(
                                name = supplement.name,
                                marker = { FrequencyMarker(timesPerDay = supplement.timesPerDay) },
                                figures = {
                                    if (figures.isEmpty()) {
                                        QuietFigureLine(text = supplement.noDoseLine())
                                    } else {
                                        FigureRow(*figures.toTypedArray())
                                    }
                                },
                                onClick = { editing = supplement },
                            )
                        }
                    }
                }
            }
            // Two doors to the same table, and the camera is second on purpose: typing three
            // fields is not a job worth a photo, and the scan earns its place on the bottle the
            // user cannot be bothered to transcribe. Both land in the same sheet.
            DockedActionBar {
                SecondaryButton(
                    label = stringResource(R.string.profile_supplements_scan),
                    onClick = onOpenScan,
                    icon = AppIcons.Camera,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    label = stringResource(R.string.profile_supplements_add),
                    // A blank row with id 0 — the sheet reads that as the add, so there is one
                    // sheet and one save path rather than two of each.
                    onClick = { editing = Supplement(name = "") },
                    icon = AppIcons.Add,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // A supplement is something the user authored, so its delete asks first — the same call the
    // library makes, rather than the diary's swipe-and-undo.
    pendingDelete?.let { supplement ->
        DeleteConfirmDialog(
            name = supplement.name,
            body = stringResource(R.string.profile_supplements_delete_body),
            onDelete = {
                onEvent(SupplementsEvent.OnDelete(supplement.id))
                pendingDelete = null
            },
            onKeep = { pendingDelete = null },
        )
    }

    editing?.let { supplement ->
        SupplementEditSheet(
            supplement = supplement,
            onDismiss = { editing = null },
            onSave = { saved ->
                onEvent(SupplementsEvent.OnSave(saved))
                editing = null
            },
            // The sheet closes and the dialog takes over: one scrim at a time, and the question
            // is asked in the one place that already asks it.
            onDelete = {
                pendingDelete = supplement
                editing = null
            },
        )
    }
}

@PreviewLightDark
@Composable
private fun SupplementsScreenPreview() {
    AppTheme {
        SupplementsContent(
            uiState = SupplementsUiState(
                supplements = listOf(
                    Supplement(id = 3, name = "Magnesium"),
                    Supplement(id = 2, name = "Creatine", dose = "5 g", timesPerDay = 2),
                    // Mon · Wed · Fri — the narrowed schedule, so the row's second figure has
                    // something to say in the preview.
                    Supplement(id = 4, name = "Iron", dose = "18 mg", days = 0b0010101),
                    Supplement(id = 1, name = "Vitamin D", dose = "2000 IU"),
                ),
                loaded = true,
            ),
            onEvent = {},
            onOpenScan = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun SupplementsScreenEmptyPreview() {
    AppTheme {
        SupplementsContent(uiState = SupplementsUiState(loaded = true), onEvent = {}, onOpenScan = {})
    }
}
