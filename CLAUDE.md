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

**`FEATURES.md` is the index of what already ships** — every screen, card, tab
and surface, plus a "Deliberately absent" list of what was ruled out. Read it
before proposing or building a feature, and add a line to it when one lands.
It says *what*; this file says *why*.

**`ROADMAP.md` is the index of what is planned** — one implementable spec per
feature, each naming its module, its schema cost, its pre-argued decisions and
what it deliberately excludes. A feature moves out of it into `FEATURES.md`
when it lands, and its decisions move into this file. It says *what next*.

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
  `exercise` (logged activity *and* the routines that seed one), `mood`, `health`, `fasting`,
  `supplement`, `bloodpressure`, `coach`. Two
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
- **Progress → Badges is derived, and that is what keeps the no-celebration rule intact.** Seven
  families of thresholds on one tab, every figure a fold over what `ProgressViewModel` already
  combines — no table, no schema bump, no repository, no export, and nothing to notify off.
  `badgeGroups()` sits in `:feature:progress/ui/achievement/` rather than `:core:data` for
  `recap()`'s reason: one screen shows it and every input is a `:core:data` type. Two of the
  seven read their thresholds straight off `StreakBadge` and `WeightBadge` rather than a retyped
  copy, so the tab and Home's streak card can never disagree about what a badge is worth; the
  streak family scores off `best`, never `current`, for the same reason `earnedBadges()` does. The
  weight family is *absent* for a Maintain goal (no direction to move), matching the card's hidden
  weight line — but a negative delta reads as zero rather than vanishing, because a badge row that
  disappeared on a bad week is a row nobody could trust. `BadgeDot` moved to `:core:designsystem`
  when the tab arrived (two screens draw it now), carrying its KDoc about why the colour fade is
  not the ruled-out celebration. *ponytail: days-logged and workouts ride windowed inputs — a dense
  year of nutrition, a rolling year of exercise — so both stop well inside a year; a `COUNT(*)`
  flow on the two DAOs is the upgrade path if that ever grates.*
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
- **Both viewfinders carry a gallery door and a manual door** (`ViewfinderActions`, in
  `:feature:food`'s `ui/shared/components/` because the two flows share it). A picked image runs
  the pipeline its screen's live camera runs — `decodeRotatedBitmap(context, uri)` then
  `startAnalysis` for the photo flow, `scanBarcode(context, uri)` then the same lookup for the
  barcode one — so nothing downstream can tell a pick from a capture, and the offline and cancel
  paths are the ones that already existed. Manual entry stays *inside* the flow (the photo flow's
  `NoFood` search screen, the barcode flow's blank confirmation): back returns to the camera, which
  is why neither needs a route or an exit signal to the diary. `ScanFlow.NoBarcode` is its own
  state rather than a flag on `NotFound` — "no barcode in that photo" and "we don't have that
  product" are different answers, and a flag would need resetting on every path back to Scanning.
  The gallery affordance is icon-only for a layout reason recorded at the call site; the flash icon
  the prototype drew beside it is still absent, since nothing implements it.
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
  every zero-padding at once in one unquoted query, which FDC ORs. The barcode scan is the *only*
  thing left in the app that calls FDC — free-text search is local, below — so `fdcGet` and
  `toScannedProduct` exist for that one caller.
- **Food search is a list shipped in the APK, not an API call.** `COMMON_FOODS` in
  `:core:data/food/CommonFoods.kt` is ~120 hand-written staples, per 100 g like every FDC row, and
  `searchCommonFoods()` is a case-insensitive substring over it — pure data, no table, no
  repository, no query, the `localMealIdeas()` shape. It replaced a `foods/search` call for three
  reasons that are the whole design: it answers with no debounce, it answers offline, and it spends
  nothing from the key budget. What it gives up is branded packages, which is what the scanner is
  for, and anything neither knows is still typed in by hand. A blank field is **every** food rather
  than an idle hint, paged at `FOOD_PAGE_SIZE` — a lazy list is not allowed inside `AppBottomSheet`
  (it hands its children unbounded height), so paging is also what fits where the panel is drawn.
  `FoodSearchViewModel` therefore takes no dependencies at all and exists for the page, which
  survives a rotation where a composable's `remember` would not.
- **The FDC key is a gradle property, and its budget is app-wide.** `fdcApiKey` lives in
  `~/.gradle/gradle.properties` and reaches the code as `BuildConfig.FDC_API_KEY` in `:core:data`,
  the same untracked-and-degrade-gracefully rule the release signing config follows — absent it the
  build still compiles and a *scan* reports `Failed` (search is unaffected; it needs no key). It is
  one signed key shared by every install, so the 3600 requests/hour ceiling is the *app's*, not each
  user's; only a deliberate scan spends it now, and `pageSize` is capped at 25 because FDC ignores
  `nutrients=` on this endpoint and ships ~21 KB per food.
- **The today-only repository overloads are deliberate** — Home and the streak
  genuinely mean today, so don't collapse them into the dated ones.
- **The diary's top field is a local filter over logged entries**, not a
  food search. Food search is `searchCommonFoods()`/`FoodSearchPanel`.
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
  on **three axes** — silhouette, eyes, one accent — because two characters differing only in
  outline read as the same character badly drawn; any two of them differ on at least two of the
  three, which is what `MascotCharacterTest` asserts now that colour can't help. What stays shared
  is the **mouth geometry and the state vocabulary**: all five states read identically whichever
  buddy is picked, so no character can come to mean something. The whole avatar is one
  `Canvas` rather than a shaped `Box` — that is what lets an antenna or an ear sit *above* the head
  (`topInset`/`sideInset` carve the room, and Bibo's are zero so it renders exactly as it always
  has) with nothing clipping the Celebrating sparkles.
- **Colour is the user's second pick, not the character's.** `MascotPalette` rides `AppTheme` beside
  `LocalMascot` off `Profile.mascotPaletteName` — a nullable String resolved by `mascotPaletteOf()`,
  the same shape and the same degrade-to-default reading as `mascotName`, and only the picker passes
  `palette` explicitly. Its five entries are the five pairs that *were* the characters' fills, so
  every one is already proven in light, dark and all three contrast schemes and no new colour was
  invented: `Soft` (Bibo's, the default, so an untouched install is unchanged), `Bold`, `Muted`,
  `Contrast` (the one pair that inverts with the theme) and `Neutral` (the one whose *features*
  carry the accent — the grey chassis with a lit face that made Zed read as a machine, now available
  to any buddy). The list stops there because of what a fill may not be: never `tertiary` or
  `tertiaryContainer` (the AI accent and the carbs colour), never `error` (off-track only), and
  never `secondaryContainer` — both picker rows fill their selected cell with exactly that, and a
  mascot that vanished the moment it was chosen is the one thing a picker must not do. The colour
  cells are plain swatches sharing the buddy row's `PickerCell`, each ringed in `outlineVariant`
  whatever its fill — `Neutral` is a near neighbour of the card behind it. They carry no visible
  label, unlike the buddy cells: the scheme flips in dark mode, so a hue name would be wrong half
  the time — the name rides a `contentDescription` instead. `mascotSwatchColor()` is public where
  `mascotColors()` is internal because a circle needs the fill and nothing else.
- **The mascot blinks and breathes, and both rest at phase `1f`.** One
  `rememberInfiniteTransition` inside `MascotAvatar` drives a ~140ms blink every 3.6s and a 2.6s bob
  of 2% of the avatar's height, so no call site passes anything and none can forget to. The end
  value is the resting pose on purpose: Compose pins an infinite transition to its end and suspends
  when **Remove animations** is on, so a `RepeatMode.Reverse` cycle would park the mascot mid-bob
  with its eyes shut for exactly the people who asked for stillness — that is what
  `MascotAvatarTest` guards. The blink borrows the closed eyes `Sleepy` already draws rather than
  adding five more shapes; Sleepy never blinks and still breathes. The start offset is per instance
  so the picker's cells don't blink in lockstep, and frozen under `LocalInspectionMode` so previews
  render the rest pose.
- **The Home card order is one nullable String on `Profile`, not a `home_card` table.**
  `homeLayout` holds the card names in display order with a `-` prefix on the hidden ones, and
  `homeCardLayout()`/`encodeHomeCardLayout()` in `:core:designsystem` are the only things that read
  or write that format — the exact shape `mascotName` takes, for the exact reason: the card
  vocabulary (`HomeCard`) lives in `:core:designsystem` beside `MascotCharacter` because the screen
  that renders it (`:feature:home`) and the picker that sets it (`:feature:profile`) are two feature
  modules, and those never import each other. `:core:data` therefore stores the *name*. Null means
  the declaration order with nothing hidden, and "Reset to default" writes **null rather than the
  default encoded**: a stored default is a pin, and it would freeze that install's Home against
  every card a later build adds. The parser bends in two directions for the same reason — a name
  this build doesn't know is **dropped** (a card retired later can't leave a hole), a card the
  string never mentions is **appended visible** (a card added later must not be silently hidden from
  exactly the users who customised their Home). `HomeCardLayoutTest` guards both.
- **The greeting and the AI insight are pinned; the other thirteen cards move.** The greeting is the
  app's one door to the coach, so hiding it would strand a whole feature, and the insight owns an
  expand/collapse whose *exit* is what stops the cards below it jumping — neither survives being
  dropped into an arbitrary slot. Everything else on Home is one `forEachIndexed` over the resolved
  layout, each slot `key`ed on its card so the entrance stagger follows a card across a reorder
  rather than staying with the slot it left.
- **A visibility switch can only ever remove a card, never force one.** Every data gate that was in
  `HomeCards` stays inside its `when` branch: Sleep left *on* with no watch synced is as absent as
  it was before the editor existed, and the editor row says so under the switch (`HomeCard.note`) —
  a control that looks broken is worse than one that explains itself. A hidden card's flows are
  still collected: Home combines everything in one chain, and splitting it per card would be a
  large conditional-flow change for an unmeasured gain.
- **Profile → Home layout authors; Home renders.** The same division the supplement list draws
  against Home's card, and it is why the editor is a route above Profile (`SupplementsRoute`'s
  argument — a thirteen-row list outgrows a sheet) with its own `HomeLayoutViewModel`: a second
  `ViewModelStoreOwner` sharing `ProfileViewModel` would spin up a second copy of all ten of that
  one's repositories to write a single column. The screen seeds a working copy off the first
  *loaded* emission and never re-seeds — its own writes come back through Room, and re-seeding on
  one mid-drag would yank the row out from under the finger. Rows are a fixed height so the drag
  works off one constant instead of measuring, the handle alone starts the drag (a long-press on
  the row would both delay the gesture and compete with the list's scroll), and every row carries
  **Move up / Move down** accessibility actions, because a drag nobody using TalkBack can perform
  is not a control.
- **Every recap window is rolling-and-ending-today**, never a calendar week or month — a calendar
  week reports a half-empty Monday. The card is *hidden* when nothing was logged in the window
  rather than rendering zeros, and its "days logged" uses the streak's four-domain definition
  while its calorie average uses food days only — the two denominators can differ, so the card
  says which is which.
- **`RecapPeriod` is a third window vocabulary, and it is not `ChartRange`.** `ChartRange` has no
  week, and `List<WeightEntry>.inRange` deliberately anchors to the latest *entry* — which is
  right for a chart re-centring on its data and wrong for a page headed "Last 30 days". So
  `recap()` slices every sparse input itself against `today - (days - 1)`, and only the dense
  `dailyNutrition` gets a `takeLast`. The card's weight cell stays the *seven-day*
  `trendVsSevenDaysAgo` on every period, because that is the figure it has always shown;
  `Recap.weightArcKg` is the window's own start-to-latest answer, and the screen labels both so
  neither can be read as the other. A single weigh-in in the window reports **no** arc rather
  than a zero delta — one end is not two.
- **The recap screen is an overlay inside the Progress tab, not a route.** A route earns its own
  `ViewModelStoreOwner`, and with it a second copy of `ProgressViewModel`'s twelve repositories,
  to render a page that writes nothing — so it reads `ProgressUiState` verbatim and folds its own
  `recap()`, the call `TimelapseScreen` and `PhotoComparisonScreen` already make (and, like them,
  it wires its own `NavigationBackHandler`, or back would leave the tab). The Progress header's
  icon opens it; the week share it used to open lives inside it now, on whichever period is
  showing. Its movement row and its lift notes are drawn for Month and Year only, which is what
  leaves the shipped weekly card untouched.
- **The share image is one card, never the page.** `captureToPicture` records what was *drawn*,
  so capturing the recap's scrolling column would hand the chooser a screenshot clipped at the
  fold. `ShareRecapSheet` therefore still renders exactly one `RecapCard` plus the brand footer —
  the preview is the PNG, which is the whole contract.
- **Badges are absent from the recap, and that is not an oversight.** `BadgeGroup` records no earn
  date, so "badges earned last month" is unanswerable; a windowed report showing all-time badges
  would be a category error. The Badges tab already answers it. *ponytail: add them when a badge
  records when it was earned.* Sleep, heart, blood pressure, fasting and supplements are out for a
  plainer reason — each would be another card on one page, and none has earned the scroll yet.
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
  window implicit would be a card contradicting its own heading. It stays out of `recap()`
  for the same reason — every other field there is a seven-day figure. The line is
  `onSurfaceVariant` on every surface, never `error`: the delta beside it already carries the
  verdict colour, and a red date reads as a second one.
- **The energy check-in measures the target the formula guessed, and it stores nothing.**
  `energyCheckIn()` sits in `:core:data/profile/` — pure derivation, no table, `goalProjection()`'s
  shape — and back-computes maintenance as `average intake - slope × 7700`: the deficit the weight
  change accounts for, added back onto what was eaten. Its window is **28 days, not the
  projection's 30**, because a 30-day mean carries four extra weekdays and weekend intake is
  systematically higher — four whole weeks weigh every day of the week equally. It fits the rate
  with `List<WeightEntry>.slopeKgPerDay()`, the projection's own least-squares fit made `internal`
  rather than copied, so the two cards on the Weight tab can never quote different trends; the goal
  adjustment and both clamps are `DailyTargets`', for the same reason. It refuses rather than
  guesses — half the window logged, four weigh-ins over a fortnight, a weigh-in inside the last
  seven days, and a maintenance inside `MAINTENANCE_SANITY_KCAL` — but still reports the two counts,
  because a card that goes quiet without saying why gives nobody a reason to keep weighing in.
- **Applying it writes `Profile.calorieOverrideKcal`, which is why there is no dismissal state.**
  A measured target *is* a pin — it should not keep drifting with the next weigh-in — so it reuses
  the column a typed target already uses, inherits "Reset to calculated" as its undo, and adds no
  schema, no migration and no export bump. Applying also drives `deltaKcal` to zero, so the button
  takes itself away with nothing persisted; a "dismissed" flag would be state that exists only to
  hide a suggestion the arithmetic has already withdrawn. `MIN_MEANINGFUL_DELTA_KCAL` (75) is where
  the adjustment drops inside the estimate's own noise. The overlay is `RecapScreen`'s call — an
  overlay in the Progress tab with its own `NavigationBackHandler`, not a route — but unlike the
  recap it writes, so it carries `EnergyCheckInViewModel` (`BloodPressureViewModel`'s precedent) and
  `ProgressViewModel` stays read-only. It shows every figure it measured from: a screen that tells
  someone to eat 250 kcal more without showing its working is asking to be believed rather than
  read.
- **A measured maintenance already contains the user's workouts, and the check-in only says so.**
  `budgetKcal()` is still the one place burned calories fold in, so with `addExerciseToBudget` on,
  a workout is credited on top of a target that already accounted for it. The overlay names the
  switch and the screen it lives on and changes nothing itself — the warn-and-point shape
  `HomeCard.note` uses. *ponytail: no Home card, no coach field, no reminder and no history of past
  adjustments — nothing is stored, so there is nothing to chart.*
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
- **Launcher shortcuts are static resources, never `ShortcutManager` dynamic ones.** A dynamic
  shortcut needs code that runs to publish it and state to keep it in step with what it points at;
  a static one is a resource the launcher reads, and nothing in the four varies per user. A
  shortcut *cannot* name a route — routes are Nav3 keys inside a Compose back stack, not Activity
  intents — so `@xml/shortcuts` carries `EXTRA_ACTION`, the sibling `EXTRA_TAB` already had, and
  `MainActivity` resolves it with `shortcutActionOf()` into nullable state cleared once consumed:
  `tabRequest`'s exact shape, which is what lets a shortcut tapped on an already-running app
  re-point it the way a second notification does. `shortcutActionOf` takes the extra's **String**
  rather than the `Intent` for the reason `mascotCharacterOf` does — it is the pure half a JVM test
  can reach, and an unknown name (a shortcut pinned by an older build) degrades to null.
- **`EXTRA_ACTION`'s vocabulary is the FAB sheet's rows, and water is its one exception.** A
  shortcut is `QuickActionSheet` with the tap pre-made, so *Say what you ate*, *Log food* and
  *Weigh in* resolve to the same `topLevelBackStack.add(…)` or the same `ActiveSheet` value that
  sheet's own row does, each carrying day `0` — a launcher tap has no diary date, the reason the
  FAB itself passes `0`. **Add water has neither**: water is an inline `WaterGlassRow` on a Home
  card, and that card can be hidden by `Profile.homeLayout`, so a navigational shortcut could land
  on a screen with no water on it. It is therefore handled in `MainActivity` as a *write* — the
  shared `addGlass()`, then Home so the card shows the new count — and `AppScaffold`'s `when` says
  so rather than pretending it is a destination.
- **`MainActivity` is `singleTop` because `@xml/shortcuts` cannot carry intent flags.**
  `Intent.parseIntent` inflates `action`, the target, `data`, `mimeType` and `<extra>`, and no
  `flags`, so the notification's `FLAG_ACTIVITY_CLEAR_TOP` has no shortcut equivalent; without
  `singleTop` a shortcut tapped on a running app would stack a second `MainActivity` instead of
  reaching `onNewIntent`. It is also what makes that method's comment true for both callers.
- **Only the water reminder gets an action button, and answering it cancels it.** `addGlass()` is a
  single unambiguous write already shared with the widget and the watch, so a fourth surface caps
  at the same goal for free; "Log breakfast" has no single write — it needs the sheet, and a button
  that opened a sheet would be the tap the notification body already is. `WaterActionReceiver`
  re-reads Room before writing, the rule `PhoneWearListenerService` follows (a notification posted
  at 11:00 and tapped at 14:00 must not write against the count that was true when it was posted),
  reaching the repositories through Koin's global context and doing the write inside `goAsync()` —
  `onReceive` is on the main thread, so the wear service's `runBlocking` is not available here. The
  notification id is the `Reminder.ordinal` already, so the cancel is one call; a notification left
  in the shade with an updated count would be a second surface reporting today's water, and the
  widget is that. Nothing pushes to the widget or the watch from the receiver — the Application's
  `todaySnapshotFlow` collector fires the moment Room emits.
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
  pushed by `FitPulseApplication`'s collector the moment Room emits, and the flows themselves
  re-point at midnight (`todayFlow()`), so the tick is not what keeps a *live* session honest.
  It is what restarts a session that ended — a widget nobody has touched since yesterday has no
  collector left to re-point.
- **One snapshot type, three glanceable surfaces.** `TodaySnapshot` in `:core:today` is what the
  home-screen widget, the watch app and the watch tile all draw, and `todaySnapshotFlow()` in
  `:app/today/` is the only thing that builds one. The widget's Glance session and
  `FitPulseApplication`'s collector were two near-identical `combine` chains over the same Room
  flows before the watch arrived, which is exactly how two surfaces come to disagree about today;
  there is now one chain and `distinctUntilChanged` compares the snapshot itself rather than a bag
  of raw flow values. `todayFlow()` is in that combine for the streak alone — every other input is
  a today-only overload that re-points itself, but `streakStats` takes the day as an argument.
- **`:core:today` depends on kotlinx.serialization and nothing else, and that is the whole point.**
  It is on the watch's classpath, so a dependency on `:core:data` there would ship Room, Firebase
  AI and play-services-auth to a wrist. Two consequences: `unit: UnitSystem` became
  `waterLabel: String` (the phone owns the profile, so the phone formats it with the existing
  `waterVolumeLabel()`), and `formatClockTime`/`todayEpochDay` are duplicated as two stdlib
  one-liners in `:wear/ui/Clock.kt` rather than shared. The snapshot travels as one JSON string in
  one `DataMap` key, not a key per field: the two APKs update independently, and `decodeSnapshot`
  drops a field it doesn't know rather than throwing.
- **The watch has no database and is not getting one.** Room stays the phone's, and the Data
  Layer's own persisted data item *is* the watch's cache — the last snapshot is readable with the
  phone switched off, so a cold start out of range draws the morning's numbers instead of a
  spinner. Nothing pushed yet is drawn as "Open FitPulse on your phone", never as zeros: a watch
  reporting a 0 kcal day the user has been eating through is worse than one admitting it doesn't
  know. `TodaySnapshot.dateEpochDay` is what lets the watch tell yesterday's push from today's.
- **The watch sends an intent to log, never a row.** Two messages, `/fitpulse/add-glass` and
  `/fitpulse/toggle-fast`, both with **no payload**: `PhoneWearListenerService` re-reads Room
  before writing, exactly as the widget's button does, so a wrist showing breakfast's snapshot
  can't add a glass to a stale count or end a fast the phone already ended. The glass goes through
  the shared `addGlass()` so the two surfaces cap at the same goal. A message that isn't delivered
  is *reported* — the watch has no Room to write to, and an optimistic tick that evaporated on the
  next push would be worse than a refused one. *ponytail: no offline queue; a data-item outbox
  deleted by the phone once applied is the upgrade path.*
- **The watch is pinned to the dark scheme and to Wear's own typography.** `Profile.darkThemeOn`
  reaches the widget because a home screen can be light; a watch face is black by convention and
  by battery, and Wear Material 3 is drawn against black. Colours still come from the frozen
  `Color.kt` (`:wear` maps the `*Dark` vals into Wear's `ColorScheme`, and the tile maps the same
  vals into protolayout's identically-named one — two classes with no common supertype, so a
  shared mapper would be longer than either). Contrast schemes are absent for the widget's reason:
  no `UiModeManager.getContrast()` equivalent. Typography is Wear's, the call Glance forced and
  this one makes freely: its scale is drawn for a round display.
- **The tile shows and never writes; the watch app is where the taps are.** A tile's targets are
  coarse enough that "+1 glass" there is a glass logged by a sleeve, and the app it opens is one
  tap away. It has no lifecycle to hold a ViewModel, so it reads the stored data item directly —
  the call `TodayWidget` and `ReminderWorker` make. A phone cannot poke a tile either, so
  `WearDataListenerService` on the *watch* turns a push into a refresh request; the tile's
  30-minute freshness interval is the fallback for a push that never came, not the mechanism. It
  is built on `Material3TileService` (suspend, no `ListenableFuture` plumbing) with
  `allowDynamicTheme = false` — dynamic colour is disabled app-wide, and a tile following the
  watch face's wallpaper would be the one FitPulse surface that didn't.
- **`:wear` is one screen with no navigation graph.** The watch app is today; the diary, the
  charts and the coach stay on the phone, and saying that by omission beats a wrist-sized diary.
  It keeps the house architecture (Koin + Orbit, the `*Data`/`*State`/`*ViewModel`/`*Screen`
  quartet, flat like `:feature:home` and `:feature:coach`) because that rule is binding; the tile
  and the two listener services do not, because they are system surfaces. Its `applicationId` and
  version must move with `:app`'s — that is what pairs the two halves rather than shipping two
  products — and it is signed with the same key.
- **The coach and the daily insight describe the same day, in one place.** `InsightRequest` is
  the *only* payload either sends — the goal, the calorie/macro/water gaps, the streak and the
  weekly weight delta, still never age, sex, height, absolute weight, name or email. `insightFor()`
  and `insightRequest()` moved into `:core:data/insight/` when the coach arrived, because two
  feature modules now need them and `:feature:*` modules never import each other — the same call
  `goalProjection()` made. Home builds the request from state it has already combined for its
  cards; the coach has no such state and uses `observeInsightRequest()`, which combines the seven
  flows itself. Both land in `insightRequest()`, and both prompts format their numbers with
  `dayNumbersBlock()`, so a field added to one is shown by the other — a coach contradicting the
  card that sent the user to it is the failure this prevents. The coach is additionally told *what
  it does not know* (no yesterday, no individual meals, no weight) and pointed at the tab that
  does, because a free-form question will otherwise be answered with an invented figure the diary
  contradicts two taps away.
- **A question is only persisted once it is answered.** `CoachRepository.send()` writes both rows
  in one `@Transaction` when the reply lands, so `chat_message` needs no `pending` column and there
  are no half-conversations to reconcile after process death. A call killed by leaving the screen
  loses the un-sent question — the reading `FastingRepository.discardActive()` gives an unfinished
  fast: it never became history. A retry is therefore a fresh send, not a repair, which is why
  `CoachFailure` carries the question text. Clearing the chat is a soft delete like everything
  else, and it *asks first* — a conversation is user-authored, the saved-meal rule, not the
  diary's swipe-and-undo.
- **The coach's model is rebuilt on every send; the insight's is a field.** Its system instruction
  carries the day's numbers, and those move while the screen is open — a glass logged in another
  tab must not leave it quoting a stale figure. A `GenerativeModel` is a config object, so this
  costs nothing. Nothing is cached either, unlike the insight's one line per day: every question is
  its own answer. `sanitizeReply` is the whole trust boundary and keeps line breaks where
  `sanitizeInsight` collapses them (an answer legitimately spans a short paragraph), and rejects
  past `MAX_REPLY_CHARS` rather than truncating, for the reason the insight cap gives.
- **The mascot greeting card is the app's one door to the coach.** The insight card would be the
  more contextual tap and is the wrong one: it is hidden on day one, hidden when the model has
  nothing to say, and gone once dismissed, so a door on it is a door that isn't there most days.
  The `AIChip` under the greeting is what makes the tap visible. `CoachRoute` is a route above the
  Home tab rather than a fifth tab or a sheet — `AppScaffold`'s existing `isTopLevel` rule then
  gives it a back toolbar with no bottom bar and no FAB, which is exactly what a chat with a
  keyboard wants, and no new case was added there.
- **The coach is not exported, not a streak domain, has no reminder and no widget surface.** The
  backup file is a record of what the user *did*; a conversation about one day's numbers has no
  meaning restored on another device — `health_link`'s reasoning. And talking to a coach is not
  logging.
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

- **A strength workout is an `ExerciseEntry` with sets, not a second kind of thing.** One table for
  the workout, one child table for the sets, and `sets.isEmpty()` is what says "cardio" — the
  discriminator rule `saved_meal.servings` already follows. So the streak, `budgetKcal()`,
  `burnSeries()`, the diary row and the Google Health import all keep working with no special case,
  and `ExerciseEntry.sets` is defaulted precisely so every existing construction site stayed valid.
  Two levels, not three: each `strength_set` row carries its own `exerciseName`, and "the exercises
  in this workout" is a `groupBy` — the call `saved_meal_item` makes with its plain `mealId` column,
  down to `entryId` being an indexed column rather than a foreign key.
- **A set's `weightKg` of 0 is bodyweight, and that is a real value** — not the "never entered"
  reading `mood_day`'s zero and `pulseBpm`'s zero have. It counts no volume and
  `estimatedOneRepMax` returns 0 for it, so a bodyweight set charts and lists without ever claiming
  a record in kilograms; the editor's "Add set" therefore gates on a name and reps, never the load.
- **An edit supersedes the workout, so `replace()` re-points its sets in the same transaction.**
  The id changes (the existing rule), which would orphan the children — so the old rows are
  hard-deleted and re-inserted under the new id. That is not a breach of soft-delete-only for the
  reason `discardActive()` gives: the superseding row carries the history, and a child of a row
  that no longer exists never became history of its own. `strength_set` consequently has no
  `isDeleted` column at all; every read joins back to `exercise_entry` filtering on the parent's.
- **Records and volume are derived, no table** — `badgeGroups()`/`goalProjection()`'s call. That is
  why the Progress Strength tab needed **no `ProgressViewModel` change**: `exerciseEntries` already
  carried the year window, and once the sets rode on the entry the tab had its data. Ranking is by
  **estimated 1RM (Epley), not heaviest weight** — 100 kg × 1 and 80 kg × 8 are not comparable on
  the bar alone — and `LiftRecord.dateEpochDay` is when the best was *first* hit, so matching it
  again doesn't reset the date. The tab's stats re-fold over the *selected* window so they can't
  describe different days from the chart above them, while records stay all-time: re-scoring a best
  against a 1M filter would retire records every month.
- **"Repeat last workout" reads back the entry that already exists**, so it costs no schema, and
  `recentStrength` selects on *having sets* rather than on `type = 'Strength'` — a strength session
  logged through the sheet has none, and a run of those would fill the limit with workouts there is
  nothing to repeat or suggest from (it also keeps an enum name out of the SQL).
- **A routine is a saved meal for workouts, and it stores no load.** `routine`/`routine_lift` mirror
  `saved_meal`/`saved_meal_item` down to the plain non-FK child column and the join-in-Kotlin fold,
  because it is the same idea: a thing authored once by *naming what is already on screen* and
  re-used later, never linked to what it produced — rename or delete one and not a row of history
  moves. What earns the second concept is what a routine deliberately leaves out: `routine_lift` is
  `(exerciseName, sets, reps)`, and a programme is "Squat 3×5" whose load moves every week, so a
  stored target would go stale within days and turn the routine into something to maintain.
  `toSets()` prices each seeded set at **what was last lifted** (`lastPerformances()`, falling back
  to 0 — bodyweight, a real value here), which is progressive overload for free and one column
  fewer. *ponytail: a per-lift target load is the upgrade path if users ask; it would ride the
  entity and win over the last-lifted figure in `toSets()`.*
- **`lastPerformances()` is derived off a read the screen already makes.** No table and no query of
  its own — a third fold over the `recentStrengthEntries()` that already feeds the repeat seed and
  the lift chips, so the three can never disagree. It answers "what do I put on the bar today"
  where `personalRecords()` answers "how strong am I", which is why it takes the most recent
  session's top set rather than the all-time best, and it is keyed by `liftKey()` (trim +
  lowercase) because lift names are free text and the chip row already matches them that way. It is
  drawn under the *exercise field*, not beside the routine chips: a freestyle session needs the
  number as much as a routine does. *ponytail: `RECENT_STRENGTH_WORKOUTS` = 10 is the ceiling — a
  lift untouched for ten sessions reads as new and seeds at bodyweight; a per-lift `MAX(date)`
  query is the upgrade.*
- **A routine is authored by naming a workout, never in a builder.** "Save as routine" collapses the
  set list with `toRoutineLifts()` (`groupBy` the lift, count the sets, take the *modal* reps — 8/8/6
  is a routine of 8s with a set that fell short), which is the gesture "Save this meal" makes on a
  diary section, and it saves nothing else: logging the session is still the Save button beside it.
  Starting one shares "Repeat last workout"'s guard — offered only while the set list is empty,
  because it seeds the whole list. There is no confirmation toast when a routine is saved (the
  saved-meal path has none either); the button reports its own result and re-arms when the set list
  changes, so a fuller session can be saved again.
- **Profile → Workout routines renames and deletes; it cannot start one.** The division the food
  library draws against the add-entry sheet: starting a routine needs a workout in progress and a
  day, and Profile has neither. It is `FoodLibraryScreen`'s twin one domain over, which is what
  moved `LibraryRow` and `RenameSheet` into `:feature:profile`'s `ui/shared/components/`. Routines
  are **not exported**, for the reason saved meals and recipes aren't — convenience data, not
  history — so no export schema bump; and nothing else moved, because a started routine saves as an
  ordinary `ExerciseEntry` with sets.
- **The training plan is one `Int` column on `routine`, not a `routine_day` table.** A weekday
  bitmask (bit 0 = Monday), `0` = unscheduled: seven booleans per routine is not a relation, and
  this is the call `Profile.homeLayout` makes one table over. `Routine.days` is defaulted so every
  construction site that predates it stayed valid (`ExerciseEntry.sets`' precedent), and
  `TrainingPlan.kt` holds the whole model — pure, no repository of its own, the `streak/` and
  `goalProjection()` shape. The schedule is **current-only and never snapshotted**, the opposite
  call to `fast_session.goalHours` and the same one `Profile.stepGoal` makes: re-planning your week
  re-scores this week's strip, because a routine is intent, not history.
- **A planned day is "done" when *anything* was lifted, not when the routine was performed.**
  `trainingWeek()` scores a day off `withSets()` — the same discriminator the Strength tab uses —
  because nothing links a logged workout back to the routine that seeded it, and the plan
  deliberately does not add that link. So the Home card's Start button gives way to "Logged" on a
  day with any strength session in it, and the reminder stays quiet on that day too. *ponytail: a
  freestyle session ticks the Push day; a `routineId` on `exercise_entry` is the upgrade path.*
- **Home's plan card is hidden until a routine has days set, and then a rest day says so.** The
  first half is `SupplementsCard`'s rule (nothing is missing, nothing has been authored yet); the
  second is its opposite, because once a plan exists "nothing today" is the answer the user opened
  Home for. `plannedSoFar()` counts only elapsed days — a Monday must not report Friday as missed —
  and the ratio is dropped entirely when the week has asked for nothing yet, since "0 of 0" reads
  as a broken counter rather than a rest.
- **Profile → Workout routines authors the plan; Home starts it.** The weekday picker hangs off
  `LibraryRow`'s new `trailing` slot (null for both food libraries, which are untouched), and Home
  reaches `StrengthWorkoutRoute`'s new `routineId` through `AppScaffold` — `:feature:home` cannot
  import `:feature:food`, the shape `onOpenCoach` already has. A started routine seeds through
  `toSets()`, the *same* path the strength screen's own chips take, so an opened-from-Home workout
  and a chip-tapped one are the same workout. Routines are still not exported, so `days` isn't
  either.

- **The sheet hands off to a screen; it does not redirect.** Picking Strength grows one "Log sets
  instead →" button rather than navigating on the chip tap, so the plain duration-and-kcal path
  stays reachable — that path is what an imported watch session is. A screen because a set list
  plus its editor doesn't fit above a keyboard, the recipe builder's argument, and
  `StrengthWorkoutRoute` gets its back toolbar from `AppScaffold`'s existing `isTopLevel` rule with
  no new case. Two things fall out: the screen forces its seed to `Strength` (it draws no type
  chips, and can be reached from the edit sheet with a cardio row already in it), and it holds its
  content back until `strengthLoaded` — `rememberLogExerciseState` keys its saveable on the seed,
  so composing blank and re-seeding when the row lands would wipe what had been typed, the guard
  `DiarySheets` already applies. Tapping a logged row routes there only when it has sets.
- **The set editor's draft survives a commit, which is why there is no "same again" button.** Three
  sets of one lift at one load is the shape of most programmes, so pressing "Add set" again *is*
  the repeat gesture. The load steps by a plate (2.5 kg, 5 lb) rather than by 1, and is entered and
  shown in the user's unit while stored in kilograms — the rule every weight in this app follows.
  Lift names are free text with chips derived from recent workouts: a fixed exercise list would be
  wrong for somebody within a week, and the chips cost no schema.
- **Sets are exported (schema v14) and are not pushed to Google Health.** They are history, not
  convenience data — a workout you can't see the lifts of is the record the feature exists to keep,
  and the field is defaulted so a v13 file still imports. Google Health is import-only for exercise
  and has no shape for a set, so nothing changed there.

- **Meal ideas is the one screen that answers "what should I eat?", and it is an overlay, not a
  route.** Everything it needs — the day's gap, the recents, the recipes — is already combined by
  the diary underneath, so `MealIdeasScreen` reads `FoodUiState` and folds `mealIdeaRequest()`
  itself, exactly as `RecapScreen` and `TimelapseScreen` read `ProgressUiState`: a route would have
  earned its own `ViewModelStoreOwner` and with it a second copy of `FoodViewModel`'s five
  repositories to draw a screen that writes nothing. `MealIdeasViewModel` therefore takes two
  dependencies — the model call and `NetworkMonitor` — and it wires its own `NavigationBackHandler`,
  or back would leave the Food tab. **Picking an idea seeds the add-entry sheet, it does not log**:
  an estimate has to be repriceable by the portion stepper, and the sheet is the landing a recipe, a
  recent and a search hit already have, so the feature adds no write path at all. The button sits
  *above* the sheet's four panels because it answers a different question — they are faster ways to
  log something already decided on.
- **`MealIdeaRequest` is a second payload off the device, and it is narrower than the first.**
  `InsightRequest` describes the whole day (water, streak, the week's weight delta) because a
  one-line nudge can be about any of it; an idea can only be about the gap it fills, so a payload
  shaped for this call sends strictly less than reusing that one would. Still no age, sex, height,
  absolute weight, name or diary rows. The one field neither other call carries is
  **`Profile.dietaryPreference`** — collected in onboarding, stored, migrated and exported since
  Phase 1 with *no reader in the app* until this screen: a four-value enum the user chose, and the
  difference between an idea and suggesting chicken to a vegan. `None` and null send no diet line at
  all, because "no restrictions" is one more thing for a model to over-read.
- **The offline answer is the user's own foods, and it is also the failed-call answer.**
  `localMealIdeas()` in `:core:data/food/` is pure derivation over the two lists the sheet already
  loaded — no table, no repository, no query, the `mergeSuggestions()` shape — keeping what fits the
  remaining calories and leading with protein, because that is the gap `insightFor()` already nags
  about and a rule the user can predict. It is drawn with **no `AIChip`**: those rows are things
  they logged, and the AI accent over them would be a lie about where they came from. `fitting()` is
  the trust boundary on the model's side, a pure function for `sanitizeInsight`'s reason (the
  `org.json` parse around it is stubbed on the JVM) — it drops a nameless or zero-calorie idea
  outright and anything past 1.2× of what's left, because the header says how much is left and a
  card twice that size makes the screen a liar. An empty result is reported as `Failed`, since the
  fallback list beats a heading over nothing. Nothing is cached: the budget moves with every row
  logged, so an idea from two meals ago answers a question nobody is asking — the coach's rule, not
  the daily insight's.
- **Talk-to-log is the photo flow with a sentence where the plate goes**, and that is what keeps it
  small. `VoiceLogRoute(dateEpochDay)` carries the day like `BarcodeScanRoute` (0 from the FAB means
  today, `StrengthWorkoutRoute`'s convention), `VoiceLogViewModel` takes
  `PhotoCaptureViewModel`'s three dependencies exactly, one always-mounted `NavigationBackHandler`
  branches per state, and the write is the batched `addEntries()` a saved meal already logs
  through. It reuses **`RecognizedFood`** rather than adding a fourth ten-field type beside
  `MealIdea`, `ScannedProduct` and `AddEntryForm`: a parse is an identification carrying a
  confidence, exactly as a photo is — which is why `RecognizedFood.toAddEntryForm()` moved to
  `ui/shared/` when the second caller arrived. `MealIdea` stays separate for the reason its KDoc
  already gives: an idea is not an identification of anything.
- **The sentence is the narrowest payload FitPulse sends.** `InsightRequest` describes the whole
  day and `MealIdeaRequest` the gap that is left; `MealParseRepository.parse()` sends the user's own
  words and nothing else — no goal, no gaps, no diet, no profile, because parsing "two eggs" needs
  none of them. It is capped at `MAX_PARSE_CHARS` on the way out (a dictated meal is a sentence) and
  at `MAX_PARSED_FOODS` on the way back, and the prompt's load-bearing constraint is *only the foods
  actually named*: a model asked what someone ate will otherwise butter the toast and milk the
  coffee, and every invention is a row the user has to notice and delete.
- **`loggable()` is a thin trust boundary because the review screen is the real one.** It drops a
  nameless or zero-calorie item — not a shorter item, not one — and stops there. There is
  deliberately no per-item calorie *ceiling* like `fitting()`'s: an idea is offered against a budget
  the header has just quoted, while a parse is a claim about a meal already eaten, and every figure
  is shown and adjustable before Log writes anything. `NoFoodFound` is its own result rather than an
  empty `Success`, `ScanFlow.NoBarcode`'s call: "you named nothing edible" and "the call failed" get
  different screens and different buttons.
- **One meal slot for the whole batch, and picking an idea's landing is not this screen's.** A
  sentence is one meal; a slot per row would ask four questions to log one breakfast. Rows are
  collapsed by default and open one at a time — the list is the thing being checked, and five
  expanded forms is not a list. Changing the slot moves the parsed copy too, so it never counts as
  an edit to discard, the call `PhotoCaptureScreenState.selectMealType` makes.
- **Speech is the system's dialog, never an in-app `SpeechRecognizer`.** `RecognizerIntent` needs no
  `RECORD_AUDIO`, so there is no permission screen to write, nothing to deny permanently, and no
  listener lifecycle or dozen error codes to map; the transcript lands in a field that stays
  editable, and typing is the identical path. The mic is *hidden* where no recognizer is installed
  rather than failing on tap — Home's supplements-card rule — which is the only thing the manifest's
  `<queries>` entry exists for. *ponytail: no live waveform and no partial transcript; an in-app
  recognizer behind a permission screen is the upgrade path if dictation ever needs to feel
  in-house.*
- **The offline screen offers no "Log manually" button**, unlike `PhotoOfflineScreen`. That flow's
  manual door is a state inside itself; back out of this route lands on the diary, where the
  add-entry sheet already is, so the copy says so and saves a button that only navigates.

- **The button is hidden, never disabled, when there is no day to ask about.** No profile means no
  target and no gap; under `MIN_IDEA_KCAL` (100) there is no meal left in the day, only a mint. Same
  rule the supplements card follows — a control that can't answer shouldn't be there. The budget it
  spends is `budgetKcal()` over `dayBurnedKcal()`, the *same* arithmetic the diary's summary bar
  draws, so the screen can never offer more calories than the bar above it says are left.

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
- **A timelapse is a way of looking at the grid, not a thing to store.** `TimelapseScreen` plays
  `uiState.photos` (already ascending by date, already combined) in place — no schema, no
  repository, no ViewModel, the `badgeGroups()` and `goalProjection()` shape — which is also why it
  sits in `ui/photo/components/` beside `PhotoComparisonScreen` rather than earning a flow package:
  a second ViewModel is what earns one. Playback **loops** rather than stopping at the end, since
  stopping would need a restart control for a gesture the loop gives away free, and scrubbing
  pauses it — a slider that kept advancing under the finger fights whoever is looking for one
  particular week. The Photos tab offers it at **two** photos, the same floor the comparison slider
  has: one control appearing without the other reads as a bug.
- **One share sheet serves both photo shares.** A before/after *is* a two-frame strip, so the
  comparison slider hands `SharePhotoStripSheet` its two photos and the timelapse hands it the
  whole set; `sampleFrames()` spreads up to four evenly with the first and last always in, because a
  strip whose ends aren't the start and the end of the run isn't the story being told. The capture
  itself (`Modifier.captureToPicture` + `sharePng`) moved to `ui/shared/` when the second caller
  arrived — `ShareRecapSheet` had owned it — so the two images can't drift apart in how they reach
  the chooser, and both still render exactly the PNG that leaves the app. Photo shares stay
  PNG-only: an MP4 needs either a new media3 dependency or an EGL renderer, since a `MediaCodec`
  input surface can't be `lockCanvas`'d at minSdk 24. *ponytail: media3-transformer is the upgrade
  path if a video is ever asked for.*
- **`rememberBitmapFromFile` downsamples, and every progress photo in the app goes through it.**
  `inSampleSize` against the width the caller actually draws into (`GRID_TILE_PX` for a grid cell,
  `FULL_FRAME_PX` otherwise) — a grid holding a year of camera JPEGs was keeping every one at
  capture resolution, and a timelapse cycling them would have finished the job. The player holds
  the last decoded frame across a swap: the function re-keys on the path and reports null while the
  next decode is in flight, which at eight frames a second would strobe the frame to empty.
  *ponytail: no bitmap cache — one downsampled decode per frame off the IO dispatcher; an LRU is
  the upgrade if the fast speed stutters.*
- **Both photo overlays take back themselves.** `PhotoComparisonScreen` and `TimelapseScreen` are
  full-screen overlays inside the Progress tab, not routes, so each wires its own
  `NavigationBackHandler` — without one, back out of a comparison left the Progress tab entirely
  rather than clearing the selection.
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
- **The step goal is current-only, and that is the opposite call to `fast_session.goalHours`.**
  `Profile.stepGoal` (nudge-only over `STEP_GOAL_STEPS`, on Profile → Exercise beside the budget
  switch) is *not* snapshotted per day, so raising it re-scores every past day's "hit". Forced,
  not sloppy: `step_day` rows belong to the watch and `StepDayDao.upsert` REPLACEs them wholesale
  on every re-sync, so a target parked beside them would be overwritten by the next import. The
  Progress stat is therefore labelled "Hit today's goal", and the goal line moves under bars
  already drawn — the one place in the app where that is allowed.
- **The Activity tab draws two charts because steps and kcal share no axis.** Steps come from
  `step_day` (import-only, sparse, windowed anchored to today like sleep and heart) and the burn
  series from `burnSeries()` in `:core:data/health/Activity.kt`, which folds `step_day` and
  `exercise_entry` with the existing `dayBurnedKcal()` so an imported walk is never counted twice.
  It lives in its own file only because JVM erasure puts every top-level `List<T>.inRange` in one
  file facade. The burn chart shows what was *burned* and so ignores `addExerciseToBudget` — that
  switch decides what reaches the budget, not what happened. Steps are still not a streak domain
  and still not exported.
- **`DayBarChart` is the zero-based day-bar drawing; `RangeBarChart` is the floating-bar one.**
  Sleep, Fasting and both Activity charts call the first (`minAxisValue` is the floor a full night
  or a full day sets, `goalValue` the dashed line); Heart and Blood pressure call the second,
  which is deliberately not zero-based. Mood, Nutrition and Supplements keep their own canvases —
  two series with a legend, a target line over a dense series, and percentages. A fifth near-copy
  of the same `Canvas` is the thing to avoid, not a fourth parameter on `DayBarChart`.

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
  `recipe`, `search`, `ideas`, `shared`), `:feature:progress` (`progress`, `weight`,
  `measurement`, `photo`, `nutrition`, `activity`, `strength`, `mood`, `sleep`, `heart`, `fasting`,
  `supplement`, `pressure`, plus a `shared/` holding `RangeBarChart`, `DayBarChart` and
  `SharePng` (the capture-and-share pair two flows draw), the first two of which
  between them draw every tab's bars except Mood's, Nutrition's and Supplements'),
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
