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
  WorkManager (reminders), USDA FoodData Central (`api.nal.usda.gov/fdc/v1`) over
  `HttpURLConnection` + kotlinx.serialization — no HTTP client dependency, don't add one
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
  `exercise`, `mood`, `health`, `fasting`, `supplement`, `bloodpressure`. Two
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
- **FoodData Central has no barcode endpoint, and the `gtinUpc` check is what makes a scan
  trustworthy.** A scan is a `foods/search` restricted to `dataType=Branded`, and search is not a
  lookup: an unlisted code usually answers HTTP 200 with an empty `foods`, but a code that tokenizes
  to nothing (all zeros, say) makes FDC fall back to relevance and hand back the top of the entire
  branded database — 433,403 "hits", every one of them a real product.
  `parseFdcProduct` therefore compares `gtinUpc` back against the scanned code, leading zeros
  stripped on both sides, and that comparison is the only thing standing between an unknown
  package and a diary row for someone else's chicken nuggets — never "simplify" it away as a
  redundant check on a result the server already filtered. The leading-zero stripping is not
  cosmetic either: FDC stores `gtinUpc` at whatever width its source used (`028400642255` for one
  product, `0099447210127` for the next) and matches query tokens exactly, so the lookup asks for
  every zero-padding at once in one unquoted query, which FDC ORs. Search, by contrast, sets no
  `dataType` — whole foods and branded packages both belong in the results.
- **The FDC key is a gradle property, and its budget is app-wide.** `fdcApiKey` lives in
  `~/.gradle/gradle.properties` and reaches the code as `BuildConfig.FDC_API_KEY` in `:core:data`,
  the same untracked-and-degrade-gracefully rule the release signing config follows — absent it the
  build still compiles and search reports `Failed`. It is one signed key shared by every install,
  so the 3600 requests/hour ceiling is the *app's*, not each user's; that is what the search
  debounce is protecting, and `pageSize` is capped at 10 because FDC ignores `nutrients=` on this
  endpoint and ships ~21 KB per food.
- **The today-only repository overloads are deliberate** — Home and the streak
  genuinely mean today, so don't collapse them into the dated ones.
- **The diary's top field is a local filter over logged entries**, not a
  database search. Database search is `FoodSearchRepository`/`FoodSearchPanel`.
- **A nameless entry is a quick add, not an invalid one.** `AddEntryForm.isValid()` accepts a bare
  calorie figure, and `toFoodEntry()` fills the blank with `QUICK_ADD_NAME` and collapses the
  portion to one serving — the form's default 100 g is a number the user never supplied. The guard
  is shared with the photo and barcode confirmation screens on purpose: all three log through
  `toFoodEntry()`, so no path can write a blank name, and clearing a name on a confirmation screen
  degrades to a quick add instead of deadlocking the button. `FoodEntryDao.observeRecent` excludes
  that name so every quick add doesn't collapse into one meaningless row eating a
  `MAX_SUGGESTIONS` slot — which is also why the suggestion panel's one-tap re-log callback is
  `onLogAgain`, not `onQuickAdd`.
- **`Profile.darkThemeOn` is nullable and null means follow the device.** A plain `false`
  default would force light on a phone already in dark mode; the Profile switch resolves it
  with `darkThemeOn ?: isSystemInDarkTheme()`, the same expression `MainActivity` uses to pick
  the scheme. Contrast stays system-driven.
