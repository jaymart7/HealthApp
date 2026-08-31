package ph.mart.healthapp.core.data.health

import android.content.Intent
import kotlinx.coroutines.flow.first
import ph.mart.healthapp.core.data.epochDayOf
import ph.mart.healthapp.core.data.epochDayStartMillis
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.FoodRepository
import ph.mart.healthapp.core.data.todayEpochDay
import ph.mart.healthapp.core.data.water.GLASS_ML
import ph.mart.healthapp.core.data.water.WaterRepository
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
    private val networkMonitor: NetworkMonitor,
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

    override suspend fun completeConsent(data: Intent?): Boolean {
        cachedToken = auth.tokenFromConsentResult(data)
        return cachedToken != null
    }

    override suspend fun sync(): HealthSyncResult {
        if (!networkMonitor.isOnline()) return HealthSyncResult.Offline
        val token = cachedToken ?: when (val result = auth.authorize()) {
            is HealthAuthResult.Granted -> result.accessToken.also { cachedToken = it }
            is HealthAuthResult.NeedsConsent -> return HealthSyncResult.NeedsConsent(result.pendingIntent)
            HealthAuthResult.Unavailable -> return HealthSyncResult.Failed
        }

        var imported = 0
        var failed = false

        // Each type is independent: a weight page that 500s must not throw away the workouts that
        // already landed, so the failure is remembered and the rest still runs.
        when (val result = importExercise(token)) {
            is Outcome.Wrote -> imported += result.items
            Outcome.Revoked -> return HealthSyncResult.NeedsConsent(pendingIntent = null)
            Outcome.Failed -> failed = true
        }
        when (val result = importWeight(token)) {
            is Outcome.Wrote -> imported += result.items
            Outcome.Revoked -> return HealthSyncResult.NeedsConsent(pendingIntent = null)
            Outcome.Failed -> failed = true
        }
        when (val result = importSleep(token)) {
            is Outcome.Wrote -> imported += result.items
            Outcome.Revoked -> return HealthSyncResult.NeedsConsent(pendingIntent = null)
            Outcome.Failed -> failed = true
        }
        when (val result = importSteps(token)) {
            is Outcome.Wrote -> imported += result.items
            Outcome.Revoked -> return HealthSyncResult.NeedsConsent(pendingIntent = null)
            Outcome.Failed -> failed = true
        }
        // The one type that cannot fail the sync — see importHeart. Its scope is a guess, and a
        // wrong one has to cost the card, not a permanent error on the Connections screen.
        (importHeart(token) as? Outcome.Wrote)?.let { imported += it.items }

        // Push last: an import that worked is worth reporting even if the outbound leg didn't.
        if (!pushNutrition(token)) failed = true

        return if (failed && imported == 0) HealthSyncResult.Failed else HealthSyncResult.Imported(imported)
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

    /**
     * `weight_entry` holds one row per day, so an imported weigh-in must not overwrite one the
     * user typed. Days that already have an entry are skipped rather than replaced — the manual
     * number is the one they chose to record.
     */
    private suspend fun importWeight(token: String): Outcome {
        val takenDays = progressRepository.observeWeightEntries().first().map { it.dateEpochDay }.toMutableSet()
        return importAll(
            dataType = HealthDataType.Weight,
            token = token,
            localTable = WEIGHT_TABLE,
            parse = ::parseWeightPage,
        ) { remote ->
            val day = epochDayOf(remote.timeMillis)
            if (!takenDays.add(day)) {
                SKIPPED
            } else {
                progressRepository.upsertWeightEntry(
                    WeightEntry(dateEpochDay = day, weightKg = remote.weightKg, note = "Google Health"),
                )
                // The table is keyed by date, so the date is the local id.
                day
            }
        }
    }

    /** Keyed by the day the night ended — see [SleepDayEntity]. */
    private suspend fun importSleep(token: String) = importAll(
        dataType = HealthDataType.Sleep,
        token = token,
        localTable = SLEEP_TABLE,
        parse = ::parseSleepPage,
    ) { remote ->
        val day = epochDayOf(remote.endMillis)
        sleepDao.upsert(
            SleepDayEntity(
                date = day,
                minutesAsleep = remote.minutesAsleep,
                startMillis = remote.timeMillis,
                endMillis = remote.endMillis,
            ),
        )
        day
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
        // Priced at one weight for the whole window: the latest weigh-in, else the profile's.
        // The MET estimate is coarse by construction, so a per-day weight would be false
        // precision. No profile means onboarding is unfinished and there is nothing to price at.
        val weightKg = progressRepository.observeWeightEntries().first()
            .maxByOrNull { it.dateEpochDay }?.weightKg
            ?: profileRepository.observeProfile().first()?.weightKg
            ?: return Outcome.Wrote(0)

        val since = stepsWindowStart()
        var pageToken: String? = null
        val totals = mutableMapOf<Long, Int>()

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

            page.items.forEach { bucket ->
                val day = epochDayOf(bucket.timeMillis)
                totals[day] = (totals[day] ?: 0) + bucket.count
            }
            pageToken = page.nextPageToken ?: return writeSteps(totals, weightKg)
        }
        return writeSteps(totals, weightKg)
    }

    private suspend fun writeSteps(totals: Map<Long, Int>, weightKg: Double): Outcome {
        totals.forEach { (day, steps) ->
            stepDao.upsert(
                StepDayEntity(date = day, steps = steps, burnedKcal = stepsBurnedKcal(steps, weightKg)),
            )
        }
        return Outcome.Wrote(totals.size)
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
            pageToken = page.nextPageToken ?: return writeHeart(samples)
        }
        return writeHeart(samples)
    }

    private suspend fun writeHeart(samples: List<RemoteHeart>): Outcome {
        val days = aggregateHeartByDay(samples)
        days.values.forEach { day ->
            heartDao.upsert(
                HeartDayEntity(date = day.dateEpochDay, averageBpm = day.averageBpm, minBpm = day.minBpm),
            )
        }
        return Outcome.Wrote(days.size)
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

    private suspend fun pushMeals(token: String, entries: List<FoodEntry>): Boolean {
        val alreadySent = links.pushedLocalIds(FOOD_TABLE).toSet()
        var ok = true
        entries.filterNot { it.id in alreadySent }.forEach { entry ->
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
            .filter { it.dateEpochDay < today && it.dateEpochDay !in alreadySent && it.glasses > 0 }
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
        val since = windowStart(dataType)
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

            imported += store(page.items, dataType, localTable, write)
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
        dataType: HealthDataType,
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
                    dataType = dataType.id,
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
    private suspend fun windowStart(dataType: HealthDataType): Long {
        val latest = links.latestImportedTime(dataType.id)
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
            links.links(pushed = false).forEach { link ->
                when (link.localTable) {
                    EXERCISE_TABLE -> exerciseRepository.deleteEntry(link.localId)
                    WEIGHT_TABLE -> progressRepository.deleteWeightEntry(link.localId)
                    SLEEP_TABLE -> sleepDao.delete(link.localId)
                }
            }
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
}
