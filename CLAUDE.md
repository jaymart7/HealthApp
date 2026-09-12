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
It says *what*; this file says what binds.

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
  WorkManager (reminders), USDA FoodData Central (`api.nal.usda.gov/fdc/v1`) and Open Food
  Facts (`world.openfoodfacts.org/api/v2` for a barcode, `search.openfoodfacts.org` for text),
  both over `HttpURLConnection` + kotlinx.serialization — no HTTP client dependency, don't add one
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
:feature:onboarding | home | food | training | progress | profile | coach
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

`AppScaffold` is the **only** place in the app that reads the window's width; everything
downstream is handed a plain `Boolean`. Two breakpoints, from `currentWindowAdaptiveInfo()`:

- **≥600dp** (`WIDTH_DP_MEDIUM_LOWER_BOUND`) — the bottom bar becomes `NavRail`, which is
  `BottomNavBar`'s sibling in `:core:designsystem`, never a `when` inside it. The docked FAB
  moves into the rail *collapsed*.
- **≥840dp** (`WIDTH_DP_EXPANDED_LOWER_BOUND`) — Profile draws its detail routes beside it as a
  Nav3 `ListDetailSceneStrategy` scene; Progress draws its own two panes (a `Row`, not a scene,
  with `SubjectDetail` taking an `embedded` flag); the diary draws `CalendarPanel` as a fixed
  320dp pane and `DiaryDateHeader`'s `onOpenCalendar` goes null. Back is
  `BackNavigationBehavior.PopLatest`, never the default.
- **Below 600dp none of this is reachable** — a phone renders exactly the path it always did.

`ProfileDetailRoutes` (`:app/ui/AppScaffold.kt`) is the one list of the **eight** routes that
draw beside Profile, read by *both* the pane metadata and `showsTabChrome`, so the scene and the
chrome can never disagree. The entry beneath must be `ProfileRoute`. `showsTabChrome` is a pure
function and `TabChromeTest` is its test.

**Home and the camera flows stay one pane at every width**, and single columns are not
width-capped.

Why each of those calls was made — the `NavigationSuiteScaffold` refusal, the top bar spanning
both panes, why Progress cannot earn a scene, why the calendar pane is fixed rather than
weighted — is `DECISIONS.md` → **Adaptive layout**.

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
  **One exception, and it is the whole list:** `MascotPalette`'s thirty hues are
  computed from a hue angle by `Color.hsl()` in
  `:core:designsystem/component/MascotPalette.kt`. They exist because the scheme
  cannot supply a pink or a red — the roles that would are the ones reserved
  below — and they are safe because they take part in no scheme: a mascot fill is
  decorative, nothing else is drawn from them, and a contrast swap has nothing to
  say about them. Still no hex: a colour is one number, and
  `MascotPaletteTest` holds every pair at ≥ 4.5:1.
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
  `fasting`, `supplement`, `bloodpressure`, `coach`, plus two that keep the shape without
  owning a table — `insight/` (the cached daily line) and `transfer/` (export, import and
  the local backups). Two non-domains sit beside them: `network/` (a `NetworkMonitor`
  recheck, not a listener) and `streak/`, which is pure derivation — no table,
  no repository, no schema.
- **Calorie/macro math is Mifflin–St Jeor**, computed live from profile inputs
  (age, sex, height, weight, activity, goal) — never hardcoded, never cached
  separately from the fields that display it. Safety floor: 1200 kcal (female) /
  1500 kcal (male), clamped with a warn-don't-block UX if the user edits below
  it manually.
- **Offline-first.** Room is the source of truth for all core data. AI features
  degrade gracefully to a manual-entry path when offline.

## Localization

Every module owns a `res/values/strings.xml` and every user-facing string reads from it —
about 1,100 strings across twelve modules. **No translation ships**; this is the scaffolding
that makes one possible. The rules below are what stop the next pass undoing it; the arguments
behind them are `DECISIONS.md` → **Localization**.

- **`./gradlew checkUiLiterals` is the gate**, not stock lint (`HardcodedText` scans XML layouts
  and this app has none). It greps every module in `localizedModules` for a capitalized literal
  in a copy-carrying argument, plus three positional patterns that run only where copy lives.
  Preview fixtures are skipped. A module joins `localizedModules` in its own commit, and
  `literalExceptions` in the root build is the only place the gate can be argued with.