- **The mascot pick rides the theme, and `Profile` stores its *name*.** `MascotAvatar` has ~16 call
  sites across five feature modules, all writing `MascotAvatar(state = …)`; threading a character
  through them would be a sixteen-file diff for a value that is constant app-wide. So `AppTheme`
  provides `LocalMascot` beside the colour scheme — one appearance choice resolved where the other
  already is — and only the picker passes `character` explicitly. `Profile.mascotName` is a
  nullable **String**, not the enum, because `MascotCharacter` lives in `:core:designsystem` and
  `:core:data` does not depend on it; `mascotCharacterOf()` resolves it, and null (or a name from a
  newer build) degrades to Bibo, exactly the reading `darkThemeOn`'s null has. Each character varies
  on **four axes** — silhouette, fill pair, eyes, one accent — because two characters differing only
  in outline read as the same character badly drawn. What stays shared is the **mouth geometry and
  the state vocabulary**: all five states read identically whichever buddy is picked, so no
  character can come to mean something. Fills never take a `tertiary` or `error` role
  (`tertiaryContainer` is the AI accent, `error` is off-track only). The whole avatar is one
  `Canvas` rather than a shaped `Box` — that is what lets an antenna or an ear sit *above* the head
  (`topInset`/`sideInset` carve the room, and Bibo's are zero so it renders exactly as it always
  has) with nothing clipping the Celebrating sparkles.
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
  floor existed — the branch only ever fires for the eight Progress tabs, and on very narrow
  screens, where scrolling replaces clipping. The Progress labels stay trimmed ("Nutrition" reads
  "Food", "Measurements" reads "Body"); another tab costs nothing but scroll distance now, so
  never shorten a label further to avoid one.
- **The recap's weight cell goes blank when the last weigh-in predates the window.**
  `trendVsSevenDaysAgo()` anchors to the latest *entry*, not to today, so without that guard a
  card headed "Last 7 days" would report a delta between two entries from two months ago.
- **The goal projection is one sentence on three screens, and it names its own window.**
  `goalProjection()` sits in `:core:data/progress/` — pure derivation, no table, the `streak/`
  shape — because Home's weight card, Progress's weekly recap and Progress's Weight tab all show
  it, and `:feature:*` modules never import each other. The words come from
  `goalProjectionLine()` in `:core:designsystem`, which takes primitives (that module has no
  `:core:data` dependency) and prints "On the last 30 days' trend, …" off `PROJECTION_WINDOW_DAYS`:
  the recap card is headed "Last 7 days" while the fit runs over thirty, so a line that left its
  window implicit would be a card contradicting its own heading. It stays out of `weeklyRecap()`
  for the same reason — every other field there is a seven-day figure. The line is
  `onSurfaceVariant` on every surface, never `error`: the delta beside it already carries the
  verdict colour, and a red date reads as a second one.
- **A saved meal is a snapshot, not a live link.** Saving copies the section's entries into
  `saved_meal`/`saved_meal_item`; editing or deleting the original diary rows never touches it,
  and deleting the saved meal never touches what was logged from it. Re-logging always writes
  fresh entries stamped with the diary's *selected* day. The panel shows the newest
  `MAX_SAVED_MEALS` only, and saved meals stay out of the data export for the same reason
  favorites do — convenience data, not history.
- **A recipe is a saved meal with a servings count, and logs as one row.** Both live in
  `saved_meal`/`saved_meal_item`; `servings IS NULL` *is* the discriminator, and two DAO queries
  keep the lists apart so neither can evict the other from its own newest-5 window. The difference
  that earns the extra concept is at log time: a saved meal re-logs its items as one diary row
  each, while a recipe seeds the add-entry form with a *single* row priced at `perServing()` — the
  diary should read "Chili · 1 serving", not list the onions. The seeded row stays editable, like a
  search hit or a suggestion, because that is the only way to log half a portion today. Recipes
  stay out of the data export for the same reason saved meals do, and the builder is a screen
  rather than a sheet sub-view because an ingredient list plus its editor doesn't fit above a
  keyboard.
- **The panels' newest-5 windows are a display choice, not a cap.** `MAX_SAVED_MEALS` and
  `MAX_RECIPES` keep the add-entry sheet short; Profile → Saved meals & recipes reads
  `observeAllSavedMeals()`/`observeAllRecipes()` and is the only place the rest can be reached — a
  sixth saved meal used to be out of view *and* out of reach of its own delete button, which is the
  bug that screen exists to fix. Both windows share one join helper per type in
  `FoodRepositoryImpl`, so the panel's list and the library's cannot drift apart in grouping or
  order. The library renames and deletes and **cannot log**: logging needs a meal slot and a day,
  and Profile has neither. Rename is one column (`SavedMealDao.rename`) precisely because a recipe
  and a saved meal are the same row shape and `servings` is what tells them apart. The Profile row
  carries no count, for the same reason the Connections row caches no connection state.
