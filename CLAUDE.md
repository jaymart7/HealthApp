# CLAUDE.md — FitPulse

Persistent context for every Claude Code session on this project. Read this
before writing any code. If something here conflicts with what you find in
the codebase, stop and ask — don't silently pick one.

## What this project is

Android app, Kotlin + Jetpack Compose, Material 3. Two pillars: body
tracking (weight/measurements/progress photos) and nutrition tracking
(calories/macros with AI photo food logging). Offline-first for core data
(Room). AI features require network (Firebase AI Logic / Gemini).

The app is built and shipping — all nine build phases are done (`BUILD_PLAN.md`
is history, kept for provenance). **The shipped code is the source of truth.**
The Claude Design prototype in `claude-design/project/` is still the reference
for layout, copy, and interaction on anything *not yet built*, and
`claude-design/project/COMPONENTS.md` is its component inventory — prototype-era,
so its own "Deferred" list is stale. For what components actually exist, read
`:core:designsystem`.

## Stack

- **Package:** `ph.mart.healthapp`
- **DI:** Koin · **State:** Orbit MVI — use skill `/orbit-mvi-screen-split`
  for every screen
- **Persistence:** Room (`androidx.room3`) + bundled SQLite
- **Camera:** CameraX (`androidx-camera-compose`) + `androidx-exifinterface`
- **Navigation:** Navigation 3 — use skill `/navigation-3`
- **Predictive back:** `androidx.navigationevent` (see below)
- **Also in use:** ML Kit barcode scanning, Firebase AI + App Check,
  WorkManager (reminders), Open Food Facts over `HttpURLConnection` +
  kotlinx.serialization — no HTTP client dependency, don't add one
- **Google Health API** (`health.googleapis.com/v4`, the Fitbit Web API's
  successor — *not* Health Connect, *not* Google Fit): REST over the same
  `HttpURLConnection`, OAuth via `play-services-auth`'s Authorization API

Versions live in `gradle/libs.versions.toml`. Read it — never restate or
duplicate versions here or in a parallel catalog.

## Module map

```
:app                    Application, MainActivity, nav host, ph.mart.healthapp.reminder
:core:designsystem      theme (Color/Theme/Type) + every shared component
:core:data              Room, repositories, all persistence
:core:camera            CameraX wrapper
:core:navigation        route types
:feature:onboarding | home | food | progress | profile
```

### Predictive back

Back must step through sub-levels, not blow past them. Any new bottom sheet,
swap-in sub-view, or multi-state flow wires its own `NavigationEventHandler` /
`NavigationBackHandler` (`androidx.navigationevent:navigationevent-compose`) —
never rely on default Activity back. Existing cases show the shape: a sheet's
back closes the sheet, not the screen under it; a date-picker swapped into a
sheet returns to the sheet's fields, one level; onboarding back steps one step
and preserves data; the photo flow branches per state (Capture exits, Analyzing
cancels, Confirmation returns to Capture with a discard confirm if edited).

## Non-negotiable constraints

- **`Color.kt` is frozen.** `:core:designsystem/theme/Color.kt` is a complete
  Material Theme Builder export — light + dark, standard/medium/high contrast.
  Never regenerate it, never change a value. If a token seems missing, check
  the file again before assuming.
- **Dynamic color (Material You) is disabled.** Always the fixed palette,
  never derived from wallpaper.
- **Never hardcode a hex inline.** Every color reads from the theme — that is
  what lets `AppTheme` swap in the medium/high-contrast schemes from
  `UiModeManager.getContrast()` (API 34+) without touching component code.
- **Semantic color assignments are fixed:** Protein = `primary`, Carbs =
  `tertiary`, Fat = `secondary` — identical in every macro bar, chart, and
  legend across the whole app. AI/insight accent = `tertiaryContainer`
  background + `onTertiaryContainer` text, and it's the *only* place
  `tertiaryContainer` is used as a card background.
