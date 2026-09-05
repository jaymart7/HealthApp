package ph.mart.healthapp.core.data.health

import ph.mart.healthapp.core.data.bloodpressure.BloodPressureReading
import ph.mart.healthapp.core.data.cycle.CycleDay
import ph.mart.healthapp.core.data.cycle.FlowLevel
import ph.mart.healthapp.core.data.health.local.HealthLinkEntity

/**
 * Health Connect — the *local* half of FitPulse's health integration, beside the Google Health API
 * in [GoogleHealthApi]. Same six types, read on-device instead of over the network.
 *
 * This file is the vocabulary and every pure part, split from the client exactly the way
 * [GoogleHealthApi] splits its parsers from its sockets: nothing here imports
 * `androidx.health.connect`, so a JVM test can drive all of it. The one file that does touch the
 * SDK is [HealthConnectSourceImpl], and it hands back the same `Remote*` types the cloud leg's
 * parsers produce — which is what lets both providers share one set of writers, one dedup table
 * and one cursor rule rather than growing a second copy of each.
 *
 * Read-only. Meals and water still go out over the Google Health API, so no `WRITE_*` permission
 * is requested and there is no push path here.
 */

/** The `health_link.remoteName` prefix for a Health Connect record. */
private const val CONNECT_NAME_PREFIX = "hc:"

/**
 * The one vocabulary both providers are indexed by.
 *
 * [permission] is the Android permission string Health Connect gates the type behind, spelled out
 * rather than derived from a record class so this file stays free of the SDK — the manifest lists
 * the same six in this order. [connectDataType] is what a Health Connect import writes into
 * `health_link.dataType`: distinct from [HealthDataType]'s ids on purpose, so the two providers'
 * cursors (`MAX(remoteTimeMillis)` per type) are independent and revoking one cannot advance the
 * other past data it never wrote.
 *
 * [Steps] and [Heart] carry a [connectDataType] for completeness but never record a link: both
 * aggregate to one row per day, which is bookkeeping enough — see `importSteps`.
 */
enum class HealthMetric(val permission: String, val connectDataType: String) {
    Exercise("android.permission.health.READ_EXERCISE", "hc/exercise"),
    Weight("android.permission.health.READ_WEIGHT", "hc/weight"),
    Sleep("android.permission.health.READ_SLEEP", "hc/sleep"),
    Steps("android.permission.health.READ_STEPS", "hc/steps"),
    Heart("android.permission.health.READ_HEART_RATE", "hc/heart-rate"),
    BloodPressure("android.permission.health.READ_BLOOD_PRESSURE", "hc/blood-pressure"),

    /**
     * The one metric that is not requested unconditionally — see
     * [HealthSyncRepository.connectPermissions]. Cycle tracking is off by default and may be
     * permanently irrelevant to whoever is holding the phone, and a permission sheet nobody has a
     * use for is worse than one type fewer.
     */
    Menstruation("android.permission.health.READ_MENSTRUATION", "hc/menstruation"),
}

/** Every permission FitPulse would ask Health Connect for, in [HealthMetric] order. */
val CONNECT_PERMISSIONS: Set<String> = HealthMetric.entries.mapTo(LinkedHashSet()) { it.permission }

/**
 * What Health Connect says right now — deliberately parallel to [HealthConnection], the Google
 * Health API's own four states, and **never a stored flag** for that type's exact reason: a cached
 * "connected" boolean becomes a lie the moment permissions are revoked from the Health Connect app,
 * and the screen showing it is the screen that must not lie.
 */
sealed interface HealthConnectState {
    /** Asked but not answered yet — the first read is in flight. */
    data object Checking : HealthConnectState

    /** Below Android 9, or no Health Connect on the device. The panel renders nothing at all. */
    data object Unsupported : HealthConnectState

    /** Installed but too old to talk to. The panel offers the Play Store. */
    data object UpdateRequired : HealthConnectState

    /**
     * Available, with [granted] empty or short of [CONNECT_PERMISSIONS].
     *
     * A partial grant is ordinary here — Health Connect lets a user allow steps and deny heart —
     * which is why this carries the set rather than a boolean, and why per-type precedence
     * ([cloudMetrics]) falls out of it for free.
     */
    data class Available(val granted: Set<HealthMetric>) : HealthConnectState
}

/** The metrics Health Connect is currently answering for. Empty for every other state. */
val HealthConnectState.granted: Set<HealthMetric>
    get() = (this as? HealthConnectState.Available)?.granted.orEmpty()

/**
 * **One writer per table per sync**, decided once so the two legs can never both write.
 *
 * Health Connect wins wherever it is granted; the Google Health API covers the rest — a device
 * below Android 9, a user who declined, a type they didn't tick. There is deliberately *no* fuzzy
 * time-window matcher anywhere in this file: the same watch commonly feeds both Health Connect and
 * the Google cloud with no shared identifier, so a tolerance-based matcher would be wrong in both
 * directions with nothing to appeal to. Precedence needs no tolerance to be right.
 */
fun cloudMetrics(connectGranted: Set<HealthMetric>): Set<HealthMetric> =
    HealthMetric.entries.toSet() - connectGranted

