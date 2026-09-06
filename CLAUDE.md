# CLAUDE.md — FitPulse

Persistent context for every Claude Code session on this project. Read this
before writing any code. If something here conflicts with what you find in
the codebase, stop and ask — don't silently pick one.

## What this project is

Android app, Kotlin + Jetpack Compose, Material 3. Two pillars: body
tracking (weight/measurements/progress photos) and nutrition tracking
(calories/macros with AI photo food logging). Offline-first for core data
(Room). AI features require network (Firebase AI Logic / Gemini).

The app is built and shipping — all nine build phases are done. **The shipped
code is the source of truth.** The Claude Design prototype in
`claude-design/project/` is still the reference
for layout, copy, and interaction on anything *not yet built*, and
`claude-design/project/COMPONENTS.md` is its component inventory — prototype-era,
so its own "Deferred" list is stale. For what components actually exist, read
`:core:designsystem`.

**`FEATURES.md` is the index of what already ships** — every screen, card, tab
and surface, plus a "Deliberately absent" list of what was ruled out. Read it
before proposing or building a feature, and add a line to it when one lands.
It says *what*; this file says *why*.

**`DECISIONS.md` is the decision log** — one entry per argued decision, grouped
by area, each one easy to "fix" back into a bug. This file says what is binding;
that one says why. **Read the entries for an area before changing its
behaviour**, and add one when a decision is taken.

## Stack

- **Package:** `ph.mart.healthapp`
- **DI:** Koin · **State:** Orbit MVI — use skill `/orbit-mvi-screen-split`
  for every screen
- **Persistence:** Room (`androidx.room3`) + bundled SQLite
- **Camera:** CameraX (`androidx-camera-compose`) + `androidx-exifinterface`
- **Navigation:** Navigation 3 — use skill `/navigation-3`
- **Predictive back:** `androidx.navigationevent` (see below)
- **Also in use:** ML Kit barcode scanning, Firebase AI + App Check,
  WorkManager (reminders), USDA FoodData Central (`api.nal.usda.gov/fdc/v1`) over
  `HttpURLConnection` + kotlinx.serialization — no HTTP client dependency, don't add one
- **Health Connect** (`androidx.health.connect:connect-client`) — the *local* health
  provider, Android 9+, read-only. `implementation`-scoped in `:core:data` only
- **Google Health API** (`health.googleapis.com/v4`, the Fitbit Web API's
  successor — *not* Google Fit): REST over the same `HttpURLConnection`, OAuth via
  `play-services-auth`'s Authorization API

Versions live in `gradle/libs.versions.toml`. Read it — never restate or
duplicate versions here or in a parallel catalog.

## Module map

