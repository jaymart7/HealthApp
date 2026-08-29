# CLAUDE.md — FitPulse

Persistent context for every Claude Code session on this project. Read this
before writing any code. If something here conflicts with what you find in
the codebase, stop and ask — don't silently pick one.

## What this project is

Android app, Kotlin + Jetpack Compose, Material 3. Two pillars: body
tracking (weight/measurements/progress photos) and nutrition tracking
(calories/macros with AI photo food logging). Offline-first for core data
(Room). AI features require network (Firebase AI Logic / Gemini).

The design has already been fully prototyped in Claude Design across four
sessions (onboarding, photo logging, home/diary, progress/profile). The
prototype is the source of truth for layout, copy, states, and interaction
— not for architecture. `COMPONENTS.md` (pasted at the bottom of this file,
or attached separately) is the authoritative component inventory from that
prototype.

## Stack

- **Package:** `ph.mart.healthapp`
- **DI:** Koin
- **Persistence:** Room `3.0.1`
- **Camera:** `androidx-camera-compose` (CameraX) + `androidx-exifinterface`,
  for photo food logging and progress photos
- **Navigation:** Navigation 3 — use skill `/navigation-3`. Version catalog
  entries below; fold into the project's existing `libs.versions.toml`,
  don't create a parallel one.
- **Predictive back / system back:** `androidx.navigationevent` (stable
  `1.1.2`). Navigation 3 sits on top of this — use it explicitly wherever
  back needs to step through a sub-level rather than exit a screen (see
  "Predictive back requirements" below).
- **Architecture / screen pattern:** use skill `/orbit-mvi-screen-split`
  for every screen

### Version catalog additions

```toml
[versions]
nav3Core = "1.1.6"
lifecycleViewmodelNav3 = "2.12.0-alpha01"
kotlinSerialization = "2.2.21"
kotlinxSerializationCore = "1.9.0"
material3AdaptiveNav3 = "1.3.0"
navigationEvent = "1.1.2"

[libraries]
androidx-navigation3-runtime = { module = "androidx.navigation3:navigation3-runtime", version.ref = "nav3Core" }
androidx-navigation3-ui = { module = "androidx.navigation3:navigation3-ui", version.ref = "nav3Core" }
androidx-lifecycle-viewmodel-navigation3 = { module = "androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "lifecycleViewmodelNav3" }
kotlinx-serialization-core = { module = "org.jetbrains.kotlinx:kotlinx-serialization-core", version.ref = "kotlinxSerializationCore" }
androidx-material3-adaptive-navigation3 = { group = "androidx.compose.material3.adaptive", name = "adaptive-navigation3", version.ref = "material3AdaptiveNav3" }
androidx-navigationevent = { module = "androidx.navigationevent:navigationevent", version.ref = "navigationEvent" }
androidx-navigationevent-compose = { module = "androidx.navigationevent:navigationevent-compose", version.ref = "navigationEvent" }

[plugins]
jetbrains-kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlinSerialization" }
```

### Predictive back requirements

Several parts of the app have back behavior that must step through a
sub-level rather than close the whole screen or exit. Use
`NavigationEventHandler` / `NavigationBackHandler`
(`androidx.navigationevent:navigationevent-compose`) explicitly for each
of these — don't rely on the default Activity back behavior:

- **Any bottom sheet** (quick-action sheet, Log weight, Add photo, Add
  measurement) — system back closes the sheet, not the screen underneath it.
- **The date-picker swap-in view inside those sheets** — system back
  returns to the sheet's fields, one level, same as the on-screen back
  arrow. It must not close the whole sheet in one gesture.
- **Onboarding steps** — system back matches the on-screen back button
  exactly: one step back, preserving entered data. Step 1's back returns
  to Welcome.
- **Photo capture flow** — back behavior differs per state: from Capture,
  back exits the flow; from Analyzing, back cancels the analysis; from
  Confirmation, back returns to Capture (confirm discard if fields were
  edited); from Retry/No-food/Offline, back exits the flow. Don't let a
  single default handler apply the same behavior to all six states.

## Non-negotiable constraints

- **`Color.kt` already exists and is complete** — a full Material Theme
  Builder export, light + dark, standard contrast only. It currently
  lives in `ph.mart.healthapp.ui.theme`; its final home is
  `:core:designsystem`. **Moving the file is allowed (updating only the
  package declaration); changing any value inside it is not.** Never
  regenerate it. If a token seems missing, check the file again before
  assuming.