- **Trend arrows** (weight, etc.): `onSurfaceVariant` for neutral, `primary`
  for on-track, `error` only for genuinely off-track. Direction depends on
  the user's goal — never default to green-for-loss/red-for-gain.
- **Typography:** Poppins for Display/Headline/Title, Inter for Body/Label.
  Numeric values (calories, weight, macros) use tabular figures so they don't
  jitter on update.
- **Spacing scale:** `4 / 8 / 12 / 16 / 24 / 32 / 48` dp. Screen horizontal
  padding `16dp`. Vertical gap between cards `12dp`. No other spacing values.
- **Feature modules** (`:feature:*`) have internal `ui` / `di` packages only —
  no `data` package, since persistence lives entirely in `:core:data`.
  Cross-feature references stay plain Strings/IDs, never a direct import of
  another feature's type. Soft delete only, no hard deletes.
- **`:core:data` module boundary is load-bearing, not conventional.** Room is
  `implementation`-scoped in `:core:data`'s build file only — never `api`, and
  never referenced from any `:feature:*` build file. DAOs and `@Entity` classes
  live in `<domain>/local/`; the repository interface sits at the domain root
  (e.g. `food/FoodRepository.kt`) with `local/` and `di/` beneath it.
  Repository interfaces are the only public surface; their Room-backed `Impl`
  classes are `internal`. Each domain's `di/` module binds interface → impl,
  reached from features via `koinViewModel()`. Feature ViewModels take the
  repository interface by constructor injection and never touch `AppDatabase`,
  a DAO, or an Entity. Domains: `food`, `profile`, `progress`, `water`,
  `exercise`, `mood`, `health`. Two non-domains sit beside them: `network/` (a `NetworkMonitor`
  recheck, not a listener) and `streak/`, which is pure derivation — no table,
  no repository, no schema.
- **Calorie/macro math is Mifflin–St Jeor**, computed live from profile inputs
  (age, sex, height, weight, activity, goal) — never hardcoded, never cached
  separately from the fields that display it. Safety floor: 1200 kcal (female) /
  1500 kcal (male), clamped with a warn-don't-block UX if the user edits below
  it manually.
- **Offline-first.** Room is the source of truth for all core data. AI features
  degrade gracefully to a manual-entry path when offline.

## Decisions that aren't obvious from the code

Keep these — each one was argued once and is easy to "fix" back into a bug.

- **Streak:** a grace day (today still empty counts back from yesterday), and
  badges are earned off the *best* run, so breaking a streak never un-earns one.
  A day counts if *anything* was logged — food, water, weigh-in, or exercise.
- **No streak celebration toast.** The badge lighting up is the reward;
  announcing it would need persisted "already celebrated" state.
- **`budgetKcal()` is the only place burned calories fold into the day**, and
  `Profile.addExerciseToBudget` (default on) can switch that credit off —
  `calculateDailyTargets()` already applies an activity multiplier, so crediting
  a workout on top of it can count the same training twice. The Mifflin–St Jeor
  target itself is never touched.
- **Estimated burn stops re-estimating** (MET × kg × hours) once the user edits
  the kcal field by hand.
- **Diary date navigation:** forward stepping stops at today (there are no
  planned meals), and system back from a past day returns to today rather than
  leaving the tab.
- **The barcode route carries the day** (`BarcodeScanRoute(dateEpochDay)`) so a
  scan lands on the day you're looking at; photo capture stays today-only,
  because the FAB launches it outside the diary's date context.
- **The today-only repository overloads are deliberate** — Home and the streak
  genuinely mean today, so don't collapse them into the dated ones.
- **The diary's top field is a local filter over logged entries**, not a
  database search. Database search is `FoodSearchRepository`/`FoodSearchPanel`.
- **`Profile.darkThemeOn` is nullable and null means follow the device.** A plain `false`
  default would force light on a phone already in dark mode; the Profile switch resolves it
  with `darkThemeOn ?: isSystemInDarkTheme()`, the same expression `MainActivity` uses to pick
  the scheme. Contrast stays system-driven.