```
:app                    Application, MainActivity, nav host, ph.mart.healthapp.reminder,
                        .backup, .today, .widget + .wear
:core:designsystem      theme (Color/Theme/Type) + every shared component
:core:data              Room, repositories, all persistence
:core:camera            CameraX wrapper
:core:navigation        route types
:core:today             TodaySnapshot — the day-at-a-glance summary + the watch wire format
:feature:onboarding | home | food | progress | profile | coach
:wear                   the Wear OS companion app + its tile (its own APK)
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

### Window width

`AppScaffold` is the **only** place in the app that reads the window's width, and everything
downstream is handed a plain `Boolean`. Two breakpoints, from `currentWindowAdaptiveInfo()`:
`WIDTH_DP_MEDIUM_LOWER_BOUND` (600) swaps the bottom bar for a `NavRail`, and
`WIDTH_DP_EXPANDED_LOWER_BOUND` (840) splits Progress and Profile into two panes. Below 600dp
nothing about this feature is reachable, so a phone renders exactly the path it always did.

- **`NavRail` is `BottomNavBar`'s sibling in `:core:designsystem`, not a `when` inside it.** Same
  `BottomNavItem` list, index and callback, same `secondaryContainer` pill — one bar rotated, not a
  second design — and `AppScaffold` picks. Deliberately **not**
  `NavigationSuiteScaffold`: that artifact is not on the classpath, and its M3 defaults would
  replace the hand-drawn pill, which is a phone-visual regression for a tablet feature. The docked
  FAB moves into the rail *collapsed* — an extended FAB does not fit 80dp, and
  `rememberFabExpanded` is a scroll affordance a rail has nothing to say about. The rail's tabs sit
  in a `weight(1f)` column arranged `SpaceEvenly`, because the window that is wide is usually the
  one that is short: a landscape phone is ~410dp tall and a fixed stack put the fourth tab off the
  bottom edge.
- **`showsTabChrome` is a pure function, and `beneath` is what keeps it honest.** A tab always
  wears the rail/bar and the FAB; so does a Profile detail at two-pane width, because its tab root
  is still on screen beside it. The five routes that qualify are one `ProfileDetailRoutes` set read
  by *both* the pane metadata and the chrome rule, so the scene and the chrome cannot disagree —
  and the entry beneath must be `ProfileRoute`, so Health Connect's rationale intent landing on the
  Home tab stays single-pane. It is the one branch here a JVM test can reach, and `TabChromeTest`
  is that test.
- **The top bar is *not* folded into that rule.** In a two-pane Profile it spans both panes, names
  the detail and keeps its back arrow — back is the only way to dismiss a pane back to its
  placeholder. That is a deliberate departure from the `adaptive` skill's "no back arrow in a
  list-detail layout", which assumes the list is the way out.
- **`BackNavigationBehavior.PopLatest`, never the default.** Closing a Profile detail leaves the
  list beside its placeholder, which is *still* a two-pane scaffold value, so
  `PopUntilScaffoldValueChange` keeps popping and back walks out of the tab. One press is one
  entry, which is what `NavDisplay`'s `onBack` (`TopLevelBackStack.removeLast()`) already means.
- **Profile is a Nav3 list-detail scene; Progress is a `Row`.** Profile's five sub-routes are real
  nav entries, so they take `ListDetailSceneStrategy` metadata — the skill's binding call, and
  `shouldHandleSinglePaneLayout = false` plus gating `sceneStrategies` on the expanded breakpoint is
  what leaves narrow windows on the path they were already on. Progress cannot: its detail is a
  swap-in over `selectedSubject`, and routing it to earn a scene would buy a second
  `ViewModelStoreOwner` and a second copy of twelve repositories — the exact thing the swap-in was
  chosen to avoid. So the tab draws its own two panes and `SubjectDetail` takes an `embedded` flag:
  no back handler and no back arrow, because a pane beside its own list is not a level.
- **Home, the diary and the camera flows stay one pane at every width.** A two-pane Home would need
  a second card order to author and `Profile.homeLayout` stores one; the diary has no list to put
  beside its day — it is a single scrolling day, and the calendar swap-in
  (`FoodScreenState.calendarOpen`) is the pane that would earn one; `fullBleed` is unchanged, because a viewfinder
  beside a list is not a viewfinder. Single columns are **not** width-capped either — that is a
  visual-design decision and this work is layout only.

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
  `exercise` (logged activity *and* the routines that seed one), `mood`, `cycle`, `health`,
  `fasting`, `supplement`, `bloodpressure`, `coach`. Two
  non-domains sit beside them: `network/` (a `NetworkMonitor`
  recheck, not a listener) and `streak/`, which is pure derivation — no table,
  no repository, no schema.
- **Calorie/macro math is Mifflin–St Jeor**, computed live from profile inputs
  (age, sex, height, weight, activity, goal) — never hardcoded, never cached
  separately from the fields that display it. Safety floor: 1200 kcal (female) /
  1500 kcal (male), clamped with a warn-don't-block UX if the user edits below
  it manually.
- **Offline-first.** Room is the source of truth for all core data. AI features
  degrade gracefully to a manual-entry path when offline.

## Backlog

- Health Connect reads health data, which needs the Play Console data-types declaration form —
  a separate obligation from the Google Health API's OAuth verification and CASA assessment
  below, and cheaper: no CASA, and no per-scope justification. `READ_MENSTRUATION` is on that form
  too and is the one entry in a sensitive category, so declare it even though it is requested only
  while cycle tracking is on.
- FoodData Central runs on one signed key shipped in the APK (extractable, and its 3600 req/hour
  budget is shared by every install). A proxy holding the key is the upgrade path if either the
  ceiling or the exposure starts to matter.
- Google Health's `nutritionLog` sends `DIETARY_FIBER`, `TOTAL_SUGARS` and `SODIUM` on unverified
  names — the v4 reference publishes no `Nutrient` enum values. Pin the three against a live
  response and `nutritionLogBody`'s `micronutrients` flag *and* `pushMeals`' retry both go, the
  same outstanding job as the weight timestamp and the step-bucket field.
- Final mascot illustration. The geometric placeholders (Bibo, Pip, Zed, Momo, Sprig) are used
  throughout; a commissioned set replaces the five drawings, not the picker around them.
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
- `parseHeartPage` hedges twice over — the timestamp from `sampleTime.physicalTime`, then a flat
  `physicalTime`, then an interval start; the value from `beatsPerMinute`, then `bpm`, then
  `value` — and the scope heart rate rides is itself a guess. Pinning both against a live response
  is what lets `sync()` treat a heart 403 like every other type's.
- No CI workflow, and no instrumented test but the generated `ExampleInstrumentedTest`. The deps
  are already wired in `:app` (`ui-test-junit4`, `espresso-core`, `androidx-junit`,
  `ui-test-manifest`), so nothing new goes in the version catalog. A workflow runs `assembleDebug`
  and `testDebugUnitTest` **only** — an emulator in CI is a large, slow, flaky dependency for a
  solo project, and the JVM tests are where the derivation logic lives — and it must pass with
  `fdcApiKey` absent, which is the degrade-gracefully rule rather than a secret to work around it.
  What is worth an instrumented test is what no JVM test can reach and would break silently:
  onboarding's steps writing a profile, the add-entry sheet's shared `isValid()` across its three
  log paths, the diary's date navigation stopping at today, and predictive back through the photo
  flow's four states. No Robolectric, no MockK, no Turbine — a second idiom is a second thing to
  keep in step.

### Localization

Every module owns a `res/values/strings.xml` and every user-facing string reads from it. No
translation ships — this is the scaffolding that makes one possible, and the decisions below are
what stop the next pass undoing it.

- **`./gradlew checkUiLiterals` is the gate, and stock lint is not.** `HardcodedText` scans XML
  layout resources; this app has none, so it would pass clean on a module with three hundred
  Kotlin literals. The task in the root build greps every module in `localizedModules` for a
  capitalized literal in a copy-carrying argument (`text =`, `label =`, `contentDescription =`,
  and the rest) and skips preview fixtures — a `fun *Preview()` body or a `val PREVIEW_*` block,
  debug-only sample data no translator reads. A module joins the list in its own commit.
- **A positional literal is copy too, and three more patterns say so.** `StatRow("Systolic", …)`
  carries a name and is not a named argument, which is how thirteen empty-state pages stayed
  English through the pass that was supposed to have converted them — the named rule ran clean over
  every one. So the gate also flags a literal opening an argument (`("Cap`), a literal alone on its
  own line (the multi-line-constructor shape), and a `when` branch returning one (`-> "Cap"`).
  Those three run **only** where copy lives — a `ui/` tree or `:core:designsystem`'s `component/` —
  because `("Branded"` reads identically in a Room query, an AI prompt or a request header, and
  scoping the rule was cheaper than allowlisting every file that holds one. `error()` and
  `require()` lines are skipped: an exception message is not copy. `literalExceptions` in the root
  build is the six files whose English is a decision recorded at its own definition, one name per
  line, and it is the only place the gate can be argued with. *ponytail: still a line-based grep,
  not a parser — a literal split across lines, or one starting with a template (`"$n tracked"`),
  slips through, and a Compose lint rule is the upgrade path.*
- **Keys are `<module>_<screen>_<thing>`,** flat, lowercase. Enough to grep, not a taxonomy.
- **A resource id is never a `const val`.** A library module's R fields are runtime values, and
  `const` inlines the placeholder `0` — which is a `Resources$NotFoundException: String resource
  ID #0x0` at the call site, not a compile error. Three of these shipped into a crash on Profile
  before the device run caught them. `@StringRes val`, always.
- **Composables resolve; ViewModels name.** A `message` field that crosses a ViewModel boundary
  carries an `@StringRes Int` (onboarding's health step, the coach's `CoachFailure.reason`) — or,
  where the message has arguments the screen cannot work out for itself, a small type the screen
  turns into words (`HealthMessage`). No Context reaches a ViewModel. A string built in a
  coroutine or a permission callback is the exception, and reads through `LocalContext`.
- **A semantics lambda cannot read a resource**, so every `contentDescription` inside
  `clearAndSetSemantics {}` is resolved one line above it. That is a dozen call sites and the
  pattern is uniform on purpose.
- **Weekday names come from `DateFormatSymbols`, not a resource array.** The stdlib already has
  them per locale, so `weekdayNames()`/`weekdayShort()`/`weekdayInitials()` in
  `:core:data/exercise/TrainingPlan.kt` replaced three English lists and there is nothing to
  translate. They index from Sunday and this app counts from Monday — `WeekdayNamesTest` is the
  guard, because getting it wrong rotates every routine's plan by a day.
- **`:core:data` has a `strings.xml`, and that is not a layering breach.** Six enums there
  (`ChartRange`, `MoodLevel`, `ExerciseType`, `BloodPressureCategory`, `FlowLevel`,
  `CycleSymptom`) carry labels a feature renders, and `:core:designsystem` has no dependency on
  that module — so the alternative was the same six lists copied into every feature that draws a
  chip. `CALORIE_FLOOR_WARNING` lives there for the reason it always did: one safety warning, three
  screens.
- **Display names live where the enum's `name` is not the display name.** `MealType.labelRes()`
  sits in `:feature:food/ui/shared/`, `ActivityLevel.label()` in `:feature:profile`, the four tab
  names in `:app` — because each enum's `name` is a stored token (a diary row, an export field, a
  profile column) and six screens were printing it at the user. `:core:navigation` lost
  `TopLevelDestination.label` outright: a leaf module with no resources has nowhere to put one.
