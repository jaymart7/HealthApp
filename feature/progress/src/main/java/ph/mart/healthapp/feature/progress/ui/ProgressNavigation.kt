package ph.mart.healthapp.feature.progress.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import ph.mart.healthapp.core.navigation.route.ProgressRoute

/** Stub targets for the FAB's "Log weight" / "Add photo" actions — real sheets arrive in Phase 6. */
@Serializable
data object LogWeightStubRoute : NavKey

@Serializable
data object AddPhotoStubRoute : NavKey

fun EntryProviderScope<NavKey>.progressEntries(onCloseStub: () -> Unit) {
    entry<ProgressRoute> { ProgressPlaceholderScreen() }
    entry<LogWeightStubRoute> { LogWeightStubScreen(onClose = onCloseStub) }
    entry<AddPhotoStubRoute> { AddPhotoStubScreen(onClose = onCloseStub) }
}