/**
 * The cloud-imported rows a Health Connect handover supersedes.
 *
 * The one genuine duplication case this integration has: someone who has been syncing the Google
 * Health API for a month and then grants Health Connect would otherwise hold every workout in that
 * month twice, because the two providers key their links differently — a
 * `users/me/dataTypes/exercise/dataPoints/…` resource name against a `hc:`-prefixed Health Connect
 * id. Nothing correlates them, so the duplicate is retired by *provenance and window* instead:
 * this type, imported rather than pushed, and inside the window Health Connect is about to
 * re-import.
 *
 * Anything older than [sinceMillis] is outside Health Connect's own reach and therefore cannot be
 * duplicated, so it stays — which is what keeps a handover from throwing away history the new
 * provider will never backfill.
 */
internal fun supersededByConnect(
    links: List<HealthLinkEntity>,
    cloudDataType: String,
    sinceMillis: Long,
): List<HealthLinkEntity> = links.filter {
    !it.pushed && it.dataType == cloudDataType && it.remoteTimeMillis >= sinceMillis
}

/** Health Connect's own record id, namespaced so it can never collide with a cloud resource name. */
internal fun connectName(recordId: String): String = CONNECT_NAME_PREFIX + recordId

/** Everything one Health Connect read produced, already in the cloud leg's own vocabulary. */
internal data class ConnectRecords(
    val exercise: List<RemoteExercise> = emptyList(),
    val weight: List<RemoteWeight> = emptyList(),
    val sleep: List<RemoteSleep> = emptyList(),
    val steps: List<RemoteSteps> = emptyList(),
    val heart: List<RemoteHeart> = emptyList(),
    val bloodPressure: List<RemoteBloodPressure> = emptyList(),
    val menstruation: List<RemoteMenstruation> = emptyList(),
)

/**
 * One cuff reading. The sixth type, and the only one with no cloud twin — blood pressure was
 * manual-only because the Google Health scope was ruled out at verification, and Health Connect's
 * `READ_BLOOD_PRESSURE` carries no such cost.
 *
 * There is no pulse: Health Connect records a pulse as a `HeartRateRecord`, not a field of the
 * reading, so [BloodPressureReading.pulseBpm] gets its `0` — which already means "not entered"
 * there, not a pulse of zero.
 */
internal data class RemoteBloodPressure(
    override val remoteName: String,
    override val timeMillis: Long,
    val systolic: Int,
    val diastolic: Int,
) : RemotePoint

internal fun RemoteBloodPressure.toBloodPressureReading() = BloodPressureReading(
    takenAtMillis = timeMillis,
    systolic = systolic,
    diastolic = diastolic,
)

/**
 * One period, as Health Connect records it: a span of days with **no intensity on it**. The seventh
 * type, and the second with no cloud twin — the Google Health API has no menstruation scope, and
 * nothing here would justify one on the verification form.
 *
 * It carries the span rather than the days because that is what `MenstruationPeriodRecord` is, and
 * because the record's own id is what [health_link] keys on — one record, one link, the cursor
 * every other linked type already uses.
 */
internal data class RemoteMenstruation(
    override val remoteName: String,
    override val timeMillis: Long,
    val startEpochDay: Long,
    val endEpochDay: Long,
) : RemotePoint

/**
 * Expanded to one row per day, every one [FlowLevel.Unstated]: the record reports *that* the period
 * ran, never how heavy it was, and writing "Medium" for one would invent a figure the source never
 * reported — the refusal an imported reading's `pulseBpm = 0` already makes. The card and the sheet
 * both draw that level as its own answer and invite a tap to replace it.
 */
internal fun RemoteMenstruation.toCycleDays(): List<CycleDay> =
    (startEpochDay..endEpochDay).map { CycleDay(dateEpochDay = it, flow = FlowLevel.Unstated.value) }

/**
 * True when [reading] is one the user already has — a cuff reading typed by hand *and* written to
 * Health Connect arrives twice otherwise, since `blood_pressure_reading` autogenerates its ids and
 * so cannot collide the way `health_link` does.
 *
 * The tolerance is a minute, not a window worth tuning: a typed reading and its Health Connect
 * twin are the same measurement, transcribed within a minute of each other and carrying identical
 * numbers. This is not the fuzzy matcher [cloudMetrics] exists to avoid — both figures must agree
 * exactly, so the only thing being tolerated is which second the two writers stamped.
 */
internal fun RemoteBloodPressure.alreadyHeld(existing: List<BloodPressureReading>): Boolean =
    existing.any {
        it.systolic == systolic &&
            it.diastolic == diastolic &&
            kotlin.math.abs(it.takenAtMillis - timeMillis) <= MINUTE_MILLIS
    }

private const val MINUTE_MILLIS = 60_000L

/**
 * Minutes actually asleep in a Health Connect sleep session.
 *
 * [awakeMillis] is the summed length of the session's AWAKE stages, which Health Connect records
 * inside the session rather than trimming from it — so a night with a restless hour must not report
 * that hour as sleep. A session with no stages reports its whole length, which is what a watch that
 * only writes a start and an end is claiming.
 */
internal fun asleepMinutes(startMillis: Long, endMillis: Long, awakeMillis: Long): Int =
    (((endMillis - startMillis) - awakeMillis) / MINUTE_MILLIS).toInt().coerceAtLeast(0)