- **What stays in Kotlin, each commented at its definition.** Two rules, and only two.
  **Persisted or compared:** `QUICK_ADD_NAME`, the `COMMON_FOODS` names (`searchFoods()` dedupes
  on them), portion units (`portionStep` switches on `"g"`/`"oz"`/`"cup"`/`SERVING_UNIT`), every
  enum `name`, `HomeCard`'s stored layout format, Room queries, Data Layer paths, `@SerialName`s,
  intent extras. An imported workout's fallback name takes `ExerciseType.name` for the same
  reason — a resource would freeze the import-time language into a row that outlives it.
  **Pure functions with a JVM test over their wording:** `insightFor()`, `goalProjectionLine()`,
  `:feature:home`'s `greetingFor`, `:feature:progress`'s
  `summarize()` and `captionFor()`, `:feature:food`'s `diaryDateLabel` ("Today"/"Yesterday"), and
  `:core:data/exercise/Strength.kt`'s three label functions (`loadLabel`, `summaryLabel`,
  `LiftPerformance.label` — "Bodyweight × 20", "3 sets", "Last: 60 kg × 8"). Converting those means
  returning a case type per branch for a composable to resolve, or handing a non-composable a
  `Context` for a noun and a plural; that is one decision, not eight, and it has not been taken —
  the test is what earns each of them the exemption, so a label without one gets a test rather than
  a comment. Also staying: AI prompts (the model reads them in English), `Reminder.title`/`body`,
  `MascotCharacter`'s five proper names, `parseExport`'s `require()` message and the import
  fallback beside it (an exception's text is an exception's text), and unit symbols — kg, lb, cm,
  in, kcal, g, mg are not copy.