- **Changing a portion reprices the entry.** `AddEntryForm.withPortionAmount()` (and its
  `SavedMealItem` twin) scale calories, all three macros and the three micronutrients by the
  portion ratio, because every
  seeded figure in this app is a figure *for a stated amount* — 539 kcal per 100 g off Open Food
  Facts, an AI estimate for the plate it saw, a recipe's serving. The barcode screen instructs the
  user to "adjust the portion to match what you ate"; without this that instruction wrote 539 kcal
  against 30 g. The factor applies to the *current* pair rather than a remembered original, so
  there is no seed to carry and a run of stepper taps stays within a unit of the one-shot answer.
  A zero starting portion has no price-per-unit, so the amount moves alone.
- **Fiber, sugar and sodium are reported, never graded.** They ride every path a macro rides —
  `FoodEntry`, the favorite, the saved-meal item, the recipe serving, the FDC parse, the AI
  estimate, the export — but they have no *targets*: Mifflin–St Jeor yields calories and a 30/40/30
  split, and there is nothing on the profile to derive a fiber goal from, so `DailyTargets` and
  `ConfirmTargetsScreen` are deliberately untouched. Consequences: `MacroBar` stays three segments
  (fiber is a subset of carbs — a fourth would double-count the day), the three carry **no semantic
  colour** because they appear in no bar or chart, `MicronutrientLegend` renders *nothing* when all
  three are zero rather than a row of zeros against no goal, and `MicronutrientInputGroup` is a
  sibling of `MacroInputGroup` rather than three more rows inside it — one of that component's
  callers is onboarding's target screen. `0` means unknown-or-none, the same reading a missing FDC
  macro already gets. The entry group opens itself only when a value arrives non-zero, so a scanned
  packet shows its sodium without a tap while a quick add keeps the sheet its current height.
  They reach Google Health too, as `nutrients` entries beside `PROTEIN` — sodium converted to
  grams, since that array carries one unit and mg is the app's own.
- **A meal push that is rejected retries once without the three micronutrients.** Their enum
  names could not be verified (see the backlog), an unknown one fails the *whole* `nutritionLog`,
  and `pushMeals` records no link on failure — so a wrong guess would strand that meal forever,
  re-failing on every later sync. The fallback body is what makes guessing safe, and a value of
  zero is omitted anyway, so a quick add already sends it. Delete the retry when the names are
  pinned, not before.
- **FDC reports sugar under two ids** — `2000` on branded rows, `1063` on Foundation ones — so
  `toScannedProduct` tries the branded id first and falls back. Sodium (`1093`) is the one nutrient
  in the app counted in milligrams, which is why its stepper steps by 50 and its field is a digit
  wider than the macro fields.
- **Targets are editable from Profile, and a manual calorie target reprices the split.** The four
  `Profile` overrides used to be reachable only from onboarding's Confirm step, which left a user
  who wanted a different target with no path but a reinstall — Goals is now an editable card, like
  Water and Fasting, not a sheet or a route. Two things fall out of the overrides being nullable:
  `dailyTargets()` prices the 30/40/30 split off the *effective* calories, because 1800 kcal printed
  over a macro bar summing to 2400 is two numbers that disagree (per-macro overrides still win on
  top, and with no calorie override it is exactly `calculateDailyTargets`); and "Reset to
  calculated" exists because an override is a **pin** — without a way back to null, one nudge means
  a later weigh-in never moves the targets again. The calorie field is the stepper-only variant on
  purpose, the one place that rule bends: the write is clamped to `CALORIE_TARGET_KCAL`, and a
  clamped value re-seeds `StepperValueField`'s text, so a half-typed "1" would snap to "800"
  mid-keystroke. `CALORIE_FLOOR_WARNING` lives in `:core:data` beside the floors so the two screens
  that show it can't drift — it warns, it never blocks, and it is not a clamp.
- **Every numeric figure is typable, not just steppable.** `StepperValueField` (in
  `NumericStepperField.kt`) backs the calorie, macro, portion, duration and burn values; the ±
  buttons nudge a figure that is already about right. Stepper-only entry meant 320 kcal cost 32
  taps and 48 g of carbs cost 48 — which made the manual path, the offline fallback *and* the
  correction path after a low-confidence AI estimate all unusable. The field holds its own text so
  a backspace to empty stays empty while the model reads zero, and re-seeds only when the incoming
  value is a different *number*, which is what leaves a half-typed "1." alone.
  `NumericStepperField` keeps its read-only mode for callers that genuinely only nudge.
