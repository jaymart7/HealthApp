package ph.mart.healthapp.core.data.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder
import java.util.GregorianCalendar
import ph.mart.healthapp.core.data.epochDayOf
import ph.mart.healthapp.core.data.epochDayStartMillis
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType
import ph.mart.healthapp.core.data.food.Nutrients

/**
 * A steps roll-up: one reconciled total per civil day. The second day walked nothing, the third
 * carries no total and the fourth no date — only the first is a day worth writing.
 */
private const val STEPS_ROLLUP = """
{
  "rollupDataPoints": [
    {
      "civilStartTime": { "date": { "year": 2026, "month": 4, "day": 20 } },
      "civilEndTime": { "date": { "year": 2026, "month": 4, "day": 21 } },
      "steps": { "countSum": "8412" }
    },
    {
      "civilStartTime": { "date": { "year": 2026, "month": 4, "day": 21 } },
      "steps": { "countSum": "0" }
    },
    {
      "civilStartTime": { "date": { "year": 2026, "month": 4, "day": 22 } },
      "steps": {}
    },
    { "steps": { "countSum": "500" } }
  ]
}
"""

/** A page shaped like the API's own documented response, trimmed to the fields FitPulse reads. */
private const val PAGE = """
{
  "dataPoints": [
    {
      "name": "users/me/dataTypes/exercise/dataPoints/abc123",
      "exercise": {
        "interval": { "startTime": "2026-04-20T08:00:00Z", "endTime": "2026-04-20T08:35:00Z" },
        "exerciseType": "RUNNING",
        "displayName": "Morning Trail Run",
        "activeDuration": "1800s",
        "metricsSummary": { "caloriesKcal": 380.0, "steps": "6200" }
      }
    },
    {
      "name": "users/me/dataTypes/exercise/dataPoints/def456",
      "exercise": {
        "interval": { "startTime": "2026-04-21T18:00:00+02:00", "endTime": "2026-04-21T18:45:00+02:00" },
        "exerciseType": "SOMETHING_NEW_IN_2027",
        "metricsSummary": { "caloriesKcal": "210" }
      }
    },
    {
      "name": "users/me/dataTypes/exercise/dataPoints/no-interval",
      "exercise": { "exerciseType": "YOGA" }
    }
  ],
  "nextPageToken": "page-2"
}
"""

private const val WEIGHTS = """
{
  "dataPoints": [
    {
      "name": "users/me/dataTypes/weight/dataPoints/w1",
      "weight": {
        "sampleTime": { "physicalTime": "2026-04-20T06:30:00Z" },
        "weightGrams": 72400
      }
    },
    {
      "name": "users/me/dataTypes/weight/dataPoints/w2",
      "weight": {
        "sampleTime": { "physicalTime": "2026-04-21T06:30:00Z" },
        "weightGrams": 0
      }
    },
    {
      "name": "users/me/dataTypes/weight/dataPoints/w3",
      "weight": { "weightGrams": 70000 }
    }
  ]
}
"""

private const val NIGHTS = """
{
  "dataPoints": [
    {
      "name": "users/me/dataTypes/sleep/dataPoints/s1",
      "sleep": {
        "interval": { "startTime": "2026-04-20T22:00:00Z", "endTime": "2026-04-21T06:00:00Z" },
        "type": "STAGES",
        "stages": [
          { "startTime": "2026-04-20T22:00:00Z", "endTime": "2026-04-21T01:00:00Z", "type": "LIGHT" },
          { "startTime": "2026-04-21T01:00:00Z", "endTime": "2026-04-21T02:00:00Z", "type": "AWAKE" },
          { "startTime": "2026-04-21T02:00:00Z", "endTime": "2026-04-21T06:00:00Z", "type": "DEEP" }
        ]
      }
    },
    {
      "name": "users/me/dataTypes/sleep/dataPoints/s2",
      "sleep": {
        "interval": { "startTime": "2026-04-21T23:00:00Z", "endTime": "2026-04-22T06:30:00Z" }
      }
    }
  ]
}
"""

private const val HEART_ROLLUP = """
{
  "rollupDataPoints": [
    {
      "civilStartTime": { "date": { "year": 2026, "month": 4, "day": 20 } },
      "heartRate": { "beatsPerMinuteAvg": 68.4, "beatsPerMinuteMin": 52, "beatsPerMinuteMax": 141 }
    },
    {
      "civilStartTime": { "date": { "year": 2026, "month": 4, "day": 21 } },
      "heartRate": {}
    }
  ]
}
"""

/** The app's day key for a local calendar date, taken at noon so no zone can move it. */
private fun localDay(year: Int, month: Int, day: Int): Long =
    epochDayOf(GregorianCalendar(year, month - 1, day, 12, 0).timeInMillis)