- **A test that asserted wording now asserts the rule.** `FoodLibraryDataTest` checks the totals,
  the per-serving division and the portion's dropped trailing zero rather than the sentence the
  resource now owns. Nothing it covered was lost.

## Composable structure & previews

- **File breakdown:** a screen's composable is `ScreenName.kt`; its
  sub-composables go in a sibling `components/`. Don't leave a 400-line
  composable with five nested private functions in one file. Follow whatever
  `/orbit-mvi-screen-split` prescribes — don't run two conventions in parallel.
- **A feature holding more than one flow nests one level per flow.** A single-flow
  feature is flat: `ui/ScreenName.kt` + `ui/components/`. A multi-flow one is
  `ui/<flow>/ScreenName.kt` + `ui/<flow>/components/`, one package per flow, with
  the `*Data`/`*State`/`*ViewModel`/`*Screen` quartet intact inside each. Anything
  genuinely used by two or more flows goes in `ui/shared/` (or
  `ui/shared/components/`) rather than being left in whichever flow happened to
  declare it first. `:feature:food` (`diary`, `photo`, `barcode`, `exercise`,
  `recipe`, `search`, `ideas`, `voice`, `shared`), `:feature:progress` (`progress` — the overview, the
  detail chrome and the recap — plus `weight`, `measurement`, `photo`, `nutrition`, `activity`,
  `strength`, `mood`, `cycle`, `sleep`, `heart`, `fasting`, `supplement`, `pressure`, `energy` and
  `achievement`, one per subject holding that subject's `*Detail.kt` body and its own charts, and a
  `shared/` holding `RangeBarChart`, `DayBarChart` and `SharePng` (the capture-and-share pair two
  flows draw), the first two of which between them draw every subject's bars except Mood's,
  Nutrition's and Supplements'),
  `:feature:profile` (`profile`, `health`, `library`, `routine`, `supplement`, `layout`, plus a
  `shared/` holding `LibraryRow` and `RenameSheet`, which the food library and the routine library
  both draw) and `:feature:onboarding`
  (`onboarding`, `health`, `shared`) are the worked examples. Grouping is by *subject*, not by
  owning screen: `ExerciseSection` sits under `exercise/` and `RecipePanel` under
  `recipe/` though `FoodScreen` renders both, and Progress's eleven tab bodies sit
  with the charts they draw rather than with the shell that dispatches them. Only
  the `*Navigation.kt` file stays at the `ui/` root, because its route types and
  `<feature>Entries` are what `:app` reaches for.