- **The portion stepper steps per unit** (`portionStep`): 10 for g/oz, 0.5 for cup/serving. Ten
  servings is not a nudge, and a fixed step of 10 is why a seeded recipe row could never become
  half a portion.
- **Delete gets an undo; user-authored things get a confirmation.** A swipe on a diary or exercise
  row is deliberate and loses one row, so it raises a snackbar with Undo (soft delete has no
  restore-by-id — `OnRestoreEntry` writes the row again from what the screen still holds, a new id
  for the same meal). A saved meal or recipe is something the user built, and its delete icon sits
  beside the one that logs it, so those ask first. Don't collapse the two into one pattern.
- **An edit supersedes a row; it never rewrites one.** Tapping a diary or exercise row reopens it
  in the sheet that logged it, and saving soft-deletes the old row and inserts the corrected one
  in a single `@Transaction` (`FoodEntryDao.replace`/`ExerciseEntryDao.replace`), carrying the
  original `loggedAt` across so the row keeps its place in the day. The id therefore changes,
  exactly like `OnRestoreEntry`'s undo. That is not incidental: `pushMeals()` skips entries in
  `links.pushedLocalIds(FOOD_TABLE)`, so an in-place `UPDATE` would leave the Google Health copy
  stale forever with nothing to notice it, whereas retiring the id lets the existing
  delete-then-push pass correct both sides for free. One transaction is also what stops the diary
  flow emitting a frame with the row missing. Two things ride along: the exercise sheet seeds
  `burnedEdited = true` so opening a past workout can't re-estimate its burn at today's weight,
  and `updateEntry` passes `steps` through untouched rather than re-deriving it like `addEntry`
  does, because an imported workout's step count is the watch's own figure.
- **The edit sheet hides the add sheet's four shortcut panels.** Recipes, saved meals, recents and
  search all seed a *new* log, and two of them write rows the moment they're tapped — which is not
  a thing that can happen while one row is being corrected. Same sheet, `editing` flag, one `if`.
- **The `RecipeBuilderRoute` carries no bottom nav and no FAB**, joining the two camera flows in
  `AppScaffold`'s `showChrome` for a different reason: it is an authoring screen with its own Save,
  and leaving the tab bar up put a "Log food" FAB over it and let a tab tap walk away from a
  half-written recipe without the discard question `NavigationBackHandler` asks.
- **A fast is a session, not a day.** Fasts cross midnight by design, so `fast_session` holds
  `startMillis`/`endMillis` rather than an epoch day, and `endMillis IS NULL` *is* the active-fast
  marker — no status column, no "currently fasting" flag on the profile, so the two can never
  disagree. `FastSession.dateEpochDay` keys off the day it **ended**, the same choice `SleepNight`
  makes: a 16-hour fast started at 20:00 is yesterday evening's discipline paying off at lunchtime,
  and charting it on the start day would put every bar a day early.
- **`goalHours` is snapshotted onto each fast at start.** The target lives on the profile
  (`Profile.fastingGoalHours`, nudge-only over `FAST_GOAL_HOURS`), but raising it next month must
  not retroactively un-hit a fast already finished — the same rule `step_day.burnedKcal` and
  `exercise_entry.burnedKcal` follow. Changing the target moves the Progress chart's goal *line*
  and prices the *next* fast; never a bar already drawn.
- **Fasting is not a streak domain**, for the same reason mood and sleep aren't: the streak means
  "you logged something you did", and a timer left running is not that. So `FastingRepository` has
  no `observeLoggedDays()`, `loggedDays()` is untouched, and Home's `isDayOne` ignores fasting.
- **Discarding an *active* fast is a hard delete, and that does not breach the soft-delete rule.**
  An unfinished fast never became history — it is the mis-tap being undone. `deleteActive()` can
  only ever reach a row with `endMillis IS NULL`; completed sessions have no delete path at all.
  The guard against two open fasts is a read-then-write in `start()`, not a unique index: SQLite
  treats every NULL as distinct, so `endMillis` cannot carry one that means anything.