- **The weekly recap window is rolling-7-ending-today**, not a calendar week — a calendar week
  reports a half-empty Monday. The card is *hidden* when nothing was logged in the window
  rather than rendering zeros, and its "days logged" uses the streak's four-domain definition
  while its calorie average uses food days only — the two denominators can differ, so the card
  says which is which.
- **Mood is not a streak domain.** The streak's definition of a logged day stays food, water,
  weigh-in, exercise — a two-tap reflection holding a 40-day run would cheapen it, and folding
  mood in retroactively lengthens past runs. So `MoodRepository` has no `observeLoggedDays()`,
  `loggedDays()` is untouched, and Home's `isDayOne` ignores mood too (the mood card only
  appears once something real has been logged, which is the right order anyway).
- **In `mood_day`, `0` means "not tapped", never a zero score.** A day with a mood and no energy
  is a first-class row — that is what keeps both columns non-null and stops the card demanding
  two taps to record one. The averages skip zeros and keep *separate* denominators per series,
  so a mood-only week reports a mood average and a blank energy one. The two writes are partial
  upserts (`ON CONFLICT … DO UPDATE SET mood`) precisely so a mood tap can't wipe that day's
  energy.
- **`List<MoodDay>.inRange()` anchors to today; `List<WeightEntry>.inRange()` anchors to the
  latest entry.** The difference is deliberate: a mood chart headed "1M" must show the last 30
  days with their gaps intact, while a weight chart re-centres on the data it has. The Mood tab
  therefore hands its chart the *window bounds*, not just the list — the series is sparse, and
  the x-position of a bar is its date.
- **`SegmentedToggle` splits its width evenly until a pill would fall below 64dp, then scrolls.**
  64dp is what five pills already left on a 360dp screen, so every caller with five or fewer
  options (the unit toggles, the four `ChartRange` pills) renders exactly as it did before the
  floor existed — the branch only ever fires for the six Progress tabs, and on very narrow
  screens, where scrolling replaces clipping. The Progress labels stay trimmed ("Nutrition" reads
  "Food", "Measurements" reads "Body"); a seventh tab costs nothing but scroll distance now, so
  never shorten a label further to avoid one.
- **The recap's weight cell goes blank when the last weigh-in predates the window.**
  `trendVsSevenDaysAgo()` anchors to the latest *entry*, not to today, so without that guard a
  card headed "Last 7 days" would report a delta between two entries from two months ago.
- **A saved meal is a snapshot, not a live link.** Saving copies the section's entries into
  `saved_meal`/`saved_meal_item`; editing or deleting the original diary rows never touches it,
  and deleting the saved meal never touches what was logged from it. Re-logging always writes
  fresh entries stamped with the diary's *selected* day. The panel shows the newest
  `MAX_SAVED_MEALS` only, and saved meals stay out of the data export for the same reason
  favorites do — convenience data, not history.
- **Reminders never touch a `:feature:*` module.** The Profile switches are a
  plain Room write; `FitPulseApplication` reconciles WorkManager off
  `ProfileRepository.observeProfile()`.
- **The home-screen widget lives in `:app`, for the same reason reminders do** — a widget is a
  system surface, not a screen. It reads the repository interfaces directly (no ViewModel; there
  is no Compose lifecycle to hold one) and gets them from Koin's global context via
  `KoinComponent`, the same trick `ReminderWorker` uses. Three things it does differently from
  the app, all forced by RemoteViews rather than chosen: Home's calorie *ring* becomes a linear
  bar (Glance cannot draw arcs), the text uses the system face (Glance has no custom fonts, and
  no tabular figures — nothing on the widget animates, so there is nothing to jitter), and the
  card reads `surface` instead of `AppCard`'s `surfaceContainerLow`, which Glance's
  `ColorProviders` has no slot for. `Profile.darkThemeOn` *is* honored — null hands Glance both
  schemes and lets the system pick, an explicit choice pins both slots — but contrast stays
  Standard-only, since `UiModeManager.getContrast()` has no Glance equivalent. `lightScheme`
  and `darkScheme` in `Theme.kt` are public for exactly this one caller.