- **`:wear` follows the same rules on a smaller graph**: `ui/` + `ui/components/` + `ui/theme/`,
  one flow, one ViewModel — flat for the reason `:feature:home` is. Its previews are
  `@WearPreviewDevices` / `@WearPreviewFontScales` rather than `@PreviewLightDark`: the watch has
  one scheme but several shapes and font scales, which is where a wrist layout actually breaks.
- **`:feature:home` and `:feature:coach` are deliberately flat**, and should stay that way.
  Each holds exactly one flow with one ViewModel, so `ui/` + `ui/components/` is already what
  the rule above prescribes. Don't "finish the job" by sub-packaging them.
- **What earns a flow package is a second ViewModel**, not a second screen. `StrengthWorkoutScreen`
  is the counter-example that proves it: a route with its own back handler and discard dialog, yet
  it sits in `:feature:food`'s existing `ui/exercise/` because it shares `LogExerciseViewModel` with
  the sheet — one form, two presentations, and `ExerciseFormFields` is the trio they both draw.
  `:feature:onboarding` was flat on the argument that its seven steps are sub-views of
  `OnboardingScreen`'s `when (step)` — true of six of them, but the Google Health step
  owns `OnboardingHealthViewModel`, and everywhere else in this repo that means its own
  package (`:feature:profile`'s `ui/health/` is the near-identical case). So onboarding
  is `ui/onboarding/` + `ui/health/`, with `OnboardingStepHeader` in `ui/shared/components/`
  because both flows draw it. The six steps stay sub-views in `ui/onboarding/components/`
  and keep their `*Screen` names — `:feature:food`'s `ui/photo/components/` does the same
  with `CaptureScreen`/`AnalyzingScreen`/`ConfirmationScreen`. All seven are `internal`;
  only `OnboardingScreen` is public, because `AppRoot` renders it directly (onboarding has
  no `*Navigation.kt` — it is not in the Nav3 graph).
- **Shared vs. screen-specific placement is not optional.** Used in ≥2 screens
  (`FoodItemRow`, `AIChip`, `MascotAvatar`, `WaterGlassRow`, `CalendarPanel`) →
  `:core:designsystem`, never duplicated into a feature. One screen only → that
  screen's own `components/`. `ui/shared/` is for the middle case: crossing flows
  inside one feature, but not crossing features.
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
  implementation is done — a build or a test run is not a precondition for the
  commit. Say plainly what was and wasn't verified in the commit message or the
  reply; don't hold the work uncommitted waiting on a check.
