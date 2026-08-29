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
  `exercise`. Two non-domains sit beside them: `network/` (a `NetworkMonitor`
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
- **The recap's weight cell goes blank when the last weigh-in predates the window.**
  `trendVsSevenDaysAgo()` anchors to the latest *entry*, not to today, so without that guard a
  card headed "Last 7 days" would report a delta between two entries from two months ago.
- **Reminders never touch a `:feature:*` module.** The Profile switches are a
  plain Room write; `FitPulseApplication` reconciles WorkManager off
  `ProfileRepository.observeProfile()`.

## Backlog

- Final mascot illustration (the geometric placeholder "Bibo" is used throughout).

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
