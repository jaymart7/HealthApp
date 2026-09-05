package ph.mart.healthapp.core.data.health

import android.content.Intent
import kotlinx.coroutines.flow.first
import ph.mart.healthapp.core.data.bloodpressure.BloodPressureRepository
import ph.mart.healthapp.core.data.epochDayOf
import ph.mart.healthapp.core.data.epochDayStartMillis
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.data.water.GLASS_ML
import ph.mart.healthapp.core.data.water.WaterRepository
import ph.mart.healthapp.core.data.cycle.CycleRepository
import ph.mart.healthapp.core.data.exercise.ExerciseRepository
import ph.mart.healthapp.core.data.health.local.HealthLinkDao
import ph.mart.healthapp.core.data.health.local.HealthLinkEntity
import ph.mart.healthapp.core.data.health.local.HeartDayDao
import ph.mart.healthapp.core.data.health.local.HeartDayEntity
import ph.mart.healthapp.core.data.health.local.SleepDayDao
import ph.mart.healthapp.core.data.health.local.SleepDayEntity
import ph.mart.healthapp.core.data.health.local.StepDayDao
import ph.mart.healthapp.core.data.health.local.StepDayEntity
import ph.mart.healthapp.core.data.network.NetworkMonitor
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.progress.ProgressRepository
import ph.mart.healthapp.core.data.progress.WeightEntry

private const val EXERCISE_TABLE = "exercise_entry"
private const val WEIGHT_TABLE = "weight_entry"
private const val SLEEP_TABLE = "sleep_day"
private const val FOOD_TABLE = "food_entry"
private const val WATER_TABLE = "water_day"
private const val BLOOD_PRESSURE_TABLE = "blood_pressure_reading"
private const val CYCLE_TABLE = "cycle_day"

/**
 * ponytail: a page cap instead of a real budget. 25 sessions per page × 20 pages is over a year of
 * daily training in one sync, and the cursor picks up where it stopped. Raise it only if someone
 * with a genuinely enormous backlog complains.
 */
private const val MAX_PAGES = 20

/** Skip signal from a per-type writer: the data point is fine, we just don't want to store it. */
private const val SKIPPED = -1L

