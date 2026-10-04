# Filipino (`fil`) — the first shipped translation

**Status:** approved 2026-10-04. Next: implementation plan.

## Goal

A user whose app language is Filipino sees Filipino on every screen, notification, the widget,
the watch, and in every AI reply the app writes for them. English is unchanged, byte for byte.

## Why Filipino first

The package is `ph.mart`, and Filipino writes decimals with `.` — so the ASCII round trip
`DECISIONS.md` → *Localization* pins (`formatOneDecimal` → `keepDigits` → `toDoubleOrNull`) holds
as it is. A comma-decimal language would first need a `NumberFormat` parser at the three input
sites; this one does not.

## 1. Kotlin copy becomes a `Phrase`

The pure-function-with-a-JVM-test exemption ends. One value type, in `:core:data` (the leaf every
feature sees, already holding a `strings.xml`):

```kotlin
data class Phrase(@StringRes val id: Int, val args: List<Any> = emptyList(), val quantity: Int? = null)
fun Phrase.resolve(res: Resources): String
```

`quantity != null` means `id` is a `@PluralsRes`. An argument that is itself a `Phrase` is resolved
first, which covers "Last: {load} · {n sets}".

Converted (every production caller is a composable; each resolves at the call site):

| Function | Module | Becomes |
|---|---|---|
| `insightFor`, `earnedInsightLine` | `:core:data/insight` | `Phrase?` |
| `goalProjectionLine` | moves `:core:designsystem` → `:core:data/progress` | `Phrase`; the caller pre-formats the date, as it already pre-formats `goalWeightLabel` |
| `summarize`, `trendWord`, `daysAgo` | `:feature:progress` | `Phrase` |
| `captionFor` + its three helpers | `:feature:progress` | `Phrase` |
| `diaryDateLabel` | `:feature:food` | `Phrase` |
| `catchUpDateLabel` | `:feature:progress` | `Phrase` |
| the two `FoodHistoryData` labels | `:feature:food` | `Phrase` |
| `loadLabel`, `summaryLabel`, `LiftPerformance.label` | `:core:data/exercise` | `Phrase` |
| `greetingFor`, `greetingSubFor` | `:feature:home` | `@StringRes Int` |
| `Reminder.title` / `.body` | `:app/reminder` | `@StringRes titleRes` / `bodyRes`, resolved by `ReminderWorker` through its Context |

Tests move from string equality to `Phrase` equality: branch and figures stay guarded, wording
moves to `strings.xml`. `literalExceptions` shrinks to `MascotAvatar.kt` (proper names).

**Not converted** (still not copy): unit symbols, `RestTimerCard`'s `M:SS`, mascot names,
everything persisted or compared, prompts, exception messages, debug fakes.

This step is a pure refactor — English output identical.

## 2. Template-literal leak sweep

The gate's own ponytail note: a literal opening with a template (`"$n tracked"`) slips through.
One sweep of `ui/` trees and `:core:designsystem/component/` for those; convert what is copy. If
the sweep leaves the tree clean, tighten the gate to flag `"$x <lowercase word>` in those
positions; if the rule is too noisy, leave the note and record why.

## 3. Translations

- `values-fil/strings.xml` in each of the 11 modules with strings (~1,750 entries, `:wear` and the
  widget included).
- Every `<plurals>` carries `one` and `other` (Filipino's CLDR categories).
- `docs/glossary-fil.md` — the term list every entry follows: Google-Android register, imperative
  `I-` verbs ("I-save", "Kanselahin"), "Mga Setting", Protein → "Protina", Fat → "Taba", "Carbs"
  kept. Untranslated: FitPulse, Health Connect, Google Health, Gemini, unit symbols, mascot names.
- Dates need no change: `SimpleDateFormat`/`DateFormatSymbols` follow the locale and fil's order
  ("Okt 4, 2026") matches the patterns in use.
- **Not reviewed by a native speaker.** Written by Claude from the glossary; say so in the commit.

## 4. Choosing the language

- `:app/res/xml/locales_config.xml` listing `en` and `fil`, referenced by `android:localeConfig`.
  Hand-written rather than `generateLocaleConfig`, so a library's own translations can't offer a
  language the app doesn't speak.
- Settings gains a "Language" row: `Settings.ACTION_APP_LOCALE_SETTINGS` on API 33+, the device
  `ACTION_LOCALE_SETTINGS` below.
- No in-app picker below 13 — that needs AppCompat, a dependency for one row.
- The watch follows the watch's own language.

## 5. AI replies follow the app language

- `replyLanguageLine()` in `Ai.kt`: empty for English, otherwise one line telling the model to
  write every user-read sentence in the app's language (`Locale.getDefault()`, display name in
  English). Read **per request**, not at model construction — repositories are Koin singletons and
  outlive a per-app language change.
- Added to: coach (per turn, not the cached system prompt), daily insight, quick log, meal ideas,
  photo recognition, voice meal parse.
- Not added to: label scan and supplement scan (transcription), exercise and recipe parse (names
  echo the user's text; exercise tokens are compared).
- The prompts themselves stay English.
- A cached insight in the old language stays until the next day's line.
- **Risk to check in the plan:** quick log matches a sentence's foods against label-saved foods.
  If that match is by name on the phone, a Filipino-named row must still match an English-named
  saved food.

## 6. Gate, tests, verification

- `checkTranslations`, a root task beside `checkUiLiterals`, added to CI and CLAUDE.md's
  Build & check. For each module in `localizedModules`, fails on:
  - a key in `values/` missing from `values-fil/`, or the reverse (`translatable="false"` skipped);
  - a string whose placeholder multiset differs (`%1$s`, `%d`, `%.1f`…) — a mismatch is a runtime
    crash, not a typo;
  - a `<plurals>` without `one` and `other`.
- From then on every new string needs its fil entry; CLAUDE.md says so.
- Existing JVM tests, converted per §1, run green.
- Emulator: `adb shell cmd locale set-app-locales ph.mart.healthapp --locales fil`; screenshot the
  four tabs, the coach, the quick-log sheet, Settings and the widget; fix overflow from text running
  ~25% longer. Onboarding needs a fresh install, which needs the user's go-ahead.

## 7. Build order (one commit each)

1. `Phrase` refactor (English unchanged)
2. Template-literal sweep
3. `checkTranslations` + locale config + Settings row
4. Translations, module by module
5. AI language line
6. Emulator fixes
7. Docs — CLAUDE.md *Localization*, FEATURES.md, DECISIONS.md entries (the exemption retired,
   `Phrase`, the AI language line, the hand-written locale config, the gate)

## Out of scope

In-app language picker below Android 13 · RTL · a second language · native-speaker review.
