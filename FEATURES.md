# FEATURES.md — what FitPulse already does

The shipped-feature index. **Read this before proposing or building a feature** — if it's
listed here it exists, and if it's under "Deliberately absent" it was ruled out on purpose.

This file says *what* exists, one line each. `CLAUDE.md` is the binding architecture
reference and `DECISIONS.md` the log of why each call was made; nothing here restates
either. `DECISIONS.md` also holds what was weighed and deferred.

---

## Surfaces

**Four tabs** (`:core:navigation`): `HomeRoute` · `FoodRoute` · `ProgressRoute` · `ProfileRoute`.

**Routes above a tab** (own back toolbar, no bottom bar/FAB):
`CoachRoute` · `FoodCaptureRoute(dateEpochDay)` · `BarcodeScanRoute(dateEpochDay)` ·
`VoiceLogRoute(dateEpochDay)` · `RecipeBuilderRoute` ·
`StrengthWorkoutRoute` · `HealthConnectionRoute` · `FoodLibraryRoute` · `RoutinesRoute` ·
`SupplementsRoute` · `HomeLayoutRoute`.

**In-tab overlays** (no route, no second ViewModelStoreOwner): weekly/monthly/yearly recap ·
photo timelapse · photo comparison slider · meal-photo gallery · meal ideas.

**Off-phone surfaces:** Glance home-screen widget (`:app/widget/`) · Wear OS app (`:wear`) ·
Wear tile · WorkManager notifications (`:app/reminder/`).

**FAB quick-action sheet** (`:app/ui/QuickActionSheet.kt`): six rows split by a rule. Above it,
the diary chip row's three food doors — say what you ate · scan a barcode · log food (the camera)
— each with a `tertiaryContainer`-badged glyph. Below it, log exercise · add photo (a body
progress shot) · log weight, bare glyphs. Three rows carry a supporting line.

**Launcher shortcuts** (`@xml/shortcuts`, static): Say what you ate · Log food · Add water ·
Weigh in. The first three of those are the FAB sheet's own rows; water writes a glass and lands
on Home.

**Window width** (`AppScaffold`, the app's one reader of it): at ≥600dp the bottom bar becomes a
`NavRail` with the FAB in it; at ≥840dp the Progress tab draws its overview beside the open
subject page, the Profile tab draws its eight sub-routes beside Profile as a Nav3 list-detail
scene, and the Food tab draws the calendar as a fixed 320dp pane beside the day. Narrower than
600dp is unchanged. Home and the camera flows stay one pane at every width.

**Onboarding** is not in the nav graph — `AppRoot` swaps it out once a profile is written.

---

## Onboarding

Seven steps under one chrome — a 48dp app bar, Rui's avatar and one line, the headline — with a
slide between them and back never clearing a thing.

- Welcome: 104dp mascot, the bubble beside it, title/body/button anchored to the bottom.
- Goal (lose / maintain / build) and Activity level: full-height cards with a 64dp icon, and
  **no Next button** — a tap selects, holds 400ms and advances. The selected activity card names
  its own multiplier ("× 1.375 on your maintenance").
- Basics: units and sex toggles, then age, height, weight and optional target weight on draggable
  ruler pickers; tap a value to type it. Everything starts unset.
- Dietary preference (four-value enum; read only by meal ideas): the one optional step, outlined
  cards and a tonal Next, with Skip in the header.
- Google Health disclosure + connect step (step 5 of 6, before Confirm): one glyph row per
  requested scope and three assurance rows, with unavailable and declined states.
- Confirm targets: the calorie figure at 57sp with its derivation printed under it
  ("1,961 kcal maintenance − 500 for steady loss"), Mifflin–St Jeor + 30/40/30 macro split,
  editable, 1200/1500 kcal warn-don't-block floor, and a 900ms celebration on finish.

## Home

A pinned header block, then fifteen reorderable/hideable cards laid out in paired rows:

Calories (hero ring) · Water · Macros · Streak · Weight · Steps · Sleep · Heart rate ·
Blood pressure · Fasting · Mood · Supplements · Cycle · Today's workout · Progress photo.

- Pinned header block — mascot greeting + the app's one door to the coach, a two-or-three-cell
  Today strip mirroring visible cards, and the AI insight as a dismissible inset band (with a
  rules-based offline fallback).
- Half-width cards pair with an adjacent half; an unpaired half falls back to full width. Gated
  cards are removed before pairing, so survivors close up rather than leaving holes.
- An 8dp status dot on the four cards where on-track is a fact the app measures (calories, streak,
  fasting goal, weight direction) and nowhere else.
- Card order and visibility edited in Profile → Home layout (drag handle + move up/down a11y
  actions), reachable from a "Rearrange your Home" link at the foot of Home; "Reset to default"
  restores declaration order.
- Mascot picker: five characters (Rui, Gel, Mart, Alo, Lala) × thirty-five colours — five
  theme-derived (Soft, Bold, Muted, Contrast, Neutral) and fifteen named hues in a pale and a
  vivid tier (Blush/Red through Rose/Crimson), picked from a bottom sheet off Settings →
  Appearance; blink + breathe,
  a per-state performance (Celebrating hops and twinkles, Thinking tilts over a three-dot mouth,
  Sleepy breathes under a drifting "z", Happy sways), a spring on every state change, and a poke
  on the four screens where the mascot is the subject — all of it resting when system animations
  are off.

## Food diary

- Diary by meal section (breakfast/lunch/dinner/snack), any past day, never forward past today.
- Day picked by stepping a day at a time or from a calendar — a bottom sheet on a phone, and at
  ≥840dp a permanent pane beside the day, where the date header stops being a door onto it.
- Add-entry sheet: name, calories, protein/carbs/fat, fiber/sugar/sodium, portion + unit.
- Seven nutrients per entry — fiber, sugar, sodium plus the Nutrition Facts panel's four (vitamin
  D, calcium, iron, potassium). The four are filled by a barcode scan or a picked food, never typed;
  all seven reprice with the portion. The day's summary bar carries them as one quiet line that
  opens into rows graded against a target, with sodium and sugar as limits rather than goals, and a
  line naming how many of the day's foods actually carried figures.
