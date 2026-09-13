package ph.mart.healthapp.feature.progress.ui.measurement

import ph.mart.healthapp.core.data.profile.UnitSystem
import ph.mart.healthapp.core.data.progress.MeasurementEntry
import ph.mart.healthapp.core.data.progress.MeasurementPart

/**
 * Each tracked part's own sparse history, plus the three things the page's two derived cards need
 * and cannot get from a tape measure: the newest weigh-in, the profile's height and the unit.
 *
 * [heightCm] and [latestWeightKg] are nullable on purpose — a waist-to-height ratio or a fat-mass
 * split with a side missing is not a reading that hasn't been taken, it is a figure that does not
 * exist, and the cards draw nothing rather than a dash.
 */
data class MeasurementsUiState(
    val measurements: Map<MeasurementPart, List<MeasurementEntry>> = emptyMap(),
    val latestWeightKg: Double? = null,
    val heightCm: Double? = null,
    val unit: UnitSystem = UnitSystem.Metric,
)