- **Only completed fasts are exported, and the fasting goal notification is the one one-shot.**
  A running fast is a timer, not history, so it never reaches the file (schema v6) — restoring one
  on another device would resume a clock nobody started there. And every `Reminder` entry is
  periodic and reconciled off the profile row, whereas a fast's target lands at an hour the user
  chose by stopping eating: `FitPulseApplication` derives it off `observeActive()` instead,
  `distinctUntilChanged` on the **absolute** target (the profile re-emits on every weight edit, so a
  delay recomputed against a moving `now` would churn the queue), and `FastingGoalWorker` re-reads
  the fast at fire time so it can never congratulate someone on a fast they ended two hours ago.
- **The widget prints the fast's target *time*, not its elapsed time.** Glance cannot tick and
  `updatePeriodMillis` is 30 minutes, so "14h 20m" would be wrong for up to half an hour after every
  redraw; "Fasting until 12:30" is computed once and stays true. Home's card, which *can* tick, runs
  a 1-second ticker only while a fast is open and reads it inside a draw lambda.
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
- **The AI insight is an upgrade to the insight card, never its source.** Home renders
  `uiState.aiInsight ?: insightFor(...)`: the three rules that shipped before there was a model
  still draw the card offline, on a failed call, and when the model answers `NONE` — the offline
  rule applied to the one Gemini feature that isn't food recognition. Consequences worth keeping:
  the answer comes back as **plain text, not JSON**, so the only validation (`sanitizeInsight`) is
  a pure function a JVM test can reach, unlike the photo path's `org.json` parse; it is cached in
  one `@Volatile` day-keyed field rather than a table, because an insight is derived like the
  streak and has no meaning tomorrow; and `HomeViewModel` asks **once per ViewModel**, waiting for
  a loaded, non-day-one state, because the state flow re-emits on every glass of water. That last
  one is why `observeHome`'s collect carries `aiInsight` across by hand — it rebuilds the whole
  state from Room, and a plain `reduce { newState }` would erase the line on the next tap. The
  prompt is sent the gaps (consumed vs target, water, streak, weekly weight delta) and never age,
  sex, height or absolute weight — same data-minimisation rule as the health backfill.

- **A blood pressure reading is a reading, not a day.** `blood_pressure_reading` holds
  `takenAtMillis` per row, because morning and evening readings are the entire point and a
  day-keyed table where the second overwrites the first throws away what is being tracked — the
  opposite call to `mood_day` and `heart_day`, which aggregate because that is all the source
  gives. There is deliberately **no stored date column**: `BloodPressureReading.dateEpochDay`
  derives it with `epochDayOf`, so the two can never disagree, the same reason `fast_session`
  carries no status flag beside its null `endMillis`. The tab folds the readings with `byDay()`
  before charting, and `averages()` is a mean of the **days** — a morning someone measured four
  times is not four mornings, `heartAverages`' rule. Un-deleting is not a thing, so the delete is
  soft like a diary row's.
- **`pulseBpm` of 0 is "not entered", never a pulse of zero**, and it keeps its own denominator in
  `averages()` — mood's rule, so a month of readings off a cuff that shows no pulse reports a blank
  pulse rather than a quietly halved one. It never reaches `heart_day`: that table is the watch's,
  and folding a cuff reading into it would claim a measurement the watch never took. `addReading`
  clamps the two pressures into their ranges but leaves a zero pulse alone, since clamping it would
  invent a figure nobody read.
- **`categoryOf` is checked worst-first, and the order is load-bearing.** 185/70 is a crisis; a
  normal-first chain would read its diastolic and call the same reading Elevated. Only
  `BloodPressureCategory.severe` (Crisis alone) is coloured, in `error` — the trend-arrow rule
  applied once, rather than a five-colour scale that would have the app grading a reading. The
  labels carry no advice copy: naming the band is what makes 128/82 mean something, and that is
  the whole of the claim.
- **Blood pressure has a Google Health scope, and it is deliberately not requested.** The four the
  app already asks for cap it at 100 users pending OAuth verification and a CASA assessment; a
  fifth would need its own justification on that form. Manual entry only, like measurements — and
  not a streak domain, for the reason mood, sleep, fasting and supplements aren't.