class GoogleHealthApiTest {

    @Test
    fun `a sleep page subtracts time awake when stages are reported`() {
        val page = parseSleepPage(NIGHTS)

        assertEquals(2, page.items.size)
        // Eight hours in bed, one of them awake.
        assertEquals(7 * 60, page.items[0].minutesAsleep)
        // No stages: the interval is all there is to go on.
        assertEquals(450, page.items[1].minutesAsleep)
    }

    @Test
    fun `a meal is sent with its slot's clock time and its macros`() {
        val body = nutritionLogBody(
            FoodEntry(
                id = 7,
                name = "Chicken salad",
                dateEpochDay = 0,
                mealType = MealType.Lunch,
                portionAmount = 1.0,
                portionUnit = "serving",
                calories = 420,
                proteinG = 38,
                carbsG = 12,
                fatG = 24,
            ),
            dayStartMillis = 0L,
        )

        assertTrue(body.contains("\"foodDisplayName\":\"Chicken salad\""))
        assertTrue(body.contains("\"mealType\":\"LUNCH\""))
        // Lunch is midday, so the interval starts twelve hours into the day.
        assertTrue(body.contains("\"startTime\":\"1970-01-01T12:00:00Z\""))
        assertTrue(body.contains("\"kcal\":420"))
        assertTrue(body.contains("\"nutrient\":\"PROTEIN\""))
        assertTrue(body.contains("\"grams\":38"))
        // Nothing was measured for the three, so nothing is asserted about them.
        assertFalse(body.contains("DIETARY_FIBER"))
    }

    @Test
    fun `fiber and sugar ride the nutrients array and sodium converts to grams`() {
        val packet = FoodEntry(
            name = "Cheese crackers",
            mealType = MealType.Snacks,
            portionAmount = 30.0,
            portionUnit = "g",
            calories = 150,
            proteinG = 3,
            carbsG = 18,
            fatG = 8,
            nutrients = Nutrients(fiberG = 2, sugarG = 4, sodiumMg = 480),
        )
        val body = nutritionLogBody(packet, dayStartMillis = 0L)

        assertTrue(body.contains("\"nutrient\":\"DIETARY_FIBER\""))
        // `SUGAR`, the reference's name — `TOTAL_SUGARS` is not one, and failed the whole meal.
        assertTrue(body.contains("\"nutrient\":\"SUGAR\""))
        // The array carries grams, and sodium is the app's one milligram figure.
        assertTrue(body.contains("\"nutrient\":\"SODIUM\""))
        assertTrue(body.contains("\"grams\":0.48"))

        // The fallback the push retries with drops exactly those three and nothing else.
        val pinned = nutritionLogBody(packet, dayStartMillis = 0L, micronutrients = false)
        assertFalse(pinned.contains("DIETARY_FIBER"))
        assertFalse(pinned.contains("SODIUM"))
        assertTrue(pinned.contains("\"nutrient\":\"PROTEIN\""))

        // A quick add measures none of the three, so it already sends the fallback's body —
        // which is what makes the retry a no-op for everything but a scanned packet.
        val quickAdd = packet.copy(nutrients = Nutrients(fiberG = 0, sugarG = 0, sodiumMg = 0))
        assertEquals(
            nutritionLogBody(quickAdd, 0L, micronutrients = false),
            nutritionLogBody(quickAdd, 0L),
        )
    }

    @Test
    fun `water goes out as millilitres and deletions as a names array`() {
        assertTrue(hydrationLogBody(millilitres = 1500, dayStartMillis = 0L).contains("\"milliliters\":1500"))
        assertEquals("""{"names":["a","b"]}""", batchDeleteBody(listOf("a", "b")))
    }

    @Test
    fun `the created data point's name is what gets linked`() {
        // `create` answers with an Operation; the point is its `response`.
        val operation = """
            {"name":"operations/op-1","done":true,"response":{
            "@type":"type.googleapis.com/google.devicesandservices.health.v4.DataPoint",
            "name":"users/me/dataTypes/nutrition-log/dataPoints/p1"}}
        """.trimIndent()
        assertEquals("users/me/dataTypes/nutrition-log/dataPoints/p1", parseCreatedName(operation))
        // A bare point still links.
        assertEquals(
            "users/me/dataTypes/hydration-log/dataPoints/p2",
            parseCreatedName("""{"name":"users/me/dataTypes/hydration-log/dataPoints/p2"}"""),
        )
        // A pending operation names only itself, and that is no handle to delete a point by.
        assertNull(parseCreatedName("""{"name":"operations/op-2","done":false}"""))
        assertNull(parseCreatedName("{}"))
        assertNull(parseCreatedName("nope"))
    }

