package ph.mart.healthapp.core.data.transfer

import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import ph.mart.healthapp.core.data.epochDayStartMillis

/**
 * The export as a zip of CSVs — the spreadsheet's copy of what [buildExportJson] writes for the
 * importer.
 *
 * **One-way, and it renders the same DTOs.** There is no CSV importer: a flat table cannot hold
 * [ExportExercise.sets], and a second all-or-nothing write path is not worth a format nobody
 * restores from. What it *is* worth is not drifting from the JSON, so this walks
 * [buildExport]'s own `Export*` types through their serializers rather than keeping a parallel
 * list of columns — a field added at the next schema version appears in both files from one edit.
 *
 * ponytail: no UTF-8 BOM. Sheets and LibreOffice read these correctly; Excel on Windows can
 * mangle a non-ASCII food name. Prepend `﻿` if that turns up — it is one character, and the
 * only reason it is not here is that a BOM breaks naive parsers the other way.
 */

/** RFC 4180 says CRLF, and it is also the ending Excel is happiest with. */
private const val EOL = "\r\n"

/** Millis columns that don't say so in their name. [ExportSupplement.createdAt] is the only one. */
private val MILLIS_FIELDS = setOf("createdAt")

private const val MILLIS_SUFFIX = "Millis"

/**
 * Three name-driven conversions, and they are the whole point of the format: a column reading
 * `20714` is not a date to anything that opens a CSV.
 *
 * `dateEpochDay` becomes an ISO `date`, a `*Millis` (or [MILLIS_FIELDS]) becomes a date and a
 * clock time, and `minuteOfDay` becomes the `time` it has always been. Everything else travels as
 * stored — kg and cm, the units the JSON uses, named in the header.
 */
private fun csvHeader(name: String): String = when {
    name == "dateEpochDay" -> "date"
    name == "minuteOfDay" -> "time"
    name.endsWith(MILLIS_SUFFIX) -> name.removeSuffix(MILLIS_SUFFIX)
    else -> name
}

/**
 * The cell for one field. An absent key and a JSON null both write **empty**, never `0` — the
 * diary's own em-dash rule applied to a file, and the reason a weigh-in with no recorded time
 * doesn't claim midnight.
 */
private fun csvCell(name: String, element: JsonElement?): String {
    if (element == null || element is JsonNull) return ""
    val raw = (element as? JsonPrimitive)?.content ?: element.toString()
    val text = when {
        name == "dateEpochDay" ->
            raw.toLongOrNull()?.let { isoDate(epochDayStartMillis(it)) } ?: raw
        name == "minuteOfDay" ->
            raw.toIntOrNull()?.let { clockOf(it) } ?: raw
        name.endsWith(MILLIS_SUFFIX) || name in MILLIS_FIELDS ->
            raw.toLongOrNull()?.let { if (it > 0) isoDateTime(it) else "" } ?: raw
        else -> raw
    }
    return csvField(text)
}

/** RFC 4180 quoting. Load-bearing, not defensive: a day note carries newlines, a scanned
 * supplement's `panel` carries a whole transcript, and a food is called "Rice, fried". */
private fun csvField(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }

// The default zone and Locale.US, matching `epochDayOf` on one side and NumberFormat.kt's ASCII
// rule on the other. A formatter per call: SimpleDateFormat is not thread-safe, and only the
// dated cells pay for it.
private fun isoDate(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))

private fun isoDateTime(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(millis))

private fun clockOf(minuteOfDay: Int): String =
    "%02d:%02d".format(Locale.US, minuteOfDay / 60, minuteOfDay % 60)

/**
 * One table, driven by the serializer's own descriptor rather than by a hand-kept column list.
 *
 * The header comes from the descriptor, not from the first row, so an **empty table still writes
 * its header** — a file that is one line is readable; a file that is zero bytes looks broken.
 * [json] sets `encodeDefaults`, so a defaulted field is a real column.
 */
private fun <T> csvTable(
    serializer: KSerializer<T>,
    rows: List<T>,
    skip: Set<String> = emptySet(),
): String {
    val descriptor = serializer.descriptor
    val names = (0 until descriptor.elementsCount).map { descriptor.getElementName(it) } - skip
    return buildString {
        append(names.joinToString(",") { csvField(csvHeader(it)) }).append(EOL)
        rows.forEach { row ->
            val fields = json.encodeToJsonElement(serializer, row).jsonObject
            append(names.joinToString(",") { csvCell(it, fields[it]) }).append(EOL)
        }
    }
}

/**
 * The one table the generic writer can't do: [ExportExercise.sets] is a list, and no cell holds
 * one. `workout` is the row's index in `exercises.csv` — the two files are written from the same
 * list in the same order, so that index is the join. The date rides along so the file reads on
 * its own.
 */
private fun strengthSetsCsv(exercises: List<ExportExercise>): String = buildString {
    append("workout,date,exerciseName,reps,weightKg").append(EOL)
    exercises.forEachIndexed { index, workout ->
        val date = csvCell("dateEpochDay", JsonPrimitive(workout.dateEpochDay))
        workout.sets.forEach { set ->
            append(index).append(',')
            append(date).append(',')
            append(csvField(set.exerciseName)).append(',')
            append(set.reps).append(',')
            append(set.weightKg).append(EOL)
        }
    }
}

/**
 * Fourteen files for thirteen domains — see [strengthSetsCsv] for the extra one. Photos are
 * absent here for the reason they are absent from the JSON: they are image files.
 */
fun buildExportCsvZip(data: ImportData): ByteArray {
    val export = buildExport(data)
    val bytes = ByteArrayOutputStream()
    ZipOutputStream(bytes).use { zip ->
        // The profile is one row, or a header alone when onboarding hasn't written one yet.
        zip.entry("profile.csv", csvTable(ExportProfile.serializer(), listOfNotNull(export.profile)))
        zip.entry("food_entries.csv", csvTable(ExportFoodEntry.serializer(), export.foodEntries))
        zip.entry("weight_entries.csv", csvTable(ExportWeightEntry.serializer(), export.weightEntries))
        zip.entry("measurements.csv", csvTable(ExportMeasurement.serializer(), export.measurements))
        zip.entry("water_days.csv", csvTable(ExportWaterDay.serializer(), export.waterDays))
        zip.entry(
            "exercises.csv",
            csvTable(ExportExercise.serializer(), export.exercises, skip = setOf("sets")),
        )
        zip.entry("strength_sets.csv", strengthSetsCsv(export.exercises))
        zip.entry("mood_days.csv", csvTable(ExportMoodDay.serializer(), export.moodDays))
        zip.entry("fast_sessions.csv", csvTable(ExportFastSession.serializer(), export.fastSessions))
        zip.entry("supplements.csv", csvTable(ExportSupplement.serializer(), export.supplements))
        zip.entry("supplement_days.csv", csvTable(ExportSupplementDay.serializer(), export.supplementDays))
        zip.entry(
            "blood_pressure.csv",
            csvTable(ExportBloodPressureReading.serializer(), export.bloodPressure),
        )
        zip.entry("cycle_days.csv", csvTable(ExportCycleDay.serializer(), export.cycleDays))
        zip.entry("day_notes.csv", csvTable(ExportDayNote.serializer(), export.dayNotes))
    }
    return bytes.toByteArray()
}

private fun ZipOutputStream.entry(name: String, body: String) {
    putNextEntry(ZipEntry(name))
    write(body.toByteArray())
    closeEntry()
}