- **Home shows the *latest* reading, not today's.** The three watch cards are hidden when today has
  no row because a watch fills them in nightly; nobody takes their blood pressure daily, so a card
  that vanished on the six days between readings would be a card nobody ever saw. It is hidden only
  until the first reading exists, like `SupplementsCard`. It is read-only and does not navigate:
  logging needs the sheet, and the sheet lives in `:feature:progress`.
- **The Blood pressure tab scrolls itself**, joining Photos outside `ScrollingTab` — its list is
  per-reading rather than per-day, so a 3M window can hold a couple of hundred rows. Its delete
  asks first rather than raising an undo snackbar: the diary's swipe-and-undo needs a snackbar host
  Progress doesn't have. `BloodPressureViewModel` carries both the save and the delete because the
  tab and its sheet sit under one `ViewModelStoreOwner`, which leaves `ProgressViewModel` the
  read-only container its KDoc says it is. The sheet asks for no date or time — a reading is
  stamped when Save is tapped, and transcribing a paper log is not the workflow a backdated
  weigh-in is.

- **A supplement carries a dose *label* and a times-per-day *number*.** The dose is free text —
  "2000 IU", "5 g", "one scoop" — and nothing parses it, for the same reason fiber, sugar and
  sodium are reported and never graded: there is no field on the profile a supplement target could
  be derived from. `timesPerDay` is a real number only because "2x daily" turns the day's tick into
  a count out of N, which is what makes the Home row a counter rather than a checkbox. One tap
  advances a dose and wraps to zero at the target, so both shapes share one gesture and a mis-tap
  is corrected by the gesture that made it — the same call `MoodCard`'s rows make.
- **`supplement_day.dueTimes` is snapshotted at write time and never re-read.** Dropping a
  supplement from twice daily to once next month must not turn a past day that read "2 of 2" into
  "2 of 1" — the rule `fast_session.goalHours` and `step_day.burnedKcal` already follow. The
  Progress chart therefore prices every bar off that day's own summed `dueTimes`, and
  `Supplement.timesPerDay` only ever prices *today*.
- **Ticking one supplement writes the whole day's row set.** `SupplementDao.setTakenOn` inserts a
  zero row for every active supplement on that day (IGNORE, so it can never reset a count already
  tapped) inside one `@Transaction`, then sets the one that was tapped. Without the seed the
  chart's denominator would be only whatever was ticked, and someone who took 1 of 3 would chart
  100%. *ponytail: a supplement added later the same day gets no row for that day — it starts
  counting tomorrow.*
- **A `taken` of 0 is a real row, an absent row is a gap.** Un-ticking is an update, which keeps
  this domain inside the soft-delete-only rule with no deleted flag on the day table — the same
  reading `mood_day`'s zero has, and a fully-zeroed day is simply not exported. On the chart the
  two are drawn differently on purpose: a zero day is a slot with no height (seen and missed), a
  day with no rows draws nothing (before the user had a list at all).
- **Deleting a supplement is a soft delete, and the export carries its id.** Past `supplement_day`
  rows keep a row to name, so removing something today can't rewrite the chart's history — which
  is also why soft-deleted supplements ride the backup file. The id is the one thing in the whole
  export that travels verbatim: a day row points at a supplement by id, so letting Room regenerate
  them on import would restore a log of ticks with nothing to tick.
- **Supplements are not a streak domain**, same reasoning as mood, sleep and fasting: the streak's
  four domains are things the user *did* that day, and adding a fifth now would change what a past
  run meant. `SupplementRepository` has no `observeLoggedDays()`, `loggedDays()` is untouched, and
  Home's `isDayOne` ignores supplements.
- **Home hides the card when the list is empty; Profile is the only place it is authored.** The
  card is hidden rather than rendered as an invitation, like the three watch cards — but for the
  opposite reason: there is nothing to import, there is nothing the user has written yet. Profile →
  Supplements has edit and delete and **cannot tick anything**, because a tick belongs to a day and
  Profile has none — the same division the food library draws against the add-entry sheet. Delete
  asks first (a supplement is user-authored, like a saved meal), and one sheet with `id == 0`
  meaning "add" is what keeps the add and the edit on one save path.
