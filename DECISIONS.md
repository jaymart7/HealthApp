# DECISIONS.md — why FitPulse is built the way it is

The decision log. Each entry was argued once and is easy to "fix" back into a bug, so
**read the entries covering an area before changing behaviour in it.** `CLAUDE.md` is
binding and holds the constraints; this file holds the reasoning behind them.
`FEATURES.md` says what ships.

**Areas:** Streak & badges · The day's calorie budget · The diary & sharing a day · Camera,
barcode & Open Food Facts · Meal photos · Food search, the diary filter & history · The
add-entry form & the review screen · Theme, mascot & colour · Home · Progress, recap & the
energy check-in · Saved meals, recipes & the food library · Nutrients · Targets & editing an
entry · Fasting · Export, backup & reminder plumbing · Launcher shortcuts & the quick-action
sheet · Reminders & notifications · Widget & Wear · AI — the coach & the daily insight ·
Training, strength & routines · Meal ideas & talk-to-log · Blood pressure, BMI & measurements ·
Cycle · Progress photos & timelapse · Supplements · Steps, activity & charts · Adaptive layout ·
Onboarding · Profile & Settings · Health Connect · Google Health · Localization.

---

## Decisions that aren't obvious from the code

Keep these — each one was argued once and is easy to "fix" back into a bug.

### Streak & badges

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

### The day's calorie budget

- **`budgetKcal()` is the only place burned calories fold into the day**, and
  `Profile.addExerciseToBudget` (default on) can switch that credit off —
  `calculateDailyTargets()` already applies an activity multiplier, so crediting
  a workout on top of it can count the same training twice. The Mifflin–St Jeor
  target itself is never touched.
- **Estimated burn stops re-estimating** (MET × kg × hours) once the user edits
  the kcal field by hand.

### The diary & sharing a day

- **Diary date navigation:** forward stepping stops at today (there are no
  planned meals), and system back from a past day returns to today rather than
  leaving the tab.
- **The shared day card names an absolute date and no food.** `diaryDateLabel()` says "Today" on
  a screen the user is looking at now; the PNG outlives the day it was made, so the card formats
  the date outright — "Today" in a chat thread tomorrow names the wrong day. And it carries meal
  *subtotals* only: a shared image is read by people the diary was never written for, so how the
  day went travels and what was eaten does not. `DiarySummaryBar` is rendered verbatim inside it,
  for the reason `RecapCard` is: two layouts for one set of figures is two places for them to
  disagree. The link sits at the foot of the scroll rather than in the date header — that row
  already carries three 48dp buttons and a label it goes out of its way to protect at large font
  scales — and it is absent on a day with nothing logged.
- **`ShareImageSheet` lives in `:core:designsystem`, and that move was forced rather than tidy.**
  The capture-and-share pair was `:feature:progress`'s while the recap and the photo strip were the
  only pictures the app shared; the diary's day card is the third, `:feature:*` modules never
  import each other, and duplicating it is what the shared-component rule exists to stop. Taking
  the sheet chrome with it — the opaque ground, the brand footer, the `picture.width > 0` guard —
  was the difference between moving two functions and moving two functions plus the same
  twenty-five lines that were already copied into both callers. Net deletion.
- **The FAB is today-only; every in-diary door carries the diary's day.**
  `BarcodeScanRoute`, `VoiceLogRoute` and `FoodCaptureRoute` all take a
  `dateEpochDay`, and the diary's three icon doors pass `uiState.selectedDate`, so a
  scan, a sentence or a photo taken while reviewing Tuesday lands on Tuesday. `0`
  means today, and it is what the FAB's sheet and the launcher shortcuts pass —
  both launch outside the diary's date context and have no day to carry. The date
  threads to `toFoodEntry(dateEpochDay)` in all three flows, so nothing downstream
  can tell one past-day log from another. Forward-dating is still absent: the diary
  never steps past today, and there are no planned meals. The day comes from the
  screen you left, never from a control on a viewfinder.

- **"Copy a day" is a copy, never a link.** Picking a source day writes fresh rows onto the day
  being shown — the diary has no concept of a day that *refers* to another one, and one would have
  to survive the source being edited or deleted for the rest of the app's life. The rows it writes
  are ordinary: they swipe-delete, they edit, they export, and nothing downstream can tell one from
  a hand-logged row. Three fields are re-stamped and the argument is in each. `id = 0` so the write
  is an insert. `photoPath = null`, the call `FoodHistoryViewModel.logAgain` already makes for a
  single re-logged row — a plate belongs to the meal it was taken of, and 500 kept photos is a cap,
  not a budget to spend duplicating. `steps = 0` on a copied workout, because
  `ExerciseRepositoryImpl.addEntry` re-estimates a step count from the type and the minutes when it
  sees zero, and the figure being dropped is the watch's own, recorded against the day it was
  actually walked.
- **Water is set, and only from a day that had some.** Food and exercise are rows and add; water is
  one row per day holding a count, so a copy can only overwrite. A source day with no water
  therefore copies nothing rather than zeroing a count already standing on the target — the one
  place in this feature where "copy everything" would destroy data instead of adding it. That is
  also why water is a tick like the rest: the user can see it is going to be replaced.
- **The copy sheet has no undo, and the ticks are what stands in for one.** `addEntries` hands back
  no ids, so an undo would mean widening the repository or deleting by name match; a copied row
  swipes away like any other, and the sheet shows every part with its item count and calories
  *before* anything is written. The empty parts draw disabled rather than being left out, so the
  sheet reads as a report of that day — a missing Dinner row would look like the sheet forgot about
  dinner, where a disabled one says the day had none.
- **Two calendars, not one with a mode.** The date header's calendar moves the diary; the copy
  picker names a source and leaves the day where it is. They also live differently: at ≥840dp the
  first is a permanent pane and the second is still a sheet. The day being shown is drawn selected
  in the picker and is the one day that is not a source — copying a day onto itself only doubles
  it, so the ViewModel ignores it and the tap does nothing.
- **The loaded source day rides the diary's own combine.** `observeDiary` ends in
  `reduce { newState }`, which replaces state wholesale, so a day held beside it would be wiped the
  next time Room spoke. It reads through the three dated flows the diary already uses
  (`observeEntries`, `ExerciseRepository.observeEntries`, `WaterRepository.observeDay`) — no query
  of its own — and is null whenever the sheet is closed, which is what keeps those reads off the
  diary's path. The sheet's visibility is that null, not a flag, so it survives a rotation; only
  the picker's open/closed is screen state.

### Camera, barcode & Open Food Facts

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
- **A resolved barcode is remembered, a miss is not.** `scanned_product` is a cache keyed by the
  normalised barcode, read before the network on every scan, and it is what reopened the
  "considered and declined" note below: the scanner used to be *dead* offline —
  `BarcodeScanScreen`'s opening `LaunchedEffect` dropped to `ScanFlow.Offline` before the camera
  even opened — and every rescan re-spent a 3600 req/hour budget shared by every install. A GTIN's
  nutrition panel does not change, so the cache is read first rather than as a fallback; the quota
  is the thing worth saving. **Only a hit is written.** FDC gains products over time, so a stored
  miss would blind the app to a package that starts existing next month, and the not-found screen
  leads to manual entry anyway — the rescan a cached miss would save is the rare one. `barcodeKey`
  is the single normaliser: digits only (the code comes off an image decoder, untrusted on its way
  into a URL) with leading zeros stripped, so a 12-wide and a 13-wide read of one package share a
  row instead of caching it twice — the identity `parseFdcProduct` already compares on. It returns
  null where nothing survives, which also stops an all-zeros read spending a request on the query
  that returns the whole branded database. It is a pure function because every test in `:core:data`
  is, and a fake DAO would be a second idiom to keep in step.
- **The offline gate moved from before the camera to after the lookup.** A scan of a remembered
  product resolves with no network, so refusing to open the viewfinder would hide the feature.
  `ScanFlow.Offline` is unchanged and still reached, one step later, from the lookup's own
  `Failed -> if (isOnline()) NotFound else Offline`. The cost is deliberate: offline with an empty
  cache is now one extra step (aim, scan, *then* the offline screen) rather than an immediate one,
  which is the price of the cache being reachable at all.
- **The cache is not exported and not migrated.** It is derived data with an upstream — nothing in
  it is the user's, so `EXPORT_SCHEMA_VERSION` does not move — and `scanned_product` is the one
  table where `fallbackToDestructiveMigration` costs literally nothing: a dropped cache refills
  itself on the next scan.
- **Open Food Facts answers a scan first, and FDC is the fallback.** Four reasons and they compound:
  OFF is keyless, so it spends nothing from the app-wide 3600 req/hour budget the Backlog worries
  about; `api/v2/product/{code}.json` is a real *lookup*, so it cannot hand back someone else's
  product; it returns a few kilobytes where FDC's `foods/search` ships 25 × ~21 KB; and it is
  stocked internationally, which for a `ph.mart` app is the entire point. A consequence worth
  naming: a clone with no `fdcApiKey` now scans, where before `fdcGet` refused before opening a
  socket. The FDC leg moved into a private `fdcLookup()` and is otherwise untouched — the padding
  query and the `gtinUpc` comparison are exactly what they were.
- **`Failed` only when *neither* source could answer; a `NotFound` from either is an answer.** This
  is the one line in the chain that is easy to "tidy" into a bug. `ScanFlow` maps `Failed` to "Try
  again" (or the Offline screen), so letting FDC's keyless `Failed` outrank OFF's genuine miss would
  park an unknown package on a retry button forever instead of the manual-entry path a miss exists
  to lead to.
- **There is no `gtinUpc`-style echo check on the OFF leg, and that is not the FDC check being
  relaxed.** FDC's is load-bearing because `foods/search` falls back to relevance on a code it
  cannot tokenize; a v2 lookup has no relevance to fall back to. Different endpoint kind, not a
  dropped guard — and the FDC one stays exactly as the entry above it says. OFF also normalises
  zero-padding server-side (`28400642255` and `0028400642255` answer with the same product,
  verified), so `barcodeKey`'s stripped code goes straight into the path with none of FDC's
  four-width query.
- **`nutriments_estimated` is never read, and a test is what keeps it that way.** OFF publishes it
  beside `nutriments` and computes it from the ingredient list — Nutella's real payload declares
  calcium, iron, potassium and vitamin D *only* there. Reading it would put a derived number in the
  same field a label figure goes in, which is `FEATURES.md`'s "AI-estimated micronutrients" rule
  with a different model behind it, and it would quietly break what `Nutrients` promises: that `0`
  means unknown-or-none and `foodsWithMicronutrients` can count coverage. Every `<key>_100g` OFF
  publishes is in **grams** (`sodium_100g: 0.0428`, `calcium_100g: 0.0253`,
  `iron_100g: 0.00094`) — pinned against live products rather than inferred, because a `_unit` field
  in the payload describes `_value`, not `_100g`, and reading the wrong one is a 1000× error in a
  number shown to the user.
- **The text search is search-a-licious, not `cgi/search.pl`.** Measured back to back, `search.pl`
  answered 200, then 503, then 503; `search.openfoodfacts.org/search` served five rapid queries at
  ~0.7s each and finds the products this app exists for ("Sky Flakes"). *ponytail: two OFF hosts and
  one mapper between them; if search-a-licious ever gains the product endpoint, this collapses to
  one.*
- **The online search tier is additive, and the local list is untouched.** `COMMON_FOODS` was chosen
  on three grounds — it answers with no debounce, it answers offline, it spends nothing — and OFF
  answers only the third, so it does not get to replace anything. It is folded in **behind** both
  local tiers by a defaulted third parameter on `searchFoods()` (every existing call site and its
  tests unchanged), deduped by the `nameKey()` that was already the identity there. Behind, not
  in front, because it arrives late and rows appended to the back cannot move a page someone is
  reading. 500 ms debounce, three characters minimum, and a blank field never asks — "list
  everything" is a local concept. Offline short-circuits to `Idle`, **not** `Failed`: the local list
  answering with no network is the feature, and an error message for working as designed is worse
  than silence. *Skipped: a per-query cache — measured, the endpoint is fast and unthrottled; add
  one keyed on the trimmed query if backspacing ever shows up.*
- **`onlineStatus` exists because "No matches" became a lie.** The panel has always answered an
  empty result with "No matches — enter it by hand instead.", which is wrong while a request is in
  flight and wrong in a different way when it failed. Three values, and `Idle` deliberately covers
  nothing-asked, answer-landed and offline alike, because the panel draws all three identically.
  The tiers themselves draw as one list with no badge or divider: they are all per-100 g figures a
  row can be seeded from, the panel has silently mixed the first two since it existed, and a
  "where this came from" mark is something to explain on a surface whose whole job is to be picked
  from. The order *is* the ranking.
- **`brandedName()` is one rule for every source.** FDC's all-caps recasing and brand-leads join
  moved out of `FoodDataCentral.kt` into its own file the OFF mapper shares, so a scanned package
  reads the same however it was resolved — and "Nutella" branded "Nutella, Ferrero, Yum yum" stays
  "Nutella" rather than becoming "Nutella · Nutella". `brands` is a comma-string on the product
  endpoint and an **array** on the search one; the first entry is the one on the package.
- **The analyzed plate is kept, and the rule for which meals get one is "whatever the flow is
  holding".** The photo used to be thrown away at the moment of logging, which left the app's
  headline feature — point the camera and it logs — with a text row to show for it. It is now
  written by `FoodRepositoryImpl.addEntry(entry, photo)`, and *every* exit from the camera flow
  passes what it has: the recognized plate, the gallery pick, and the meal the analyzer missed that
  was searched or typed by hand. That last one matters — the numbers being hand-entered does not
  make the picture less a picture of the meal — and it is one branch (`state.photo`) rather than a
  policy per state. Nothing outside that flow attaches one: a barcode viewfinder is a picture of a
  package, a saved meal is a re-log of something already photographed once, and the add-entry sheet
  gets **no** new camera door. The bitmap goes down to the repository rather than a path coming up
  from the UI, because where a plate lives, what it is scaled to and how many are kept are all
  `:core:data`'s to know — the same division `ProgressRepository.addPhoto` already draws.

### Meal photos

- **The photo survives an edit, and that is what `AddEntryForm.photoPath` is for.** Correcting a
  logged row *supersedes* it — soft delete plus a fresh insert — so the entry is rebuilt from the
  form every time, and a form that didn't carry the path would silently drop the plate on a
  one-kilocalorie fix. `AddEntryFormTest` is the guard. The 64dp thumbnail on the edit sheet is
  there so the round trip is visible rather than merely true.
- **`FoodEntryDao.replace` inserts with `id = 0`.** Found while wiring the above: the old row is
  still there (soft-deleted, not gone), so re-inserting the caller's id was a primary-key collision
  on a table that still held it. Superseding means a *new* row — the interface has always said the
  id changes — and zero is what tells Room to generate one.
- **Storage is answered by a cap, not by a promise.** 768px on the long edge at JPEG 85 (~80 KB),
  and the newest `MAX_MEAL_PHOTOS` = 500 survive; the prune runs after each write, deletes the
  files and nulls the column. Three consequences, all deliberate: the *meal* is never pruned (a row
  whose picture aged out is still every calorie it ever was), soft-deleted rows are counted because
  their files are on the same disk, and a swipe-delete leaves its file for the prune rather than
  the delete — the diary's Undo would otherwise restore a row pointing at nothing. The scale is the
  one thing that differs from a progress photo, which is stored as captured: one of those is taken
  a fortnight, and one of these is taken three times a day.
- **Meal photos are not exported and not cloud-backed-up.** `ExportFoodEntry` simply has no photo
  field, so `EXPORT_SCHEMA_VERSION` does not move — a path is meaningless on another device and the
  export has never carried an image. `meal_photos/` joins `progress_photos/` in the two backup-rule
  XMLs for the same reason it did: Auto Backup's 25 MB cloud cap. A direct device transfer still
  takes both, since it has no cap.
- **The history lives on the Progress tab's Food page, not in a fifteenth subject.** A strip of the
  newest twelve under the calorie chart, and a full-screen in-tab overlay grid grouped by day
  behind it — the timelapse's and the recap's shape, so it costs no route and no second copy of
  `ProgressViewModel`'s thirteen repositories. The strip is **unranged**: it shows the newest kept
  plates, not a slice of the chart's 1M/3M/6M/1Y toggle, because a photo history that thinned out
  when someone picked "1M" would be lying about what it has. It draws nothing at all when there are
  no photos, unlike every other card on a detail page, which has a number even on a bare day — an
  empty "Meal photos" card is an ad for the camera on a page about what was eaten. The frame's
  caption says the day and the calories and *not* the meal name: `MealType.labelRes()` is
  `:feature:food`'s string, and features do not import each other.
- **The confirmation plate opens, and the viewer is a boolean rather than a ninth `CaptureFlow`.**
  A 64dp centre-crop is the worst available look at the photo the estimate was read off, and it sat
  above a form asking the user to trust that estimate. `PhotoCaptureScreenState.viewingPhoto` is a
  flag *inside* `Confirmation`: the form underneath is unchanged and still dirty or not, so a ninth
  state would only have had to answer what logging and discarding mean from a picture. Back reads
  it in the flow's one always-mounted handler — the viewer first, the form second — which is
  `MealPhotoGallery`'s frame-over-grid shape, not a second handler competing with the first.
- **The viewer is full-bleed, and it lives in `:feature:food`.** Every non-camera state in the flow
  is inset by the screen's `safeDrawingPadding`; the viewer is drawn outside that box, because one
  that letterboxes itself inside the system bars shows less of the plate than the camera did — its
  close button carries the inset instead. It stays in `ui/photo/components/` rather than moving to
  `:core:designsystem` because one screen draws it: the diary rows and the edit sheet keep their
  inert thumbnails, and the second caller is what would earn the move. `maxPan` is a pure Float
  function with `PhotoViewerZoomTest` over it, which is the only real arithmetic in the thing.
