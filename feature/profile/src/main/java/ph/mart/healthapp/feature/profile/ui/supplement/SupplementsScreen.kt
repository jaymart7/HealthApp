package ph.mart.healthapp.feature.profile.ui.supplement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.core.data.supplement.appliedTo
import ph.mart.healthapp.core.designsystem.component.DockedFab
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.component.FullScreenState
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
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
 * **Add is a FAB, with Scan label as a small one above it.** Both float over the list rather than
 * scrolling with it. The app's own FAB never competes: below 600dp this route wears no tab chrome,
 * and from 600dp up the app's FAB lives in the rail, so the pair only ever shares the bottom-end
 * corner of *this* pane.
 */
@Composable
fun SupplementsScreen(
    onOpenScan: () -> Unit,
    viewModel: SupplementsViewModel = koinViewModel(),
) {
    val uiState by viewModel.collectAsState()
    // Hoisted out of the content below, unlike `FoodLibraryScreen`'s: the lookup's answer arrives
    // as a side effect and lands *in* the open sheet, so the sheet's seed has to be reachable from
    // where side effects are collected. Still plain `remember` and still for that screen's reason —
    // a sheet that survived process death would reopen on a row the user has stopped looking at.
    var editing by remember { mutableStateOf<Supplement?>(null) }
    var lookupError by remember { mutableStateOf<Int?>(null) }
    var estimated by remember { mutableStateOf(false) }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            // `editing?.let` is the guard as well as the read: a result that lands after the sheet
            // was dismissed is dropped rather than reopening it. Layering it over the draft is what
            // keeps an edit's id, schedule and created-at intact — `appliedTo` argues that.
            is SupplementsSideEffect.LookedUp -> editing?.let { draft ->
                editing = effect.reading.appliedTo(draft)
                estimated = true
            }

            is SupplementsSideEffect.LookupFailed -> lookupError = effect.messageRes
        }
    }

    SupplementsContent(
        uiState = uiState,
        editing = editing,
        // Opening or closing the sheet clears both: the message was about the last name looked up,
        // and the chip belongs to the seed it came with.
        onEditingChange = {
            editing = it
            lookupError = null
            estimated = false
        },
        lookupError = lookupError?.let { stringResource(it) },
        // The draft is what is on screen, not the seed the sheet opened with: the reading is
        // layered over it, so a reply with no name leaves the typed one standing instead of
        // blanking it. Same values, so re-seeding the sheet with it changes nothing visible.
        onLookUp = { draft ->
            editing = draft
            lookupError = null
            viewModel.handleEvent(SupplementsEvent.OnLookUp(draft.name))
        },
        estimated = estimated,
        onEvent = viewModel::handleEvent,
        onOpenScan = onOpenScan,
    )
}

@Composable
private fun SupplementsContent(
    uiState: SupplementsUiState,
    editing: Supplement?,
    onEditingChange: (Supplement?) -> Unit,
    lookupError: String?,
    onLookUp: (Supplement) -> Unit,
    estimated: Boolean,
    onEvent: (SupplementsEvent) -> Unit,
    onOpenScan: () -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<Supplement?>(null) }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.loaded && uiState.supplements.isEmpty()) {
                // Centred rather than top-hugged, which the shipped inline version had to be: the
                // FAB already guarantees Add is on screen, so the state can read as an invitation
                // instead of a truncated screen. Idle, not Sleepy — nothing has failed here, there
                // is simply nothing yet.
                FullScreenState(
                    icon = { MascotAvatar(state = MascotState.Idle, size = 88.dp) },
                    heading = stringResource(R.string.profile_supplements_empty_heading),
                    body = stringResource(R.string.profile_supplements_empty_body),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    // Rests clear of both FABs: the stack's 72dp, plus the small FAB's 48dp touch
                    // target and the 16dp between them.
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = DockedFabContentPadding + 64.dp,
                    ),
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
                            onClick = { onEditingChange(supplement) },
                        )
                    }
                }
            }
            // Two doors to the same table, and the camera is the small one on purpose: typing
            // three fields is not a job worth a photo, and the scan earns its place on the bottle
            // the user cannot be bothered to transcribe. Both land in the same sheet — where the
            // name field's sparkle is the third way in, on the bottle that isn't in the room.
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                SmallFloatingActionButton(
                    onClick = onOpenScan,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        imageVector = AppIcons.Camera,
                        contentDescription = stringResource(R.string.profile_supplements_scan),
                    )
                }
                DockedFab(
                    // A blank row with id 0 — the sheet reads that as the add, so there is one
                    // sheet and one save path rather than two of each.
                    onClick = { onEditingChange(Supplement(name = "")) },
                    label = stringResource(R.string.profile_supplements_add),
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
            onDismiss = { onEditingChange(null) },
            onSave = { saved ->
                onEvent(SupplementsEvent.OnSave(saved))
                onEditingChange(null)
            },
            // The sheet closes and the dialog takes over: one scrim at a time, and the question
            // is asked in the one place that already asks it.
            onDelete = {
                pendingDelete = supplement
                onEditingChange(null)
            },
            onLookUp = onLookUp,
            lookingUp = uiState.lookingUp,
            lookupError = lookupError,
            estimated = estimated,
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
            editing = null,
            onEditingChange = {},
            lookupError = null,
            onLookUp = {},
            estimated = false,
            onEvent = {},
            onOpenScan = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun SupplementsScreenEmptyPreview() {
    AppTheme {
        SupplementsContent(
            uiState = SupplementsUiState(loaded = true),
            editing = null,
            onEditingChange = {},
            lookupError = null,
            onLookUp = {},
            estimated = false,
            onEvent = {},
            onOpenScan = {},
        )
    }
}
