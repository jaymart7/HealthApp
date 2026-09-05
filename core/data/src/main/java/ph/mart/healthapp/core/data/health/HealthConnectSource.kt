package ph.mart.healthapp.core.data.health

import android.content.Context
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import kotlin.math.roundToInt
import kotlin.reflect.KClass
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.epochDayOf
import ph.mart.healthapp.core.data.exercise.estimateBurnedKcal

/**
 * The only file in FitPulse that touches `androidx.health.connect`, and the local twin of
 * [GoogleHealthAuth] and [GoogleHealthApi] rolled into one — Health Connect needs no OAuth and no
 * transport, so the two halves the cloud leg keeps apart collapse into a single reader here.
 *
 * Everything above it sees the same `Remote*` types the cloud's parsers produce, plus a framework
 * [ActivityResultContract]. That is what keeps `:feature:profile` free of the dependency — the rule
 * [GoogleHealthAuth] follows by handing back a `PendingIntent` rather than a Play services type.
 *
 * **One version gate, in [client].** Health Connect is Android 9+, and `health-connect-client`
 * declares minSdk 26 against this app's 24 (hence the `tools:overrideLibrary` in this module's
 * manifest). The SDK's classes ship inside the APK, so naming one is safe at any API level; what is
 * not safe is *calling* into it, because it reaches platform APIs this app's floor predates. So
 * every entry point resolves the client through [client] and returns the do-nothing answer when it
 * is null, and no read is reachable without one.
 */
internal interface HealthConnectSource {
    /** Silent — never raises UI, it only reports what Health Connect says right now. */
    suspend fun state(): HealthConnectState

    /** The contract the screen hands `rememberLauncherForActivityResult`. */
    fun permissionContract(): ActivityResultContract<Set<String>, Set<String>>

    /**
     * One read per granted metric, each from its own cursor: [windows] maps a metric to the instant
     * its window opens, so its keys *are* the set being read. [weightKg] prices a session's burn —
     * see [readExercise].
     *
     * A type whose read throws comes back empty rather than failing the whole sync, the same
     * independence the cloud leg's per-type `Outcome` gives it: a provider that misbehaves on one
     * record type must not cost the other five.
     */
    suspend fun read(windows: Map<HealthMetric, Long>, weightKg: Double): ConnectRecords
}

internal class HealthConnectSourceImpl(private val context: Context) : HealthConnectSource {