- **The supplement reminder is appended to the `Reminder` enum, never slotted in.** `ordinal` is
  the notification id, so inserting one beside the other daily reminders would re-point every
  notification already pending on a device. It rides `checksSupplements`, the third flag of its
  kind, and stays quiet both when everything is already ticked *and* when the list is empty — a
  reminder about an empty list is a nudge to open a screen with nothing on it.

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
- **Steps are the first of two types that don't ride `health_link`.** The API reports intra-day buckets
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
- **Heart rate takes the step shape, not the `health_link` one**, and for the same reason: the
  API reports intra-day samples and `heart_day` stores one row per local day, so there is no
  point-to-row relationship a link could key. `MAX(date)` in `heart_day` is the cursor, the window
  is day-aligned, days are replaced rather than merged, and nothing is written until every page
  lands. Not a streak domain, not exported, no manual write path — sleep and steps again.
- **A heart 403 is neither a revocation nor a sync failure.** Every other type reads a scope
  `HEALTH_SCOPES` explicitly requests, so a 403 there really is a revocation. Heart rate rides
  `health_metrics_and_measurements.readonly` on the *assumption* that a BPM reading is a health
  metric, and no live account has confirmed it. Reporting a wrong guess as a revocation would drop
  a good connection to "needs consent" forever; reporting it as a failure would put "Couldn't
  reach Google Health" on the Connections screen after every sync with nothing new to import. So
  `sync()` takes heart's items on success and discards every other outcome — a wrong guess costs
  the card and nothing else. Don't make this consistent with the other four until the scope is
  pinned.
- **`minBpm` is the day's lowest reading, never a resting heart rate**, and is labelled "Lowest"
  everywhere it appears. FitPulse aggregates whatever samples the watch happened to take; calling
  a minimum "resting" would claim a measurement nobody made. The day's other figure is a mean of
  the samples, but `heartAverages()` over a window is a mean of the *days* — a day the watch
  sampled twice as often is not twice the day. Progress's Heart tab windows anchored to today,
  like sleep and mood, and its chart is the one in the app whose bars are **not zero-based**:
  nobody's heart visits 0–45 bpm, so a zero-based axis would squash the beats that actually
  differ. Each bar spans that day's lowest reading up to its average.
- **Every window is re-queried one day behind the cursor.** Watches sync hours late; the primary
  key makes the overlap free. First sync backfills 30 days — asking for only what's needed is
  the data-minimisation answer on the verification form, not a performance tweak.
- **Imported workouts are ordinary `exercise_entry` rows**, so `budgetKcal()` and the streak
  pick them up with no special case. `RemoteExercise.toExerciseEntry()` is the *only* place one
  becomes a row, so no field can be dropped on the way in: `addEntry` re-derives a zero `steps`
  from the MET estimate, which would throw away the watch's own count and let `stepsCreditKcal()`
  credit the difference a second time. Imported weigh-ins *skip* days that already have an entry
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
  `recipe`, `search`, `shared`), `:feature:progress` (`progress`, `weight`,
  `measurement`, `photo`, `nutrition`, `mood`, `sleep`, `heart`, `fasting`, `supplement`,
  `pressure`, plus a `shared/` holding `RangeBarChart`, which Heart and Blood pressure both draw),
  `:feature:profile` (`profile`, `health`, `library`, `supplement`) and `:feature:onboarding`
  (`onboarding`, `health`, `shared`) are the worked examples. Grouping is by *subject*, not by
  owning screen: `ExerciseSection` sits under `exercise/` and `RecipePanel` under
  `recipe/` though `FoodScreen` renders both, and Progress's ten tab bodies sit
  with the charts they draw rather than with the shell that dispatches them. Only
  the `*Navigation.kt` file stays at the `ui/` root, because its route types and
  `<feature>Entries` are what `:app` reaches for.
- **`:feature:home` is deliberately flat**, and should stay that way. It holds exactly
  one flow with one ViewModel, so `ui/` + `ui/components/` is already what the rule
  above prescribes. Don't "finish the job" by sub-packaging it.
- **What earns a flow package is a second ViewModel**, not a second screen.
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
  implementation is done and verified.
