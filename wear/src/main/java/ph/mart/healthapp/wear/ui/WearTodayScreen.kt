package ph.mart.healthapp.wear.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import androidx.wear.compose.material3.FailureConfirmationDialog
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.confirmationDialogCurvedText
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ph.mart.healthapp.core.today.TodaySnapshot
import ph.mart.healthapp.wear.R
import ph.mart.healthapp.wear.ui.components.CaloriesRing
import ph.mart.healthapp.wear.ui.components.FastButton
import ph.mart.healthapp.wear.ui.components.NoDataMessage
import ph.mart.healthapp.wear.ui.components.StaleNote
import ph.mart.healthapp.wear.ui.components.StreakAndSteps
import ph.mart.healthapp.wear.ui.components.WaterButton
import ph.mart.healthapp.wear.ui.theme.FitPulseWearTheme

@Composable
fun WearTodayScreen(viewModel: WearTodayViewModel = koinViewModel()) {
    val uiState by viewModel.collectAsState()
    val state = rememberWearTodayState()
    viewModel.collectSideEffect { effect ->
        when (effect) {
            WearTodaySideEffect.PhoneUnreachable -> state.failureShown = true
        }
    }
    WearTodayContent(uiState, state, onEvent = viewModel::handleEvent)
}

/**
 * One screen and no navigation: the watch app is today, and everything deeper — the diary, the
 * charts, the coach — stays on the phone. Saying that by omission beats a wrist-sized diary.
 */
@Composable
internal fun WearTodayContent(
    uiState: WearTodayUiState,
    state: WearTodayScreenState,
    onEvent: (WearTodayEvent) -> Unit,
) {
    AppScaffold {
        val listState = rememberTransformingLazyColumnState()
        val transformationSpec = rememberTransformationSpec()
        val snapshot = uiState.snapshot

        ScreenScaffold(scrollState = listState) { contentPadding ->
            when {
                // Before the first read comes back, nothing — a message that flashed for 200ms
                // saying the phone was missing would be wrong more often than it was right.
                !uiState.loaded -> Unit
                snapshot == null ->
                    NoDataMessage(stringResource(R.string.wear_today_no_snapshot))
                snapshot.onboarding ->
                    NoDataMessage(stringResource(R.string.wear_today_no_profile))
                else -> TodayList(
                    snapshot = snapshot,
                    uiState = uiState,
                    listState = listState,
                    transformationSpec = transformationSpec,
                    contentPadding = contentPadding,
                    onEvent = onEvent,
                )
            }
        }

        // The failure path is a dialog rather than an inline line because the tap it answers is
        // the last thing the user did before dropping their wrist — an inline note would go
        // unread. It dismisses itself. The style is read out here because the curved slot below
        // is not a composable scope.
        val curvedStyle = ConfirmationDialogDefaults.curvedTextStyle
        val phoneUnreachable = stringResource(R.string.wear_today_phone_unreachable)
        FailureConfirmationDialog(
            visible = state.failureShown,
            onDismissRequest = { state.failureShown = false },
            curvedText = {
                confirmationDialogCurvedText(phoneUnreachable, curvedStyle)
            },
        )
    }
}

@Composable
private fun TodayList(
    snapshot: TodaySnapshot,
    uiState: WearTodayUiState,
    listState: TransformingLazyColumnState,
    transformationSpec: TransformationSpec,
    contentPadding: PaddingValues,
    onEvent: (WearTodayEvent) -> Unit,
) {
    val enabled = !uiState.sending
    TransformingLazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize(),
    ) {
        // First, not last: the note qualifies every figure below it, and a caveat read after
        // the numbers is a caveat that arrives too late.
        if (uiState.isStale(todayEpochDay())) {
            item { StaleNote(Modifier.fillMaxWidth()) }
        }
        item { CaloriesRing(snapshot) }
        item { StreakAndSteps(snapshot, Modifier.fillMaxWidth()) }
        item {
            WaterButton(
                snapshot = snapshot,
                enabled = enabled,
                onAddGlass = { onEvent(WearTodayEvent.OnAddGlass) },
                transformation = SurfaceTransformation(transformationSpec),
                modifier = Modifier
                    .transformedHeight(this, transformationSpec)
                    .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                    .fillMaxWidth(),
            )
        }
        item {
            FastButton(
                snapshot = snapshot,
                enabled = enabled,
                onToggleFast = { onEvent(WearTodayEvent.OnToggleFast) },
                transformation = SurfaceTransformation(transformationSpec),
                modifier = Modifier
                    .transformedHeight(this, transformationSpec)
                    .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                    .fillMaxWidth(),
            )
        }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun WearTodayPreview() {
    FitPulseWearTheme {
        WearTodayContent(
            uiState = WearTodayUiState(loaded = true, snapshot = PREVIEW_SNAPSHOT),
            state = WearTodayScreenState(),
            onEvent = {},
        )
    }
}

@WearPreviewDevices
@Composable
private fun WearTodayNoPhonePreview() {
    FitPulseWearTheme {
        WearTodayContent(
            uiState = WearTodayUiState(loaded = true, snapshot = null),
            state = WearTodayScreenState(),
            onEvent = {},
        )
    }
}

private val PREVIEW_SNAPSHOT = TodaySnapshot(
    dateEpochDay = 20_000,
    consumedKcal = 1450,
    budgetKcal = 2000,
    glasses = 5,
    goalGlasses = 8,
    waterLabel = "1.3 L",
    streakDays = 12,
    steps = 8432,
)