- **Keys are `<module>_<screen>_<thing>`,** flat, lowercase. Enough to grep, not a taxonomy.
- **A resource id is never a `const val`** — `@StringRes val`, always. `const` inlines the
  placeholder `0` and crashes at the call site rather than failing to compile.
- **Composables resolve; ViewModels name.** A message crossing a ViewModel boundary carries an
  `@StringRes Int`, or a small type the screen turns into words (`HealthMessage`) where it takes
  arguments the screen can't work out. **No Context reaches a ViewModel** — a string built in a
  coroutine or a permission callback is the exception, and reads through `LocalContext`.
- **A semantics lambda cannot read a resource**, so every `contentDescription` inside
  `clearAndSetSemantics {}` is resolved one line above it.
- **Weekday names come from `DateFormatSymbols`** (`:core:data/exercise/TrainingPlan.kt`), never
  a resource array. They index from Sunday and this app counts from Monday — `WeekdayNamesTest`
  is the guard.
- **Display names live where the enum's `name` is not the display name** — `MealType.labelRes()`
  in `:feature:food/ui/shared/`, `ActivityLevel.label()` in `:feature:profile`, the tab names in
  `:app`. `:core:data` owns a `strings.xml` for the six enums whose labels a feature renders.
- **What stays in Kotlin, each commented at its definition.** Two rules, and only two.
  **Persisted or compared:** `QUICK_ADD_NAME`, the `COMMON_FOODS` names, portion units, every
  enum `name`, `HomeCard`'s stored layout format, Room queries, Data Layer paths, `@SerialName`s,
  intent extras. **A pure function with a JVM test over its wording:** `insightFor()`,
  `goalProjectionLine()`, `greetingFor`, `summarize()`, `captionFor()`, `diaryDateLabel`, and
  `Strength.kt`'s three label functions. The test is what earns the exemption — a label without
  one gets a test rather than a comment. Also staying: AI prompts, `Reminder.title`/`body`,
  `MascotCharacter`'s five proper names, exception messages, and unit symbols (kg, lb, cm, in,
  kcal, g, mg are not copy).

## Build & check

```
./gradlew assembleDebug
./gradlew testDebugUnitTest      # 75 JVM test files across 14 modules
./gradlew checkUiLiterals        # the localization gate, defined in the root build
```

`fdcApiKey` is a Gradle property and the build must pass without it — that is the
degrade-gracefully rule, not a secret to work around. There is no CI; see Backlog.

## Backlog

**Google Health — verification.** Not done. The Cloud project is settled
(`app/google-services.json` points at `fitpulse-8d951`, the same project Firebase AI Logic runs
in) and the Android OAuth clients now exist — two `client_type: 1` entries carrying the debug
and release SHA-1. What is left: the consent screen branded for FitPulse, per-scope
justifications submitted, and a CASA Letter of Validation.

**Google Health — four hedged parsers, one job.** The v4 reference publishes no `Nutrient` enum
values and no settled response shapes, so four parsers guess and fall back. Capture one live
response of each and pin them:

- `nutritionLog` sends `DIETARY_FIBER`, `TOTAL_SUGARS`, `SODIUM`, `CALCIUM`, `POTASSIUM`,
  `VITAMIN_D` and `IRON` on unverified names. Pinning all seven retires both
  `nutritionLogBody`'s `micronutrients` flag *and* `pushMeals`' retry.
- Weight reads the timestamp from `sampleTime.physicalTime` with a flat `physicalTime` fallback.
- `parseStepsPage` reads the bucket count from `count`, then `steps`, then `delta`.
- `parseHeartPage` hedges twice over — the timestamp from `sampleTime.physicalTime`, then a flat
  `physicalTime`, then an interval start; the value from `beatsPerMinute`, then `bpm`, then
  `value` — and the scope heart rate rides is itself a guess. Pinning both is what lets `sync()`
  treat a heart 403 like every other type's.

**Health Connect — the Play Console data-types declaration form.** A separate obligation from
the OAuth work above, and cheaper: no CASA, and no per-scope justification. `READ_MENSTRUATION`
is on that form too and is the one entry in a sensitive category, so declare it even though it
is requested only while cycle tracking is on.

**FoodData Central runs on one signed key shipped in the APK** — extractable, and its 3600
req/hour budget is shared by every install. A proxy holding the key is the upgrade path if
either the ceiling or the exposure starts to matter; the barcode cache took the rescan traffic
off that ceiling, so what is left is the exposure and first scans.

