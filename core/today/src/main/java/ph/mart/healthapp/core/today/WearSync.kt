package ph.mart.healthapp.core.today

import kotlinx.serialization.json.Json

/**
 * The whole phone-to-watch contract: one data item carrying one snapshot, and two messages
 * going back the other way.
 *
 * The snapshot travels as a JSON string in a single `DataMap` key rather than as a field per
 * value. A `DataMap` of twelve loose keys would need both sides changed in lockstep for every
 * field added, whereas [decodeSnapshot] tolerates a payload from a phone the watch hasn't caught
 * up with — the two APKs update independently, and often don't.
 */
const val TODAY_PATH = "/fitpulse/today"
const val KEY_SNAPSHOT = "snapshot"

/** Watch → phone. Both carry no payload: the phone re-reads the day's state before writing, the
 * same reason `AddGlassAction` re-reads rather than trusting an action parameter. */
const val MSG_ADD_GLASS = "/fitpulse/add-glass"
const val MSG_TOGGLE_FAST = "/fitpulse/toggle-fast"

private val json = Json {
    // A field this build doesn't know about is dropped rather than thrown — an older watch
    // paired with a newer phone still shows the fields it does understand.
    ignoreUnknownKeys = true
}

/** The serializer is named rather than reified on purpose: an explicit reference is one R8 keeps
 * on its own, so neither APK needs a keep rule to survive minification. */
fun encodeSnapshot(snapshot: TodaySnapshot): String =
    json.encodeToString(TodaySnapshot.serializer(), snapshot)

/** Null on anything unparseable. A watch that crashed on a malformed item would keep crashing
 * every time the item was re-delivered, with no way for the user to clear it. */
fun decodeSnapshot(text: String): TodaySnapshot? = runCatching {
    json.decodeFromString(TodaySnapshot.serializer(), text)
}.getOrNull()