- **Dynamic color (Material You) is disabled.** Always use the fixed
  palette. Never derive colors from wallpaper.
- **Never hardcode a hex inline.** Read every color from the theme. This is
  what lets the medium/high-contrast schemes (already in `Color.kt`, not yet
  wired up) drop in later without touching component code.
- **Semantic color assignments are fixed:** Protein = `primary`, Carbs =
  `tertiary`, Fat = `secondary` — identical in every macro bar, chart, and
  legend across the whole app. AI/insight accent = `tertiaryContainer`
  background + `onTertiaryContainer` text, and it's the *only* place
  `tertiaryContainer` is used as a card background.
- **Trend arrows** (weight, etc.): `onSurfaceVariant` for neutral, `primary`
  for on-track, `error` only for genuinely off-track. Direction depends on
  the user's goal — never default to green-for-loss/red-for-gain.
- **Typography:** Poppins for Display/Headline/Title, Inter for
  Body/Label. Numeric values (calories, weight, macros) use tabular
  figures so they don't jitter on update.
- **Spacing scale:** `4 / 8 / 12 / 16 / 24 / 32 / 48` dp. Screen horizontal
  padding `16dp`. Vertical gap between cards `12dp`. No other spacing
  values.
- **Feature modules** (`:feature:*`) have internal `ui` / `di` packages —
  no `data` package of their own, since persistence lives entirely in
  `:core:data` (see below). Cross-feature references stay plain
  Strings/IDs, never a direct import of another feature's type. Soft
  delete only, no hard deletes.

- **`:core:data` module boundary is load-bearing, not conventional.** Room
  is `implementation`-scoped in `:core:data`'s build file only — never
  `api`, and never referenced from any `:feature:*` build file. DAOs and
  `@Entity` classes live in `:core:data/<domain>/local/`. The repository
  interface itself lives at the domain root — e.g.
  `:core:data/food/FoodRepository.kt` — with `local/` and `di/` as
  subpackages beneath it. Repository interfaces are the only public
  surface; their Room-backed `Impl` classes are `internal`. Each domain's `di/` module (inside `:core:data`) binds
  interface → impl and exposes it via `koinViewModel()`. Feature-module
  ViewModels take the repository interface by constructor injection and
  never reference `AppDatabase`, a DAO, or an Entity. Replicate this
  exactly, per domain (`onboarding`, `food`, `progress`, `profile`) — it's
  settled, not open for reinterpretation.
- **Calorie/macro math is Mifflin–St Jeor**, computed live from profile
  inputs (age, sex, height, weight, activity, goal) — never hardcoded, never
  cached separately from the fields that display it. Safety floor: 1200
  kcal (female) / 1500 kcal (male), clamped with a warn-don't-block UX if
  the user edits below it manually.
- **Offline-first.** Room is the source of truth for all core data. AI
  features (photo food recognition) require network and must degrade
  gracefully to a manual-entry path when offline — this state exists in the
  prototype, build it for real.

## What's simulated in the prototype (needs real implementation)

- Camera feed → real CameraX capture
- AI food recognition → real Firebase AI Logic / Gemini call
- ~1.8s "analyzing" delay → real async state while the model call is in flight
- Chart seed data → real Room-backed queries
- localStorage cross-screen state → real Koin-scoped ViewModel / Room

## Deferred from the prototype — explicit backlog, not silently dropped

- Final mascot illustration (placeholder geometric mascot "Bibo" was used
  throughout)

Barcode scanning is built — the diary's barcode icon opens a real ML Kit
scanner, looked up against Open Food Facts (`BarcodeLookupRepository` in
`:core:data/food`, `BarcodeScanScreen` in `:feature:food`).

Food text search is built — `FoodSearchRepository` (`:core:data/food`, same
Open Food Facts transport as the barcode lookup) behind `FoodSearchPanel`
(`:feature:food/ui/components`), used by the diary's add-entry sheet and the
photo flow's no-food state. The diary's own top field is a local filter over
logged entries, not a database search.

Water tracking is built — `WaterRepository` (`:core:data/water`, one row per day
holding a glass count), the shared `WaterGlassRow` in `:core:designsystem`, a
`WaterCard` on Home and a `DiaryWaterRow` in the diary, a goal stepper in
Profile > Water, and two water reminders that stay quiet once the goal is met.

Nutrition trends are built — the Progress tab's Nutrition sub-tab charts daily calories against
the profile's target and averages macros over the selected `ChartRange`. The aggregation is pure
(`NutritionTrend.kt` in `:core:data/food`, on top of one bounded `observeSince` query); the chart
and averages card live in `:feature:progress/ui/components`.