- **`rememberBitmapFromFile` moved to `:core:designsystem`.** Two features draw stored photos now,
  and a second decoder is a second downsampling rule to keep in step. Nothing about it changed but
  its package and one more size constant (`THUMB_PX`, for the diary row's 40dp tile).

### Food search, the diary filter & history

- **Food search is a list shipped in the APK, not an API call.** `COMMON_FOODS` in
  `:core:data/food/CommonFoods.kt` is ~120 hand-written staples, per 100 g like every FDC row, and
  `searchCommonFoods()` is a case-insensitive substring over it — pure data, no table, no
  repository, no query, the `localMealIdeas()` shape. It replaced a `foods/search` call for three
  reasons that are the whole design: it answers with no debounce, it answers offline, and it spends
  nothing from the key budget. What it gives up is branded packages, which is what the scanner is
  for, and anything neither knows is still typed in by hand. A blank field is **every** food rather
  than an idle hint, windowed at `FOOD_PAGE_SIZE` — a lazy list is not allowed inside
  `AppBottomSheet` (it hands its children unbounded height), so a counter over a list already in
  memory is also what fits where the panel is drawn. That is equally why this is not Paging3: there
  is no paged source to write a `PagingSource` over, and nowhere to put a `LazyPagingItems`. The
  window **grows on scroll** rather than stepping through pages — but inside the panel's own
  bounded, scrolling results box, not in the host's scroll: two of the three hosts draw their form
  directly beneath the panel, and a list that got taller as you read it would walk that form down
  the screen. `RESULTS_MAX_HEIGHT` is not a multiple of the row height on purpose — the row cut in
  half at the bottom edge is the affordance the Next button used to be. The photo flow's manual
  search is the host that *does* have height to give, so it passes `fillHeight = true` with a
  `weight(1f)` and its box runs to the buttons. That is a parameter rather than something the panel
  infers because a weight measures 0dp inside the other two hosts' `verticalScroll`. `FoodSearchViewModel` holds
  that window, which survives a rotation where a composable's `remember` would not.
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
- **Searching *across* days is a route, not a third meaning for that field.** The header filter
  narrows the open day and `FoodSearchPanel` searches foods-you-could-log; neither can answer "when
  did I last eat that", and the add-entry sheet's recents cap at five names. `FoodHistoryRoute`
  does, and it is a route rather than an overlay for the reason meal ideas is an overlay: ideas are
  folded out of state the diary already holds, while this has its own query, its own Room read and
  its own ViewModel — which is what `CLAUDE.md` says earns a flow package. The door sits at the
  foot of the scroll beside "Share the day", and unlike that one it is drawn on an **empty** day
  too: a day with nothing on it is exactly when you want to look backwards. It carries
  `FoodScreenState.searchQuery` along, so a word already typed into the header isn't typed twice.
- **The history search is a suspend one-shot, and its results do not live-update.** Every other
  read in `FoodRepository` is a `Flow`; this one is answered per keystroke, and a flow would tear
  down and re-subscribe a query instead of running it. The consequence is deliberate: a row logged
  elsewhere while the screen is open doesn't appear until the next keystroke, because a result list
  that reorders itself under a reading finger is worse than a slightly stale one. A late answer to
  a superseded query is dropped by comparing `state.query` in the second `reduce`.
- **`likeContains()` escapes; `FoodEntryDao.searchByName` declares `ESCAPE '\'`.** They are two
  halves of one decision and `LikeContainsTest` is what keeps them in step — without it a food
  named "100% oats" turns its own name into a wildcard and the search quietly returns the table.
- **A re-logged row is a copy with no `id` and no `photoPath`.** `addEntry` keeps a path it is
  handed, and two rows pointing at one file would break the `MAX_MEAL_PHOTOS` prune, which counts
  paths — it would reclaim the file out from under the row that earned it. The source row keeps its
  plate; the copy has none. The copy keeps the **source row's** meal slot rather than
  `defaultMealTypeForNow()`: unlike the photo and barcode flows that helper exists for, this one
  already knows where the food belongs. And no Undo on the confirmation — `addEntry` returns no id
  to undo with, and the row it wrote is a swipe away in the diary it just landed in.
- **History dates are absolute, always.** No "Today"/"Yesterday" here, unlike `diaryDateLabel()`
  two files over. A list spanning months is scanned by date rather than read top-down, and two
  relative labels among forty absolute ones are the two that have to be decoded. It also leaves
  that helper `internal` to `ui/diary/components/` where its test already lives.
- **The diary's calendar is a pane at ≥840dp, and the same calendar either way.** It was declined
  once — the diary has no list to put beside its day — and reopened on the condition recorded with
  it: the swap-in `FoodScreenState.calendarOpen` opens is the list, so at expanded width
  `FoodContent` draws `CalendarPanel` beside the day instead of over it. A `Row`, not a scene:
  there is no second route and no second `FoodViewModel`, the same reason Progress's subject pages
  stayed a swap-in. Three details are the whole of the decision. The pane is a **fixed 320dp**
  where Progress's are weighted, because a month grid is seven fixed 44dp cells and a weighted pane
  spends its extra width spreading them apart (7 × 44 = 308, plus padding). `onOpenCalendar` on
  `DiaryDateHeader` became **nullable**, and null takes the chevron and the tap target with it —
  the label still names the day, but a control that offers to open what is already on screen
  teaches the wrong thing; the prev/next chevrons are untouched, and are still how you walk a past
  day forward. And `FoodContent` clears `calendarOpen` when it becomes two-pane, because opening
  the sheet on a phone and unfolding would otherwise draw the sheet over its own pane — the reset
  is what keeps `DiarySheets` from having to know the window's width at all. `markedDates` is
  still `emptySet()` for the reason the sheet gave: dots would cost a query the diary never makes.
  The add sheet is unchanged and still a sheet — its panels and form are built for a sheet's
  scroll, and nothing about a wider window changes that.

### The add-entry form & the review screen

- **The form's four figures are nullable; the store's are not, and that split is the point.** A
  `FoodEntry` cannot tell a zero from an unknown — `Nutrients` says so at its own definition and
  reports coverage alongside instead — but `AddEntryForm` can, because it knows whether anything
  seeded it. So `calories`/`proteinG`/`carbsG`/`fatG` are `Int?` defaulting to null, and
  `MacroFieldCell` prints an em dash rather than a `0` nobody typed; clearing a field returns to
  null, so the distinction survives a correction, and a food that genuinely is fat-free is typed as
  `0` and prints as `0`. It collapses at `toFoodEntry()` and `toSuggestion()` with `?: 0` and
  nothing downstream of those two sees a null — **`:core:data`, Room, the export and `Nutrients`
  itself are untouched**, which is what kept this a feature-local change. The three micronutrients
  the review screen edits map `0 → null` at the cell boundary only, by `takeIf { it > 0 }` in each
  caller: the conflation stays exactly where it already was. `SavedMealItem` is a `:core:data` type
  with non-null figures, so the recipe editor's cells never draw a dash.
- **`isValid()` did not change, and the handoff's version of it was the thing to refuse.** The
  redesign specified `name non-empty && calories != null`, which would deadlock the quick add the
  entry below this one exists to protect. It stays `name.isNotBlank() || (calories ?: 0) > 0` —
  identical semantics once calories is nullable — and the "Required" label that went with the
  handoff's rule was dropped rather than shipped: it names an invalid state this app does not have.
- **Calories already followed the portion, so the redesign's one real behaviour change was already
  built.** The brief called out "today the user recomputes by hand"; `withPortionAmount()` has
  repriced calories, all three macros and every nutrient since it was written. What was *not* taken
  is the `caloriesOverridden` flag that came with it — under it, correcting a figure at 100 g and
  then moving to 150 g leaves the corrected number behind, in the direction that under-counts.
  Scaling off the current pair means a correction is still correct at the next portion, so the
  screen shows the big figure and the edit affordance the handoff drew and keeps the arithmetic it
  already had. The hint reads "Follows the portion." with no override clause.
- **The per-100 g caveat moved to the number it is about.** It was the review screen's subtitle, a
  card away from the portion; it now sits under the portion control and names the factor currently
  applied ("Scaled ×1.5.") so the arithmetic is visible at the moment it starts being true. The
  factor is printed **only in grams** — the seed is per 100 g so `amount / 100` is the factor, but
  switching the unit moves neither the amount nor the values, and "×1.5" against a number meaning
  ounces would be arithmetic nobody performed. A found product now carries no subtitle at all.
- **`manualEntry` is a parameter, not a third subtitle string.** Three paths reach the review
  screen: a barcode match, a search hit, and a blank form from either flow's hand-entry door. The
  blank one needs the opposite caveat *and* a different title ("Add this item" — there is nothing to
  review), and both hosts already knew which it was from `originalForm.name.isBlank()`. Wiring it
  also fixed a standing mismatch: the photo flow's hand-entry path was printing "From the food
  database" over a form the user was typing.
- **The search is a screen in `ui/search/`, not a state in `ui/photo/`.** `ManualSearchScreen` was a
  photo-flow sub-view that happened to be a search; grouping in this repo is by *subject*, and
  `FoodSearchViewModel` and `FoodSearchPanel` were already in `ui/search/`. The photo flow is one
  caller. Three things went with the move. The **mascot and its apology are gone** — an apology is a
  thing to read on a screen whose job is to be typed into, and by the time anyone lands here they
  know the photo failed. **Cancel became the bar's back arrow**, beside the finger already typing
  rather than under a full-height list. And **`fillHeight` was deleted** with its `weight(1f)`
  branch: it existed for exactly this host, and every host the panel has left is a bounded one, which
  is the shape it was always for.
- **The online tier's status lives at the end of the list, and a failure is a row.** The local tiers
  have already answered and their rows are pickable; the packaged-food tier is a thing happening
  behind them, which is where `searchFoods()` already puts it. A banner would interrupt an answer
  that is not waiting on anything and a spinner over the list would imply the rows under it are
  provisional. So: two skeleton rows and a spinner at the tail while it runs, an inline `cloud_off`
  row with a **Retry** that re-asks *only* the online leg when it fails, and the 2dp progress rule
  under the search bar because progress belongs to the query. `OnRetryOnline` goes back through
  `searchOnline()` so the same three guards and the same debounce apply — one path, half a second.
- **The count says "on this device" while anything is still being asked.** "12 of 40" is a whole
  answer only once `onlineStatus` is `Idle`; while the lookup is in flight the total is about to
  change, and after it failed the total is whatever this phone happens to carry. `countIsLocalOnly`
  is that rule and `FoodSearchWindowTest` holds it — including that **offline is `Idle`**, so the
  count there does *not* hedge: the local list answering with no network is the feature.
- **`FoodItemRowVariant.Result` is a third variant, and the search's card went with it.** A hit was
  a `surfaceContainerHighest` card per row, which is what a *stack of things* looks like; a search
  result is one line being scanned down, so rows are separated by start-inset rules instead. That
  freed 12dp either side for the name and promoted the calorie figure to the heaviest thing on the
  row — what a picker actually aims at. `Display` (the diary) is untouched, the detail line is the
  same `macroLine()` both variants draw, and all three panel hosts get the new row: one search, one
  row, wherever it appears.
- **Macro tiles replace the stepper rows for foods, and only for foods.** `MacroFieldGroup` draws
  three typable cells in one row's height where `MacroInputGroup` stacked three rows, each spending
  its width on steppers for a two-digit number that was already typable. It is used by all five
  food-logging forms. `MacroInputGroup` **stays** for onboarding's confirm-targets step and
  Profile's calorie section: a target is nudged toward a split, which is what steppers are for; a
  food's macros are copied off a label.
- **`MacroBar` draws a track when there is nothing to split.** Three zero-weight segments rendered an
  8dp strip of nothing, which reads as a broken view rather than an empty one — so a zero total now
  fills the bar's shape with `surfaceContainerHighest`, the tone the unfilled half of a segment
  already uses.
- **The review screen's bar is `AppTopBar` with a `titleStyle`, not a `TopAppBarScrollBehavior`.**
  That M3 type is still experimental, and putting it in `AppTopBar`'s signature would push an
  `@OptIn` onto every screen in the app that wears a toolbar — to answer a question the screen's own
  `ScrollState` already answers. Both scroll reads are `derivedStateOf` so the screen re-composes at
  the thresholds rather than per pixel, and `form.name` is deliberately *outside* them: a
  `derivedStateOf` captures a non-state value at the composition it was remembered in, which would
  have frozen the bar on whatever the name was when the screen opened.
- **Discard leaves while the keyboard is up.** A destructive full-width button directly under the
  IME is a button placed where a mis-swipe at the suggestion bar lands, and nothing on the review
  screen needs discarding mid-word. Back still does it, and still asks first.
- **A nameless entry is a quick add, not an invalid one.** `AddEntryForm.isValid()` accepts a bare
  calorie figure, and `toFoodEntry()` fills the blank with `QUICK_ADD_NAME` and collapses the
  portion to one serving — the form's default 100 g is a number the user never supplied. The guard
  is shared with the photo and barcode confirmation screens on purpose: all three log through
  `toFoodEntry()`, so no path can write a blank name, and clearing a name on a confirmation screen
  degrades to a quick add instead of deadlocking the button. `FoodEntryDao.observeRecent` excludes
  that name so every quick add doesn't collapse into one meaningless row eating a
  `MAX_SUGGESTIONS` slot — which is also why the suggestion panel's one-tap re-log callback is
  `onLogAgain`, not `onQuickAdd`.

### Theme, mascot & colour

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
  newer build) degrades to Rui, exactly the reading `darkThemeOn`'s null has. Each character varies
  on **three axes** — silhouette, eyes, one accent — because two characters differing only in
  outline read as the same character badly drawn; any two of them differ on at least two of the
  three, which is what `MascotCharacterTest` asserts now that colour can't help. What stays shared
  is the **mouth geometry and the state vocabulary**: all five states read identically whichever
  buddy is picked, so no character can come to mean something. The whole avatar is one
  `Canvas` rather than a shaped `Box` — that is what lets an antenna or an ear sit *above* the head
  (`topInset`/`sideInset` carve the room, and Rui's are zero so it renders exactly as it always
  has) with nothing clipping the Celebrating sparkles.