    /**
     * Null below Android 9 or where Health Connect isn't installed, and re-resolved on every call
     * rather than cached: the provider can be installed, updated, or have its permissions revoked
     * while FitPulse is running, and a cached client is the stored flag [HealthConnectState] exists
     * to avoid.
     */
    private fun client(): HealthConnectClient? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return null
        return runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
    }

    override suspend fun state(): HealthConnectState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return HealthConnectState.Unsupported
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_UNAVAILABLE -> return HealthConnectState.Unsupported
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> return HealthConnectState.UpdateRequired
        }
        val client = runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
            ?: return HealthConnectState.Unsupported
        // A failed call here is neither an absence nor a revocation — it is one round trip that
        // didn't answer. Reporting no grants is the honest reading: the panel offers Allow, and
        // Health Connect's own sheet is what settles it.
        val granted = runCatching { client.permissionController.getGrantedPermissions() }
            .getOrDefault(emptySet())
        return HealthConnectState.Available(
            granted = HealthMetric.entries.filterTo(LinkedHashSet()) { it.permission in granted },
        )
    }

    override fun permissionContract(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()

    override suspend fun read(windows: Map<HealthMetric, Long>, weightKg: Double): ConnectRecords {
        // Repeated rather than left to [client]: the helpers below are @RequiresApi(P) because
        // Health Connect's API is java.time-based and this module has no desugaring, and lint's
        // flow analysis can follow an explicit check where it cannot follow a nullable factory.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return ConnectRecords()
        if (windows.isEmpty()) return ConnectRecords()
        val client = client() ?: return ConnectRecords()
        return ConnectRecords(
            exercise = windows.on(HealthMetric.Exercise) { readExercise(client, it, weightKg) },
            weight = windows.on(HealthMetric.Weight) { readWeight(client, it) },
            sleep = windows.on(HealthMetric.Sleep) { readSleep(client, it) },
            steps = windows.on(HealthMetric.Steps) { readSteps(client, it) },
            heart = windows.on(HealthMetric.Heart) { readHeart(client, it) },
            bloodPressure = windows.on(HealthMetric.BloodPressure) { readBloodPressure(client, it) },
            menstruation = windows.on(HealthMetric.Menstruation) { readMenstruation(client, it) },
        )
    }

    /**
     * Health Connect keeps a session's calories in a *separate* `ActiveCaloriesBurnedRecord`, so
     * the burn is the MET estimate a hand-logged workout already gets ([estimateBurnedKcal]) and
     * the step count is [estimatedSteps]'s — the same two figures, from the same two functions,
     * that the diary's own exercise sheet seeds. Both stay editable on the row afterwards, which is
     * what makes an estimate the right default rather than a placeholder.
     *
     * ponytail: one `aggregate(ACTIVE_CALORIES_TOTAL)` per session would read the watch's own
     * figure, at the cost of a call per workout. Worth it only if the estimates read wrong.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private suspend fun readExercise(
        client: HealthConnectClient,
        sinceMillis: Long,
        weightKg: Double,
    ): List<RemoteExercise> = page(client, ExerciseSessionRecord::class, sinceMillis).map { record ->
        val type = connectExerciseType(record.exerciseType, record.title)
        val minutes = minutesBetween(record.startTime, record.endTime)
        RemoteExercise(
            remoteName = connectName(record.metadata.id),
            timeMillis = record.startTime.toEpochMilli(),
            type = type,
            // [ExerciseType.name] for the reason `GoogleHealthApi`'s twin gives: a persisted row.
            name = record.title.orEmpty().ifBlank { type.name },
            minutes = minutes,
            burnedKcal = estimateBurnedKcal(type, minutes, weightKg),
            steps = estimatedSteps(type, minutes),
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private suspend fun readWeight(client: HealthConnectClient, sinceMillis: Long): List<RemoteWeight> =
        page(client, WeightRecord::class, sinceMillis).map { record ->
            RemoteWeight(
                remoteName = connectName(record.metadata.id),
                timeMillis = record.time.toEpochMilli(),
                weightKg = record.weight.inKilograms,
            )
        }

    /** Dated downstream by the day it *ended* — see `SleepDayEntity`. */
    @RequiresApi(Build.VERSION_CODES.P)
    private suspend fun readSleep(client: HealthConnectClient, sinceMillis: Long): List<RemoteSleep> =
        page(client, SleepSessionRecord::class, sinceMillis).map { record ->
            val start = record.startTime.toEpochMilli()
            val end = record.endTime.toEpochMilli()
            val awake = record.stages
                .filter { it.stage == SleepSessionRecord.STAGE_TYPE_AWAKE }
                .sumOf { it.endTime.toEpochMilli() - it.startTime.toEpochMilli() }
            RemoteSleep(
                remoteName = connectName(record.metadata.id),
                timeMillis = start,
                endMillis = end,
                minutesAsleep = asleepMinutes(start, end, awake),
            )
        }

    /** Raw records, folded to daily totals by `writeSteps` — the cloud's buckets, locally. */
    @RequiresApi(Build.VERSION_CODES.P)
    private suspend fun readSteps(client: HealthConnectClient, sinceMillis: Long): List<RemoteSteps> =
        page(client, StepsRecord::class, sinceMillis).map { record ->
            RemoteSteps(
                remoteName = connectName(record.metadata.id),
                timeMillis = record.startTime.toEpochMilli(),
                count = record.count.toInt(),
            )
        }

    /**
     * One [RemoteHeart] per *sample*, so `aggregateHeartByDay` folds these exactly as it folds the
     * cloud's. A record holds many samples under one id, so the sample's own time namespaces it.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private suspend fun readHeart(client: HealthConnectClient, sinceMillis: Long): List<RemoteHeart> =
        page(client, HeartRateRecord::class, sinceMillis).flatMap { record ->
            record.samples.mapNotNull { sample ->
                val bpm = sample.beatsPerMinute.toInt()
                if (bpm <= 0) return@mapNotNull null
                RemoteHeart(
                    remoteName = connectName("${record.metadata.id}:${sample.time.toEpochMilli()}"),
                    timeMillis = sample.time.toEpochMilli(),
                    bpm = bpm,
                )
            }
        }

    @RequiresApi(Build.VERSION_CODES.P)
    private suspend fun readBloodPressure(
        client: HealthConnectClient,
        sinceMillis: Long,
    ): List<RemoteBloodPressure> = page(client, BloodPressureRecord::class, sinceMillis).map { record ->
        RemoteBloodPressure(
            remoteName = connectName(record.metadata.id),
            timeMillis = record.time.toEpochMilli(),
            systolic = record.systolic.inMillimetersOfMercury.roundToInt(),
            diastolic = record.diastolic.inMillimetersOfMercury.roundToInt(),
        )
    }

    /**
     * A period is a span, so both ends go through [epochDayOf] here rather than being carried as
     * instants: `cycle_day` is keyed by local midnight, and a record that ended at 01:00 belongs to
     * the day the phone calls 01:00 — the conversion every other day-keyed table in this app makes.
     * [RemotePoint.timeMillis] stays the start instant, because that is what `health_link`'s cursor
     * compares.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private suspend fun readMenstruation(
        client: HealthConnectClient,
        sinceMillis: Long,
    ): List<RemoteMenstruation> =
        page(client, MenstruationPeriodRecord::class, sinceMillis).map { record ->
            RemoteMenstruation(
                remoteName = connectName(record.metadata.id),
                timeMillis = record.startTime.toEpochMilli(),
                startEpochDay = epochDayOf(record.startTime.toEpochMilli()),
                endEpochDay = epochDayOf(record.endTime.toEpochMilli()),
            )
        }

    /**
     * Pages to the end, capped for `MAX_PAGES`' reason in `HealthSyncRepositoryImpl`: a provider
     * that never stops handing back a page token must not spin here, and the cursor picks up
     * wherever a capped read stopped.
     *
     * ponytail: a windowed read rather than `getChanges(token)`. A changes token is state that has
     * to survive process death — a column, a migration and a reconcile path — where a window needs
     * nothing at all, because the cursor is already derived from rows actually written. Move to
     * tokens if a sync ever costs enough to notice.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private suspend fun <T : Record> page(
        client: HealthConnectClient,
        type: KClass<T>,
        sinceMillis: Long,
    ): List<T> {
        val filter = TimeRangeFilter.after(Instant.ofEpochMilli(sinceMillis))
        val all = mutableListOf<T>()
        var token: String? = null
        repeat(MAX_CONNECT_PAGES) {
            val response = client.readRecords(
                ReadRecordsRequest(recordType = type, timeRangeFilter = filter, pageToken = token),
            )
            all += response.records
            token = response.pageToken ?: return all
        }
        return all
    }
}

/**
 * Runs [read] only for a metric the caller asked for, and swallows its failure into an empty list —
 * the per-type independence [HealthConnectSource.read] promises, in one place rather than six.
 */
private suspend fun <T> Map<HealthMetric, Long>.on(
    metric: HealthMetric,
    read: suspend (Long) -> List<T>,
): List<T> {
    val since = this[metric] ?: return emptyList()
    return runCatching { read(since) }.getOrDefault(emptyList())
}

/**
 * Health Connect's `exerciseType` is an `Int` and the SDK publishes no reverse map, so the types
 * carrying a distinct MET in [ExerciseType] are named here — against the SDK's own constants, which
 * is why this lives in the one file that can see them rather than being retyped as bare integers
 * somewhere a JVM test could reach and a renumbering could rot.
 *
 * Everything else falls through to the session's title and [exerciseTypeOf]'s keyword matching, the
 * same function the cloud leg's 175-odd activity strings go through. An unrecognised session still
 * lands on [ExerciseType.Other] carrying its own name and duration; only the icon and the MET move.
 */
private fun connectExerciseType(exerciseType: Int, title: String?): ExerciseType = when (exerciseType) {
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
    -> ExerciseType.Run

    ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
    ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
    -> ExerciseType.Walk

    ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
    -> ExerciseType.Cycle

    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
    -> ExerciseType.Swim

    ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
    ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
    -> ExerciseType.Strength

    ExerciseSessionRecord.EXERCISE_TYPE_YOGA,
    ExerciseSessionRecord.EXERCISE_TYPE_PILATES,
    -> ExerciseType.Yoga

    ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> ExerciseType.Hiit

    else -> exerciseTypeOf(title)
}

@RequiresApi(Build.VERSION_CODES.P)
private fun minutesBetween(start: Instant, end: Instant): Int =
    ((end.toEpochMilli() - start.toEpochMilli()) / 60_000L).toInt().coerceAtLeast(0)

private const val MAX_CONNECT_PAGES = 20