Streaks are built — Home's `StreakCard` shows the current logging streak, five badges
(3/7/14/30/100 days), and weight-progress milestones (2/5/10 kg toward the goal). A day
counts if *anything* was logged: food, water, or a weigh-in. All of it is derived in
`core/data/streak/LoggingStreak.kt` (pure, no repository, no table, no schema change) from
`observeDailyNutrition()`, `WaterRepository.observeLoggedDays()`, and
`observeWeightEntries()`. Two rules are deliberate and tested: a grace day (today still
empty counts back from yesterday) and badges earned off the *best* run, so breaking a
streak never un-earns one. There is no celebration toast — the badge lighting up is the
reward; announcing it would need persisted "already celebrated" state.

Exercise logging is built — a fourth `:core:data` domain (`exercise/`, one soft-deleted row per
activity), logged from the FAB's quick-action sheet and from the diary's own Exercise section
(`LogExerciseSheet` in `:feature:food`, beside the diary that shows it). Burned kcal are estimated
MET × kg × hours from a fixed `ExerciseType` table, pre-filled and editable — once the user touches
the kcal field it stops re-estimating. One rule is deliberate and tested: `budgetKcal()` is the
only place burned calories are folded into the day, and `Profile.addExerciseToBudget` (a Profile
switch, default on) can turn the credit off — `calculateDailyTargets()` already applies an activity
multiplier, so crediting a workout on top of it can count the same training twice. The
Mifflin–St Jeor target itself is never touched. Exercise days also count toward the logging streak.

Reminders are wired — the four Profile switches now schedule real notifications.
`ph.mart.healthapp.reminder` in `:app` holds the schedule table and the worker;
`FitPulseApplication` reconciles WorkManager off `ProfileRepository.observeProfile()`,
so the switches stay a plain Room write and no `:feature:*` module touches
WorkManager.

Cleared in Phase 9 — these are built, don't re-defer them: FAB
scroll-collapse (`DockedFab(expanded = …)` + `rememberFabExpanded`),
tap-active-tab-to-scroll-to-top (`AppScaffold` owns each tab's
`ScrollState`), the photo comparison draggable slider, and the
medium/high-contrast schemes (`AppTheme` now follows
`UiModeManager.getContrast()` on API 34+).

## Composable structure & previews

Applies to every phase, not just Phase 2 — check this before writing any
screen or component.

- **File breakdown:** each screen's composable lives in its feature
  module's `ui/` package as `ScreenName.kt`. Extract sub-composables into
  a `ui/components/` package sibling to the screen — don't leave a
  400-line composable with five nested private functions in one file.
  Confirm this matches whatever `/orbit-mvi-screen-split` already
  prescribes before applying it; don't run two conventions in parallel.
- **Shared vs. screen-specific placement is not optional.** Check
  `COMPONENTS.md`'s build-priority column:
  - **High priority** (used in ≥2 screens, e.g. `FoodItemRow`, `AIChip`,
    `MascotAvatar`) → `:core:designsystem`. Never duplicated into a
    feature's local `ui/components/`.
  - **Medium/Low priority** (one screen, e.g. a screen-specific layout
    helper) → that feature's own `ui/components/`.
- **Every screen composable gets a `@PreviewLightDark`** (not two
  hand-written `@Preview` functions) wrapped in `Surface` using the app
  theme — the app has fully specified light and dark tokens, so both
  must render correctly, and one annotation showing both makes it much
  harder to add one mode and silently skip the other.
- **Every component composable gets the same treatment** — `@PreviewLightDark`,
  wrapped in `Surface`. If a component has meaningfully different
  variants (e.g. `AIChip`'s `default` vs `onAccent`), preview both.
- **Dialogs and bottom sheets need a preview wrapper**, since they render
  as transparent/floating by default and are otherwise invisible in
  isolation — wrap the preview in a `Box` with a scrim behind the
  `Surface` so the dialog or sheet is actually visible when previewed.

## Working agreement

- **Scope one phase or feature per session.** Don't let a session sprawl
  across phases.
- **Propose architecture and flag concerns before writing code.** If
  something in the prototype looks wrong for Android (e.g. an interaction
  pattern that doesn't fit Compose idioms), say so before implementing it
  literally.
- **`COMPONENTS.md` is the component inventory** — check it before building
  something that might already be named and scoped there.

---

## COMPONENTS.md
claude-design/project/COMPONENTS.md