- Quick add — a bare calorie figure with no name.
- Every numeric field is typable and steppable; changing a portion reprices the whole entry.
- Edit a logged row (supersedes it: soft-delete + insert, keeps its place in the day) — a row
  logged from the camera wears a 40dp thumbnail, shown again on the edit sheet and kept by the edit.
- Swipe to delete with Undo snackbar.
- Local text filter over the day's logged entries.
- Search your diary — a route above the tab searching every food ever logged, by name, newest
  first under absolute per-day headings, with a one-tap re-log onto the day the diary was
  showing. Reached from a link at the foot of the diary, which carries the day's filter query
  with it. Capped at 200 hits; the copy never inherits the source row's meal photo.
- Copy a day — a link at the foot of the diary opens a calendar, then a sheet listing what that
  day held: each meal section with its item count and calories, water, and the day's workouts.
  Tick what to bring over and it lands on the day being shown. Copied plates leave their photos
  behind, a copied workout re-estimates its own steps, and water is set rather than added.
- Recent-food suggestions with one-tap re-log.
- Favorites — starring a food is also how you author one: it becomes yours.
- Your own foods — save what's in the add-entry form as a food ("Save as my food") without
  logging it first, and it leads every later food search ahead of the built-in list, replacing
  the built-in row of the same name.
- Saved meals — snapshot a diary section, re-log as separate rows.
- Recipes — a saved meal with servings; logs as one priced row.
- Food search — the user's own foods first, then a built-in list of ~120 common foods, offline
  and instant; the empty field lists all of them, eight rows at a time in a box that appends the
  next eight when it is scrolled to its bottom. Behind both, an Open Food
  Facts tier folds packaged products in at the back — debounced, three characters minimum,
  never blocking or replacing the local answer, and silently absent offline. One row design
  everywhere it draws: name, portion, coloured P/C/F initials, and the calorie figure as the
  heaviest thing on the row.
- Food search as a screen — the photo flow's no-food fallback and the barcode flow's dead ends
  open the search full height: a search bar whose back arrow is the way out, results running to a
  docked bar carrying the count and the hand-entry door, skeleton rows and a retry while the
  packaged-food tier is in flight or after it failed, and a named "nothing matches" that says where
  it looked.
- Talk-to-log — say or type "two eggs, toast and a black coffee" and log the whole meal at
  once, every parsed row editable and repriceable before anything is written. Speech is the
  system's own dialog; typing is the same path.
- Water row in the diary; water goal and glass size configurable.
- Share the day — a PNG card of the day's summary bar and its per-meal totals, from a link at
  the foot of the diary. Absolute date, no food names.
- Meal ideas — AI suggestions sized to the day's remaining calories, with an offline fallback
  built from the user's own recents and recipes; picking one seeds the add sheet, never logs.