- **The widget's `updatePeriodMillis` is about midnight, not freshness.** Every in-app change is
  pushed by `FitPulseApplication`'s collector the moment Room emits. But the today-only
  repository overloads resolve `todayEpochDay()` when their flow is *built*, so a Glance session
  that spans midnight would keep reporting yesterday; the 30-minute tick is what restarts it.

### Google Health

The four requested scopes are Restricted, so the app is capped at 100 users until it passes
OAuth App Verification plus an annual CASA/OWASP-ASVS assessment. Everything below exists
because of that, not because it was the nicest design available.

- **No refresh token, no client secret, no credentials file on the device.** Google holds the
  grant; `GoogleHealthAuth.authorize()` mints a fresh ~1-hour access token silently on every
  later call, and that token lives in one `@Volatile` field in `HealthSyncRepositoryImpl` —
  never Room, never SharedPreferences. A `credentials.json` is not needed and must never be
  committed; Play Services resolves the OAuth client from package name + signing certificate.
- **"Connected" is never a stored flag.** It is whatever `authorize()` says right now. A local
  boolean would quietly become a lie the moment someone revoked access from
  myaccount.google.com, and the screen showing it is exactly the screen that must not lie.
- **`health_link` is the whole of sync bookkeeping** — no cursor table, no sync-state row. The
  remote resource name is its primary key (so re-syncing an overlapping window cannot duplicate
  anything), `MAX(remoteTimeMillis)` per type *is* the cursor (derived, so a failed sync cannot
  advance past data it never wrote), and `pushed` separates "delete what we imported" from
  "delete what we sent". It stays out of the data export for the same reason saved meals do: a
  restored backup on another device has no relationship to those remote names.
- **Steps are the one type that doesn't ride `health_link`.** The API reports intra-day buckets
  and FitPulse stores a daily total, so there is no one-point-to-one-row relationship for a link
  to record. `MAX(date)` in `step_day` is the cursor instead — still derived from rows actually
  written, which is the property that made the link table's cursor safe. Consequences worth
  keeping: the window is *day-aligned* (`epochDayStartMillis(latest - 1)`, not a millisecond
  offset), because a mid-day boundary would return a partial day; each re-queried day is summed
  and **replaced**, so a re-sync is idempotent and a revised bucket self-corrects; nothing is
  written until every page lands, so a half-read window can't replace a good total with a
  fragment; and Profile's "N items imported" doesn't count step days.
- **`step_day.burnedKcal` is computed at import and scaled at read.** Stored, not recomputed —
  the same rule `exercise_entry` follows, so a later weigh-in can't rewrite what a past day
  burned. It prices the day's *whole* step count at one weight (latest weigh-in, else the
  profile's); `stepsCreditKcal()` then subtracts the steps a logged workout already claims and
  scales the stored figure down proportionally. That subtraction is why `exercise_entry` carries a
  `steps` column at all: the watch's own `metricsSummary.steps` for an imported session, and
  `estimatedSteps()` — Walk/Run/HIIT only — for one logged by hand.
- **Steps ride `addExerciseToBudget`; they do not get a second switch.** `budgetKcal()` is still
  the only fold-in point and is unchanged: callers now pass `dayBurnedKcal(exercise, steps)`. What
  this does *not* correct for is the walking already priced into `calculateDailyTargets()`'s
  activity multiplier — the existing switch is the user's answer to that, and the upgrade path (a
  baseline step count per `ActivityLevel`) is marked in `Steps.kt`.
- **Steps are not a streak domain and not exported**, same reasoning as sleep and `health_link`
  respectively. `step_day` is import-only telemetry with no manual write path; `StepsRepository`
  is read-only and has no `observeLoggedDays()`.
- **Every window is re-queried one day behind the cursor.** Watches sync hours late; the primary
  key makes the overlap free. First sync backfills 30 days — asking for only what's needed is
  the data-minimisation answer on the verification form, not a performance tweak.