    @Test
    fun `a page maps to entries the diary can hold`() {
        val page = parseExercisePage(PAGE)

        assertEquals("page-2", page.nextPageToken)
        // The third point has no interval: an undated workout can't be placed in a dated diary.
        assertEquals(2, page.items.size)

        val run = page.items[0]
        assertEquals("users/me/dataTypes/exercise/dataPoints/abc123", run.remoteName)
        assertEquals(ExerciseType.Run, run.type)
        assertEquals("Morning Trail Run", run.name)
        // activeDuration wins over end - start, which would have said 35.
        assertEquals(30, run.minutes)
        assertEquals(380, run.burnedKcal)

        val unknown = page.items[1]
        // No activeDuration, so the interval is the fallback; the offset has to be honoured.
        assertEquals(45, unknown.minutes)
        // An unrecognised type still lands, and kcal came through as a quoted string.
        assertEquals(ExerciseType.Other, unknown.type)
        assertEquals("Other", unknown.name)
        assertEquals(210, unknown.burnedKcal)
    }

    @Test
    fun `a workout carries the steps the watch counted`() {
        val page = parseExercisePage(PAGE)

        // metricsSummary.steps, quoted on the wire.
        assertEquals(6200, page.items[0].steps)
        // An unrecognised activity reports none and is assumed to have taken none, so it can't
        // subtract a day's real walking. estimatedSteps' own cases are covered in StepsTest.
        assertEquals(0, page.items[1].steps)
    }

    @Test
    fun `an imported workout reaches the diary with every measured field intact`() {
        val page = parseExercisePage(PAGE)
        val entry = page.items[0].toExerciseEntry()

        // The one number here nobody guessed: dropping it lets addEntry re-derive 3000 from the
        // MET estimate, and stepsCreditKcal() then credits the difference a second time.
        assertEquals(6200, entry.steps)
        assertEquals(380, entry.burnedKcal)
        assertEquals(30, entry.minutes)
        assertEquals(ExerciseType.Run, entry.type)
        assertEquals("Morning Trail Run", entry.name)
        // The local day the interval starts, not the raw instant. Asserted through the same
        // conversion rather than a literal, because the answer moves with the test JVM's zone.
        assertEquals(epochDayOf(page.items[0].timeMillis), entry.dateEpochDay)
    }

    @Test
    fun `a steps roll-up keeps the days that walked and drops what it cannot date`() {
        assertEquals(mapOf(localDay(2026, 4, 20) to 8412), parseStepsRollup(STEPS_ROLLUP))
        assertEquals(emptyMap<Long, Int>(), parseStepsRollup("not json"))
    }

    @Test
    fun `a roll-up asks for whole local days, months counted from one`() {
        val day = localDay(2026, 4, 20)
        val body = dailyRollUpBody(fromDay = day, toDayExclusive = day + 14)

        assertTrue(body.contains(""""start":{"date":{"year":2026,"month":4,"day":20}}"""))
        assertTrue(body.contains(""""end":{"date":{"year":2026,"month":5,"day":4}}"""))
        assertTrue(body.contains(""""windowSizeDays":1"""))
        assertEquals(
            "https://health.googleapis.com/v4/users/me/dataTypes/heart-rate/dataPoints:dailyRollUp",
            dailyRollUpUrl(HEART_RATE),
        )
    }

    @Test
    fun `a body that isn't a page yields nothing rather than throwing`() {
        assertEquals(0, parseExercisePage("not json").items.size)
        assertEquals(0, parseWeightPage("not json").items.size)
        assertNull(parseExercisePage("""{"dataPoints":[]}""").nextPageToken)
    }

    @Test
    fun `a weight page maps grams to kilograms and drops broken readings`() {
        val page = parseWeightPage(WEIGHTS)

        assertEquals(1, page.items.size)
        assertEquals(72.4, page.items[0].weightKg, 0.001)
        assertEquals("users/me/dataTypes/weight/dataPoints/w1", page.items[0].remoteName)
    }