## Camera & barcode

- AI photo food logging: capture → analyze → confirm, with retry, offline and manual-search paths.
  The confirm screen's 64dp plate opens full-screen on a tap — pinch to 4x, pan, X or back to close.
- The plate is kept. Every exit from the camera flow attaches its photo to the entry — recognized,
  gallery-picked, or hand-entered after a failed analyze — scaled to 768px, newest 500 retained.
  No other logging path attaches one.
- Barcode scanning (ML Kit) → **Open Food Facts first, FoodData Central second**. OFF is keyless,
  is a real barcode lookup and is stocked internationally, so a locally-packaged product resolves
  where FDC's US database has nothing; FDC's `foods/search` with its `gtinUpc` match check is the
  fallback. Not-found only when both answered; try-again only when neither could.
- Barcode memory — a resolved product is remembered by its barcode, so a rescan is instant, works
  offline and spends none of the shared FDC budget. Not exported.
- The diary's mic, barcode and camera doors all log to the day being reviewed; the FAB and the
  launcher shortcuts log to today.
- Both viewfinders carry a gallery door and a manual-entry door.
- Review this item — the confirm step for a barcode match, a search hit or a hand entry. One card
  carries what is being checked (name, portion, calories); the portion is a stepper with presets, a
  unit toggle and the per-100 g caveat against the number it is about, and every value reprices
  with it. Macros and micronutrients are tiles below, printing a dash where nobody has supplied a
  figure. Log and Discard are docked.
- Camera permission screen; predictive back branches per flow state.
- Progress photo capture with a date-stamped file.

## Exercise & strength

Logging and the strength screen live in `:feature:training`; the diary keeps its burn section,
Profile keeps routine authoring, Progress keeps the history. There is no Training tab or screen —
see `DECISIONS.md`.