- **Colour is the user's second pick, not the character's.** `MascotPalette` rides `AppTheme` beside
  `LocalMascot` off `Profile.mascotPaletteName` — a nullable String resolved by `mascotPaletteOf()`,
  the same shape and the same degrade-to-default reading as `mascotName`, and only the picker passes
  `palette` explicitly. Its five entries are the five pairs that *were* the characters' fills, so
  every one is already proven in light, dark and all three contrast schemes and no new colour was
  invented: `Soft` (Rui's, the default, so an untouched install is unchanged), `Bold`, `Muted`,
  `Contrast` (the one pair that inverts with the theme) and `Neutral` (the one whose *features*
  carry the accent — the grey chassis with a lit face that made Mart read as a machine, now available
  to any buddy). The list stops there because of what a fill may not be: never `tertiary` or
  `tertiaryContainer` (the AI accent and the carbs colour), never `error` (off-track only), and
  never `secondaryContainer` — both picker rows fill their selected cell with exactly that, and a
  mascot that vanished the moment it was chosen is the one thing a picker must not do. The colour
  cells are plain swatches, each ringed in `outlineVariant` whatever its fill — `Neutral` is a near
  neighbour of the card behind it. `mascotSwatchColor()` is public where `mascotColors()` is
  internal because a circle needs the fill and nothing else.
- **Then thirty more, and those are hue angles rather than theme roles.** "Pink and red" cannot come
  from the scheme: the roles that would carry them are exactly the three the entry above rules out.
  So `MascotPalette` gained thirty entries carrying a `hue: Float?`, and `mascotColors()` returns
  early on it into `Color.hsl()`. This is the app's one exception to *never hardcode a hex*, and it
  is narrow on purpose: a mascot fill takes part in no scheme, so a contrast swap has nothing to say
  about it. **Angles, not a hex table**, because thirty colours are then thirty numbers and one edit
  to five constants retunes all of them, where sixty hand-picked values are sixty things to
  re-eyeball. **The five stay**, and stay first: `MascotPalette.name` is a persisted token, so
  deleting `Bold` re-defaults every install that picked it, and they are the only theme-reactive
  entries left.
- **Fifteen families of two, not thirty points on one wheel.** The first cut *was* thirty points on
  one wheel, and it was wrong on sight: thirty hues evenly spaced sit 12° apart, 12° of a pale fill
  is a colour nobody can tell from the one beside it, and a whole row of the picker read as one
  colour. So the wheel now carries fifteen hues at 15–35° — the wide gaps go to the greens and
  blues, where hue moves slowest to the eye — and each appears twice, once pale and once vivid.
  What separates a tier is **chroma, not lightness** (`0.26` against `0.52`), because chroma is the
  axis a pale fill has none of; lightness stays high in both, since a mascot is drawn on `surface`
  in either scheme and a genuinely dark tier would be a silhouette in dark mode. Declaration order
  is the grid's order and it alternates pale, vivid, pale, vivid, so **every neighbour in the grid
  differs by a whole tier or by a family's worth of hue** — `MascotPaletteTest` asserts exactly
  that, and it is the guard against packing the list tight again.
- **One near-black face for all thirty, and that is what fixes the tier's lightness.** `0.72` for
  the vivid tier is not a taste call: at `0.64` a saturated blue body lands at luminance `0.14`,
  too dark to carry a dark face and still too dark to carry a light one, and `Red`, `Blue` and
  `Indigo` all fell under 4.5:1 whichever way the face went. Lifting the tier put the darkest body
  back above `0.23`, where a single `hsl(h, 0.80f, 0.15f)` serves every entry — which deleted the
  pick-the-face-off-the-body's-luminance branch the failure first bought. `MascotPaletteTest` sweeps
  all thirty for ≥ 4.5:1, and `mascotFeatureColor()` is public so the picker's tick mark rides the
  one colour that ratio is asserted for rather than an `on*` role that knows nothing about the
  swatch under it.
- **The colour row became a door; the buddy row did not.** Thirty-five swatches in the Appearance
  card would push Notifications, Connections and Data off the bottom of Settings, so
  `SettingsColourPicker` is now one `AppListRow` carrying the current swatch and its name, opening
  `MascotColourSheet`. Five buddies still fit in the card, so `SettingsBuddyPicker` is untouched —
  and `SettingsAppearanceSection`'s signature is unchanged, because `sheetOpen` is a `remember` in
  the picker itself: which sheet is open is UI-only state with nothing on the other side of it.
  The sheet's grid is a **`FlowRow`, never a `LazyVerticalGrid`** — `AppBottomSheet` hands its
  children unbounded height, which its own KDoc says no lazy list may take. Picking applies
  immediately and leaves the sheet open, because the buddy behind the scrim *is* the preview.
  No `NavChevron` on the row: that composable means "this row leaves the screen" everywhere else in
  Profile, and a sheet does not. Back dismisses the sheet rather than the screen under it, which
  comes free from `ModalBottomSheet` — the same reason `SupplementEditSheet` wires no handler.
- **Hue names are now legitimate, and that reverses half of one rule.** The colour cells carried no
  visible label because the scheme flips in dark mode and a hue name would have been wrong half the
  time. A fixed hue does not flip, so the thirty name themselves honestly — `ds_mascot_colour_*` in
  `:core:designsystem`, alongside the five moved out of `:feature:profile`, reached through a public
  `@StringRes val labelRes` on the enum. That constructor param is what deleted the feature's
  `label()` `when`, which would otherwise be a thirty-five-branch second list to keep in step with
  the first. The names still do not appear *under* the swatches — thirty-five labels is a wall of
  text — so each cell keeps the name on its `contentDescription`, and the row that opens the sheet
  prints the chosen one.
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
- **Every state performs, and every channel rests at phase `1f` — `pulse` is why.** The bob is now
  one branch of `mascotMotion(state, phase)`, a pure function returning a translate, a rotation and
  two scales: `Celebrating` hops twice a cycle and *stretches in the air* (the ground is where
  phase `1f` leaves it, so the ground must be the neutral pose — squashing on landing would park a
  flattened mascot for anyone with animations off), `Thinking` tilts over a three-dot mouth,
  `Sleepy` breathes in place under a drifting "z", `Happy` sways. `Idle` is deliberately
  **unchanged**: it is what ~20 call sites draw, and a greeting that started hopping is a change
  nobody asked for. Because the whole vocabulary is one pure function, `MascotAvatarTest` sweeps
  `MascotState.entries` at phase `1f` and asserts the identity pose — a state added later cannot
  quietly skip the rule — plus an envelope sweep, since "full character" has to stay inside a 24dp
  chat row too (every amplitude is a fraction of the avatar's own size).
- **Staggered by harmonic, never by phase offset.** Three sparkles and three thinking dots need to
  be out of step with each other, and the obvious way — offsetting each one's phase — breaks the
  rest rule, because `sin(2π·(1 + offset)) ≠ 0` leaves each of them somewhere arbitrary when the
  loop is pinned. So they ride `pulse(phase, k)` for k = 1, 2, 3: visibly out of step frame to
  frame, exactly `0` at phase `1f` for every k. Both depths are subtractive from the rest value
  (`1f - depth * abs(pulse(…))`) so full size and full opacity *are* the rest pose, and the sleepy
  "z" fades on `sin(π·phase)` so at rest there is no "z" at all rather than one stuck halfway up.
- **The state change springs, and it springs *to* neutral.** An `Animatable` snapped to 0.86 and
  released on a bouncy spring whenever `state` changes, so the `Idle → Celebrating` flip in
  `ConfirmTargetsScreen` and the coach's `Thinking → Idle` are moves rather than cuts. It animates
  *to* the rest pose for the same reason the loop does: an `Animatable` snaps to its target under
  **Remove animations**, so the target has to be the pose the mascot should be left in. The first
  composition is skipped via the `remember { arrayOf(state) }` idiom `rememberFillDirection`
  already uses — three dozen avatars squashing on screen entry is not an entrance.
- **The poke is opt-in, and it is a `pointerInput` rather than a `clickable`.** `interactive`
  defaults to **false** because a tap handler consumes the gesture, and most of the ~35 call sites
  sit inside something already clickable — the buddy picker's cell (a mascot that stopped selecting
  itself would be the picker's one unforgivable bug), a Progress card, a chat row. Four screens
  where the mascot is the subject opt in: Home's greeting, onboarding's Welcome, the Profile header
  and the coach's empty state. `pointerInput` because a ripple over a drawn character is wrong and
  a decorative avatar must not appear in the accessibility tree as an unlabelled control; the
  bounce runs on `rememberCoroutineScope()`, whose context carries the recomposer's
  `MotionDurationScale`, so it collapses to a cut with the rest. No haptic: this app has none
  anywhere, and a mascot is not where a second feedback idiom starts.
- **The body is a `Path` so the sheen can be clipped to it.** `drawBody`'s five draw calls became
  `bodyPath()` returning one path — the same pixels, since a `RoundRect` added to a path is what
  `drawRoundRect` drew — which lets a single low-alpha circle in `MascotColors.feature` shade the
  lower right of all five silhouettes instead of needing a per-shape special case. The Celebrating
  sparkles are drawn four-point stars for the same reason they now twinkle: they were two `Text("✦")`
  glyphs, and a glyph cannot be scaled per frame off a draw-phase read. Brows are the one piece of
  face geometry added, and like the mouth they are **shared across every buddy** — arched for
  Happy/Celebrating, one raised for Thinking, absent for Idle and Sleepy, so no character can come
  to mean something a state does not.

### Home

- **The photo card reports the run, and `weightArc()` sits in `:core:data` because two features
  draw it.** Home's card counted the days since the last shot and said nothing about the shots
  themselves, while `HomeViewModel`'s first combine was already collecting the whole list and
  keeping only its newest date. It now carries the count and the oldest-to-newest weight across the
  shots that have one — the same figure the Progress tab's Photos card reports, folded once in
  `:core:data/progress/` rather than twice: `:feature:*` modules never import each other, so a
  feature-local copy on each side is two answers to one question waiting to diverge. The line on
  Home is deliberately **uncoloured**, which is where it differs from the Progress card: the arrow
  carries direction, but the weight card on this same screen already prints the goal-relative
  verdict on that movement, and a second coloured one would be Home grading one number twice.
  `StatusMark`'s KDoc already put photos among the subjects with no verdict of their own, and that
  stays true — the card still wears no dot, because the *cadence* has no target to miss.
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
- **The greeting, the day-at-a-glance strip and the AI insight are one pinned block, above the
  fifteen cards that move.** `HomeHeaderBlock` replaced two separate cards that were answering the
  same question from two blocks. It wears a 24dp radius against the reorderable cards' 20dp, so the
  pinned block reads as chrome rather than as the first card of an editable list. The greeting is
  the app's one door to the coach, so hiding it would strand a whole feature — the `AIChip` is the
  tap target now, not the whole card, because the strip and the band sit in the same card and a card
  that navigated away when you touched a number would be the wrong answer to either. The insight is
  an **inset band inside** that block, which is what preserves the reason it was pinned: dismissing
  it collapses within the block, so the cards below shift by the band's height and no further. It is
  still the screen's one `tertiaryContainer` background; the chip beside the greeting is a chip, not
  a card background. `AIInsightCard` in `:core:designsystem` is untouched — Progress still draws it,
  Home simply stopped being a caller.
- **The Today strip carries no new data, and its priority list is a guess that says so.**
  Each cell restates a card that is *currently visible*, chosen by `todayStripCards()` from
  `[Calories, Water, Steps, Streak, Weight]` — so hiding a card takes its cell with it and the strip
  can never report a figure the user switched off. Fewer than two survivors hides the strip
  entirely: one cell is not a summary. *ponytail: the app records no per-card tap counts, so there
  is no signal to rank by; drive it off real counts if one ever exists rather than inventing a
  ranking to justify a different list.*
- **Cards have a width class, and `homeRows()` pairs them.** Seven single-number cards are half
  width (Streak, Weight, Steps, Sleep, Heart, Blood pressure, Progress photo) and pair with an
  adjacent half; everything else takes its own row. Fifteen equal full-width cards was a flat scroll
  where last week's blood pressure sat in the same register as today's budget. `isHalf()` lives in
  `:feature:home`, deliberately **not** as a property on the `HomeCard` enum: `:core:designsystem`
  knows nothing about a running fast, and Fasting is the one card whose width moves — a running fast
  owns a timer, a goal bar and two buttons, an idle one is a label and a Start button, so it borrows
  `MetricCard` rather than drawing a zeroed timer nobody started. An unpaired half falls back to
  full width rather than leaving a hole beside it, which is why **no card may depend on its position
  or its neighbour**: `MetricCard(wide)` is one composable with two arrangements and the same
  content in both. `HomeRowsTest` is the guard.
- **Gating runs *before* pairing, not inside each `when` branch.** That is the one thing the
  redesign moved rather than kept: every data gate is now `HomeCard.hasData(uiState)`, applied to
  the layout before `homeRows()`, because a card hidden for want of data would otherwise still claim
  a slot and split a pair that should have closed up. Each gate keeps the reasoning it was written
  with, and the rule is unchanged — **a visibility switch can only ever remove a card, never force
  one.** Sleep left *on* with no watch synced is as absent as it was before the editor existed, and
  the editor row says so under the switch (`HomeCard.note`): a control that looks broken is worse
  than one that explains itself. A hidden card's flows are still collected: Home combines everything
  in one chain, and splitting it per card would be a large conditional-flow change for an unmeasured
  gain. `HomeScreenGatedPreview` is the artboard-C case — no profile, no watch, cycle off — and it
  is what proves the survivors re-pair.
- **The status mark is sparse, and the absence is the design.** An 8dp dot appears on four cards
  only — calories inside the budget, a streak that is running, a fast past its goal, a weight moving
  the way the goal asks. Everything else carries none, because on-track/off-track is not a question
  the app can answer about water, macros, steps, sleep, heart, blood pressure, mood, cycle,
  supplements, workouts or photos, and a dot on every card would be the screen grading the user.
  `error` is reserved for genuinely off track (only Weight can reach it) — over budget and a broken
  streak are **neutral**, since a day in progress is not a verdict. The mark carries no content
  description: it restates the figure beside it.
- **Calories is the only hero, and that is why nothing else got promoted.** A 120dp ring with a 24dp
  stroke against every other card's flat treatment. Two heroes is no hero.
- **Both mood rows are meters now.** Mood used to be a single choice on the argument that lighting
  up sad *and* neutral *and* happy to reach "good" says the wrong thing; the redesign reversed that
  deliberately. Every other tappable row on Home — energy, water, cycle flow, supplements — fills up
  to its value, and one row that looked identical and answered a tap differently was the odd one
  out. The glyphs already carry the difference between a 2 and a 5, so the fill does not have to.
  Tapping the level you are on still clears it, on every row.
- **Sleep has no stage bar, and never will without a schema change.** `SleepNight` carries
  `minutesAsleep` and nothing else; neither health leg imports Deep/REM/Light/Awake. The redesign
  asked for a four-segment bar and it was dropped rather than faked — which also closed the colour
  conflict it would have created, since four stage colours would collide with the frozen
  Protein/Carbs/Fat semantics.
- **Macros is three tracks, one per macro, each against its own target.** The single stacked
  `MacroBar` shows the *goal* split — segment widths are the targets, not the intake — so it could
  say what the day was supposed to look like and never how close any one macro was to its own
  number. `MacroBar` itself is untouched: the diary's summary and Profile's Goals card both still
  draw it, and the goal split is exactly what those two mean.
- **The streak card's two sentences are gone.** The unlit badge already says what is next, and the
  weight-badge line said what the Weight card says one row over — which also retired `captionFor()`,
  `weightLineFor()`, their tests, `StreakCard.kt`'s `literalExceptions` entry and
  `HomeUiState.weightProgressKg` (Progress keeps its own). `BadgeDot` gained a `size` parameter for
  this one caller: five 32dp dots do not fit inside a paired card's 126dp of content on a 360dp
  screen.
- **"Rearrange your Home" is a link, not a second editor.** It navigates to the same
  `HomeLayoutRoute` Profile's own row opens, via an `onOpenHomeLayout` callback of the shape
  `onOpenCoach` already has (`:feature:home` cannot import `:feature:profile`). It adds no drag
  handles to Home and no reorder capability there — the editor is a screen with a ViewModel of its
  own, and duplicating it into a tab would be a second way to write one column. Its whole job is to
  say the block is the user's to edit, which is otherwise undiscoverable.
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

### Progress, recap & the energy check-in

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
- **Progress is an overview plus one detail page per subject, and a detail page is a swap-in, not a
  route.** Thirteen peer pills in a scrolling `SegmentedToggle` meant nothing past the fifth was
  reachable without a swipe nobody knew to make, and no pill said whether there was anything behind
  it. `ProgressScreenState.selectedSubject` is the whole navigator: null draws `ProgressOverview`,
  anything else draws `SubjectDetail`. A route would have earned its own `ViewModelStoreOwner` and
  with it a second copy of `ProgressViewModel`'s twelve repositories, to render a page that writes
  nothing — `RecapScreen`'s argument, one level up. Two things follow. The page wires its own
  `NavigationBackHandler`, or back would leave the tab; and the bottom bar and the FAB stay up,
  because `AppScaffold`'s `isTopLevel` rule never sees a new route — so the page draws its own
  `DetailHeader` rather than borrowing `AppTopBar`. The overview's scroll survives the round trip
  for free (`AppScaffold` hoists it), and each detail holds its own so a sibling hop opens at the
  top.
- **Every subject page becomes a route, and the entry above is superseded.** The argument that kept
  them swap-ins was that a route "would have earned its own `ViewModelStoreOwner` and with it a
  second copy of `ProgressViewModel`'s twelve repositories." That overstates it, and the overstated
  version is what made the decision look settled. A per-subject container reads **its own one to
  three flows**, not twelve; the overview beneath keeps the twelve either way; and only one subject
  entry is ever on the back stack, so the peak is the tab's set plus that page's slice, not two
  sets. Nutrition and Badges are the only two where it genuinely bites — one copies a targets fold,
  the other reads five repositories to draw an achievement list.
  What it buys is the thing `PhotoComparisonScreen` bought a commit earlier: a chart page drawn
  inside the `ProgressRoute` entry renders inside the `Scaffold` that draws the bottom bar and the
  docked FAB, so a subject page wore tab chrome while sitting a level below the tab, cleared the FAB
  with 72dp of `DockedFabContentPadding`, and hand-wired a `NavigationBackHandler` so back would not
  leave the tab. As routes: Nav3 owns back, each container dies with its entry rather than living as
  long as the tab, and the page draws its own `AppTopBar` at `WindowInsets(0)` — the scaffold's
  `innerPadding` has already cleared the status bar, and the default clears it twice.
  What it costs, beyond the source: the sibling switcher's value column (below), Progress's two
  panes at ≥840dp (**Adaptive layout**), and about fifty files.
  The conversion runs one subject per commit. `RoutedSubjects` in `ProgressScreenState` and the
  `else -> null` arm of `Subject.route()` are the migration's two moving parts, and both go with
  `SubjectDetail.kt` when the last subject lands.
- **The sibling switcher names its siblings and no longer quotes their figures.** A row used to
  read "Mood · 4.2 / 5", folded out of `summarizeAll()` — every subject at once. A page owning one
  series cannot fold that, and the alternative was a second container per page whose only job was
  captioning a navigation row, which is the cost the entry above is at pains to say it is *not*
  paying. It degrades exactly one group: the ≤3-sibling branch draws pills and never showed a
  value, and Body, Nutrition and Training all have three or fewer — only Wellbeing's four rows lose
  the column. For the same reason the page's toolbar share is unconditional where `DetailHeader`'s
  was gated on there being a week worth reporting: `RecapScreen` folds its own recap and says so
  when there is nothing, which is a better answer than a control that silently is not there.
- **A hop between siblings replaces the page rather than pushing it**, which is what the switcher's
  own KDoc has always promised — Sleep → Mood → Heart leaves one back step, not three.
  `TopLevelBackStack` has no `replace`, so `AppScaffold` spends a `removeLast()` before the `add()`.
  That is also the one place the half-converted state shows: a sibling that is still a swap-in has
  no route, so the pop lands on the overview rather than on its page.
- **The conversion landed, and `SubjectDetail.kt` is gone.** All fourteen subject pages are routes:
  `Body()`'s fourteen-way dispatch, `EmptyDetail`, `emptyCopy`, `DetailHeader`, the `embedded` flag
  and `SelfScrolling` are deleted, and `ProgressScreenState` is down to the overview's own three
  fields — which groups are expanded and the two log sheets its empty-card hints raise. The
  `pendingRoute` indirection went with the dispatch it existed to avoid threading callbacks through:
  `ProgressOverview` takes a plain `onOpenSubject` and `onOpenRecap` now.
  What the thirteen containers actually cost, measured rather than feared: eleven read one to three
  flows, Activity and Weight and Nutrition and Measurements read three, and only Badges reads six —
  and that one folds its six down to five numbers and a unit, so no series reaches its state.
  `ProgressViewModel` is unchanged at thirteen, because the overview still folds every subject for
  its cards, its Patterns card and its weekly recap.

- **`Subject` replaced `ProgressTab`, and `group == null` is Badges.** Twelve metric subjects in
  four groups (Body · Nutrition · Training · Wellbeing) plus Badges, which is drawn as a summary row
  under the grids because it is an achievement list, not a trend — a metric card promising a preview
  line would lie about what is behind it. It still has a detail page, so all thirteen stay
  reachable. `Subject.accent` is an explicit column rather than a fold over `group`, because
  Activity breaks the pattern: it sits in Training with the lifting, but its steps come off a watch
  like Sleep's and Heart's, and every imported series draws in `secondary`.
- **`summarize()` is the one fold behind every card, and every branch calls the derivation that
  subject's own page already calls.** `sleepAverages()`, `stepAverages()`, `personalRecords()`,
  `byDay().averages()` — so a card and the page behind it cannot quote different numbers at the
  default range. The detail's own figures are **range-scoped** and the card's are not: the card is a
  standing summary of everything, the page is the window you picked, and the toggle saying so is
  right there. That is also why the bodies take no `SubjectSummary` — they derive their own hero.
- **There is no sparse-account flag.** Every difference the sparse overview shows falls out of the
  data: the mascot note appears when any group has nothing tracked, the recap's cells omit
  themselves, the insight card is already null-hidden, and a group collapses to one expandable row
  when none of its subjects has data. A threshold ("fewer than N tracked") would be a number to
  maintain and a second thing that could disagree with the grid.
- **The Patterns card compares two things the user logs, with a median split and an effect-size
  floor — not a correlation.** Fourteen subjects and nothing ever related two of them, which is the
  one question this data can answer and no screen asked. `patterns()` is feature-local for
  `recap()`'s reason (one screen shows it, every input is a `:core:data` type) and derived for
  `streakStats`' reason: no table, no repository, no schema, nothing written, nothing sent to a
  model. Four calls matter. **The split is at the driver's median**, because a fixed cutoff would be
  this app deciding what "enough sleep" is — the median only says "your better half against your
  worse half", and ties fall to the low side unless that empties one, which is what lets a yes/no
  driver (trained, hit the fasting goal) run through the same code path with no special case. **Both
  day counts are rendered**, because "12 days against 9" is what tells the reader how much to trust
  the line. **Three floors** — 14 paired days, 5 per side, and a per-spec minimum on both the driver
  spread and the outcome gap — are what stop noise being reported as a finding, and an empty result
  is the ordinary answer on a young account: the card is then omitted entirely, the recap card's
  rule. **The copy never says "because"**, names no cause and gives no advice; it is the same line
  the cycle tab holds when it refuses to derive a fertile window.
- **Sleep needs no day offset and fasting does, and both follow from where the series are dated.**
  `SleepNight.dateEpochDay` is the morning the sleep ended, so the night before day D is already
  filed under D beside D's food; `FastSession.dateEpochDay` is likewise the end day, so what a fast
  can show up in is the *next* day's eating — the one `nextDay = true` spec. `PatternsTest` asserts
  both alignments by shifting the series and watching the sign flip, because getting either wrong
  produces a plausible sentence that is simply about the wrong day.
- **Cycle, heart, blood pressure, weight, supplements and water are excluded from Patterns, each
  for its own reason.** The first three because a line relating them to anything is a clinical claim
  FitPulse cannot stand behind (and cycle data stays on the phone by rule). Weight on either side
  because a day-level weight is mostly water, and intake against the real weight trend is already
  owned — over a proper window, with its own guards — by `goalProjection()` and the energy check-in;
  a second, weaker answer to the same question would contradict the first. Supplements because a
  percent-taken split says nothing. Water because the tab holds no per-day water series, only the
  logged-day set.
- **There is no "see all" door on the card, and that is a decision, not an omission.** The fold
  returns at most `MAX_PATTERNS` and the card draws every one of them, so nothing is hidden and a
  door would open onto what is already on screen — `DiaryDateHeader`'s rule about the calendar
  pane. An overlay becomes worth its `ProgressScreenState` field and back handler the day the spec
  list is long enough that the fifth pattern is worth reading.
- **The Photos card quotes the weight the shots carry, not just their date.** A count and a
  recency nag said nothing about what a progress photo is kept for, while the field that answers
  it — `ProgressPhoto.weightKg`, the Add photo sheet's stepper — was already on every shot and
  already read by the comparison overlay. The card now leads with the oldest-to-newest arc across
  the weighed shots and keeps "last one N days ago" behind it. Three calls follow. **Two weighed
  shots on different dates is the floor**: one end is not two (`Recap.weightArcKg`'s rule), and a
  delta reported "over 0 days" is a change over no time at all — under it the card says exactly
  what it said before, arrowless and neutral, because the field is optional and logging a shot
  without a weight is an ordinary thing to do. **The span is the weighed pair's own**, not the
  first-to-last of every photo, or the number would cover ground the delta doesn't. **The
  judgement is `goalRelativeTrend`, not the sign of the delta** — the call `ComparisonHeadline`
  makes over a hand-picked pair, so the card and the overlay behind it cannot read one run two
  ways. Direction rides the arrow and the judgement rides the colour, which is what `TrendArrow`
  and `TrendDirection` are separate for; the text stays absolute, the Weight card's rule.
- **An empty subject keeps its slot, dashed.** A card that vanished when it had no data is a
  subject nobody would ever find, so an untracked one draws a 1dp **dashed** `outlineVariant`
  outline over nothing, says "Nothing yet", and still opens its page. The dash is what carries the
  difference without colour. Its affordance line is a door to that page, which carries the
  explanation — except Blood pressure's "Log a reading", the one subject whose sheet already lives
  on this screen. Tracked cards sort before empty ones inside a group.
- **The range toggle lives inside the chart card it controls, and the range is per subject.**
  `ProgressScreenState.ranges` is a `Map<Subject, ChartRange>` for the session, because one shared
  range would have a tap on the Sleep chart silently re-slice the Weight one now that they are
  different pages. `ChartCard` takes a null range for the second chart on a page that already has a
  toggle (Activity's burn chart) — two identical toggles would be two controls for one value.
- **`AIInsightCard` moved to `:core:designsystem`, and the Weight page's is the app's only one fed
  by two sources.** Home and Progress both draw it now, the `AppCard`/`BadgeDot` path; its
  `subline` and `headlineStyle` default to Home's shape so that call site is unchanged by the move.
  On the Weight page the projection takes the headline when there is one, and **the energy check-in
  takes it when there isn't** — a measured maintenance stands on its own, and it is the reason the
  card doesn't vanish for anyone who never set a goal weight. That fold is what retired
  `GoalProjectionCard` and `EnergyCheckInCard`: both said one line each, on a page that now has one
  place for a line. `tertiaryContainer` is still one card per screen.
- **The recap card's cells omit themselves rather than reporting zeros.** The 2×2 grid closes up
  around a missing average or a missing mood, and the weight cell degrades to an em dash with a flat
  glyph and the words "Too few readings" — a figure this app never invents, the rule every "—" on
  the screen already follows. `RecapCard` is one component, so the recap overlay and the shared PNG
  get the same card and cannot drift. The header still reads "Last 7 days", never "This week": the
  window is rolling, and the handoff's calendar wording would contradict a decision made above it.
- **`WeightProgressChart` still folds the goal into its axis range.** A target far from the data
  flattens the trend rather than dropping the dashed line off the chart, and that is the deliberate
  call: a goal line you cannot see is a goal you stop steering by. The axis labels are drawn
  **inside** the same `Canvas` as the gridlines they name, off the same `yFor` mapping — a label
  gutter laid out beside the plot would distribute four labels evenly and be a pixel or two out at
  every font scale.
- **The recap screen is an overlay inside the Progress tab, not a route** — and that half has
  never moved. A route earns its own `ViewModelStoreOwner` *and* a back that leaves the tab, so it
  stays a swap-in over `ProgressContent` wiring its own `NavigationBackHandler`. What did move is
  where its data comes from: it read `ProgressUiState` verbatim and folded its own `recap()` in
  composition until it was given a flow package, and now `RecapViewModel` does the folding. That
  container reads seven of `ProgressViewModel`'s repositories a second time, which is the cost the
  old entry refused — taken deliberately (see **Three overlays became flows** below), and paid
  back in one place: the **period lives in the container**, so `RecapUiState` is a single folded
  `Recap` rather than a second copy of the dozen series behind it, and a period picked once
  survives closing and reopening. The Progress header's icon opens it; the week share it used to
  open lives inside it now, on whichever period is showing. Its movement row and its lift notes
  are drawn for Month and Year only, which is what leaves the weekly card on the overview
  untouched. The header icon is hidden when there is no recap, the card's own rule.
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
- **`SegmentedToggle` splits its width evenly until a pill would fall below `minPillWidth`, then
  scrolls.** The default floor is 64dp — what five pills already left on a 360dp screen — so every
  caller renders exactly as it did before the floor existed, and the branch only fires on very
  narrow screens, where scrolling replaces clipping. The parameter exists for one caller:
  Progress's range toggle now sits *inside* a chart card beside its title, where four pills do not
  fit at 64dp, and "1M" clears 40dp with room to spare. Lower it only where every option is that
  short; the default is about a `labelLarge` word, not about a number.
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

### Saved meals, recipes & the food library

- **A saved meal is a snapshot, not a live link.** Saving copies the section's entries into
  `saved_meal`/`saved_meal_item`; editing or deleting the original diary rows never touches it,
  and deleting the saved meal never touches what was logged from it. Re-logging always writes
  fresh entries stamped with the diary's *selected* day. The panel shows the newest
  `MAX_SAVED_MEALS` only, and saved meals stay out of the data export for the same reason
  favorites do — convenience data, not history.
- **A food the user owns and a starred favorite are one row, and that collapse *is* the food
  library.** `favorite_food` was already designed to survive its origin — it carries its own macros
  "so a re-star doesn't depend on the original diary row still being there" — so authoring a food
  from the add-entry form needed a write path and nothing else: `setFavorite()` is that path, and
  starring a diary row is authoring-by-example. The original spec asked for custom foods to be
  searchable while starred favorites stayed out, which needs a column to tell them apart; the
  builder is still `fallbackToDestructiveMigration(dropAllTables = true)`, so that column costs
  every install's database, and a second concept whose only difference is which surface it appears
  on is not worth one. The collapse also closes an existing hole: a starred food past the
  newest-`MAX_SUGGESTIONS` window used to be out of view *and* out of reach of its own star.
  Custom foods stay **out of the export**, the rule saved meals, recipes and routines follow.
- **`searchFoods()` is the whole search, and the user's foods replace rather than join.** The pure
  fold sits in `:core:data/food/CommonFoods.kt` beside `searchCommonFoods()`, which it calls —
  `localMealIdeas()`'s shape, no table and no query. A custom "Chicken breast, cooked" *replaces*
  the built-in row of that name, deduped on the trimmed lowercase name `mergeSuggestions()`
  already treats as identity, because a search offering two answers for one food is the thing the
  user authored theirs to stop. It hands back `ScannedProduct`s: a barcode hit, a staple and one of
  the user's own foods all seed the form through the one `ScannedProduct.toAddEntryForm()`, and a
  third type would fork the confirmation screen. `FoodSearchViewModel` consequently takes one
  dependency where its KDoc used to boast of none — the built-in half is still a list in the APK,
  so there is still nothing to debounce and no network to check.
- **Renaming a food is a move, not an `UPDATE`.** `name` is `favorite_food`'s primary key, so
  `FavoriteFoodDao.rename` writes the row again under the new name and retires the old one in one
  `@Transaction` — `FoodEntryDao.replace`'s rule, so the library never emits a frame with the row
  in neither place. The old name stays as an `isFavorite = 0` tombstone keeping its macros, which
  is what the table has always done; renaming onto a name that already exists overwrites it,
  because name *is* the identity here. That identity is also why saving the same name twice from
  the sheet is an **edit** — which is the whole of "edit a food later", and why the library screen
  renames and deletes but never opens a form of its own.
- **"Save as my food" is one button on the sheet that already holds every field.** `AddEntryForm`
  carries the portion, the macros, the micronutrients and `withPortionAmount()` repricing, so
  authoring is a button rather than a second screen. Hidden until there is a name *and* calories
  (the meal-ideas button's rule, and a nameless food is a quick add), absent while correcting a
  logged row for the reason the four panels are, and it does **not** close the sheet or clear the
  form: keeping a food and logging it are two intentions and the user may want both. No
  confirmation toast — the saved-meal rule — because the food appears starred in the suggestion
  panel directly above it the moment Room emits.
- **Profile → Food library holds three lists now and still cannot log.** The foods list is drawn
  first (it is the one authored deliberately, and the one search leads with), on the same shared
  `LibraryRow` and `RenameSheet` the saved meals, recipes and routines use, with the same
  ask-first delete a user-authored thing gets. `FoodLibraryViewModel` reaches the foods by name —
  `deleteMyFood(name)` and `renameMyFood(old, new)` rather than a `FoodSuggestion` it would have
  to fabricate, since `:feature:profile` cannot import `:feature:food`'s converters. The screen's
  title moved from "Saved meals & recipes" to **"Food library"** in its three literal sites.
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

### Nutrients

- **~~Fiber, sugar and sodium are reported, never graded.~~ Seven nutrients, all graded.** The old
  entry's reason was specific and it no longer holds: *"there is nothing on the profile to derive a
  fiber goal from."* There is. The DRIs are published as a function of **sex and age band**, and the
  profile has carried both since onboarding — they are two of the six Mifflin–St Jeor inputs — while
  fiber (14 g/1000 kcal) and free sugars (≤10% of energy) ride the *calorie* target itself, so
  editing a calorie target moves them exactly as it moves the macro split. `nutrientTargets()` sits
  beside `calculateDailyTargets()` and is derived, never stored: no new profile column, no new
  editor, nothing on `ConfirmTargetsScreen`. `DailyTargets` is still untouched — it is the calorie
  and macro type and a seventh nutrient is not a macro. What survives from the old entry: `MacroBar`
  stays three segments (fiber is a subset of carbs — a fourth would double-count the day), these
  nutrients carry **no semantic colour** because they appear in no bar and the three macro colours
  are spoken for, and `0` still means unknown-or-none. They reach Google Health as `nutrients`
  entries beside `PROTEIN`, converted to the array's one unit.
- **The four are the Nutrition Facts panel's four** — vitamin D, calcium, iron, potassium. A closed
  set chosen because it is the same set three ways: what a package prints, what FDC branded rows
  carry, and what a user can check against the box in their hand. A twelve-nutrient set was weighed
  and declined: past the panel four, FDC's coverage falls off and the built-in list would be mostly
  zeros, so most days would read as a deficiency the app invented.
- **A nutrient with no figure is absent from the panel, not a zero against its target.** This is the
  old entry's "renders nothing when all three are zero" rule generalised, and it is now load-bearing
  rather than tidy: half the built-in foods have no calcium figure, so grading a blank would report
  a shortfall that is really a gap in the data. `Nutrients.readings()` drops any nutrient at 0, and
  `NutrientPanel` renders nothing at all when every one of them is.
- **What is missing is *said*, not inferred — `DiaryTotals.foodCount` and `.foodsWithMicronutrients`.**
  Dropping empty rows hides the gap; the coverage line names it ("from 3 of 8 foods"), so the graded
  rows read as a floor rather than a measurement. Fiber, sugar and sodium deliberately do **not**
  make a food count as covered — the built-in list has filled those for years, and counting them
  would report a full house for a day with no vitamin figure in it. The line is hidden once every
  food carried data, because "from 6 of 6" is noise. Progress's card passes no coverage: the
  averaged-over-N-days line beneath it is already a denominator, and two on one card invite being
  read against each other.
- **`Nutrients` is one `@Embedded` value type, and the trio moved into it.** Nine carriers held the
  same three fields; four more nutrients would have been ~60 repeated lines and a fifth would repeat
  the cost. `plus`, `div` and `times` on the type are what collapse `dailyTotals()`, `averages()`,
  `dailySeries()`, `Recipe.perServing()` and both `withPortionAmount()` overloads to a line each.
  Room's `@Embedded` keeps the **existing column names**, so the three columns did not move and the
  migration is additive.
- **Iron and vitamin D are stored in micrograms; calcium, potassium and sodium in milligrams.** The
  field name carries the unit, as `sodiumMg` always did. Every nutrient figure in this app is an
  `Int`, and iron at Int milligrams rounds a 0.4 mg food to nothing — over a day's eight entries
  that compounds into a shortfall the user never had. `formatNutrient` divides for display, and has
  a test over its wording, which is what earns it the stay-in-Kotlin exemption.
- **Sugar and sodium grade as limits, everything else as a goal.** `NutrientDirection` is why: only
  a `StayUnder` nutrient can draw `error`, so passing a calcium goal is never coloured as failure.
  That is the Earned Red Rule the trend arrows already follow.
- **`NutrientPanel` takes plain rows, not `Nutrients`.** `:core:designsystem` is a leaf module with
  no dependency on `:core:data` — the same fact that puts the `Nutrient` labels in `:core:data`'s
  `strings.xml`. So the arithmetic lives once in `Nutrients.readings()` and each of the two features
  that draws the panel resolves three strings, the way each already writes its own `MacroLegend`.
  It expands **in place**, not into a sheet: a disclosure is not a level, so there is no back
  handler and nothing for predictive back to do.
- **`MicronutrientInputGroup` was left alone.** The four new nutrients are seeded by a scan or a
  picked food, repriced with the portion and logged, but there is no stepper for them and no
  read-only echo in the five sheets that draw that component. Nobody hand-corrects a calcium figure,
  and the user sees all seven where they are graded. Reopened by: someone actually wanting to edit
  one.
- **The AI paths were not widened.** `FoodRecognition`, `MealParse` and `MealIdea` still ask for
  three nutrients. A model asked what calcium is in a photographed plate will produce a number, and
  a fabricated micronutrient is the exact thing the coverage count exists to expose.
- **`ExportFoodEntry` keeps its nutrients flat while `FoodEntry` embeds them.** It is a wire format,
  not a domain type: `parseExport` accepts any file at or below `EXPORT_SCHEMA_VERSION`, so nesting
  would have made every v17 file on disk unreadable — including the weekly automatic backups, which
  is the one path a user reaches for when they have already lost their data. Four defaulted fields,
  one mapping line each, and `ExportTest` pins a v17 file importing.
- **A meal push that is rejected retries once without the micronutrients.** All seven enum
  names are guesses — the four added later are no better pinned than the three that came first —
  an unknown one fails the *whole* `nutritionLog`,
  and `pushMeals` records no link on failure — so a wrong guess would strand that meal forever,
  re-failing on every later sync. The fallback body is what makes guessing safe, and a value of
  zero is omitted anyway, so a quick add already sends it. Delete the retry when the names are
  pinned, not before.
- **FDC reports sugar under two ids** — `2000` on branded rows, `1063` on Foundation ones — so
  `toScannedProduct` tries the branded id first and falls back. Sodium (`1093`) was the app's first
  milligram figure, which is why its stepper steps by 50 and its field is a digit wider than the
  macro fields; the panel four join it at `1114` (vitamin D, µg), `1087` (calcium), `1089` (iron,
  reported in mg and stored in µg) and `1092` (potassium).
- **The built-in food list was backfilled by hand, and only where the food is a source.** 118 of its
  123 rows gained at least one figure — oils and sugar gained none, correctly. The `food()` helper
  takes iron in milligrams and converts, so a reference table's number is typed once as printed
  rather than multiplied by hand 118 times. Rows with no figure stay at 0, which is what the
  coverage line is for.

### Targets & editing an entry

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
- **The `RecipeBuilderRoute` carries no bottom nav and no FAB.** It gets that for free by not
  being a tab — `AppScaffold`'s `showsTabChrome` is true for a top-level route, or a Profile
  detail drawn beside its own tab, and nothing else — but the reason it is a route rather than a
  level inside one is its own: it is an authoring screen with its own Save, and leaving the tab
  bar up put a "Log food" FAB over it and let a tab tap walk away from a half-written recipe
  without the discard question `NavigationBackHandler` asks. The camera flows want one thing more
  than that, which is why `fullBleed` is a separate test beside it.

### Fasting

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

### Export, backup & reminder plumbing

- **Reminders never touch a `:feature:*` module.** The Profile switches are a
  plain Room write; `FitPulseApplication` reconciles WorkManager off
  `ProfileRepository.observeProfile()`.
- **A reminder is a chain of one-shots, not a `PeriodicWorkRequest`.** A periodic request takes its
  initial delay once and then re-anchors every later period to the end of the previous window. Doze
  deferrals accumulate, and — the part that never self-corrected — a DST change or a move between
  timezones shifted every subsequent firing permanently: an 08:00 nudge became 07:00 and stayed
  there for the life of the install. `nextRunMillis` was only ever consulted at enqueue, so nothing
  in the schedule could notice. Now `ReminderScheduler.schedule` books exactly the **next** firing
  and `ReminderWorker` books the one after it, re-deriving `nextRunMillis` against the current clock
  and zone every time. `ReminderScheduleTest` pins the property directly — 08:00 the Saturday before
  the US fall-back is 08:00 the Sunday after, 25 hours later, not 24.
  - The reschedule is on **every** path out of `doWork`, which is why the quiet checks moved into a
    `shouldNotify` predicate: an early `return Result.success()` for "breakfast already logged"
    would have ended that reminder for good, and an early return is exactly what the old worker was
    made of.
  - It is the **last** statement rather than the first. `REPLACE` on a unique name cancels whatever
    is running under it, which is this worker — by that point the notification is posted and there
    is nothing left to lose, and `enqueueUniqueWork` hands off to WorkManager's own executor, so the
    next run is booked whether or not the coroutine survives the line.
  - `reconcile` keeps `KEEP` for the reason it always had — it runs on every app start, and
    replacing would push the pending firing back each time — but the policy now also re-forms a
    chain that was somehow broken, because a finished unique work no longer holds its name.
  - `nextRunMillis` gained a defaulted `periodDays`, consulted only when there is no `dayOfWeek`. A
    weekly reminder is pinned by its weekday already; the fortnightly photo nudge is the one caller
    that is neither daily nor weekly, and without it a chain would have fired it every morning. It
    steps with `Calendar.add` rather than multiplying out, for `epochDayStartMillis`' reason.
  - This retires the `ponytail:` note that changing an hour in a later release could not reach
    existing installs. It now reaches them at their next firing, with no unique-work rename.
- **The export format lives in `:core:data/transfer/`, and moving it there was the backup's first
  step.** `BackupWorker` is in `:app` because a background job is a system surface, not a screen —
  the rule reminders and the widget both follow — and `:app` reaching into `:feature:profile`'s
  `ui` package to serialize a file would breach the module map in the one direction it forbids.
  So `Export.kt` sits beside `ImportData` and `DataTransferRepository`, which already held the
  import half; `buildExportJson` and `parseExport` are public because they now cross a module
  boundary, and every DTO stays `internal`. The move changed no byte of the file format — the
  version gate, the defaults and the v1-onwards fixtures moved verbatim into `:core:data`'s own
  `ExportTest`, which is what proves it.
- **`exportJson()` is the one list of reads, and it is still not the `exportAll` that was ruled
  out.** Profile's export button and the worker want the identical twelve arguments, and two
  copies of that list is two places to edit at the next schema version. It adds no repository
  method and no transaction — reading stays a set of independent `all*()` calls, each on its own
  domain's repository, which is the property that made a twin unnecessary in the first place.
- **Backups are written to internal storage, three files deep, and are never restored
  automatically.** SAF needs a picker, a picker needs a user, and a user is the thing a background
  job does not have; `filesDir` is also what Android's own backup covers, so one write serves both
  mechanisms. Rotation sorts on the **name** — `fitpulse-<epochMillis>.json`, fixed-width for two
  centuries — so ordering and the date on the row come from one source and a device transfer
  resetting `lastModified()` can't scramble either. `staleBackups()` is a pure function for
  `sanitizeInsight`'s reason: it is the half a JVM test can reach. And restore stays manual and
  asks first, because a job that restored on its own could wipe a good device from a stale file,
  and a one-tap row in a settings list is not the confirmation that picking a file in SAF is.
- **The backup job is not a `Reminder`.** Every entry in that enum is a nudge whose `ordinal` is a
  notification id; this posts nothing and has no Profile switch. It is enqueued from
  `FitPulseApplication` beside the reminder reconciliation with `ExistingPeriodicWorkPolicy.KEEP`,
  for `reconcile()`'s own reason — `onCreate` runs on every launch, and `UPDATE` would reset the
  initial delay each time so the first run never lands.
- **Auto Backup was already on; the rules now say what it covers.** `allowBackup="true"` with two
  entirely-commented-out rule files meant the Room database was going to the user's Drive
  undeclared and untested. Both files are **exclude-only**: an include list has to name
  `fitpulse.db` *and* its `-wal` and `-shm` companions, and a forgotten one restores a truncated
  database. The one exclusion is `progress_photos/` from the cloud copy — the only thing that can
  blow the 25 MB cap, and the thing the export has never carried either. `device-transfer` is
  absent on purpose: it has no cap, so the photos should ride along.

### Launcher shortcuts & the quick-action sheet

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
- **The FAB sheet's rule sits at 3|4 because rows 1–3 *are* the diary's chip row.** *Say what you
  ate*, *Scan a barcode* and *Log food* route to `VoiceLogRoute`, `BarcodeScanRoute` and
  `FoodCaptureRoute` — the same three destinations `DiaryBody`'s `LabelledActionChip` row sends,
  in the same order, drawing the same three `AppIcons`. So *Log food* is the **camera**, which the
  launcher shortcut's long label ("Photograph a meal") has always said, and *Add photo* is a
  **body progress shot** (`:feature:progress`'s `AddPhotoSheet`) with no AI and no plate in it.
  A redesign handoff read those two backwards — it had *Log food* as manual search and *Add photo*
  as plate recognition, which strands the third food door below the split and needs a `tertiary`
  glyph to mark it back up again. Read correctly the two kinds are contiguous, the rule is a
  straight line between them, and the exception disappears: three `tertiaryContainer` badges above,
  three bare `onSurfaceVariant` glyphs below. The leading slot is 40dp in **both** cases — a badge
  and a bare glyph share one optical column, or the labels stop lining up across the rule.
- **The quick-action sheet is the one caller passing `AppBottomSheet(horizontalPadding = 0.dp)`.**
  Every other sheet takes the 16dp default. A list row's pressed state layer that stops 16dp short
  of each edge reads as a button rather than a row, so these rows take the gutter themselves and
  the ripple runs the sheet's full width. The rule keeps the 16dp, because a full-bleed divider
  reads as a seam between two sheets instead of one inside a list.
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

### Reminders & notifications

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
- **The weekly recap notification is the only one whose tap does more than pick a tab, and it rides
  `ShortcutAction` to say so.** `Reminder.WeeklyRecap` is appended for `Supplements`' and
  `Workout`'s reason — `ordinal` is the notification id — at Sunday 19:00, late enough that the
  week is over and early enough to still be read. Its intent carries `EXTRA_TAB=Progress` *and*
  `EXTRA_ACTION=OpenRecap`: the first is the mechanism that already existed, the second is a new
  `ShortcutAction` entry rather than the `EXTRA_OVERLAY` extra plus a third nullable state in
  `MainActivity` that would have been a second copy of one delivery mechanism — exactly the
  argument `HealthSync` made when it rode that enum instead of growing its own. So `Reminder` gains
  a nullable `action` and `notify()` a nullable param, and nothing new parses an intent.
  `AppScaffold` holds the request as state (`openRecapRequest`) rather than consuming it in the
  effect, because the Progress tab may not be composed when the intent lands; clearing it on
  consumption is what lets a second Sunday re-open a recap the user has closed, `tabRequest`'s own
  shape. **The recap stays an overlay** — nothing here makes it a route, which would earn a second
  copy of `ProgressViewModel`'s twelve repositories to draw a page that writes nothing.
- **The quiet-week guard is literally `recap()`'s own null test.** `hasRecapToShow()` asks whether
  the last seven days hold one logged day, over the streak's four-domain `loggedDays()` — the same
  fold `ProgressViewModel` and `observeInsightRequest` already make — so the notification cannot
  open an overlay the card on that tab is hidden for. The worker cannot fold `recap()` itself
  (twelve repositories), and it does not need to. The window's `6` is `RecapPeriod.Week.days - 1`
  spelled out: that enum is a `:feature:progress` UI type and `:app/reminder` has no business
  importing one, and if the two ever drift `RecapScreen` already degrades to its empty state. The
  week's numbers stay *out* of the notification body for the same repository-count reason.
- **The v16 export bump carries two switches, not one.** `recapReminderOn` is the new one;
  `workoutRemindersOn` landed on `Profile` after v15 shipped and was simply missed, so a restored
  backup silently lost a training-day switch the user had set. Both are defaulted, so a v15 file
  still imports — the rule every addition follows.

### Widget & Wear

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

### AI — the coach & the daily insight

- **The coach and the daily insight describe the same day, in one place.** `InsightRequest` is
  the *only* payload either sends — the goal, the calorie/macro/water gaps, the streak and the
  weekly weight delta, still never age, sex, height, absolute weight, name or email. `insightFor()`
  and `insightRequest()` moved into `:core:data/insight/` when the coach arrived, because two
  feature modules now need them and `:feature:*` modules never import each other — the same call
  `goalProjection()` made. Home builds the request from state it has already combined for its
  cards; the coach has no such state and uses `observeInsightRequest()`, which combines the seven
  flows itself. Both land in `insightRequest()`, and both prompts format their numbers with
  `dayNumbersBlock()`, so a field added to one is shown by the other — a coach contradicting the
  card that sent the user to it is the failure this prevents. The coach *used* to be told
  additionally what it did not know (no yesterday, no individual meals, no weight) and pointed at
  the tab that did, because a free-form question would otherwise be answered with an invented
  figure the diary contradicts two taps away. That paragraph is gone: it is no longer true. The
  coach reads those days itself now, and what replaced it is narrower and does the same job —
  **call a tool rather than guess.** The payload is unchanged, which is the point; see *The coach
  reads the diary with tools* below for why widening it was the wrong fix.
- **A question is only persisted once it is answered.** `CoachRepository.send()` writes both rows
  in one `@Transaction` when the reply lands, so `chat_message` needs no `pending` column and there
  are no half-conversations to reconcile after process death. A call killed by leaving the screen
  loses the un-sent question — the reading `FastingRepository.discardActive()` gives an unfinished
  fast: it never became history. A retry is therefore a fresh send, not a repair, which is why
  `CoachFailure` carries the question text. Clearing the chat is a soft delete like everything
  else, and it *asks first* — a conversation is user-authored, the saved-meal rule, not the
  diary's swipe-and-undo.
- **The coach streams, and Room is what ends the stream on screen.** `CoachRepository.send()`
  returns a cold `Flow<CoachReply>` over `Chat.sendMessageStream` rather than suspending on a whole
  answer: `MAX_OUTPUT_TOKENS` is 300, and a lone thinking mascot for those seconds is the longest
  wait in the app. Four things follow. **The pair of rows is still written once, on the last
  chunk** — the entry above is untouched, and a collection cancelled by leaving the screen simply
  never reaches the write. **There is no `Answered` variant**: the finished answer arrives the way
  every other row does, through `observeMessages()`, so flow completion *is* the success signal and
  only `Failed` is explicit. **A partial is sanitized cumulatively** — `sanitizeReply` takes the
  whole answer so far on every chunk, because the whole of it is what a bubble draws, and the trust
  boundary is still exactly one function; a partial past `MAX_REPLY_CHARS` sanitizes to null, the
  bubble stops growing, and the final check fails the send rather than truncating it. **And
  `pending`/`streaming` are retired by the Room emission, not by the stream ending** — `withMessages`
  clears them when the list size changes, because the write and the invalidation are not the same
  instant and clearing at completion blinks the finished turn off screen in between; a shrink counts
  too, so a `clear()` mid-send cannot strand the input bar. That pure function is the JVM test
  (`CoachUiStateTest`), the reason `sanitizeReply` is one. `CoachUiState.sending` became
  `pending: String?` in the same move: same boolean, plus the question to draw above the answer, so
  the turn assembles top-down instead of the question popping in over a finished reply.
- **The coach's model is rebuilt on every send; the insight's is a field.** Its system instruction
  carries the day's numbers, and those move while the screen is open — a glass logged in another
  tab must not leave it quoting a stale figure. A `GenerativeModel` is a config object, so this
  costs nothing. Nothing is cached either, unlike the insight's one line per day: every question is
  its own answer. `sanitizeReply` is the whole trust boundary and keeps line breaks where
  `sanitizeInsight` collapses them (an answer legitimately spans a short paragraph), and rejects
  past `MAX_REPLY_CHARS` rather than truncating, for the reason the insight cap gives.
- **The follow-up chips are derived from the day, not generated.** A finished answer was a dead
  end: the empty conversation gets four starters and every turn after it gets a blank field. The
  obvious build is a second model call per turn asking for the next questions — and it doubles the
  token cost of *every* question, adds a second AI call site with its own failure mode, and buys
  chips that a protein gap, a calorie gap, an unfinished water goal and a weigh-in already predict.
  So `followUpsFor()` is a pure rule over the same `InsightRequest` the screen already holds, which
  costs nothing, cannot fail, and is a JVM test. Three constraints it is written to. **Every chip
  is a question the coach can answer** — off the day payload, or through `get_day`/`get_history`;
  one that needs a tool the coach lacks buys a shrug that reads as a broken feature, which is why
  "how much water have I had this week?" is *not* there (a span carries calories, protein, training,
  sleep and weigh-ins — never water). **The day's own gaps come first and filler completes the
  row**, so it is the same height every turn and never degenerates into the starters with extra
  steps. **And the row is an item in the list, not a bar above the field**: it scrolls away like
  everything else the coach said, and it is hidden while a turn is in flight, because a row of new
  questions beside a half-written answer asks the user to abandon what they are reading. The tap
  sends the resolved string verbatim, the rule `CoachEmptyState` set — the question the user pressed
  is the question the coach is asked. If the rules ever visibly miss, the model call is one `intent`
  and the chips are already plumbed.
- **The mascot greeting card is the app's one door to the coach.** The insight card would be the
  more contextual tap and is the wrong one: it is hidden on day one, hidden when the model has
  nothing to say, and gone once dismissed, so a door on it is a door that isn't there most days.
  The `AIChip` under the greeting is what makes the tap visible. `CoachRoute` is a route above the
  Home tab rather than a fifth tab or a sheet — `AppScaffold`'s existing `isTopLevel` rule then
  gives it a back toolbar with no bottom bar and no FAB, which is exactly what a chat with a
  keyboard wants, and no new case was added there.
- **The coach's mic fills the field and stops.** It is the same system dialog talk-to-log uses
  (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`, no `RECORD_AUDIO`, no permission screen, no
  in-app `SpeechRecognizer`), and the manifest `<queries>` entry that makes
  `isRecognitionAvailable()` answer truthfully is already in `:app` and merges app-wide, so
  `:feature:coach` needed no manifest. Three calls beyond copying that flow. **It never
  auto-sends** — a send is a model call and a persisted pair of rows, and a misheard question
  would be spent before it could be read; the field stays editable, exactly as the voice-log
  screen's does. **It appends rather than replaces** (`withSpoken`), because a half-typed question
  is the user's — the reading `CoachFailure.question` already gives one that failed to send — and
  a trailing space is trimmed so speaking twice doesn't accumulate them. It is **absent, not
  disabled**, where no recognizer exists, Home's supplements-card rule. `speechAvailable` is
  hoisted as a defaulted parameter for one reason only: the preview renderer reports no
  recognizer, so without it no `@PreviewLightDark` could draw the control this entry is about.

- **The coach reads the diary with tools rather than being handed a bigger payload.** It was told
  one `InsightRequest` and a paragraph listing what it could not see — yesterday, any past week,
  individual meals, weight, exercise — so every question outside today's seven numbers was met with
  a deflection to another tab. Widening the payload was the cheap fix and the wrong one: the coach
  and the insight card share `InsightRequest` precisely so neither can describe the day the other
  doesn't, and a second coach-only payload would have ended that. So the payload is untouched and
  the coach got `get_day` and `get_history` instead, in `coach/CoachTools.kt`. Consequences worth
  keeping. **Two read tools, not five** — a question is nearly always about one day or one span,
  and one round trip beats four; a third domain (sleep, mood, fasting) is a branch in `runTool` and
  a line in `COACH_TOOLS`, which is why none are there up front. **Days are `days_ago`, never a
  date string** — a model handed a date format invents them (the wrong year, a timezone's
  yesterday, a 31st of February), while an offset needs no parsing and is one subtraction off
  `todayEpochDay()`; it is clamped silently, because a model asking for day 900 means "recently"
  and failing the turn over it helps nobody. **A tool answers in plain text, not JSON**, the call
  `sanitizeInsight` already makes, and `formatDay` deliberately echoes `dayNumbersBlock()` so a
  tool result and the day block cannot describe one day two ways. **An unlogged day is named, not
  dropped**: `observeDailyNutrition()` is a dense zero-filled series, so a silent omission would
  let the model average over days the user never opened the app. **And a weigh-in still leaves as a
  *change*, never as a weight** — `InsightRequest` has never sent an absolute figure, for the
  data-minimisation reason the 30-day health backfill is written against, and a tool is not a
  loophole in that rule just because the user asked the question out loud. A delta answers "is this
  going the right way?" in full, which is the whole of what anyone asks a coach about a trend; a
  model told the user weighs 94.2 kg is answering a different, unasked question. `weightDeltas()`
  reaches one entry *behind* the window so the oldest day in a span still carries a change rather
  than a shrug, and `CoachToolsTest` asserts no absolute figure appears in the text at all. And `MAX_TOOL_ROUNDS` is a flat 3
  — ponytail, not a token budget; price it if a tool ever fans out.
- **Sleep, mood and fasting widened what the two tools answer with; they did not become tools.**
  The entry below says a third domain is "a branch in `runTool` and a line in `COACH_TOOLS`" — that
  was the wrong half of its own argument. *Two read tools, not five* is justified there by "a
  question is nearly always about one day or about a span", and that is still true of *"did I sleep
  badly on the days I overate?"*: it is one span, asked once. So `formatDay` gained sleep, the mood
  check-in and a completed fast, `formatHistory` gained the day's training and its sleep, and
  `COACH_TOOLS` is unchanged — no third and fourth declaration for the model to pick wrong, and no
  extra round trip. The cost is a few dozen *input* tokens on a call that was already being made.
  Four things it turns on. **A day's whole training is one line**, not one per session: a week of
  two-a-days is fourteen lines of an answer with six to spend. **The lines of a span are counted
  off the window, not the nutrition series** — that series is dense and zero-filled *today*, but a
  day carrying only a workout must still get a line, and iterating the range makes that true of any
  series shape. **An untracked domain is omitted, never zero-filled** — the opposite call to
  `formatHistory`'s *unlogged days are named, not dropped*, and deliberately: food, water and
  activity are things the user does in this app, so a zero is a fact, while sleep comes off a watch
  and mood and fasting are opt-in surfaces, and a daily *"No sleep recorded"* has the coach nagging
  about a feature that is not switched on. The prompt carries the other half — *a category missing
  from a day is one the user does not track* — because omission alone is what a model fills in.
  **And a half-filled check-in reports the half that was filled**: `mood_day` stores 0 for "not
  set", never a zero score, which is `MoodDay`'s own rule reaching the model intact.
- **A tool round's preface is dropped, not carried into the answer.** `send()`'s `raw` builder
  accumulated across rounds and was never reset, so a model that said *"Let me check yesterday."*
  before calling `get_day` produced *"Let me check yesterday.You had 1,850 kcal…"* — unseparated,
  streamed that way, and written to Room that way; it also spent the 700-token output budget
  twice. The reset sits **below** the write-tool check, which is the whole subtlety: a *write*
  call's prose is the answer, and the copy `settle()` persists. Three things follow. A turn that
  spends every round reaching for tools now reaches `finish()` empty and fails honestly, rather
  than persisting *"let me look that up"* as the reply. An empty `CoachReply.Partial` is emitted
  with the reset and the ViewModel maps it to `streaming = null`, so the mascot returns to
  *Thinking* while the tool runs instead of the bubble jumping from preface to answer — empty is a
  real value on that type now, meaning *forget what I said*, and there is still no new variant.
  And the prompt gained the cheap half of the same fix — *do not narrate that you are about to
  look something up* — which saves tokens rather than spending them; the reset is what makes the
  behaviour correct whether or not the model obeys.
- **The coach's send button becomes a stop button, and a stopped turn costs nothing.** An answer
  takes seconds, a model can hang, and `pending != null` locks the input bar — so an unpressable
  spinner left *leaving the screen* as the only way out, which also lost the typed question. The
  shape is `PhotoCaptureViewModel`'s, held `Job` and all, because that flow had already answered
  this for the photo path. Nothing is persisted, which needed no new rule: the repository writes a
  question only once it has an answer, so a stopped turn is exactly a turn the user walked away
  from. The question goes back into the field — the reading `CoachFailure.question` already gives
  a send that failed — but only into an empty one, since the field stays editable while a turn
  runs and whatever is in it is newer. The clearing is its own `intent`, because a cancelled one
  cannot reduce, and it is `withTurnAbandoned()` rather than a second `copy`: the dismissal of a
  prose-less proposal is the same ending reached another way, and a field missed in one of the two
  strands the input bar. That pure function is the JVM test, the rule `withMessages` set.
- **The newest coach answer is a polite live region; the streaming one is not.** A finished reply
  arriving is the one thing on this screen a screen reader user would otherwise have to go looking
  for, so `ChatBubble` takes an `announce` flag and the screen sets it on the last non-user message
  only. Not on `StreamingBubble`, and not on every bubble: a live region over text that grows per
  chunk makes TalkBack restart the whole answer on every chunk, and marking them all re-announces
  the conversation.
- **The coach drafts a row; the user commits it. That narrows "talking to a coach is not logging"
  rather than repealing it.** `log_food` and `log_water` are declared to the model and *never
  executed*: a write call stops the stream, becomes a `CoachAction`, and nothing at all is
  persisted — not the row, not even the turn that drafted it. `CoachRepository.settle()` is what
  ends that turn, after the tap. So the coach still never writes; what it does is fill in the
  add-entry sheet's fields and hand them over, which is that sheet's confirm step reached through a
  different door. A read tool, by contrast, runs the instant it is asked for — it is a local Room
  query with no user-visible effect, and making the user approve a `SELECT` would be theatre.
  `parseAction()` is the whole trust boundary and it is **pure over `kotlinx.serialization` types**,
  which is the point: `FunctionCallPart.args` is a `Map<String, JsonElement>`, so unlike the photo
  path's `org.json` parse this one has a JVM test (`CoachToolsTest`), the same argument
  `sanitizeReply` won. It rejects rather than coerces — an absurd or negative figure, an unknown
  `MealType`, a name that arrived as a boolean — and its ceilings exist so a dropped decimal point
  cannot put 90,000 kcal in front of a Confirm button. Two things follow at the screen.
  `CoachUiState.proposal` is the **third** thing not in Room and is retired by exactly the
  predicate `pending` and `streaming` are, so a `clear()` mid-proposal cannot strand the card. And
  **the input bar stays locked while a card is up**: a second send would race the first turn's
  write, and `withMessages` retiring the bubbles on a list-size change would take the new question
  with it. The card carries both ways out, which is what a locked bar is for. `log_weight` is
  deliberately absent — kg/lb is a second trap for no new capability.
- **`log_exercise` is the third draft, and the one figure on its card is not the model's.**
  Everything else a proposal shows is the model's own output checked against a ceiling. A calorie
  burn is not: `estimateBurnedKcal()` is arithmetic this app already owns, the log-exercise sheet
  already prices a hand-logged workout with it, and a model asked for the number invents one — on
  the single surface whose promise is that *every figure shown is the figure written*. So
  `parseAction` stays pure and leaves `burnedKcal` at zero, and `priced()` fills it in from
  `CoachToolbox.weightKg()` — the latest weigh-in, else the onboarding weight, which is
  `:feature:training`'s own rule, so a coach-drafted run and a hand-logged one of the same length
  come out identical. It runs at the *proposal*, not at `settle`, because the card has to show what
  the tap will write; null fails the turn exactly as a rejected parse does, since a default body
  weight is a made-up figure and the draft is better refused. The prompt says the same thing to the
  model in one clause — *do not estimate the calories an activity burned*. Nothing else in the loop
  changed: `log_exercise` joins `WRITE_TOOLS` and the existing stream-stops-and-waits path carries
  it. Minutes are capped at 600, the dropped decimal at the other end of the same card from
  `MAX_ACTION_CALORIES`. A name is optional, because an empty one is what `ExerciseEntry` already
  means by "call it by its type" — unlike a nameless food, which would have nothing to show.
  `log_mood` and `log_supplement` stayed out: the energy check-in owns a 1–5 tap and does it better
  than a sentence can, and a supplement needs fuzzy name-to-id matching against the user's own list,
  which is a new trust boundary for one tap. `log_weight` is still out, for the reason below.
- **The coach is the first call site to leave `AI_THINKING`, and it moved both halves of the
  budget.** `ThinkingLevel.LOW` and `maxOutputTokens` 300 → 700, together, in
  `CoachRepositoryImpl`. That constant's own entry names this case and its condition — *"If a call
  site ever genuinely needs to reason, it raises the level **and** `maxOutputTokens` together —
  they are one budget"* — and choosing a tool and filling in its arguments is the first thing this
  app does that genuinely reasons; the alternative is a model guessing `days_ago` or inventing a
  calorie count. The shared `MINIMAL` is unchanged and still right for the other four, which
  flatten a photo into JSON and write a line of encouragement. `LOW`, not `MEDIUM`: this is picking
  one of four functions. The reply cap moved with it (`MAX_REPLY_CHARS` 900 → 1400) because the
  prompt now permits up to six short lines where a list genuinely answers better — `sanitizeReply`
  already preserved line breaks and `Text` already renders them, so no component changed, but a cap
  that rejects the format the prompt asks for is a cap that fails every list.
- **A tool response goes back under the `"user"` role, not `"function"`.** `firebase-ai` 17.17.0's
  `Chat.assertComesFromUser` accepts only `"user"`, and logs *"The 'function' role is deprecated
  and will be removed in a future release"* for the role every function-calling tutorial still
  shows. Worth writing down because it is invisible until runtime and the fix is one string.
- **`MealType` carries its own `labelRes` now, in `:core:data`.** It lived as an `internal`
  `labelRes()` in `:feature:food/ui/shared/`, which was right while one feature drew a meal name.
  The coach's proposal card is the second, `:feature:*` modules never import each other, and
  `:core:data`'s `strings.xml` exists precisely so a label is not "copied into every feature that
  shows a chip or a pill" — its own comment. So it moved onto the enum, the shape `ExerciseType`
  and `MoodLevel` already have, and the nine call sites lost a pair of parentheses. The `name` is
  untouched, as always: that is what the Room row, the export and the Google Health push carry.
- **The coach is not exported, not a streak domain, has no reminder and no widget surface.** The
  backup file is a record of what the user *did*; a conversation about one day's numbers has no
  meaning restored on another device — `health_link`'s reasoning. And talking to a coach is not
  logging — still true once the coach could draft a row, because *drafting* is not writing: see
  *The coach drafts a row; the user commits it* above for where that line moved to.
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
- **One model name for five call sites, and every swallowed AI exception gets one log line.**
  `AI_MODEL_NAME` and `logAiFailure()` sit together in `:core:data/Ai.kt`, because they are two
  halves of the same failure. A Gemini model is a wasting asset — Google publishes shutdown dates
  and a retired name answers 404, not a deprecation warning — and all five AI repositories had
  their own `private const val MODEL_NAME`, so the name went stale five times at once. Worse, each
  one caught `(_: Exception)` and returned its graceful fallback, which is the right behaviour and
  also means a dead model, an App Check refusal, a disabled Anonymous provider and airplane mode
  are indistinguishable in logcat: the app just goes quiet. That is how `gemini-1.5-flash` outlived
  its shutdown here. The fallbacks stay exactly as they were — this adds a bound exception and a
  `Log.w` above each, nothing else. Remote Config is the upgrade path if the name needs changing
  without a release; one constant is enough while a release is cheap.
- **Thinking is off at all five call sites, and that is a property of the model, not of the coach.**
  `AI_THINKING` sits beside `AI_MODEL_NAME` in `:core:data/Ai.kt` for the identical reason: it
  describes the model, so five copies would go stale together. Gemini 2.5 and newer reason before
  answering unless told not to, and **thinking tokens are spent from `maxOutputTokens`** — a cap
  every caller here sized for the answer alone (60 for a one-line insight, 300 for the coach's few
  sentences). A default dynamic budget therefore spends the whole allowance reasoning and the
  response comes back finished for `MAX_TOKENS` holding nothing, which `APIController.validate()`
  turns into a `ResponseStoppedException` — it throws on *any* finish reason but `STOP`, so a
  half-written answer is discarded exactly like an empty one and each call site's `catch` renders
  the graceful fallback. The coach surfaced it first because its answer is the longest; the insight
  was nearest to going next, and the photo path has no cap at all and so was riding the model
  default.
  The fix is one setting, not five raised ceilings: raising `maxOutputTokens` pays for reasoning
  this app never asked for on tasks that do not reason — a line of encouragement, a sentence of
  coaching, a photo flattened into twelve JSON fields. `ThinkingLevel.MINIMAL` rather than
  `thinkingBudget = 0` because the zero budget is the 2.5-series idiom and the 3.x models take a
  level instead, where `MINIMAL` is the floor; `ThinkingConfig.Builder` rejects both at once.
  If a call site ever genuinely needs to reason, it raises the level *and* `maxOutputTokens`
  together — they are one budget, and that is the whole lesson here.
- **A debug build calls no model at all, and the fakes live in a source set rather than behind a
  flag.** Every debug run was billing real Gemini usage across five repositories, and two of them
  fire without being asked — the daily insight on every Home session, and the coach now spending up
  to three tool rounds at 700 output tokens per question. `core/data/src/{debug,release}/…/DebugAi.kt`
  is a pair in the shape `app/src/{debug,release}/…/DebugSeed.kt` already has: five
  `debugX(): T?` functions that are all `null` in release, so every binding reads
  `single<T> { debugX() ?: RealImpl() }` and a release build **cannot contain** a fake rather than
  merely never reaching one. A `BuildConfig.DEBUG` branch in `main` would have left five fake
  repositories sitting beside the real ones trusting R8 to notice; this way `assembleRelease` fails
  to compile if the two halves ever drift, and the release dex is checkably empty of them. One
  `USE_REAL_AI` const covers all five, because the reason to flip is always the same — checking
  against the real thing before a release — and five booleans is five ways to leave one on.
  Koin's `single {}` being lazy is what makes it airtight: the real `…Impl` is never constructed,
  and each one builds its `Firebase.ai(…)` model as a constructor field, so nothing is even created.
- **The fakes answer from local data that already ships, and that is the design, not an economy.**
  A stub returning null everywhere would cost the same and hide exactly what a debug build exists
  to show — a streamed answer that scrolls badly, a proposal card with an absurd figure on it, a
  parse that finds nothing. So `FakeInsightRepository` *is* `insightFor(request)`, the rule-based
  line Home already falls back to; the meal-idea fake applies `localMealIdeas`' own rule to
  `COMMON_FOODS`; the parse fake runs `searchCommonFoods` over the sentence and ends on the real
  `loggable()`. `FakeCoachRepository` is `CoachRepository by real` — only `send` is replaced, so
  `observeMessages`, `clear` and the Room write are the shipping code, reached through the public
  `settle(question, answer, null)` path, and it holds *the same* `CoachToolbox` as the real
  repository, so "what did I eat yesterday?" answers off the real diary. What is faked is the model
  and nothing else: the tool loop, `parseAction`, `sanitizeReply` and every write are real either
  way. The toolbox moved into `coachDataModule` to make that sharing possible, which also dropped
  three forwarded constructor params from `CoachRepositoryImpl`.
  Two things worth knowing before trusting a faked run. The coach's routing checks "log" **before**
  "yesterday", because a real model handed *"log the eggs I had yesterday"* drafts a row rather
  than reading a day — `FakeCoachScriptTest` pins that ordering, and it is the thing a rewrite gets
  backwards. And `commonFoodFor` singularises on a miss: `COMMON_FOODS` is written singular while
  people say "eggs", so a literal `searchCommonFoods` found nothing for the most common sentence
  either fake will see. The fakes also carry a deliberate `delay` — a call that returns instantly
  hides every spinner, and looking at them is the point.
  Nothing here needs a `checkUiLiterals` exception: the task walks
  `localizedModules.map { file("$it/src/main") }`, so a debug source set is outside its scope by
  construction — scaffolding that never ships is never translated. And `DebugAi.kt` logs once under
  `logAiFailure`'s own `FitPulseAI` tag, because every AI call site here swallows its exception and
  degrades gracefully, so a real call and a faked one look identical on screen; `logcat -s
  FitPulseAI` is the answer to "is this build costing me anything?" rather than the bill.
- **There is no Firebase Authentication in this app, and adding it back will not fix an AI call.**
  App Check is the only thing the Firebase AI Logic backend gates on. `firebase-ai`'s
  `AppCheckHeaderProvider.generateHeaders()` treats the auth provider as strictly optional and its
  bytecode says so twice: with no provider registered it logs `Auth not registered, skipping`,
  omits the `Authorization` header and returns the headers; with one registered but the token
  fetch failing — which is exactly what `FirebaseNoSignedInUserException` is — the whole block
  sits under a `catch (Exception)` that logs `Error getting Auth token` and returns the same map.
  It never rethrows. So an anonymous sign-in bought a network round-trip on every AI call whose
  failure the SDK already tolerated, and `ensureAuth()`, `signInAnonymously()` and the
  `firebase-auth` dependency are all gone.
  Checked against `firebase-ai` **17.17.0** specifically (what BOM 34.19.0 resolves to) — the
  branch and the catch are that version's bytecode, not a documented guarantee, so re-check on a
  major SDK bump rather than assuming. The `FirebaseNoSignedInUserException` that 506ae9a named
  was reachable: it lives in `com.google.firebase.internal.api`, shipped by
  `firebase-auth-interop`, which `firebase-ai` pulls transitively and always did — so it predates
  that commit's `firebase-auth` and an older `firebase-ai` plausibly did throw it. 17.17.0 does
  not, from either branch. What that commit *also* did, and what stays, is
  `setTokenAutoRefreshEnabled(true)` in both `AppCheckInitializer` variants — a stale App Check
  token is what the backend actually refuses, so that is the load-bearing half of the diff.
  The `useAppLanguage()` call went with the rest: the
  `Ignoring header X-Firebase-Locale because its value was null` log it silenced comes from
  `firebase-auth`'s own GMS plumbing, which is no longer here to emit it.

### Training, strength & routines

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
  import the feature that owns that route (`:feature:training`), the shape `onOpenCoach` already
  has. A started routine seeds through
  `toSets()`, the *same* path the strength screen's own chips take, so an opened-from-Home workout
  and a chip-tapped one are the same workout. Routines are still not exported, so `days` isn't
  either.
- **Training got a module, not a tab — and the screen that was built for it was deleted.** The
  domain was already whole in `:core:data/exercise/` and its UI sat in `:feature:food` for an
  accident of arithmetic: burn credits the calorie budget, so logging grew inside the diary. Fixing
  *that* is what `:feature:training` is — it owns the log-exercise sheet and the strength screen,
  and nothing else moved. A `Train` **tab** was built on top and then removed, because every block
  on it already shipped somewhere better: today's plan is Home's Workout card, today's sessions are
  the diary's exercise block (which also deletes and edits them), "Log activity" is the FAB sheet's
  own row, and the history with its charts is Progress. The one thing it had that nothing else does
  was a one-tap door to the strength screen — a row in a sheet, not a screen, and not yet worth
  adding. *A pillar earns a module when its code has no home; it earns a tab only when it has a
  surface no other tab is already drawing.*
- **The rest timer is screen state, not a domain.** No table, no repository, no ViewModel field: a
  rest is not part of the workout, so it is saved with nothing, makes nothing dirty, and travels in
  two `rememberSaveable` primitives beside the set draft that already lives there — the length and
  a wall-clock end time. **Wall-clock, not a tick count**, so a rotation or a trip to another app
  resumes on the time that actually remains; and `delay` rather than a frame clock, because frames
  stop when the app does and this has to keep counting with the phone face-down on the bench.
  **"Add set" is the only thing that starts one**, `commit()` being the one place a set lands, and
  **Off is a real choice** rather than a missing one — a lifter who counts their own rest should not
  dismiss a card every set. The cue at zero is a haptic: the app's first, and it costs no
  permission, no manifest entry and no notification channel, which a buzzer or an alarm would.
  The chosen length is **not persisted** — `rememberSaveable` survives the rotation it needs to and
  a `Profile` column would have cost a Room version and an export schema bump for a knob with four
  settings. *ponytail: the buzz only reaches a living process — a killed app loses the rest. An
  AlarmManager-backed timer with a notification is the upgrade if that starts to matter, and a
  `Profile.restSeconds` is the one if the pick turns out to be worth keeping between sessions.*
- **The exercise UI left `:feature:food`; `ExerciseSection` did not.** The diary's exercise block
  draws `MealSectionHeader`, `SectionCorner`, `EntryIndent` and `SwipeToDeleteRow` — four things
  that are the diary's — and it exists because burned calories raise `budgetKcal()`, which is the
  diary's arithmetic. So it moved one directory, to `ui/diary/components/`, where it plainly
  belonged all along, and deletion stayed with it.
- **The log-exercise sheet takes an id, not a row — and `AppScaffold` hosts the only copy.** A
  feature never imports another feature's types, so once the sheet moved out of `:feature:food` the
  diary could not host it. `AppScaffold` already hosted one for the FAB, and its sheet state is
  `rememberSaveable`, which an `ExerciseEntry` is not — hence `(dateEpochDay, editingId)`, two Longs
  beside the enum. The ViewModel resolves the row with the `exerciseRepository.entry(id)` call it
  already made for the strength screen, and the sheet holds its form back until `uiState.editing`
  names *that* id: one check doing two jobs — the hold-back (a row arriving an emission later would
  re-key the saveable form under the user, `strengthLoaded`'s reason) and the staleness guard (the
  ViewModel outlives the sheet, so a previous edit's row is still on the state when the FAB opens a
  blank one). Net: one sheet host instead of two, and `FoodScreenState`'s saver two slots shorter.

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

### Meal ideas & talk-to-log

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

### Blood pressure, BMI & measurements

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
- **BMI and waist-to-height are derived on read, and neither earned a subject card.** Both are
  ratios over figures already on screen — the profile's height against the newest weigh-in, and
  against the newest waist reading. So there is no table, no column, no export field and no
  migration: `BodyIndex.kt` is four declarations in `:core:data/progress/`, folded the way
  `withMovingAverage()` and the streak are. A fifteenth Progress subject would have bought a grid
  tile, a detail page, a range toggle and a chart to restate two numbers, so BMI is a third fact
  chip on the Weight page and waist-to-height is two stat rows above the measurement list — each
  beside the input it is derived from.
- **`bmiCategoryOf` is `BloodPressureCategory`'s rule applied a second time: the band is named and
  nothing else is said.** 24.1 means nothing to most people without the word next to it, and that
  word is the whole of the claim — no advice copy, no colour. Unlike the AHA bands there is not
  even a `severe`: a band is not a direction, so the chip carries `Neutral` and no arrow, which is
  what `FactChip`'s "never a trend without an arrow" already required. Each boundary lands in the
  **higher** band (25.0 is Overweight, never Healthy) and `BodyIndexTest` pins all three, because
  an off-by-one there inverts a label silently.
- **Waist-to-height reports the boundary and refuses the band.** One published number — keep your
  waist under half your height — is a fact the app can stand behind; the four-step scale derived
  from it would be the app grading a body, which is the line the cycle tab holds when it refuses to
  derive a fertile window. Two stat rows rather than one, because the ratio means nothing without
  the boundary beside it and `StatRow` carries a label and a value, not a caption. The card is gone
  entirely until both inputs exist: a ratio with a side missing is not a reading nobody has taken,
  it is a ratio that does not exist, so there is no dash state.
- **Neither figure converts a unit.** kg/m² and waist÷height are the same numbers in pounds and
  inches, so the display toggle has nothing to say about either, and both take stored kg/cm rather
  than what the screen happens to be showing. `BodyIndexTest` asserts it, because the obvious
  "fix" is to reach for `kgToDisplayUnit` on the way in and quietly report a BMI of 10.
- **Body fat is a sixth `MeasurementPart`, not a fifteenth subject.** `measurement_entry` is keyed
  `(part, date)` with the part as a plain string, so a new enum value writes rows of its own with
  **no migration** — and the sheet, the row, the sparkline, the delta rule and the overview card
  are all already built for "a number with a history per part". A subject of its own would have
  bought a grid tile, a detail page, a range toggle and a chart to draw one figure that changes
  every few weeks, which is the same argument that kept BMI off the grid.
- **The percentage rides `valueCm`, and the part is what says so.** Renaming the column is a
  migration and renaming the export key is a schema version; neither is bought by a better name,
  so the column stays and the *domain* field became `MeasurementEntry.value` — a compiler-enforced
  rename that stops the next reader reaching for `cmToDisplayUnit` on a percentage and printing
  18.5% as 7.3 in. Every unit decision now lives on the enum (`toDisplay`/`fromDisplay`/
  `unitLabel`/`defaultValue`/`range`), so `MeasurementRow` takes display values and a resolved
  label and converts nothing at all. Two kinds, not six constants: a percent or a length.
  `SubjectSummaryTest` asserts the card under *both* unit systems, because that conversion is the
  one that would ship silently.
- **`EXPORT_SCHEMA_VERSION` 19 adds no field.** `enumOf` throws on a name it doesn't know rather
  than falling back, so a v18 build handed a file with a `BodyFat` row would fail the whole
  all-or-nothing import on "Unrecognized MeasurementPart". The bump is what turns that into the
  version gate's own "written by a newer version of FitPulse".
- **Fat mass and lean mass are `BodyIndex.kt`'s third and fourth figures, derived on read** — no
  column, no export field, gone entirely until a body fat reading and a weigh-in both exist, and
  no dash state, exactly like the waist-to-height card above them. The one departure from their
  neighbours: **these convert.** A ratio is the same number in pounds as in kilos; a mass is not.
  They still take and return stored kg and the screen applies `kgToDisplayUnit`, and `BodyIndexTest`
  pins that, because the obvious "fix" is to convert on the way in and report a lean mass of 144 to
  somebody who weighs 82. No band and no target beside them: there is no single published healthy
  body-fat range to name the way `bmiCategoryOf` names the WHO bands, and inventing one would be
  the app grading a body.
- **The part chips wrap.** Six do not fit one phone-width line, and the day nothing is tracked yet
  is the day all six are offered — so the row is a `FlowRow`, the same call `MascotColourSheet`
  makes inside a sheet. Tapping a chip also re-seeds the figure (that day's reading for the new
  part, or its opening figure), because carrying 80 over from a waist onto body fat is absurd and
  the old sheet kept the value across the switch.

- **Blood pressure has a Google Health scope, and it is deliberately not requested.** The four the
  app already asks for cap it at 100 users pending OAuth verification and a CASA assessment; a
  fifth would need its own justification on that form. Manual entry only, like measurements — and
  not a streak domain, for the reason mood, sleep, fasting and supplements aren't.
- **Home shows the *latest* reading, not today's.** The three watch cards are hidden when today has
  no row because a watch fills them in nightly; nobody takes their blood pressure daily, so a card
  that vanished on the six days between readings would be a card nobody ever saw. It is hidden only
  until the first reading exists, like `SupplementsCard`. It is read-only and does not navigate:
  logging needs the sheet, and the sheet lives in `:feature:progress`.
- **Cycle tracking is off until it is switched on, and off means *gone*.** `Profile.cycleTrackingOn`
  is nullable like `darkThemeOn` and read the opposite way — there is no device setting to follow,
  so null means "never asked", which resolves to off. It is the **one thing in the app that can take
  a subject out of the Progress grid** rather than dashing it: every other empty subject is empty
  for want of data, while this one may be permanently irrelevant to whoever is holding the phone,
  and a card that can never say anything is worse than no card. So `subjectsIn(group, cycleTracking)`
  takes the flag, and both the grid *and* the detail page's sibling switcher read it — a page
  offering a door to a subject the overview has removed is the failure that pairing prevents. The
  Home card is gated the same way, inside its own `when` branch like every other data gate there.

### Cycle

- **No fertile window, no ovulation date, ever.** This app names things and reports numbers; it does
  not advise — and a fertile window derived from a mean cycle length is a contraception claim it
  cannot stand behind. `cyclePrediction()` reports one thing: the next start, the average it was
  fitted over, and how many cycles that was. It is **null until there are two period starts**, the
  refusal `Recap.weightArcKg` makes for a window holding one weigh-in, and a date that has passed is
  reported as *late* rather than hidden — a late period is the thing someone opens this card to
  check. `PREDICTION_WINDOW_CYCLES` is 6: enough to absorb one odd month without averaging in a
  year-old cycle the body has moved on from.
- **A period is a run of flow days, and a cycle is the gap between two runs — both derived, no
  table.** `cycle_day` is `mood_day`'s shape one domain over (`goalProjection()`'s and `streak/`'s
  rule), and `flow == 0` means "not logged", never a flow of zero — which is what keeps the column
  non-null, makes a symptom-only day a first-class row, and makes clearing a tap an update rather
  than a delete. `periods()` bridges `PERIOD_GAP_DAYS` (1) missed days, because real logging skips a
  day and a period split in two corrupts every cycle length after it — one bad tap would move the
  prediction by a fortnight. A **symptom without flow is not a period day**: a cramp three days
  early is not bleeding, and counting it would move the start every cycle length is anchored on.
  `cycleAverages()` leaves a **still-running** period out of the period-length mean (it is measured
  short by however many days are left in it) and keeps a separate denominator per series, `MoodDay`'s
  rule. `List<CycleDay>.inRange` anchors to **today**, like mood's and unlike weight's.
- **`FlowLevel.Unstated` is the importer's alone, and never a tap.** Health Connect's
  `MenstruationPeriodRecord` is a span of days with no intensity on it, so an imported day carries
  a level that fills nothing and says so — writing "Medium" would invent a figure the source never
  reported, the refusal `pulseBpm = 0` already makes. It appears in the sheet's pill row *only* when
  that day already says it, so nothing can set it by hand. `TAPPABLE_FLOW` is the three levels the
  UI offers, and the Home row is a **meter** (fill up to the level) rather than mood's picker,
  because light/medium/heavy is one scale; tapping the level already set clears it.
- **Menstruation is the one Health Connect permission that is not always requested.**
  `HealthSyncRepository.connectPermissions()` drops it while the switch is off — it lives on the
  repository because that is where the profile already is, and suspends rather than caching so
  turning the switch on and tapping Allow asks for the type in the same session. The panel lists the
  rows it would request *plus anything already granted*, so it never shows a row nobody can tick and
  never hides a permission still held. `cloudDataTypeOf` returns null for it (blood pressure's
  answer): there is no Google Health menstruation scope, and nothing here would justify one on the
  verification form. One record is one `health_link` row, so it rides the ordinary
  `MAX(remoteTimeMillis)` cursor rather than steps' and heart's `MAX(date)`; `importDays()` **skips
  a day that already carries a flow** — the typed value wins, exactly as an imported weigh-in skips
  a day already weighed by hand — and a period whose days were all typed by hand records no link and
  is not counted as imported.
- **Cycle is not a streak domain, and reaches nothing off the phone.** The streak's four domains are
  things the user *did*; a body noticing itself is not one, and folding a fifth in now would rewrite
  what every past run meant — so `CycleRepository` has no `observeLoggedDays()` and Home's
  `isDayOne` ignores it. It is in **no AI payload** (not `InsightRequest`, not `MealIdeaRequest`, not
  the coach), on no widget, on no watch surface and in no recap, and the Profile switch's sublabel
  says so, because that is the first question this feature raises. It *is* **exported** (schema
  v17, with the switch): a cycle day is history like a mood day, not convenience data like a saved
  meal. Its symptoms travel as the comma-joined string Room stores, so a tag a later build retires
  degrades on import rather than throwing — `homeCardLayout()`'s rule.
- **The Progress sheet asks for a date; the Home card does not.** A period is remembered in the
  evening as often as it is logged in the morning, and back-filling three days is the workflow —
  the reason the weigh-in sheet has a calendar and the blood-pressure sheet doesn't. The calendar
  marks **flow days only**: a marked symptom-only day would say the period ran longer than it did,
  which is the figure every cycle length rests on. Saving a blank day *clears* it (a zero row, never
  a delete), and the sheet says so under the chips. Home stays today-only and read-only beyond the
  one tap — the history and the symptoms belong where a date picker can live.

### Progress photos & timelapse

- **A timelapse is a way of looking at the grid, not a thing to store.** No schema and no table:
  the player reads the photos the repository already returns, ascending by date, and derives
  everything else — the `badgeGroups()` and `goalProjection()` shape. It used to read them
  second-hand off `ProgressUiState` and sit in `ui/photo/components/`; it is now `ui/timelapse/`
  with its own container (see **Three overlays became flows** below), which changes where the list
  comes from and nothing about what it means. Playback **loops** rather than stopping at the end, since
  stopping would need a restart control for a gesture the loop gives away free, and scrubbing
  pauses it — a slider that kept advancing under the finger fights whoever is looking for one
  particular week. The Photos tab offers it at **two** photos, the same floor the comparison slider
  has: one control appearing without the other reads as a bug.
- **One share sheet serves every photo share.** A before/after was taken to *be* a two-frame strip
  (superseded by the entry below it), so the comparison slider handed `SharePhotoStripSheet` its
  two photos and the timelapse handed it the whole set; the Photos page's own header share is the
  third caller. `sampleFrames()` spreads up to four evenly with the first and last always in, because a
  strip whose ends aren't the start and the end of the run isn't the story being told. The capture
  itself (`Modifier.captureToPicture` + `sharePng`) moved to `ui/shared/` when the second caller
  arrived — `ShareRecapSheet` had owned it — so the two images can't drift apart in how they reach
  the chooser, and both still render exactly the PNG that leaves the app. Photo shares stay
  PNG-only: an MP4 needs either a new media3 dependency or an EGL renderer, since a `MediaCodec`
  input surface can't be `lockCanvas`'d at minSdk 24. *ponytail: media3-transformer is the upgrade
  path if a video is ever asked for.*
- **A before/after shares as a strip spanning the pair, not as two frames.** This supersedes the
  rule directly above it. A two-frame share is two portraits side by side — a thin picture and a
  thinner story, since the whole claim of a progress photo is that the change was gradual rather
  than a trick of one day's light. `sampleBetween()` keeps the picked pair as the ends and fills
  the middle from whatever was logged nearest the thirds of the interval, **by date rather than by
  index**, so an unevenly photographed month doesn't hand both middle frames to its busiest week.
  Nothing is padded: a pair with nothing logged between them still shares as two.
- **The Photos page's header share is the strip, not the weekly recap** — the one subject page
  whose `DetailHeader` share differs from the other thirteen. A page that *is* a set of images has
  an obvious thing to send, and routing it to a nutrition-and-weight recap would be the header
  disagreeing with the page under it. It sits in `ProgressScreenState` as `activePhotoShare`
  beside the overlays, and the header falls back to the recap everywhere else.
- **The timelapse scrubber is a date timeline, not a frame index.** Each shot's tick sits where it
  was actually taken within the run (`tickFractions()`), and a scrub snaps to the nearest shot by
  that position (`nearestFrame()`). An index slider draws a fortnight of daily photos and the month
  of nothing after it as equal steps, which quietly tells the reader the run was evenly paced — the
  one claim a progress timelapse should never make on its own. The ticks are **drawn**, not
  composed: one node per photo would be sixty layout nodes redrawn eight times a second. Past
  twenty-four shots they thin from 2dp to 1dp and read as density rather than as countable marks,
  and the playhead carries a ring in the stage colour because a `primary` circle sitting on its own
  `primary` fill has nothing to follow at speed.
- **Frames crossfade hold-under, never cross-dissolve.** The outgoing shot stays at full opacity
  underneath while the incoming one fades in above it, so the stage colour never shows between the
  two — dissolving both through the background is what makes a player strobe, and at eight frames a
  second that reads as the photos flickering rather than as one becoming the next. The fade is
  220 / 150 / 80 ms for 2 / 4 / 8 fps, always inside its own frame interval (`TimelapseTimelineTest`
  holds that), linear, and the layer underneath is dropped once it is covered.
- **Both photo screens draw on one stage, and the only floating control is Share.** `PhotoOverlayStage`
  owns the `surfaceContainerHighest` ground and the single Share pill for the comparison and the
  player alike. The bottom row that held Share and Close side by side went first: it gave equal
  weight to the action that publishes and the one that leaves, and a full-screen viewer whose way
  out is the last item in a column reads as a form with a Cancel on it. The corner close button that
  replaced it went when both became routes — see the routes entry below — and the stage's
  `DockedFabContentPadding` went with it, since there is no longer a FAB to clear. The photo
  frames are full-bleed and square-cornered — corners belong to tiles and cards, not to a media
  surface that reaches both edges.
- **The comparison headline is the delta, coloured by `goalRelativeTrend`.** It is what the two
  photos are being read for, so it is the largest thing on the screen; a kilo gained is the point
  for someone building and the opposite for someone cutting, and the existing tested function —
  not a sign test — decides which. Maintain stays Neutral there rather than taking the handoff's
  ±0.5 kg on-track band: that threshold is exactly the "how much drift is too much" question
  `WeightTrend.kt`'s KDoc says has no defined answer. A pair missing a weight at either end keeps
  the span as its headline rather than losing the answer entirely.
- **The grid teaches its own gesture.** Tapping two tiles opens a comparison and nothing about a
  grid of photos says so, so the hint bar states the rule, then names what the second tap will do
  and offers the way back out of a half-made selection. Two states, not three: at two picks the
  comparison is already on screen, so the third would only ever be read on its way out. The tile
  badge is the same component the hint draws, because the numeral is the half the comparison
  actually depends on — the border alone says *that* a tile is picked, not which end of the pair
  it is. Tiles are 3:4 rather than square: a square centre-crop of a portrait loses the head and
  the feet first, which are the two things a body record is read for.
- **Every share sheet can save its picture, not only send it.** `savePng()` sits beside `sharePng()`
  in `:core:designsystem`, so the recap, the diary day card and the photo strip all gained the
  action at once. API 29+ writes into MediaStore's own `Pictures/FitPulse` with `IS_PENDING` held
  until the bytes land and asks for nothing; below 29 there is no scoped write, which is the whole
  reason `WRITE_EXTERNAL_STORAGE` exists in that module's manifest at `maxSdkVersion="28"`. It
  returns false rather than throwing — a full disk, a revoked permission and a refused insert all
  look the same from there, and none is worth taking the sheet down over.
- **`rememberBitmapFromFile` downsamples, and every progress photo in the app goes through it.**
  `inSampleSize` against the width the caller actually draws into (`GRID_TILE_PX` for a grid cell,
  `FULL_FRAME_PX` otherwise) — a grid holding a year of camera JPEGs was keeping every one at
  capture resolution, and a timelapse cycling them would have finished the job. The player holds
  the last decoded frame across a swap: the function re-keys on the path and reports null while the
  next decode is in flight, which at eight frames a second would strobe the frame to empty.
  *ponytail: no bitmap cache — one downsampled decode per frame off the IO dispatcher; an LRU is
  the upgrade if the fast speed stutters.*
- **Both photo overlays take back themselves.** *(Superseded by the entry below — all three are
  routes now, and Nav3 takes their back.)* `PhotoComparisonScreen` and `TimelapseScreen` were
  full-screen overlays inside the Progress tab, not routes, so each wired its own
  `NavigationBackHandler` — without one, back out of a comparison left the Progress tab entirely
  rather than clearing the selection. A flow package did not turn either into a route: `ProgressScreen`
  still drew them over `ProgressContent`, and `ProgressScreenState` still owned what opened them.
- **The three read-only surfaces became routes, and the chrome is the reason.** An overlay drawn
  inside the `ProgressRoute` entry renders inside the `Scaffold` that draws the bottom bar and the
  docked FAB, so a comparison slider was inset by the nav bar with a FAB floating over its corner —
  tab chrome on top of a full-screen viewer, which is the one place it has nothing to offer.
  `PhotoComparisonRoute`, `TimelapseRoute` and `RecapRoute` sit in `ProgressNavigation.kt` beside
  `progressEntries`, and `showsTabChrome` needed **no change at all**: a route that is neither a tab
  nor a Profile detail already wears nothing, at every width. Three things fall out for free — the
  three hand-wired `NavigationBackHandler`s are gone, each container now dies with its entry instead
  of living as long as the tab, and the recap notification's `openRecapRequest` round trip through
  `progressEntries` collapses into `addTopLevel(Progress)` + `add(RecapRoute)`. What it costs is the
  corner close control: a route wears `AppTopBar`, and a floating × an inch under a back arrow is
  two ways out of one screen, so `PhotoOverlayStage` draws only the Share pill now. The argument
  that keeps the **subject pages** swap-ins is untouched and still load-bearing — a subject page
  would clone `ProgressViewModel`'s twelve repositories, where these three already have containers
  of their own.
- **The Photos page is the fourteenth subject and the only one that is a route.** It was
  `PhotosDetailBody`, a `*DetailBody` in the *add-photo sheet's* `components/` folder, reading a
  slice of `ProgressUiState` and keeping its selection in another screen's state holder. Three
  things made it the odd one out long before this: the other thirteen subjects are charts and stat
  rows, this one is a grid whose whole job is to launch two routes; it had already been pushed out
  of `SubjectDetail`'s scrolling column into `SelfScrolling`, because a `LazyVerticalGrid` cannot
  nest in a `verticalScroll`; and the set it draws is the same set `ComparisonViewModel` and
  `TimelapseViewModel` each observe for themselves, so `PhotosViewModel` is the third of a triplet
  rather than a new idea. So `ui/photo/` is the page now — `PhotosScreen` with the full quartet and
  the grid, hint and header strip under it — and the add-photo sheet moved to `ui/addphoto/`.
  `ProgressScreenState.open()` carries the whole switch: `Subject.Photos` sets `pendingRoute`
  instead of `selectedSubject`, and the overview card, the sibling switcher and the empty-card hint
  all funnel through it already. **What it costs is the two-pane page at ≥840dp** — Photos no longer
  draws beside the overview, it opens over it. That is the right trade for a photo grid, which wants
  the full width more than it wants a list it just came from; a chart does not, which is why the
  other thirteen stay swap-ins.
- **`AppTopBar` grew an `actions` slot, and the Photos page draws its own bar.** The page's share is
  the PNG strip of its own photo set, and `AppScaffold` builds that toolbar from a `NavKey` and
  nothing else — it cannot reach the set. Rather than hoisting the photos up to the scaffold or
  re-introducing the request-flag round trip the recap notification just lost, the screen draws the
  bar and `AppScaffold` stands down: `ownsTopBar = fullBleed || current is PhotosRoute`. It is
  deliberately a second boolean rather than a wider `fullBleed`, because the two want opposite
  insets — the camera flows draw under the system bars, the Photos page clears them.
  `ScanConfirmationScreen` was already the precedent for a feature drawing this bar. The slot itself
  stays empty by default, so the sentence its KDoc used to open with — "a title and a back arrow,
  nothing else" — is now a statement about what `AppScaffold` passes, not about what the bar can do.
- **A tap inside the tab reaches the navigator through one field, not three callbacks.** Every open
  site is a `state::openX` reference threaded through the overview, `SubjectDetail`'s fourteen-way
  dispatch and the photo grid, so routing them as parameters meant about twelve new arguments across
  seven files to carry three lambdas. `ProgressScreenState.pendingRoute` records which surface was
  asked for and `ProgressScreen` — the one composable that holds the callbacks — consumes it and
  nulls it. It is deliberately **not** in the saver: after a process death the back stack has already
  restored whichever route was open, and a surviving request would push a second copy on top of it.
  `togglePhotoSelection` raises the comparison request itself, since "the second pick opens it" is a
  rule about the selection rather than about a composable watching the list reach two, and
  `ProgressScreen` clears the selection on the way out — `PhotoSelectionHint` draws nothing at two
  picks, so a selection left standing on the grid behind the comparison would have no way out of
  itself.
- **Three overlays became flows, and the duplication is the price.** `PhotoComparisonScreen`,
  `TimelapseScreen` and `RecapScreen` are screens, and a `*Screen` inside a `components/` package
  was the thing being fixed — a sub-view lives there (onboarding's six steps, the photo flow's
  three), a screen does not. Each now has `ui/<flow>/` with the full
  `*Data`/`*State`/`*ViewModel`/`*Screen` quartet, which the "a second ViewModel is what earns a
  flow package" rule is satisfied by rather than bent around. **None of the three writes anything**,
  so every container exists to re-observe series `ProgressViewModel` already streams:
  `ComparisonViewModel` and `TimelapseViewModel` read the same two flows as each other (photos +
  profile), and `RecapViewModel` reads seven of `ProgressViewModel`'s thirteen repositories. That
  is a real cost and it was taken with eyes open, not overlooked. What it buys back: each overlay
  owns the data it draws instead of a slice of `ProgressUiState`, `ProgressContent` hands down only
  the selection that opened it (the photo pairing moved into `comparisonPair()`, the two-photo
  floor into `TimelapseUiState.playable`, the period into `RecapViewModel`), and the screen-local
  state that was loose `rememberSaveable`s in three composable bodies became three state holders —
  which is how the comparison divider stopped being lost to a rotation, since it had been a bare
  `remember`. `ProgressViewModel` stays read-only, and `ProgressScreenState`'s saver was
  renumbered once, both halves in the same commit, when the recap's period left it.
- **The Blood pressure page scrolls itself**, joining Photos in `SubjectDetail`'s `SelfScrolling`
  set — its list is per-reading rather than per-day, so a 3M window can hold a couple of hundred
  rows, and a `LazyColumn` nested in a scrolling column is measured with infinite height. Its delete
  asks first rather than raising an undo snackbar: the diary's swipe-and-undo needs a snackbar host
  Progress doesn't have. `BloodPressureViewModel` carries both the save and the delete because the
  tab and its sheet sit under one `ViewModelStoreOwner`, which leaves `ProgressViewModel` the
  read-only container its KDoc says it is. The sheet asks for no date or time — a reading is
  stamped when Save is tapped, and transcribing a paper log is not the workflow a backdated
  weigh-in is.

- **`SelfScrolling` is gone, because a route owns its own column.** Photos left the set when it
  became a route; Blood pressure was the last member and left the same way, so the exemption and
  the branch in `SubjectDetail` that read it both go. A `LazyVerticalGrid` or a `LazyColumn`
  nested in a `verticalScroll` is measured with infinite height and throws — that was always the
  real constraint, and the set existed only because a swap-in had to live inside the page's own
  scrolling column. A page that draws the window draws whatever column it likes.
- **The Blood pressure page reads through one container and writes through another.**
  `BloodPressureViewModel` is now the page's read-only container and `LogBloodPressureViewModel`
  keeps the save *and* the delete, which is what the old KDoc's "the tab and its sheet sit under
  one `ViewModelStoreOwner`" was really claiming — it is true of the route as well, so the list can
  still delete the row it is showing. Its delete asks first rather than raising an undo snackbar,
  unchanged: the diary's swipe-and-undo needs a snackbar host Progress doesn't have, and a reading
  is a number the user typed rather than a row they swiped. `pendingDeleteReadingId` moved to
  `BloodPressureState` outright, unlike the two sheet flags — only the page has a list to delete
  from, so there is no second surface to keep a copy for.

- **A supplement carries a dose *label* and a times-per-day *number*.** The dose is free text —
  "2000 IU", "5 g", "one scoop" — and nothing parses it, for the same reason fiber, sugar and
  sodium are reported and never graded: there is no field on the profile a supplement target could
  be derived from. `timesPerDay` is a real number only because "2x daily" turns the day's tick into
  a count out of N, which is what makes the Home row a counter rather than a checkbox. One tap
  advances a dose and wraps to zero at the target, so both shapes share one gesture and a mis-tap
  is corrected by the gesture that made it — the same call `MoodCard`'s rows make.

### Supplements

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

### Steps, activity & charts

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

### Adaptive layout

The binding shape — the two breakpoints, what each one does, and `ProfileDetailRoutes` —
is `CLAUDE.md` → **Window width**. These are the calls behind it.

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
- **The Progress tab no longer draws two panes, and the entry above it is superseded.** That one
  argued Progress *could not* earn a `ListDetailSceneStrategy` scene — its detail was a swap-in over
  `selectedSubject`, and routing it would buy a second `ViewModelStoreOwner` and a second copy of
  twelve repositories — so the tab drew its own `Row` instead. Every subject page is a route now
  (**Progress, recap & the energy check-in**), which removes both halves of that: there is no
  `selectedSubject` for a `Row` to switch on, and the second store owner has already been bought and
  paid for, per page, as a one-to-three-flow slice rather than a clone of the tab.
  What does *not* follow is that Progress should now become a scene. A `ListDetailSceneStrategy`
  wants a list whose rows are the detail routes; the Progress overview is four collapsible grids, a
  recap card, a Patterns card and a badge row, and only the grid cards open subjects. Drawing that
  beside one subject page would leave the recap and the patterns stranded in a 40% column, and the
  fourteen pages are charts — a chart wants the width, which is the trade the Photos page already
  made when it became a route. So: **one column at every width**, `OverviewPaneWeight` /
  `DetailPaneWeight` / `twoPane` / `embedded` all gone, and the two `@PreviewScreenSizes` previews
  with them. `:feature:profile`'s list-detail scene and the diary's calendar pane are untouched —
  both have a real list.

- **The diary's second pane is the calendar, and it is 320dp wide, not weighted.** The swap-in
  `FoodScreenState.calendarOpen` opens in a sheet was named as the pane that would earn one, and it
  has: at expanded width `FoodContent` draws `CalendarPanel` beside the day rather than over it.
  Fixed width is where this departs from Progress's weights, and the reason is what is *in* the
  pane — a month grid is seven fixed 44dp cells, so every weighted pixel goes into spreading them
  apart, while Progress's card grid and its charts both use what they are given. The day itself is
  unchanged at both widths: still one scrolling column, still one `FoodViewModel`, no route, nothing
  new saved. Two consequences that are not optional — `DiaryDateHeader` takes a **nullable**
  `onOpenCalendar` and drops the chevron with the tap target when the pane is drawn (a door onto
  what is already on screen is a lie), and `FoodContent` clears `calendarOpen` on becoming
  two-pane, or unfolding mid-sheet leaves a sheet over its own calendar.
- **Home and the camera flows stay one pane at every width.** A two-pane Home would need
  a second card order to author and `Profile.homeLayout` stores one; `fullBleed` is unchanged,
  because a viewfinder beside a list is not a viewfinder. Single columns are **not** width-capped
  either — that is a visual-design decision and this work is layout only.

### Onboarding

The seven steps, the copy and the order they ask in are unchanged. What the redesign changed is
density, input, momentum and the weight given to the last screen — the flow read like a settings
form, three cards against the top of a 915dp screen and twenty-six taps to set a weight.

- **Rui is on every step, or he is not a guide.** A 104dp mascot on Welcome, a 32dp avatar and one
  line in the header on steps 1–6, 48dp for the celebration on 6. The avatar and the bubble are one
  fixed row inside `OnboardingStep`, so they never move between screens and Rui never appears to
  arrive or leave. Before this he was on 0, 1 and 6 and absent in between, which is a decoration,
  not a guide. His line is per-*option* on the steps that have options (`GoalOption.bubble`,
  `DietOption.bubble`) because a guide that says the same thing whatever you picked is not
  reacting to you.
- **Steps 1 and 3 advance on tap; step 4 does not.** A disabled Next on a mandatory single-select
  step is a receipt for a decision already made, and the header's back arrow is the undo. The
  400ms hold (`SELECTION_HOLD_MS`) is what stops it reading as a mis-tap — the check lands and the
  icon circle flips before the screen moves. **Step 4 keeps its button** because tapping the
  selected card deselects it, so auto-advance would make deselection impossible, and "no
  preference" is a real answer that needs somewhere to go. The pending advance is local to
  `OnboardingScreen`, not in the saved `OnboardingState`: a process death mid-hold should restore
  the step the user was looking at, not finish a navigation they never saw.
- **`RulerPickerField` could not have been `NumericStepperField`.** The stepper's whole interaction
  model is one tap = one unit, which is right for ±50 kcal and ±5 g and wrong for a value chosen
  out of a range — 65 kg to 78 kg is 26 taps there and one gesture on a ruler. The stepper is not
  deleted; it still draws step 6's calorie and macro adjustments and every other field in the app.
  The ruler's tick geometry is **dp and does not scale with the font**: a scale whose ticks grew
  would stop mapping 11dp to one unit and the caret would stop pointing at the numeral under it.
- **Height and weight start unset.** `OnboardingForm.heightCm` and `weightKg` became nullable. A
  170cm/65kg default the user never chose is two numbers the confirm step computes a calorie
  target from, and they look like answers. "—" and a drawn-but-inactive scale says the gesture
  exists without claiming a value; nothing is red, because an untouched form is not an error.
- **The card steps divide the column, but only when there is a column to divide.** `weight(1f)`
  with a minimum height clips silently on a short screen, and on a step with no button underneath
  the missing card is the last one. `cardsFit()` asks first and falls back to a scroll with
  fixed-height cards; a large font scale takes the same fallback.
- **Step 4 is quieter by weight, not by label.** Outlined cards, a tonal Next until something is
  chosen, and the stack bottom-anchored under one band of air. Three steps of filled cards have
  already taught the reader what required looks like. The selected state is the same
  `primaryContainer` + 2dp border as everywhere else: *optional* applies to the question, not to
  the answer. `TonalButton` exists for this one place.
- **Step 6 leads with the number and shows its working.** The calorie figure at 57sp on its own
  card, and under it "1,961 kcal maintenance − 500 for steady loss". The derivation is the part
  that earns the size: it turns the number from an assertion into a calculation the reader can
  check, which is also what makes the stepper read as an adjustment rather than a correction.
  `Profile.maintenanceKcal()` prints the figure `calculateDailyTargets` already computes and
  throws away — a function rather than a sixth field on `DailyTargets`, which has ~18 construction
  sites and one caller that wants this. After a manual change the line says "you set yourself",
  which is what makes an override visible rather than silent.
- **Below the floor is still error *text*, not an `errorContainer` strip.** The design handoff
  asked for the strip; the rule in **Profile & Settings** says a caution the user may walk past
  never gets that surface, and two treatments of one warning across the two screens that show it
  would be worse than either. The number goes `error`, the glyph and sentence go under it, and the
  button stays filled and enabled.
- **The celebration is the same four roles behaving differently.** Rui grows 32→48dp and switches
  to Celebrating, the hero card takes a `primary` border while its steppers fade out, the macro
  bar redraws left to right over 500ms, and the button is held for the 900ms the whole thing runs.
  No confetti and no new colour. With animations off the state still applies and navigation is
  immediate, because every piece of it rides the app's existing `MotionDurationScale` idiom.

### Profile & Settings

The tab was fourteen caption-over-card sections in one scroll, and none of them were about the
person: sex, age, height, weight, activity level and goal were all stored and none was shown or
editable after onboarding. The split is what fixed both halves of that.

- **Three visual tiers, and that is the whole argument.** `surfaceContainerHigh` at 28dp for the
  identity header (once per screen, the subject of the page); plain `AppCard` for things you
  adjust; a row plus a `secondaryContainer` `IconTile` plus a chevron for things that go elsewhere.
  Fourteen identical sections made a stepper touched once a year read like a switch touched weekly,
  which is a hierarchy problem, not a copy problem. `SectionHeader` (sentence-case `titleMedium`,
  four of them) replaced `SettingsSection`'s uppercase caption for the same reason.
- **A second ViewModel is what earned `ui/settings/`.** `SettingsViewModel` owns units, dark mode,
  buddy, palette, the eight reminder flags and export/import/restore — and with them the ten
  repositories only `exportJson` needs, which is why the split *removes* work from
  `ProfileViewModel` rather than duplicating it. `RemindersScreen` shares that host rather than
  declaring a third: same eight flags, plus one piece of permission state the screen owns itself.
- **About you reuses `ProfileViewModel`.** It writes the same profile row the identity header
  reads, through the same setters. No save button anywhere — every control writes on change and
  nothing caches the calorie figure, so the result card and the header reprice in the same frame.
- **The two-pane list pane stays Profile's own scroll.** The handoff proposed an eight-row list
  (About you · Calories · Day targets · Cycle · …) with a default "You & your targets" detail. That
  needs Calories, Day targets and Cycle to become *routes*, which buys a second
  `ViewModelStoreOwner` and a second copy of twelve repositories — the exact trade already rejected
  for Progress. Only the three real routes joined `ProfileDetailRoutes`; the "Pick a section"
  placeholder stays.
- **The new row primitives live in `ui/shared/components/`, not `:core:designsystem`.** Every
  consumer — Profile, Settings, Reminders, About you — is a flow inside `:feature:profile`, which
  is exactly the `ui/shared/` middle case. And there is no `SwitchRow`: `AppListRow`'s `trailing`
  slot already is one, so it is four primitives rather than five.
- **`StepperRow` is not `NumericStepperField`.** The field is a label stacked over a filled input
  and is right where a value is *entered* (onboarding, the entry sheets); the row is right where
  four targets have to line their values and buttons down one edge. Forking one into the other
  would leave a component that is neither. Both share the 48dp-touch/40dp-visual button split, and
  `StepperButton` is feature-visible only because the Calories card draws the same pair beside a
  32sp hero figure.
- **`error` is text on the Calories card and a surface on the Reminders banner, and the difference
  is deliberate.** Being under a calorie target is a caution the user may walk past — the warning
  appears, the stepper keeps decrementing, and it never gets an `errorContainer`. Android blocking
  notifications is a genuine failure, and it is the one place in the app that surface is right.
- **The header's trend is two questions, not one.** The arrow is the delta's direction, the colour
  is `goalRelativeTrend`'s verdict, and the words say which — never colour alone. A movement under
  `TREND_ARROW_DEADBAND_KG` takes the flat glyph and the neutral tone rather than a confident
  arrow. It reads the *logged* weight (`trendVsSevenDaysAgo`), falling back to `Profile.weightKg`
  only when the log is empty, because the onboarding figure stops being true the first time anyone
  steps on a scale — which is also why About you's "Current weight" row says so rather than
  claiming to be the latest weigh-in.
- **Cycle stays on Profile.** It is a fact about the person, it *creates* surfaces (a Home card, a
  Progress page) rather than restyling existing ones, and a privacy-sensitive switch should not be
  two levels deep. The line that appears when it goes on is the other half of the privacy
  sublabel's honesty: a switch that quietly adds two surfaces should name them.
- **Palette names stay Soft/Bold/Muted/Contrast/Neutral.** The handoff renamed them
  Sprout/Fern/Sage/Lagoon/Bone; `MascotPalette.name` is a persisted token, and the picker prints
  the selected name beside the label precisely so the row is not colour-only. This binds **those
  five only** — it is a rule against renaming a stored token, not against the list growing, and the
  thirty hues added beside them name themselves after their hue for the reason recorded above.
- **Nav rows still carry no counts.** Unchanged rule, and the reason is unchanged: a number here is
  one more thing that can go stale, and the screen it opens is where counting is honest.

### Health Connect

The local provider, and the one that wins. Everything here exists because there are now *two*
providers for the same six types, and two writers for one table is the failure to design against.

- **Precedence, never a merge.** `cloudMetrics()` in `:core:data/health/HealthConnect.kt` is the
  whole rule: Health Connect owns every type it is granted, the Google Health API owns the rest,
  and `sync()` decides it once so the two legs can never both write a table. There is deliberately
  **no fuzzy time-window matcher** — the same watch commonly feeds Health Connect *and* the Google
  cloud with no shared identifier, so a tolerance-based matcher would be wrong in both directions
  with nothing to appeal to. Precedence needs no tolerance to be right. Partial grants are ordinary
  (Health Connect lets a user allow steps and deny heart), which is why `HealthConnectState.Available`
  carries a *set* and why the split falls out for free.
- **The handover is one-way and windowed, and that is the only dedup with a judgement in it.**
  Someone who synced the cloud for a month and then grants Health Connect would hold every workout
  twice: the two providers key `health_link` differently (`users/me/dataTypes/…` against a
  `hc:`-prefixed record id), so nothing correlates them. `supersededByConnect()` retires the cloud
  rows by *provenance and window* instead — this type, imported not pushed, inside the window
  Health Connect is about to re-import. Anything older stays, because it is outside the new
  provider's reach and so can never be duplicated; deleting it would throw away history nothing
  will backfill. Steps and heart rate need none of this: they record no link and their day rows are
  replace-in-full, so a handover is free.
- **One set of writers, both providers.** The Health Connect reader hands back the same `Remote*`
  types the cloud's parsers produce, so `store()`, `writeSteps()`, `writeHeart()`, `weightWriter()`
  and `writeSleepNight()` are shared verbatim — a workout is the same diary row whichever leg
  fetched it, deduped by the same table and cursored by the same rule. `weightWriter(note)` is the
  only thing that tells them apart on screen, and its skip-a-taken-day guard is what keeps a typed
  weigh-in winning over both.
- **Cursors are per provider, day tables are not.** Each `HealthMetric.connectDataType` is distinct
  from every `HealthDataType.id`, so `MAX(remoteTimeMillis)` per type is independent and revoking
  one leg cannot advance the other past data it never wrote. Steps and heart rate share the cloud's
  own `MAX(date)` cursor, because those tables belong to whichever provider last wrote them.
- **One version gate, in `HealthConnectSource.client()`.** The SDK's classes ship inside the APK so
  naming one is safe at any API level; *calling* into one is not, because it reaches platform APIs
  this app's minSdk 24 predates. So every entry point resolves through `client()` and returns the
  do-nothing answer when it is null, `:core:data`'s manifest carries
  `tools:overrideLibrary="androidx.health.connect.client"`, and the reads are `@RequiresApi(P)` so
  lint agrees with the runtime. `read()` repeats the explicit `SDK_INT` check because lint's flow
  analysis can follow one but not a nullable factory.
- **`:feature:profile` never imports `androidx.health.connect`.** The permission contract crosses
  the boundary as a framework `ActivityResultContract<Set<String>, Set<String>>` and the state as
  the app's own `HealthConnectState` — the rule `GoogleHealthAuth` follows by handing back a
  `PendingIntent`. That is the only thing `androidx.activity` is in `:core:data` for.
- **Blood pressure is Health Connect's alone.** It was manual-only because the cloud scope was
  ruled out at verification; `READ_BLOOD_PRESSURE` costs no CASA assessment, so it is the sixth
  type here and has no cloud twin (`cloudDataTypeOf` returns null for it). `blood_pressure_reading`
  autogenerates ids, so nothing keeps an imported reading apart from the same one typed by hand —
  `alreadyHeld()` is the guard, and it demands *identical* figures within a minute. That is not the
  matcher ruled out above: the only thing tolerated is which second the two writers stamped.
  An imported reading carries `pulseBpm = 0`, because Health Connect records a pulse as a
  `HeartRateRecord` and claiming one the cuff never reported would be an invention.
- **A session's burn and steps are estimates, not the watch's figures.** Health Connect keeps
  calories in a separate `ActiveCaloriesBurnedRecord`, so `estimateBurnedKcal()` and
  `estimatedSteps()` price an imported session — the same two functions the diary's own exercise
  sheet seeds, and both stay editable on the row. *ponytail: one `aggregate()` per session reads
  the real figure at the cost of a call per workout; worth it only if the estimates read wrong.*
- **Read-only, and not getting a write path.** Meals and water still go out over the cloud, so
  `pushNutrition` is untouched, no `WRITE_*` permission is requested and there is no Play
  health-write declaration to file. *ponytail: a windowed read rather than `getChanges(token)` — a
  changes token is state that must survive process death, where a window needs nothing because the
  cursor is already derived from rows actually written.*
- **The rationale intent rides `ShortcutAction`.** Health Connect's "why does this app want my
  data?" tap arrives as an intent *action* (two of them — the pre-14 one and the `activity-alias`
  the platform uses from 14), not an `EXTRA_ACTION` extra, but what it needs is what a shortcut
  needs: a route request delivered by an intent that must re-point an app already running. So
  `ShortcutAction` grew a `HealthSync` entry rather than the app growing a fourth nullable state
  beside `tabRequest` and `shortcutRequest`. `HealthConnectionScreen` is its target, which is why
  that screen carries its own rationale passage rather than extending `HealthDisclosurePanel` —
  that component's bullets are written against `HEALTH_SCOPES` in the order the cloud requests
  them, and each provider needs a passage a reviewer can read on its own.
- **Onboarding is untouched.** Step 5 still offers the cloud grant alone; stacking a second
  permission sheet at the app's highest-friction moment is its own decision, and Health Connect is
  opted into from Profile → Google Health.

### Google Health

The four requested scopes are Restricted, so the app is capped at 100 users until it passes
OAuth App Verification plus an annual CASA/OWASP-ASVS assessment. Everything below exists
because of that, not because it was the nicest design available. Health Connect now covers the
same types locally where it is granted, which shrinks how often this leg runs but does not
retire it: it is still the only leg that pushes, and the only one on a device without Health
Connect.

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
- **Four glyph rows, not four two-line bullets — and the fourth one writes.** The panel is now a
  one-line row per scope in `HEALTH_SCOPES` order plus three assurance rows, ~180 words down to
  ~70: trust comes from being scannable, and a wall of grey prose above two buttons is what a
  consent form looks like. The onboarding redesign's frames said "read-only"; `HEALTH_SCOPES`
  requests `nutrition.writeonly`, so that would have been false. The assurance reads "only the
  meals and water you log are written back" instead. **One row per requested scope stays the
  rule** — if a scope is added or dropped, this panel changes with it.
- **Neither the unavailable nor the declined state is an error.** A device without Play services is
  a fact about the device and a decline is an answer, so both take a `surfaceContainerHigh` strip
  and an `onSurfaceVariant` glyph, never `error`. Declined additionally swaps the two actions'
  weight — continuing becomes the filled button and retrying drops to a text button — because the
  question has been answered and the screen's job is to get out of the way without implying they
  got it wrong.
- **Onboarding's health step sits before Confirm, not after.** Finishing onboarding writes the
  profile, and `AppRoot` swaps the whole wizard out the moment that lands.
- **Two ViewModels, not one shared.** `:feature:onboarding` and `:feature:profile` each own their
  slice of `HealthSyncRepository`; `:feature:*` modules never import each other, and only the
  disclosure UI is genuinely common.

### Localization

The rules that bind are `CLAUDE.md` → **Localization**. These are the arguments behind them.

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

## Considered and declined

Weighed and deferred — not `FEATURES.md`'s "Deliberately absent" list, which is what was
ruled out on principle. Each note says what would reopen it.

- ~~**Open Food Facts as a second food source.**~~ **Shipped** — on both legs. The reopening
  condition was the miss rate on real use, and FDC being a US database is the miss: for a `ph.mart`
  app a locally-packaged product largely is not in it. See the Food entries above for the ordering,
  the estimates that are not read and the endpoint that is not used.
- ~~**Keeping the analyzed meal photo on the diary entry.**~~ **Shipped** — the three objections
  (storage growth, downsampling, the export question) are each answered in the Food entries above:
  a 500-photo cap, 768px at JPEG 85, and images stay out of the export exactly as they always have.