internal class HealthSyncRepositoryImpl(
    private val auth: GoogleHealthAuth,
    private val links: HealthLinkDao,
    private val exerciseRepository: ExerciseRepository,
    private val progressRepository: ProgressRepository,
    private val sleepDao: SleepDayDao,
    private val stepDao: StepDayDao,
    private val heartDao: HeartDayDao,
    private val profileRepository: ProfileRepository,
    private val foodRepository: FoodRepository,
    private val waterRepository: WaterRepository,
    private val bloodPressureRepository: BloodPressureRepository,
    private val cycleRepository: CycleRepository,
    private val networkMonitor: NetworkMonitor,
    private val connect: HealthConnectSource,
) : HealthSyncRepository {

    /**
     * The access token lives here and nowhere else — never Room, never SharedPreferences. It is
     * good for about an hour; when it expires [auth] mints another silently, so losing it on
     * process death costs one round trip and nothing else.
     */
    @Volatile
    private var cachedToken: String? = null

    override suspend fun connection(): HealthConnection = when (val result = auth.authorize()) {
        is HealthAuthResult.Granted -> {
            cachedToken = result.accessToken
            HealthConnection.Connected(importedItems = links.importedCount())
        }

        is HealthAuthResult.NeedsConsent -> HealthConnection.Disconnected(result.pendingIntent)
        HealthAuthResult.Unavailable -> HealthConnection.Unavailable
    }

    override suspend fun connectState(): HealthConnectState = connect.state()

    override fun connectPermissionContract() = connect.permissionContract()

    override suspend fun connectPermissions(): Set<String> {
        val tracking = profileRepository.observeProfile().first()?.cycleTrackingOn == true
        return if (tracking) {
            CONNECT_PERMISSIONS
        } else {
            CONNECT_PERMISSIONS - HealthMetric.Menstruation.permission
        }
    }

    override suspend fun completeConsent(data: Intent?): Boolean {
        cachedToken = auth.tokenFromConsentResult(data)
        return cachedToken != null
    }

    override suspend fun sync(): HealthSyncResult {
        // The local leg first. Not only because Health Connect is the preferred provider: which
        // types it is granted is what decides which cloud legs run at all — see `cloudMetrics`,
        // where that precedence is decided once so the two legs can never both write a table.
        val granted = connect.state().granted
        var imported = syncConnect(granted)
        val cloud = cloudMetrics(granted)

        // Health Connect needs no network, so an offline sync that answered locally is a sync that
        // worked. Offline is only the whole answer when there was no local leg to run.
        if (!networkMonitor.isOnline()) {
            return if (granted.isEmpty()) HealthSyncResult.Offline else HealthSyncResult.Imported(imported)
        }
        val token = cachedToken ?: when (val result = auth.authorize()) {
            is HealthAuthResult.Granted -> result.accessToken.also { cachedToken = it }
            // A local import that already landed is worth reporting rather than discarding: the
            // Connections screen renders the Google panel's own consent state beside the Health
            // Connect one, so nothing is hidden by not making it this call's return value.
            is HealthAuthResult.NeedsConsent ->
                return if (imported > 0) HealthSyncResult.Imported(imported)
                else HealthSyncResult.NeedsConsent(result.pendingIntent)

            HealthAuthResult.Unavailable ->
                return if (imported > 0) HealthSyncResult.Imported(imported) else HealthSyncResult.Failed
        }

        var failed = false

        // Each type is independent: a weight page that 500s must not throw away the workouts that
        // already landed, so the failure is remembered and the rest still runs. A type Health
        // Connect answered is skipped outright — one writer per table per sync, so a Revoked here
        // can only ever come from a type the local leg did not cover.
        if (HealthMetric.Exercise in cloud) {
            when (val result = importExercise(token)) {
                is Outcome.Wrote -> imported += result.items
                Outcome.Revoked -> return HealthSyncResult.NeedsConsent(pendingIntent = null)
                Outcome.Failed -> failed = true
            }
        }
        if (HealthMetric.Weight in cloud) {
            when (val result = importWeight(token)) {
                is Outcome.Wrote -> imported += result.items
                Outcome.Revoked -> return HealthSyncResult.NeedsConsent(pendingIntent = null)
                Outcome.Failed -> failed = true
            }
        }
        if (HealthMetric.Sleep in cloud) {
            when (val result = importSleep(token)) {
                is Outcome.Wrote -> imported += result.items
                Outcome.Revoked -> return HealthSyncResult.NeedsConsent(pendingIntent = null)
                Outcome.Failed -> failed = true
            }
        }
        if (HealthMetric.Steps in cloud) {
            when (val result = importSteps(token)) {
                is Outcome.Wrote -> imported += result.items
                Outcome.Revoked -> return HealthSyncResult.NeedsConsent(pendingIntent = null)
                Outcome.Failed -> failed = true
            }
        }
        // The one type that cannot fail the sync — see importHeart. Its scope is a guess, and a
        // wrong one has to cost the card, not a permanent error on the Connections screen.
        if (HealthMetric.Heart in cloud) {
            (importHeart(token) as? Outcome.Wrote)?.let { imported += it.items }
        }

        // Push last: an import that worked is worth reporting even if the outbound leg didn't.
        // Cloud-only whatever Health Connect is granted — FitPulse writes nothing to Health
        // Connect, so there is no second push path to keep in step with this one.
        if (!pushNutrition(token)) failed = true

        return if (failed && imported == 0) HealthSyncResult.Failed else HealthSyncResult.Imported(imported)
    }

    /**
     * The local leg.
     *
     * It runs before the cloud one for two reasons. Which types it covers decides which cloud legs
     * run at all; and the handover it performs ([retireSupersededCloudRows]) has to happen before
     * the cloud could write a second copy of the same window.
     */
    private suspend fun syncConnect(granted: Set<HealthMetric>): Int {
        if (granted.isEmpty()) return 0
        // Priced at one weight for the whole window, exactly as importSteps prices its own: the
        // latest weigh-in, else the profile's. No profile means onboarding is unfinished, and
        // there is nothing to price a session or a day of walking against.
        val weightKg = latestWeightKg() ?: return 0
        val windows = granted.associateWith { connectWindowStart(it) }
        windows.forEach { (metric, since) -> retireSupersededCloudRows(metric, since) }
        return writeConnect(connect.read(windows, weightKg), weightKg)
    }

    /**
     * Where Health Connect's window opens, per type — the same two cursor rules the cloud leg
     * already follows, reused rather than re-derived.
     *
     * Steps and heart rate share the cloud leg's own cursor (`MAX(date)` in their day tables),
     * because those tables are replace-in-full and belong to whichever provider last wrote them;
     * that is also what makes a handover of those two need no dedup at all. Everything else keys
     * off `health_link`, under Health Connect's *own* data type, so the two providers' cursors are
     * independent and revoking one cannot advance the other past data it never wrote.
     */
    private suspend fun connectWindowStart(metric: HealthMetric): Long = when (metric) {
        HealthMetric.Steps -> stepsWindowStart()
        HealthMetric.Heart -> heartWindowStart()
        else -> windowStart(metric.connectDataType)
    }

    /**
     * The handover, run once per granted metric before Health Connect writes anything.
     *
     * Steps and heart rate fall out for free: neither records a link, so [HealthLinkDao.importedLinks]
     * answers empty and their day rows are simply replaced. Blood pressure has no cloud type at all.
     * What is left is exercise, weight and sleep — the three that could genuinely hold two copies.
     */
    private suspend fun retireSupersededCloudRows(metric: HealthMetric, sinceMillis: Long) {
        val cloudType = cloudDataTypeOf(metric) ?: return
        val superseded = supersededByConnect(links.importedLinks(cloudType), cloudType, sinceMillis)
        if (superseded.isEmpty()) return
        superseded.forEach { deleteImportedRow(it) }
        links.delete(superseded.map { it.remoteName })
    }

    /**
     * The Google Health type a metric imports as. Blood pressure has none: its scope was
     * deliberately not requested at verification, which is why Health Connect is its only importer.
     */
    private fun cloudDataTypeOf(metric: HealthMetric): String? = when (metric) {
        HealthMetric.Exercise -> HealthDataType.Exercise.id
        HealthMetric.Weight -> HealthDataType.Weight.id
        HealthMetric.Sleep -> HealthDataType.Sleep.id
        HealthMetric.Steps -> HealthDataType.Steps.id
        HealthMetric.Heart -> HealthDataType.Heart.id
        HealthMetric.BloodPressure -> null
        HealthMetric.Menstruation -> null
    }

    /**
     * Every writer here is the one the cloud leg already uses, which is the point: a workout that
     * came from Health Connect and one that came from the Google Health API are the same diary row,
     * written by the same code, deduped by the same table.
     */
    private suspend fun writeConnect(records: ConnectRecords, weightKg: Double): Int {
        var written = 0
        written += store(records.exercise, HealthMetric.Exercise.connectDataType, EXERCISE_TABLE) { remote ->
            exerciseRepository.addEntry(remote.toExerciseEntry())
        }
        written += store(
            records.weight,
            HealthMetric.Weight.connectDataType,
            WEIGHT_TABLE,
            weightWriter(note = "Health Connect"),
        )
        written += store(records.sleep, HealthMetric.Sleep.connectDataType, SLEEP_TABLE) { writeSleepNight(it) }
        written += store(
            records.bloodPressure,
            HealthMetric.BloodPressure.connectDataType,
            BLOOD_PRESSURE_TABLE,
            bloodPressureWriter(),
        )
        // A period whose days were all typed by hand writes nothing and records no link, so it is
        // not counted as imported — `bloodPressureWriter`'s rule. One record is one link, which is
        // what puts this type on `health_link`'s ordinary cursor rather than steps' and heart's.
        written += store(records.menstruation, HealthMetric.Menstruation.connectDataType, CYCLE_TABLE) { remote ->
            if (cycleRepository.importDays(remote.toCycleDays()) > 0) 1L else SKIPPED
        }
        // The two aggregating types keep their own bookkeeping and record no link — see importSteps.
        if (records.steps.isNotEmpty()) written += writeSteps(stepTotals(records.steps), weightKg)
        if (records.heart.isNotEmpty()) written += writeHeart(records.heart)
        return written
    }

    /**
     * A cuff reading typed by hand and also written to Health Connect is one measurement, but
     * `blood_pressure_reading` autogenerates its ids, so nothing keeps the two apart the way
     * `health_link`'s primary key keeps two imports apart — see [alreadyHeld]. The held list is
     * read once per sync and grown as rows land, so one batch cannot duplicate inside itself either.
     */
    private suspend fun bloodPressureWriter(): suspend (RemoteBloodPressure) -> Long {
        val held = bloodPressureRepository.observeReadings().first().toMutableList()
        return { remote ->
            if (remote.alreadyHeld(held)) {
                SKIPPED
            } else {
                val reading = remote.toBloodPressureReading()
                held += reading
                bloodPressureRepository.addReading(reading)
            }
        }
    }

    private sealed interface Outcome {
        data class Wrote(val items: Int) : Outcome

        /** The scope was revoked from myaccount.google.com while we held a token. */
        data object Revoked : Outcome
        data object Failed : Outcome
    }

    private suspend fun importExercise(token: String) = importAll(
        dataType = HealthDataType.Exercise,
        token = token,
        localTable = EXERCISE_TABLE,
        parse = ::parseExercisePage,
    ) { remote ->
        exerciseRepository.addEntry(remote.toExerciseEntry())
    }

    private suspend fun importWeight(token: String): Outcome = importAll(
        dataType = HealthDataType.Weight,
        token = token,
        localTable = WEIGHT_TABLE,
        parse = ::parseWeightPage,
        write = weightWriter(note = "Google Health"),
    )

    /**
     * `weight_entry` holds one row per day, so an imported weigh-in must not overwrite one the
     * user typed. Days that already have an entry are skipped rather than replaced — the manual
     * number is the one they chose to record.
     *
     * Shared by both providers, and [note] is what tells them apart on the row. The taken-days set
     * is built per call rather than per sync deliberately: each leg reads the table as it finds it,
     * so whichever runs first wins a day and the second skips it.
     */
    private suspend fun weightWriter(note: String): suspend (RemoteWeight) -> Long {
        val takenDays = progressRepository.observeWeightEntries().first()
            .mapTo(mutableSetOf()) { it.dateEpochDay }
        return { remote ->
            val day = epochDayOf(remote.timeMillis)
            if (!takenDays.add(day)) {
                SKIPPED
            } else {
                progressRepository.upsertWeightEntry(
                    WeightEntry(dateEpochDay = day, weightKg = remote.weightKg, note = note),
                )
                // The table is keyed by date, so the date is the local id.
                day
            }
        }
    }

    private suspend fun importSleep(token: String) = importAll(
        dataType = HealthDataType.Sleep,
        token = token,
        localTable = SLEEP_TABLE,
        parse = ::parseSleepPage,
        write = ::writeSleepNight,
    )

    /** Keyed by the day the night ended — see [SleepDayEntity]. Shared by both providers. */
    private suspend fun writeSleepNight(remote: RemoteSleep): Long {
        val day = epochDayOf(remote.endMillis)
        sleepDao.upsert(
            SleepDayEntity(
                date = day,
                minutesAsleep = remote.minutesAsleep,
                startMillis = remote.timeMillis,
                endMillis = remote.endMillis,
            ),
        )
        return day
    }

    /**
     * Steps are the one imported type that doesn't go through [importAll], because they aggregate:
     * the API reports intra-day buckets and `step_day` holds a daily total, so there is no
     * one-remote-point-to-one-local-row relationship for `health_link` to record. The cursor is
     * `MAX(date)` in `step_day` instead — still derived from rows actually written, which is the
     * property that makes the link table's own cursor safe.
     *
     * Days are summed and **replaced** rather than accumulated, so re-querying an overlapping
     * window is idempotent and a bucket the watch later revises corrects itself. That is also why
     * nothing is written until every page has landed: a half-read window would replace a good
     * total with a low one.
     */
    private suspend fun importSteps(token: String): Outcome {
        val weightKg = latestWeightKg() ?: return Outcome.Wrote(0)

        val since = stepsWindowStart()
        var pageToken: String? = null
        var totals: Map<Long, Int> = emptyMap()

        repeat(MAX_PAGES) {
            val url = dataPointsUrl(HealthDataType.Steps, sinceMillis = since, pageToken = pageToken)
            val page = when (val response = fetch(url, token)) {
                is HealthResponse.Ok -> parseStepsPage(response.body)
                HealthResponse.Forbidden -> {
                    cachedToken = null
                    return Outcome.Revoked
                }

                HealthResponse.Unauthorized, HealthResponse.Failed -> return Outcome.Failed
            }

            totals = stepTotals(page.items, into = totals)
            pageToken = page.nextPageToken ?: return Outcome.Wrote(writeSteps(totals, weightKg))
        }
        return Outcome.Wrote(writeSteps(totals, weightKg))
    }

    /**
     * One weight for a whole sync: the latest weigh-in, else the profile's. The MET estimate is
     * coarse by construction, so a per-day weight would be false precision. Null means onboarding
     * is unfinished and there is nothing to price against.
     */
    private suspend fun latestWeightKg(): Double? =
        progressRepository.observeWeightEntries().first().maxByOrNull { it.dateEpochDay }?.weightKg
            ?: profileRepository.observeProfile().first()?.weightKg

    /** Buckets to daily totals — the fold both providers' step reads go through. */
    private fun stepTotals(
        buckets: List<RemoteSteps>,
        into: Map<Long, Int> = emptyMap(),
    ): Map<Long, Int> {
        val totals = into.toMutableMap()
        buckets.forEach { bucket ->
            val day = epochDayOf(bucket.timeMillis)
            totals[day] = (totals[day] ?: 0) + bucket.count
        }
        return totals
    }

    /** Returns the days written, so both legs can add it to one count. */
    private suspend fun writeSteps(totals: Map<Long, Int>, weightKg: Double): Int {
        totals.forEach { (day, steps) ->
            stepDao.upsert(
                StepDayEntity(date = day, steps = steps, burnedKcal = stepsBurnedKcal(steps, weightKg)),
            )
        }
        return totals.size
    }

    /**
     * Day-aligned, unlike [windowStart]: a window that starts mid-morning would return a partial
     * day, and the replace-in-full write would turn a complete total into a fragment of one. The
     * cursor's own day is re-queried too, since it was almost certainly still in progress.
     */
    private suspend fun stepsWindowStart(): Long {
        val latest = stepDao.latestDate() ?: return epochDayStartMillis(todayEpochDay() - BACKFILL_DAYS)
        return epochDayStartMillis(latest - 1)
    }

    /**
     * Heart rate aggregates the way steps do — intra-day samples in, one row per day out — so it
     * takes the same shape: no `health_link` row, `MAX(date)` as the cursor, a day-aligned window,
     * and nothing written until every page has landed, so a half-read window cannot replace a good
     * day's average with a fragment of one. Days are replaced rather than merged, which makes a
     * re-sync idempotent.
     *
     * **This one type can neither revoke nor fail the sync, and that divergence is deliberate.**
     * Every other type reads a scope `HEALTH_SCOPES` explicitly requests, so a 403 there really
     * does mean the user revoked it from myaccount.google.com. Heart rate rides
     * `health_metrics_and_measurements.readonly` on the assumption that a BPM reading is a health
     * metric — an assumption no live account has confirmed. If it is wrong the API answers 403 on
     * every sync: reporting that as a revocation would drop a perfectly good connection to "needs
     * consent" forever, and reporting it as a failure would put "Couldn't reach Google Health" on
     * the Connections screen after every sync that had nothing new to import. So [sync] reads the
     * items on success and discards every other outcome — a wrong guess costs the card and
     * nothing else. Do not "fix" this into consistency with the other four before the scope is
     * pinned against a live response.
     */
    private suspend fun importHeart(token: String): Outcome {
        val since = heartWindowStart()
        var pageToken: String? = null
        val samples = mutableListOf<RemoteHeart>()

        repeat(MAX_PAGES) {
            val url = dataPointsUrl(HealthDataType.Heart, sinceMillis = since, pageToken = pageToken)
            val page = when (val response = fetch(url, token)) {
                is HealthResponse.Ok -> parseHeartPage(response.body)
                HealthResponse.Forbidden, HealthResponse.Unauthorized, HealthResponse.Failed ->
                    return Outcome.Failed
            }

            samples += page.items
            pageToken = page.nextPageToken ?: return Outcome.Wrote(writeHeart(samples))
        }
        return Outcome.Wrote(writeHeart(samples))
    }

    /** Returns the days written, so both legs can add it to one count. */
    private suspend fun writeHeart(samples: List<RemoteHeart>): Int {
        val days = aggregateHeartByDay(samples)
        days.values.forEach { day ->
            heartDao.upsert(
                HeartDayEntity(date = day.dateEpochDay, averageBpm = day.averageBpm, minBpm = day.minBpm),
            )
        }
        return days.size
    }

    /** Day-aligned for the same reason [stepsWindowStart] is: a window starting mid-morning would
     * return a partial day, and the replace-in-full write would turn a full day's average into an
     * average of one morning. */
    private suspend fun heartWindowStart(): Long {
        val latest = heartDao.latestDate() ?: return epochDayStartMillis(todayEpochDay() - BACKFILL_DAYS)
        return epochDayStartMillis(latest - 1)
    }

    /**
     * The outbound leg: meals and water FitPulse holds that Google Health doesn't.
     *
     * Deletions propagate first. A meal the user removed from their diary but that we already
     * sent would otherwise live on in their Google Health profile forever, which is not what
     * "delete" means to anyone.
     *
     * Returns false if anything failed, so the caller can report it without losing an import
     * that did work.
     */
    private suspend fun pushNutrition(token: String): Boolean {
        val entries = foodRepository.allEntries()
        var ok = pushDeletions(token, entries.mapTo(mutableSetOf()) { it.id })
        ok = pushMeals(token, entries) && ok
        return pushHydration(token) && ok
    }

    /** Data points whose diary row is gone. Chunked, because batchDelete caps per request. */
    private suspend fun pushDeletions(token: String, liveEntryIds: Set<Long>): Boolean {
        val orphans = links.links(pushed = true)
            .filter { it.localTable == FOOD_TABLE && it.localId !in liveEntryIds }
        if (orphans.isEmpty()) return true

        var ok = true
        orphans.chunked(BATCH_DELETE_SIZE).forEach { chunk ->
            val names = chunk.map { it.remoteName }
            val response = healthPost(batchDeleteUrl(NUTRITION_LOG), token, batchDeleteBody(names))
            if (response is HealthResponse.Ok) links.delete(names) else ok = false
        }
        return ok
    }

    /**
     * Windowed to [BACKFILL_DAYS], the same reach the inbound legs have. Without it the first sync
     * after connecting POSTed the user's *entire* diary — one request per row, sequentially, for
     * however many years they had been logging. That is minutes of work on a busy account, and it
     * contradicts the data-minimisation answer the 30-day read window exists to give: sending a
     * three-year archive is not a smaller ask than reading one.
     *
     * Older entries are simply never sent. They are not marked as sent either, so raising the
     * window later picks them up rather than stranding them.
     */
    private suspend fun pushMeals(token: String, entries: List<FoodEntry>): Boolean {
        val alreadySent = links.pushedLocalIds(FOOD_TABLE).toSet()
        val since = todayEpochDay() - BACKFILL_DAYS
        var ok = true
        entries.filter { it.dateEpochDay >= since && it.id !in alreadySent }.forEach { entry ->
            val dayStart = epochDayStartMillis(entry.dateEpochDay)
            // A rejected body would otherwise strand the meal forever: no link is recorded, so
            // every later sync retries it and fails again. The second attempt drops the three
            // unverified nutrient names — see `nutritionLogBody`. `?:` keeps it unbuilt when the
            // first lands, and a transient failure only costs a request that was already lost.
            val created = create(token, NUTRITION_LOG, nutritionLogBody(entry, dayStart))
                ?: create(token, NUTRITION_LOG, nutritionLogBody(entry, dayStart, micronutrients = false))
            if (created == null) {
                ok = false
            } else {
                links.upsert(
                    HealthLinkEntity(
                        remoteName = created,
                        dataType = NUTRITION_LOG,
                        localTable = FOOD_TABLE,
                        localId = entry.id,
                        remoteTimeMillis = dayStart,
                        pushed = true,
                    ),
                )
            }
        }
        return ok
    }

    /**
     * Water is one row per day holding a running count, so a day is only sent once it can no
     * longer change — that is, once it is in the past. Today's glasses go out on the next sync
     * that happens on a later day.
     *
     * ponytail: the alternative is patching the data point every time a glass is tapped. Settled
     * days only is a fraction of the code and the user still ends up with the right total.
     */
    private suspend fun pushHydration(token: String): Boolean {
        val alreadySent = links.pushedLocalIds(WATER_TABLE).toSet()
        val today = todayEpochDay()
        var ok = true
        waterRepository.allDays()
            .filter {
                // Windowed like pushMeals, and for the same reason.
                it.dateEpochDay >= today - BACKFILL_DAYS &&
                    it.dateEpochDay < today &&
                    it.dateEpochDay !in alreadySent &&
                    it.glasses > 0
            }
            .forEach { day ->
                val dayStart = epochDayStartMillis(day.dateEpochDay)
                val created = create(token, HYDRATION_LOG, hydrationLogBody(day.glasses * GLASS_ML, dayStart))
                if (created == null) {
                    ok = false
                } else {
                    links.upsert(
                        HealthLinkEntity(
                            remoteName = created,
                            dataType = HYDRATION_LOG,
                            localTable = WATER_TABLE,
                            localId = day.dateEpochDay,
                            remoteTimeMillis = dayStart,
                            pushed = true,
                        ),
                    )
                }
            }
        return ok
    }

    /** One `dataPoints.create`, with the same single 401 retry the reads get. */
    private suspend fun create(token: String, dataType: String, body: String): String? {
        val url = createDataPointUrl(dataType)
        val first = healthPost(url, token, body)
        val response = if (first == HealthResponse.Unauthorized) {
            val refreshed = (auth.authorize() as? HealthAuthResult.Granted)?.accessToken ?: return null
            cachedToken = refreshed
            healthPost(url, refreshed, body)
        } else {
            first
        }
        return (response as? HealthResponse.Ok)?.let { parseCreatedName(it.body) }
    }

    /**
     * The paging loop every data type shares: window from the cursor, page until the API stops,
     * write what we don't already have, and record a link for each row so it can be found again
     * for dedup and for deletion.
     */
    private suspend fun <T : RemotePoint> importAll(
        dataType: HealthDataType,
        token: String,
        localTable: String,
        parse: (String) -> Page<T>,
        write: suspend (T) -> Long,
    ): Outcome {
        val since = windowStart(dataType.id)
        var pageToken: String? = null
        var imported = 0

        repeat(MAX_PAGES) {
            val url = dataPointsUrl(dataType, sinceMillis = since, pageToken = pageToken)
            val page = when (val response = fetch(url, token)) {
                is HealthResponse.Ok -> parse(response.body)
                HealthResponse.Forbidden -> {
                    cachedToken = null
                    return Outcome.Revoked
                }

                HealthResponse.Unauthorized, HealthResponse.Failed ->
                    return if (imported > 0) Outcome.Wrote(imported) else Outcome.Failed
            }

            imported += store(page.items, dataType.id, localTable, write)
            pageToken = page.nextPageToken ?: return Outcome.Wrote(imported)
        }
        return Outcome.Wrote(imported)
    }

    /**
     * One retry on a 401 with a freshly minted token, because an hour-old cached token is the
     * ordinary case, not an error.
     */
    private suspend fun fetch(url: String, token: String): HealthResponse {
        val first = healthGet(url, token)
        if (first != HealthResponse.Unauthorized) return first
        val refreshed = (auth.authorize() as? HealthAuthResult.Granted)?.accessToken
            ?: return HealthResponse.Unauthorized
        cachedToken = refreshed
        return healthGet(url, refreshed)
    }

    /**
     * Writes what we don't already have. The `existing` check is belt to `health_link`'s
     * primary-key braces: without it a re-sync would insert a second local row before the link
     * upsert collapsed the two, and the diary would show the workout twice.
     */
    private suspend fun <T : RemotePoint> store(
        items: List<T>,
        dataType: String,
        localTable: String,
        write: suspend (T) -> Long,
    ): Int {
        if (items.isEmpty()) return 0
        val known = links.existing(items.map { it.remoteName }).toSet()
        var written = 0
        items.filterNot { it.remoteName in known }.forEach { remote ->
            val localId = write(remote)
            if (localId == SKIPPED) return@forEach
            links.upsert(
                HealthLinkEntity(
                    remoteName = remote.remoteName,
                    dataType = dataType,
                    localTable = localTable,
                    localId = localId,
                    remoteTimeMillis = remote.timeMillis,
                    pushed = false,
                ),
            )
            written++
        }
        return written
    }

    /** Cursor, overlapped by a day so a late-arriving data point isn't skipped. See the constants. */
    private suspend fun windowStart(dataType: String): Long {
        val latest = links.latestImportedTime(dataType)
            ?: return System.currentTimeMillis() - BACKFILL_DAYS * DAY_MILLIS
        return latest - SYNC_OVERLAP_MILLIS
    }

    override suspend fun disconnect(deleteImported: Boolean, deleteSent: Boolean) {
        if (deleteSent) {
            val token = cachedToken ?: (auth.authorize() as? HealthAuthResult.Granted)?.accessToken
            if (token != null) {
                links.links(pushed = true)
                    .groupBy { it.dataType }
                    .forEach { (dataType, group) ->
                        group.chunked(BATCH_DELETE_SIZE).forEach { chunk ->
                            healthPost(batchDeleteUrl(dataType), token, batchDeleteBody(chunk.map { it.remoteName }))
                        }
                    }
            }
        }
        if (deleteImported) {
            links.links(pushed = false).forEach { deleteImportedRow(it) }
            // No links to walk: step_day and heart_day are their own bookkeeping, so they are
            // cleared wholesale.
            stepDao.clear()
            heartDao.clear()
        }
        // The links go either way: keeping them would make a later reconnect skip data the user
        // asked us to forget, and keeping them without the rows would point at nothing.
        links.clear()
        cachedToken?.let { auth.revoke(it) }
        cachedToken = null
    }

    /**
     * One imported row, gone. Shared by [disconnect] and the Health Connect handover
     * ([retireSupersededCloudRows]), so a row retired by either path leaves the same way — a soft
     * delete through the repository that owns it, never a hard one.
     *
     * Steps and heart rate have no branch because they record no link: both are cleared wholesale
     * by [disconnect] and replaced in full by a re-import.
     */
    private suspend fun deleteImportedRow(link: HealthLinkEntity) {
        when (link.localTable) {
            EXERCISE_TABLE -> exerciseRepository.deleteEntry(link.localId)
            WEIGHT_TABLE -> progressRepository.deleteWeightEntry(link.localId)
            SLEEP_TABLE -> sleepDao.delete(link.localId)
            BLOOD_PRESSURE_TABLE -> bloodPressureRepository.deleteReading(link.localId)
        }
    }
}