- Log a workout: type, duration, MET-estimated burn (editable; stops re-estimating once touched).
- Burned calories credited to the day's budget, with a Profile switch to opt out.
- Strength workouts: a full set editor (lift name, reps, weight in the user's unit), lift-name
  chips from recent sessions, last-lifted load shown per lift.
- Bodyweight sets (0 kg) are a real value.
- "Repeat last workout" seeds the whole set list.
- Routines: save a session as a routine (modal reps), start one to seed sets at last-lifted loads.
- Training plan: a weekday picker per routine; Home shows today's routine and a week ratio.
- Edit or delete a logged workout (sets re-pointed in the same transaction).
- Rest timer between sets: Off/1:00/1:30/2:00/3:00, auto-started by "Add set", with +30 sec,
  Skip and a buzz at zero. Screen-local — it keeps time off the clock, not a tick count.

## Progress

An **overview** with a per-subject **detail page** behind every card. Fourteen subjects in four
groups: Body (Weight · Photos · Measurements) · Nutrition (Food · Fasting · Supplements) ·
Training (Activity · Strength) · Wellbeing (Sleep · Mood · Cycle · Heart · Blood pressure), plus
Badges as a summary row under the grids. Cycle is the one subject a setting can remove entirely.

- Overview: week recap ("Across everything"), one insight card carrying the goal projection, the
  Patterns card, four grouped grids of subject cards — value, a 26dp preview (sparkline, day bars or a photo strip) and
  a trend line — then the Badges row. A subject with nothing logged draws as a dashed "Nothing yet"
  card that still opens its page; a group with nothing tracked collapses to one expandable row.
- Detail page: a toolbar with back and the recap share, hero figure, fact chips, one chart card
  holding its own 1M/3M/6M/1Y range toggle and legend, stat rows, and a switcher naming the rest of
  the group. The range is remembered per subject for the session. Empty subjects get a mascot page
  and no call to action. Subject pages are becoming **routes**, one at a time — a converted one
  draws the full window with no bottom bar and no FAB over its chart, the way Photos already does.
  Converted so far: Photos, Sleep, Mood, Heart, Supplements, Strength, Fasting, Activity, Cycle, Blood pressure.
- Weight: daily line + 7-day average + dashed goal marker, axis labels pinned to the gridlines,
  goal chip, a BMI chip naming its WHO band, and an insight card carrying the projection and the
  energy check-in.
- Food: calories + macros against target over the window, the seven nutrients averaged against
  their targets, then the meal-photo strip — the newest
  twelve kept plates, opening a full-screen gallery grouped by day with a full-frame view per meal.
- Activity: two charts sharing one range toggle — daily steps against the profile's goal line
  (imported) and daily burn, the latter folding steps and logged workouts so a counted walk is not
  counted twice.
- Strength: volume chart and the window's workout/set/volume totals, then all-time personal
  records ranked by estimated 1RM (Epley) — all-time on purpose, so a 1M filter cannot retire a
  record.
- Sleep: imported nights as day bars against an eight-hour floor, average and longest, and the
  count of nights recorded. No way to type one in — FitPulse cannot measure sleep.
- Mood: mood and energy as two sparse series placed by date, each averaged over its own
  denominator, and the count of days logged. Filled by Home's two-tap card, not from here.
- Heart: imported days as bars spanning each day's lowest reading to its average — the one chart
  that is not zero-based — plus that window's average, its lowest, and the days recorded. The
  lowest is never called a resting rate.
- Supplements: adherence per day against each day's own snapshotted target, the window average,
  the count of full days and the days logged. A missed day and an untracked day draw differently.
- Fasting: completed fasts as bars on the day each one ended, against a dashed line for the
  profile's current goal, plus the window's average, longest and goals hit. Raising the goal moves
  the line, never a bar.
- Blood pressure: each day as one bar spanning its mean diastolic to its mean systolic, the
  window's systolic/diastolic/pulse averages, and every reading listed newest-first with a delete
  that asks first. Its log sheet is on the page, so its empty state carries a button too.
- Cycle: the day of the cycle and the next period's prediction as now-facts, then the window's
  flow chart, averages and every period newest-first, each dated and measured. Its log sheet is on
  the page, so this is the one subject page whose empty state carries a button. No fertile window
  and no ovulation date.
- Photos: a full-screen **route** — a 3:4 grid under
  sticky month headers, each month counted, opened by a header strip carrying the count, the span
  and the way into the player, with the strip's share in the toolbar. A hint bar teaches the
  tap-two rule and numbers each pick; the second tap opens the comparison. Before/after, the
  timelapse and the recap are routes too, so none of the four wears the bottom bar or the FAB and
  back leaves each one the way back leaves any screen. Before/after reads as a
  full-bleed stage — the weight delta as the headline in goal-relative colour, a drag-anywhere
  divider or a side-by-side toggle under it. The timelapse plays on the same stage over a
  date-positioned timeline, so a gap in logging looks like one, with a hold-under crossfade between
  frames. Both share as a PNG strip, and every share sheet in the app can now save its picture to
  the gallery.
- Body: five measurement sites (chest, waist, hips, arms, thighs) plus body fat %, chart + history,
  a waist-to-height ratio against the published 0.50 boundary once a waist reading exists, and a
  fat-mass / lean-mass split once a body fat reading and a weigh-in both exist. Body fat is logged
  from the same sheet as the tape readings, in percent — it is the one part the unit toggle does
  not touch.
- Mood + energy: two series, separate denominators.
- Cycle: **off by default**, behind Profile → Cycle. Flow chart, average cycle and period lengths,
  a next-period estimate once two periods are on record, and the period list. Days are logged from
  a Home card (today's flow, one tap) or the tab's backdatable sheet (flow + ten symptom tags).
  Health Connect fills in period days where it is granted; that permission is requested only while
  the switch is on. No fertile window and no ovulation date — see `CLAUDE.md`.
- Sleep / Heart: watch-imported, charted anchored to today.
- Fasting: session hours vs. goal line.
- Supplements: percent-taken per day.
- Blood pressure: floating-bar chart, category label per reading, manual log sheet.
- Badges: seven derived families — streak, days logged, weight moved, workouts, fasts,
  longest fast, photos.
- Patterns: up to four comparisons between two things you log — sleep against calories, training
  days against protein, steps against mood, a fast against the next day — each splitting the last
  90 days at the driver's median and reporting both averages with the day count behind each.
  Derived, never stored, never sent to a model, and drawn only once the log can support one.
- Recap: rolling 7/30/365-day summary, shareable as a single-card PNG.
- Energy check-in: maintenance calories measured from 28 days of logged intake against the real
  weight trend, with a one-tap adjustment of the calorie target (in the Weight page's insight card,
  opening a full overlay that shows its working).
- Logging sheets: weigh-in (backdatable), measurements, progress photo, blood pressure, cycle day.

## AI (Firebase AI Logic / Gemini)

- Photo food recognition (the food-logging path above).
- Daily insight — one line on Home, cached per day, falls back to three local rules.
- Coach — a chat screen told the day's numbers, which can read the rest of the diary itself:
  `get_day` for any past day's meals, macros, water and activity, `get_history` for a span of up to
  a month with weigh-ins. History persisted, clearable with a confirm. The answer streams in word
  by word, under the question, which is on screen from the moment it's sent, and may be a short
  list where a list answers better.
- Coach proposals — asked to log something, the coach drafts the row and the user taps to confirm
  it: a food into today's diary, or glasses of water. It never writes on its own, and nothing —
  not even the turn that drafted it — is persisted until the tap. Dismissing keeps the answer and
  writes nothing.
- Coach voice — a mic on the chat bar dictates the question through the system's own speech dialog
  and fills the field in. It never sends on its own, and it is absent where no recognizer is
  installed.
- Meal ideas (above).
- Talk-to-log (above) — a sentence parsed into several priced diary rows.
- Every AI path degrades to a manual or local-derivation path offline.

## Profile & settings

Four screens, not one scroll. **Profile** (the tab) is about the person: an identity header
(mascot, goal, "Male · 26 · 170 cm · Moderately active", now/target weight with a goal-relative
trend) · **Targets** (calories & macros, then water/fasting/steps as one Day-targets card) ·
**Your body** (cycle) · **Your stuff** (supplements, food library, workout routines). A gear in the
title row opens **Settings**: Display (units, appearance, cards on Home) · Notifications ·
Connections · Data · About. **About you** (from the header) holds the six Mifflin–St Jeor inputs
plus target weight and the add-exercise-calories switch. **Reminders** (from Settings) holds the
eight switches in three groups with the permission banner. All three new routes are Profile detail
panes at ≥840dp.

- Nutrient targets derived from sex, age and the calorie target (DRI/AI values; fiber at
  14 g/1000 kcal and free sugars at 10% of energy, so both move with an edited calorie target).
  Nothing to set — there is no nutrient editor.
- Editable calorie and macro targets with "Reset to calculated" — which also undoes a target
  applied from the energy check-in; a manual calorie target reprices the split.
- Metric/imperial toggles; water glass size and daily goal; fasting goal hours; step goal.
- Sex, age, height, current weight, **target weight**, goal and activity level all editable on
  About you — no save button, and the result card reprices as you nudge.
- Light / dark / follow-device; mascot character row and a colour row opening the 35-swatch sheet.
- Supplements: name, dose label, times per day; edit and delete.
- Food library — your own foods, saved meals and recipes — and routine library: rename and
  delete (neither can log or start anything).
- Data export / import — JSON, `EXPORT_SCHEMA_VERSION` 18, import is all-or-nothing.
- Automatic local backup — the same JSON, written weekly to app-private storage, newest three
  kept. Listed under Data with a confirm-first restore that runs the ordinary import. Android
  Auto Backup covers the database and those files; progress photos are excluded from the cloud
  copy.

## Health sync — two providers, one entry point

**Health Connect** (`androidx.health.connect:connect-client`, Android 9+): on-device, no account
and no network. Reads workouts, weight, sleep, steps, heart rate **and blood pressure** — the
sixth type, which the cloud leg deliberately never asked for. Read-only; FitPulse writes nothing
back. Permissions are per type and a partial grant is ordinary.

**Google Health API** (REST against `health.googleapis.com/v4`, not Google Fit; OAuth via
play-services-auth): imports whatever Health Connect isn't granted, and is the only leg that
pushes — meals and water.

Health Connect wins per type; the cloud fills the gaps. One `sync()` runs both, so precedence is
decided in one place. Both connection states are live, never stored flags. First sync backfills
30 days, and a handover retires the cloud rows inside that window so nothing lands twice.

## Reminders

Ten WorkManager notifications behind seven Profile switches: three meal reminders · weigh-in
(Mondays) · progress photo (fortnightly) · two water checks · supplements · training day · weekly
recap (Sundays 19:00). Plus a one-shot fasting-goal notification (an eighth switch), derived off
the active fast rather than scheduled periodically. The two water checks carry a **+1 glass**
action button — the only action button in the app; tapping it logs the glass without opening
anything and cancels the notification. The weekly recap is the only one whose tap does more than
pick a tab: it lands on Progress **with the recap overlay already open**, and stays quiet on a week
with nothing logged in it.

## Wear & widget

- Widget: today's calories (linear bar), water with a +1 glass button, streak, fasting target time.
- Wear app: one screen — calories ring, water, fasting, streak; buttons to add a glass and
  toggle a fast (both send an intent, never a row).
- Wear tile: read-only glance, opens the app.
- All three draw one `TodaySnapshot` (`:core:today`), pushed over the Data Layer.

## Design system (`:core:designsystem`)

AIChip · AIInsightCard · AppBottomSheet · AppCard · AppTextField · AppTopBar · BadgeDot ·
BottomNavBar · Buttons · CalendarPanel · DateFormat · DiscardConfirmDialog · DockedFab ·
FoodItemRow (with `MealThumbnail`) · FullScreenState · GoalProjectionLine ·
HealthDisclosurePanel · HomeCardLayout · MacroBar · MacroFieldCell · MacroInputGroup ·
MascotAvatar · MascotPalette (the 35 swatches) · MascotSpeechBubble · MicronutrientInputGroup ·
NavRail (`BottomNavBar`'s ≥600dp sibling) · NumericStepperField · NutrientPanel ·
PhotoBitmap (`rememberBitmapFromFile`, every stored photo in the app decodes through it) ·
SegmentedToggle · SelectableCard · ShareImage (`ShareImageSheet`, `captureToPicture` +
`sharePng` — every picture the app hands the chooser) · SheetDatePicker · StepProgressBar ·
WaterGlassRow.

Charts live in `:feature:progress/ui/shared/components/`: `DayBarChart` (zero-based) and
`RangeBarChart` (floating bars).

## Localization scaffolding

Every module owns a `res/values/strings.xml` and every user-facing string reads from it —
about 1,100 across twelve modules. **No translation ships**; this is what makes one possible.
`./gradlew checkUiLiterals` is the gate that keeps it that way.

---

## Deliberately absent

Ruled out on the record. Don't re-propose without saying why the reasoning has changed —
each one is argued in `CLAUDE.md`.

- **Accounts, sign-in, server sync.** No auth system exists. Nothing may assume one.
- **Monetization.** No pricing, subscription or paywall has been decided.
- **A streak celebration toast.** The badge lighting up is the reward.
- **Mood, sleep, fasting, supplements, steps and cycle are not streak domains.** The streak is
  food, water, weigh-in, exercise — adding a fifth rewrites what past runs meant.
- **Micronutrients beyond the Nutrition Facts four.** Vitamins A, C, E, K, B12, folate, magnesium
  and zinc were weighed: FDC carries them inconsistently and the built-in food list would be mostly
  zeros, so most days would read as a deficiency the app invented.
- **AI-estimated micronutrients.** The photo, voice and meal-idea schemas ask for three nutrients
  and will not be widened — a model asked what calcium is in a photographed plate produces a number.
- **A stepper for vitamin D, calcium, iron or potassium.** They are seeded and repriced, never typed.
- **A fertile window or ovulation date.** FitPulse names things and reports numbers; a fertile
  window derived from a mean cycle length is a contraception claim it cannot stand behind.
- **Cycle data in any AI payload, the widget, the watch or the recap.** It stays on the phone.
- **Badges in the recap.** No badge records when it was earned.
- **Sleep, heart, blood pressure, fasting, supplements in the recap.** Not enough scroll earned.
- **Blood pressure's Google Health scope.** Deliberately not requested. Health Connect reads it
  instead — that permission costs no CASA assessment.
- **Not exported:** saved meals, recipes, routines, `health_link`, coach history, steps, sleep,
  heart, running fasts, meal photos (images, like progress photos, never travel).
- **No MP4 photo share.** PNG strips only.
- **No HTTP client dependency.** `HttpURLConnection` + kotlinx.serialization, on purpose.
- **No Google Fit.** Health Connect and the Google Health API, nothing else.
- **Nothing is written to Health Connect.** Read-only — meals and water go out over the cloud leg.
- **No fuzzy dedup matcher between the two providers.** Precedence per type, and a windowed
  handover. The same watch feeds both with no shared identifier, so a tolerance-based matcher
  would be wrong in both directions.
- **Dynamic color (Material You) is off**, and `Color.kt` is frozen.
- **No hard deletes** anywhere except an unfinished fast and a superseded workout's sets.
- **The watch has no database**, and the tile never writes.
- **No planned meals** — the diary never steps past today.

## Open backlog

`CLAUDE.md` → **Backlog** holds the outstanding work (FDC key proxy, Google Health verification
and the unpinned response fields, the final mascot illustration, the missing CI workflow and
instrumented tests). Inline `ponytail:` comments
mark known ceilings and their upgrade paths.
