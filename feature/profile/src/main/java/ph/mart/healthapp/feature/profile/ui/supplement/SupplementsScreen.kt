package ph.mart.healthapp.feature.profile.ui.supplement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ph.mart.healthapp.core.data.supplement.Supplement
import ph.mart.healthapp.core.designsystem.component.DiscardConfirmDialog
import ph.mart.healthapp.core.designsystem.component.DockedFabContentPadding
import ph.mart.healthapp.core.designsystem.component.MascotAvatar
import ph.mart.healthapp.core.designsystem.component.MascotState
import ph.mart.healthapp.core.designsystem.component.PrimaryButton
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.profile.ui.supplement.components.SupplementEditSheet
import ph.mart.healthapp.feature.profile.ui.supplement.components.SupplementListRow

/**
 * The supplement list, one Nav3 level above Profile — the only place it can be authored. Home's
 * card renders it and ticks it; nothing there can add, rename or remove one, which is why this
 * screen exists at all.
 *
 * Same shape as `FoodLibraryScreen`: a list with edit and delete, no logging, and NavDisplay's own
 * back returning to Profile.
 */
@Composable
fun SupplementsScreen(viewModel: SupplementsViewModel = koinViewModel()) {
    val uiState by viewModel.collectAsState()
    SupplementsContent(uiState = uiState, onEvent = viewModel::handleEvent)
}

@Composable
private fun SupplementsContent(
    uiState: SupplementsUiState,
    onEvent: (SupplementsEvent) -> Unit,
) {
    // Local rather than in a saveable holder, for the same reason `FoodLibraryScreen` keeps its
    // own: a sheet that survived process death would reopen on a row the user has stopped looking
    // at.
    var editing by remember { mutableStateOf<Supplement?>(null) }
    var pendingDelete by remember { mutableStateOf<Supplement?>(null) }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = DockedFabContentPadding),
        ) {
            Text(
                text = "Supplements",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            // The empty state is inline rather than a FullScreenState: this screen always carries
            // an Add button, and a full-screen mascot would push the one control off it.
            if (uiState.loaded && uiState.supplements.isEmpty()) {
                MascotAvatar(state = MascotState.Sleepy, size = 64.dp)
                Text(
                    text = "Nothing here yet. Add what you take and it shows up on Home each day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            uiState.supplements.forEach { supplement ->
                SupplementListRow(
                    name = supplement.name,
                    summary = supplement.summary(),
                    onEdit = { editing = supplement },
                    onDelete = { pendingDelete = supplement },
                )
            }
            PrimaryButton(
                label = "Add supplement",
                // A blank row with id 0 — the sheet reads that as the add, so there is one sheet
                // and one save path rather than two of each.
                onClick = { editing = Supplement(name = "") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }

    // A supplement is something the user authored, and its delete sits beside the one that edits
    // it, so it asks first — the same call the library makes, rather than the diary's swipe-and-undo.
    pendingDelete?.let { supplement ->
        DiscardConfirmDialog(
            title = "Delete ${supplement.name}?",
            body = "It leaves your daily list. Days you already ticked keep their record.",
            confirmLabel = "Delete",
            dismissLabel = "Keep",
            onConfirm = {
                onEvent(SupplementsEvent.OnDelete(supplement.id))
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
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
                    Supplement(id = 1, name = "Vitamin D", dose = "2000 IU"),
                    Supplement(id = 2, name = "Creatine", dose = "5 g", timesPerDay = 2),
                    Supplement(id = 3, name = "Magnesium"),
                ),
                loaded = true,
            ),
            onEvent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun SupplementsScreenEmptyPreview() {
    AppTheme {
        SupplementsContent(uiState = SupplementsUiState(loaded = true), onEvent = {})
    }
}