**No CI workflow, and no instrumented test but the generated `ExampleInstrumentedTest`.** The
deps are already wired in `:app` (`ui-test-junit4`, `espresso-core`, `androidx-junit`,
`ui-test-manifest`), so nothing new goes in the version catalog. A workflow runs `assembleDebug`
and `testDebugUnitTest` **only** — an emulator in CI is a large, slow, flaky dependency for a
solo project, and the JVM tests are where the derivation logic lives — and it must pass with
`fdcApiKey` absent. What is worth an instrumented test is what no JVM test can reach and would
break silently: onboarding's steps writing a profile, the add-entry sheet's shared `isValid()`
across its three log paths, the diary's date navigation stopping at today, and predictive back
through the photo flow's four states. No Robolectric, no MockK, no Turbine — a second idiom is
a second thing to keep in step.

**Final mascot illustration.** The geometric placeholders (Rui, Gel, Mart, Alo, Lala) are used
throughout; a commissioned set replaces the five drawings, not the picker around them.

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
  declare it first. `:feature:food` (`diary`, `photo`, `barcode`, `history`,
  `recipe`, `search`, `ideas`, `voice`, `shared`), `:feature:progress` (`progress` — the overview and
  the detail chrome — plus `weight`, `measurement`, `photo`, `nutrition`, `activity`,
  `strength`, `mood`, `cycle`, `sleep`, `heart`, `fasting`, `supplement`, `pressure`, `energy` and
  `achievement`, one per subject holding that subject's `*Detail.kt` body and its own charts;
  the tab's three read-only overlays are flows of their own — `comparison`, `timelapse` and
  `recap`, each with the `*Data`/`*State`/`*ViewModel`/`*Screen` quartet — and a
  `shared/` holding `Recap.kt` plus a `components/` with `RangeBarChart` and `DayBarChart`, which
  between them draw every subject's bars except Mood's, Nutrition's and Supplements', beside
  `RecapCard`, `SharePhotoStripSheet`, `PhotoOverlayLabel` and `Note`),
  `:feature:profile` (`profile`, `settings`, `health`, `library`, `routine`, `supplement`, `layout`, plus a
  `shared/` holding the row primitives — `AppListRow`, `IconTile`, `SectionHeader`, `StepperRow`
  — beside `LibraryRow` and `RenameSheet`, which the food library and the routine library both
  draw) and `:feature:onboarding`
  (`onboarding`, `health`, `shared`) are the worked examples. Grouping is by *subject*, not by
  owning screen: `RecipePanel` sits under `recipe/` though `FoodScreen` renders it, and
  Progress's fourteen subject pages sit with the charts they draw rather than with the shell
  that dispatches them. The one entry that reads against the rule is `ExerciseSection`, which
  stayed in `ui/diary/components/` when the rest of the exercise UI left for `:feature:training`
  — see `DECISIONS.md` → **Training, strength & routines** for why. Only
  the `*Navigation.kt` file stays at the `ui/` root, because its route types and
  `<feature>Entries` are what `:app` reaches for.
- **`:wear` follows the same rules on a smaller graph**: `ui/` + `ui/components/` + `ui/theme/`,
  one flow, one ViewModel — flat for the reason `:feature:home` is. Its previews are
  `@WearPreviewDevices` / `@WearPreviewFontScales` rather than `@PreviewLightDark`: the watch has
  one scheme but several shapes and font scales, which is where a wrist layout actually breaks.
- **`:feature:home`, `:feature:coach` and `:feature:training` are deliberately flat**, and should
  stay that way. Each holds exactly one flow with one ViewModel, so `ui/` + `ui/components/` is
  already what the rule above prescribes. Don't "finish the job" by sub-packaging them.
- **What earns a flow package is a second ViewModel**, not a second screen. `StrengthWorkoutScreen`
  is the counter-example that proves it: a route with its own back handler and discard dialog, yet
  it sits flat in `:feature:training/ui/` beside the log-exercise sheet, because the two share
  `LogExerciseViewModel` — one form, two presentations, and `ExerciseFormFields` is the trio they
  both draw. That whole module is one flow, which is why it has no flow package at all.
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
  (`FoodItemRow`, `AIChip`, `MascotAvatar`, `WaterGlassRow`, `CalendarPanel`, `MealThumbnail`) →
  `:core:designsystem`, never duplicated into a feature. `rememberBitmapFromFile` is there for the
  same reason and is the app's **one** decoder for a stored photo file — a second one is a second
  downsampling rule to keep in step. One screen only → that
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