- **Imported workouts are ordinary `exercise_entry` rows**, so `budgetKcal()` and the streak
  pick them up with no special case. Imported weigh-ins *skip* days that already have an entry
  rather than replacing them — the typed number is the one the user chose to record.
- **Water is only pushed once the day is settled** (`dateEpochDay < today`). A day's row holds a
  running count, and patching the remote point on every glass tap costs far more code than
  letting today go out on tomorrow's sync.
- **A meal deleted from the diary is deleted from Google Health on the next sync.** Otherwise
  "delete" would mean something different on each side.
- **Sleep is not a streak domain**, same reasoning as mood: a watch recording sleep while its
  owner ignores the app is not "you logged something". `sleep_day` lives under `health/` because
  FitPulse cannot measure sleep, so there is no manual write path — and Home's card is *hidden*
  when there's no night rather than rendering a zero. Progress's Sleep tab windows the series
  **anchored to today**, like mood and unlike weight: a sparse series headed "1M" has to show the
  last 30 days with their gaps intact, not the 30 days around whenever the watch last synced.
- **The disclosure screen is a screen, not a settings row.** It renders from
  `HealthDisclosurePanel` in `:core:designsystem` because onboarding (step 5 of 6) and Profile →
  Connections both show it, and `connect()` is the only path from it to Google's consent prompt.
  It must stay in the normal flow, carry nothing unrelated, and name each scope's purpose.
- **Onboarding's health step sits before Confirm, not after.** Finishing onboarding writes the
  profile, and `AppRoot` swaps the whole wizard out the moment that lands.
- **Two ViewModels, not one shared.** `:feature:onboarding` and `:feature:profile` each own their
  slice of `HealthSyncRepository`; `:feature:*` modules never import each other, and only the
  disclosure UI is genuinely common.

## Backlog

- Final mascot illustration (the geometric placeholder "Bibo" is used throughout).
- Google Health: verification is *not* done. Needs the Cloud project's consent screen branded
  for FitPulse, an Android OAuth client (package + debug **and** release SHA-1) in that same
  project, per-scope justifications submitted, and a CASA Letter of Validation. Note that
  `app/google-services.json` points at a different project (`moviefied-3d48b`) than the OAuth
  credentials that were supplied — pick one project and confirm which consent screen the first
  `authorize()` actually raises.
- Google Health weight parsing reads the timestamp from `sampleTime.physicalTime` with a flat
  `physicalTime` fallback; pin it once a live response has been captured.
- `parseStepsPage` reads the bucket count from `count`, then `steps`, then `delta`, for the same
  reason and with the same fix: pin it to one field once a live response has been captured.

## Composable structure & previews

- **File breakdown:** each screen's composable lives in its feature module's
  `ui/` package as `ScreenName.kt`; sub-composables go in a sibling
  `ui/components/`. Don't leave a 400-line composable with five nested private
  functions in one file. Follow whatever `/orbit-mvi-screen-split` prescribes —
  don't run two conventions in parallel.
- **Shared vs. screen-specific placement is not optional.** Used in ≥2 screens
  (`FoodItemRow`, `AIChip`, `MascotAvatar`, `WaterGlassRow`, `CalendarPanel`) →
  `:core:designsystem`, never duplicated into a feature. One screen only → that
  feature's own `ui/components/`.
- **Every screen and component composable gets a `@PreviewLightDark`** (not two
  hand-written `@Preview`s) wrapped in `Surface` using the app theme — both
  modes are fully specified, and one annotation makes it hard to silently skip
  one. Preview meaningfully different variants too (e.g. `AIChip`'s `default`
  vs `onAccent`).
- **Dialogs and bottom sheets need a preview wrapper** — a `Box` with a scrim
  behind the `Surface`, or they render invisible in isolation.

## Working agreement

- **Scope one feature per session.** Don't let a session sprawl.
- **Propose architecture and flag concerns before writing code.** If something
  looks wrong for Android, say so before implementing it literally.
- **Never create a branch.** Work on `main` and commit there directly once the
  implementation is done and verified.