    @Test
    fun `the list url carries an encoded AIP-160 window and paginates`() {
        val url = dataPointsUrl(HealthDataType.Exercise, sinceMillis = 0L)

        assertTrue(url.startsWith("https://health.googleapis.com/v4/users/me/dataTypes/exercise/dataPoints"))
        assertTrue(url.contains("pageSize=25"))
        assertTrue(!url.contains("pageToken"))
        // A session other than sleep can only be filtered on its civil start: the device's wall
        // clock, no offset. Which wall-clock time 0L is moves with the test JVM's zone.
        assertTrue(
            Regex("""exercise\.interval\.civil_start_time >= "\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d"$""")
                .matches(URLDecoder.decode(url.substringAfter("filter="), "UTF-8")),
        )

        // Every field carries its type's prefix — a bare `interval.start_time` is a 400.
        assertTrue(
            URLDecoder.decode(dataPointsUrl(HealthDataType.Weight, 0L), "UTF-8")
                .contains("filter=weight.sample_time.physical_time >= \"1970-01-01T00:00:00Z\""),
        )
        // Sleep is the one session filtered on its end.
        assertTrue(
            URLDecoder.decode(dataPointsUrl(HealthDataType.Sleep, 0L), "UTF-8")
                .contains("filter=sleep.interval.end_time >= \"1970-01-01T00:00:00Z\""),
        )
        assertTrue(dataPointsUrl(HealthDataType.Exercise, 0L, pageToken = "a b").contains("pageToken=a+b"))
    }

    @Test
    fun `a heart roll-up keeps the day's mean and lowest beat`() {
        val day = localDay(2026, 4, 20)
        assertEquals(
            mapOf(day to HeartDay(dateEpochDay = day, averageBpm = 68, minBpm = 52)),
            parseHeartRollup(HEART_ROLLUP),
        )
    }

    @Test
    fun `heart samples fold to one row per local day`() {
        // Anchored to local day starts rather than to the fixture's UTC instants: which local day
        // an instant lands on moves with the test JVM's zone, and grouping by local day is the
        // one thing this is asserting.
        val day = 20_000L
        val samples = listOf(
            RemoteHeart("h1", epochDayStartMillis(day) + 6 * 60 * 60 * 1000L, 62),
            RemoteHeart("h2", epochDayStartMillis(day) + 18 * 60 * 60 * 1000L, 74),
            RemoteHeart("h3", epochDayStartMillis(day + 1) + 9 * 60 * 60 * 1000L, 90),
        )

        val byDay = aggregateHeartByDay(samples)

        assertEquals(2, byDay.size)
        // The mean of 62 and 74, and the day's lowest reading exactly as measured.
        assertEquals(68, byDay.getValue(day).averageBpm)
        assertEquals(62, byDay.getValue(day).minBpm)
        assertEquals(day, byDay.getValue(day).dateEpochDay)
        // A single-sample day is its own average and its own minimum.
        assertEquals(90, byDay.getValue(day + 1).averageBpm)
        assertEquals(90, byDay.getValue(day + 1).minBpm)
        assertEquals(emptyMap<Long, HeartDay>(), aggregateHeartByDay(emptyList()))
    }

    /**
     * The live 400 that had one tap fire 278 identical doomed requests. Pinned verbatim, because
     * the whole breaker turns on recognising it — see [HealthResponse.AccountNotLinked].
     */
    @Test
    fun `an unlinked account is told apart from a rejected body and from a server failure`() {
        val notLinked = """
            {"error":{"code":400,"message":"The account is not linked to Google Health.",
            "status":"FAILED_PRECONDITION","details":[{"@type":"type.googleapis.com/google.rpc.ErrorInfo",
            "reason":"ACCOUNT_NOT_LINKED","domain":"health.googleapis.com"}]}}
        """.trimIndent()
        assertEquals(HealthResponse.AccountNotLinked, errorResponse(400, notLinked))

        // A 4xx the body is responsible for: the one case worth a second, smaller attempt.
        assertEquals(
            HealthResponse.Rejected,
            errorResponse(400, """{"error":{"message":"Invalid value at nutrients[3].name"}}"""),
        )
        assertEquals(HealthResponse.Rejected, errorResponse(404, ""))

        // A 5xx or an answerless response is neither: only a later sync can help.
        assertEquals(HealthResponse.Failed, errorResponse(500, ""))
        assertEquals(HealthResponse.Failed, errorResponse(503, """{"error":{"message":"overloaded"}}"""))

        // The reason outranks the status code — the account is unlinked however it is reported.
        assertEquals(HealthResponse.AccountNotLinked, errorResponse(503, """{"reason":"ACCOUNT_NOT_LINKED"}"""))
    }

    @Test
    fun `remote activity names fall back to Other instead of being dropped`() {
        assertEquals(ExerciseType.Run, exerciseTypeOf("TRAIL_RUNNING"))
        assertEquals(ExerciseType.Walk, exerciseTypeOf("HIKING"))
        assertEquals(ExerciseType.Cycle, exerciseTypeOf("BIKING_STATIONARY"))
        assertEquals(ExerciseType.Hiit, exerciseTypeOf("HIGH_INTENSITY_INTERVAL_TRAINING"))
        assertEquals(ExerciseType.Strength, exerciseTypeOf("WEIGHTLIFTING"))
        assertEquals(ExerciseType.Other, exerciseTypeOf(null))
    }
}
