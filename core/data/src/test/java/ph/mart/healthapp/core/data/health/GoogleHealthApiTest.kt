package ph.mart.healthapp.core.data.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.mart.healthapp.core.data.epochDayOf
import ph.mart.healthapp.core.data.epochDayStartMillis
import ph.mart.healthapp.core.data.exercise.ExerciseType
import ph.mart.healthapp.core.data.food.FoodEntry
import ph.mart.healthapp.core.data.food.MealType

/**
 * Steps arrive as intra-day buckets, so two of these land on one local day and one on the next.
 * The last has no interval and must be dropped, exactly as an undated workout is.
 */
private const val STEPS = """
{
  "dataPoints": [
    {
      "name": "users/me/dataTypes/steps/dataPoints/s1",
      "steps": {
        "interval": { "startTime": "2026-04-20T08:00:00Z", "endTime": "2026-04-20T09:00:00Z" },
        "count": 2200
      }
    },
    {
      "name": "users/me/dataTypes/steps/dataPoints/s2",
      "steps": {
        "interval": { "startTime": "2026-04-20T14:00:00Z", "endTime": "2026-04-20T15:00:00Z" },
        "count": "3100"
      }
    },
    {
      "name": "users/me/dataTypes/steps/dataPoints/s3",
      "steps": { "count": 900 }
    }
  ],
  "nextPageToken": "steps-2"
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

private const val HEART = """
{
  "dataPoints": [
    {
      "name": "users/me/dataTypes/heart-rate/dataPoints/h1",
      "heartRate": {
        "sampleTime": { "physicalTime": "2026-04-20T06:30:00Z" },
        "beatsPerMinute": 62
      }
    },
    {
      "name": "users/me/dataTypes/heart-rate/dataPoints/h2",
      "heartRate": {
        "physicalTime": "2026-04-20T18:30:00Z",
        "bpm": "74"
      }
    },
    {
      "name": "users/me/dataTypes/heart-rate/dataPoints/h3",
      "heartRate": { "beatsPerMinute": 80 }
    },
    {
      "name": "users/me/dataTypes/heart-rate/dataPoints/h4",
      "heartRate": {
        "sampleTime": { "physicalTime": "2026-04-20T20:00:00Z" },
        "beatsPerMinute": 0
      }
    }
  ],
  "nextPageToken": "heart-2"
}
"""

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
            fiberG = 2,
            sugarG = 4,
            sodiumMg = 480,
        )
        val body = nutritionLogBody(packet, dayStartMillis = 0L)

        assertTrue(body.contains("\"nutrient\":\"DIETARY_FIBER\""))
        assertTrue(body.contains("\"nutrient\":\"TOTAL_SUGARS\""))
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
        val quickAdd = packet.copy(fiberG = 0, sugarG = 0, sodiumMg = 0)
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
        assertEquals("users/me/x/1", parseCreatedName("""{"name":"users/me/x/1"}"""))
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
    fun `a steps page keeps its buckets and drops the undated one`() {
        val page = parseStepsPage(STEPS)

        assertEquals("steps-2", page.nextPageToken)
        assertEquals(2, page.items.size)
        assertEquals("users/me/dataTypes/steps/dataPoints/s1", page.items[0].remoteName)
        assertEquals(2200, page.items[0].count)
        // A count that came through as a quoted string still parses.
        assertEquals(3100, page.items[1].count)
        assertEquals(0, parseStepsPage("not json").items.size)
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
        assertTrue(url.contains("filter=interval.start_time+%3E%3D+%221970-01-01T00%3A00%3A00Z%22"))
        assertTrue(!url.contains("pageToken"))

        // A point sample filters on its own field, not on a session interval.
        assertTrue(
            dataPointsUrl(HealthDataType.Weight, 0L)
                .contains("filter=physical_time+%3E%3D+%221970-01-01T00%3A00%3A00Z%22"),
        )
        assertTrue(dataPointsUrl(HealthDataType.Exercise, 0L, pageToken = "a b").contains("pageToken=a+b"))
    }

    @Test
    fun `a heart page reads either timestamp shape and drops what it cannot place or trust`() {
        val page = parseHeartPage(HEART)

        assertEquals("heart-2", page.nextPageToken)
        // h3 has no timestamp and h4 reads zero: an unplaceable sample and a broken one.
        assertEquals(2, page.items.size)
        assertEquals("users/me/dataTypes/heart-rate/dataPoints/h1", page.items[0].remoteName)
        assertEquals(62, page.items[0].bpm)
        // The flat `physicalTime` fallback, with a bpm that came through as a quoted string.
        assertEquals(74, page.items[1].bpm)
        assertEquals(0, parseHeartPage("not json").items.size)
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
