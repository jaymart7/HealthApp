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
`LabelScanRoute(dateEpochDay)` · `VoiceLogRoute(dateEpochDay)` · `RecipeBuilderRoute` ·
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
`NavRail` with the FAB in it; at ≥840dp the Profile tab draws its eight sub-routes beside Profile
as a Nav3 list-detail scene, and the Food tab draws the calendar as a fixed 320dp pane beside the
day. Narrower than 600dp is unchanged. Home, Progress and the camera flows stay one pane at every
width.

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

A pinned header block, then sixteen reorderable/hideable cards laid out in paired rows:

Calories (hero ring) · Water · Macros · Week budget · Streak · Weight · Steps · Sleep · Heart rate ·
Blood pressure · Fasting · Mood · Supplements · Cycle · Today's workout · Progress photo.

- Pinned header block — mascot greeting + the app's one door to the coach, a two-or-three-cell
  Today strip mirroring visible cards, and the AI insight as a dismissible inset band (with a
  rules-based offline fallback).
- Half-width cards pair with an adjacent half; an unpaired half falls back to full width. Gated
  cards are removed before pairing, so survivors close up rather than leaving holes.
- Tapping a card's body opens that card's Progress subject page (photo card → the photo set,
  Streak → Badges, Steps → Activity, Workout → Strength, Calories/Macros/Week budget → Nutrition);
  back returns to Home. The card's own controls keep their taps, and Water — the one card with no page — is not
  tappable.
- An 8dp status dot on the four cards where on-track is a fact the app measures (calories, streak,
  fasting goal, weight direction) and nowhere else.
- The calorie ring names the day's exercise credit in `primary` and draws the share of the track it
  bought as its own `primaryContainer` arc — see **Exercise & strength** → Earned calories.
- Card order and visibility edited in Profile → Home layout (drag handle + move up/down a11y
  actions), reachable from a "Rearrange your Home" link at the foot of Home; "Reset to default"
  restores declaration order.
- Week budget — the Monday-to-Sunday calorie bank: what the week's *closed* days banked or
  overspent, how many of them were logged, and what the days left could each hold to finish even
  (clamped at the calorie floor, which it names). Unlogged days are skipped rather than counted as
  credit, today is never in it, and burned calories raise a past day's budget under the same switch
  they raise today's. Derived, never stored; it never rewrites a target.
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
- Add-entry sheet — **two states and a docked action bar under both**, plus a third that owns the
  sheet's full height. *Browse* answers "which food?": a search bar, a quick-add pill, and one
  tabbed list (Recents / Recipes / Saved meals) replacing four stacked panels, with a one-line
  legend per tab saying what tapping a row does and the filled `+` logging it straight away.
  *Form* answers "how much?": name, portion + unit, calories, protein/carbs/fat and
  fiber/sugar/sodium behind a disclosure. *Search* takes the whole sheet and hands it to one list.
  Back walks the levels one at a time; the tabs and the disclosure are controls, not levels.
- "Save as my food" is a switch pinned above the form's action bar, mounted from the moment the
  form opens and dimmed until there is something worth keeping — Add then does both.
- Seven nutrients per entry — fiber, sugar, sodium plus the Nutrition Facts panel's four (vitamin
  D, calcium, iron, potassium). The four are filled by a barcode scan or a picked food, never typed;
  all seven reprice with the portion. The day's summary bar carries them as one quiet line that
  opens into rows graded against a target, with sodium and sugar as limits rather than goals, and a
  line naming how many of the day's foods actually carried figures. **A scanned supplement ticked
  that day counts into those rows too** — and into nothing above them, since a supplement has no
  calories — with the same line saying so.
- Quick add — a bare calorie figure with no name, from a pill at the top of the sheet that logs
  and closes without reaching the form at all.
- Every numeric field is typable and steppable; changing a portion reprices the whole entry. An
  empty figure prints an em dash, never `0` — a typed zero prints as `0`. Portion presets (50 g,
  150 g, and the package's own serving where one is declared) show only on a form seeded from a
  per-100 g database row, which is the only thing they would be presetting against.
- Edit a logged row (supersedes it: soft-delete + insert, keeps its place in the day) — a row
  logged from the camera wears a 40dp thumbnail, shown again on the edit sheet as a labelled row
  that says the photo survives the correction — and opens full-screen, pinch-zoomable. The edit
  sheet opens straight into the form, subtitled with the meal and the time the row was logged,
  and drops every door that would write a *new* log. Under the Save button it carries a `Delete
  entry` — text, `error`, in the slot the keep-this-food switch vacates — raising the same Undo
  snackbar the swipe does, so it asks nothing first.
- Swipe to delete with Undo snackbar.
- Local text filter over the day's logged entries.
- Search your diary — a route above the tab searching every food ever logged, by name, newest
  first. Three levels of heading: an age band ("This week", "Earlier this month", "August"), a
  sticky day header carrying the absolute date, a relative-age chip and that whole day's calorie
  total, then the rows — on rules rather than in cards, each with its meal slot, the matched word
  marked, and a chevron. A count line says what was found and what a tap does; the field carries a
  clear button, offers the three queries that have worked before when it is empty, and sits over a
  meal filter (All / Breakfast / Lunch / Dinner / Snacks) that narrows the query itself rather than
  its results. Searching never blanks the list — the rows stay and two skeletons appear at the
  tail. Tapping a result opens it for review — meal, portion, calories, macros and micronutrients,
  all corrigible — and logging it from there writes a copy onto the day the diary was showing.
  Reached from a link at the foot of the diary, which carries the day's filter query with it.
  Capped at 200 hits; the copy never inherits the source row's meal photo.
- Copy a day — a link at the foot of the diary opens a calendar, then a sheet listing what that
  day held: each meal section with its item count and calories, water, and the day's workouts.
  Tick what to bring over and it lands on the day being shown. Copied plates leave their photos
  behind, a copied workout re-estimates its own steps, and water is set rather than added.
- Recent-food suggestions with one-tap re-log.
- Favorites — starring a food is also how you author one: it becomes yours.
- Your own foods — keep what's in the add-entry form as a food ("Save as my food"), and it leads
  every later food search ahead of the built-in list, replacing the built-in row of the same name.
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
  A filled speak card leads the screen, saying what to say — and, once there are words in the box,
  that a second tap replaces them. Under it the field takes the full width and wraps to four lines,
  so a dictated meal can be read and corrected in place, with Clear inside the field's own end.
  Under an empty field, the three sentences that have already become meals, newest first, each on
  two lines behind a clock — tapping one fills the field and stops there, leaving it editable and
  the estimate a tap away. Recorded when the meal is logged, never when it merely parses. Estimate
  is docked below the scroll and rides above the keyboard.
  While the call is in flight the sentence is on screen, quoted with its slot, and both Edit and
  Cancel step back to it with the words intact. "No food in that one", "That didn't work" and the
  offline screen quote it too.
  The review screen opens on what the meal costs — calories, a macro bar and a legend, summed from
  the rows and quoted again on the Log button. A row the model was unsure of says so on itself, and
  where the model named the words it could not pin down ("a slice"), the opened row quotes them
  back; a notice above counts how many. Remove lives in the opened row, Discard is a word beside
  Log in the docked bar, and a one-row parse offers "Say it again" back to the sentence.
- Water row in the diary; water goal and glass size configurable.
- A note on the day — one free-text note per day (500 chars), under its own rule at the foot of
  the diary. A day with a note draws it as a card you tap to edit; a day without one carries an
  "Add a note" link beside Search and Copy. Written to the day being shown, never to today, and
  saving an empty field is the delete. It exports, and the coach's `get_day` reads it; it is on no
  chart, in no recap, on no shared picture and in no copied day.
- Share the day — a PNG card of the day's summary bar and its per-meal totals, from a link at
  the foot of the diary. Absolute date, no food names.
- Meal ideas — AI suggestions sized to the day's remaining calories, with an offline fallback
  built from the user's own recents and recipes; picking one seeds the add sheet, never logs.

## Camera, barcode & the label

- AI photo food logging: capture → analyze → confirm, with retry, offline and manual-search paths.
  **A plate is every food on it** — rice, chicken and greens come back as three rows, up to eight,
  each collapsible, editable and repriceable before anything is written, and the whole plate logs
  under one meal slot in a single diary emission. A plate that is one food opens with its row
  already open. The confirm screen's 64dp plate opens full-screen on a tap — pinch to 4x, pan, X or
  back to close.
- The plate is kept. Every exit from the camera flow attaches its photo to the entry — recognized,
  gallery-picked, or hand-entered after a failed analyze — scaled to 768px, newest 500 retained.
  A plate that became several rows attaches to the first of them only. No other logging path
  attaches one.
- Barcode scanning (ML Kit) → **Open Food Facts first, FoodData Central second**. OFF is keyless,
  is a real barcode lookup and is stocked internationally, so a locally-packaged product resolves
  where FDC's US database has nothing; FDC's `foods/search` with its `gtinUpc` match check is the
  fallback. Not-found only when both answered; try-again only when neither could.
- Nutrition-label scan — the answer to a barcode neither database holds. Photograph the panel on
  the back of the pack and a model transcribes it into the review screen: name, the serving in the
  label's own words, calories, the three macros and **all seven nutrients**, including the four the
  Nutrition Facts panel mandates and no other path can supply, since those are seeded and never
  typed. Nothing is estimated — a line the panel does not print arrives absent and shows an em dash,
  never a zero. The figures stay as printed, for the amount they were printed against: a per-100 g
  panel seeds 100 g, a per-serving one seeds the weight its serving declares ("1 bar (25 g)" → 25 g)
  or one serving where it declares none, and the caveat under the portion says which. What the panel
  gave is listed read-only under the macros, because the four nobody can type still have to be
  checkable. The confirmation carries "Save as my food", so a local product read once leads every
  later food search and the second packet costs no AI call. Reached from the barcode flow's
  not-found and no-barcode screens, where it is now the leading action and hand entry has dropped to
  a text button; offline, unreadable and failed all land on hand entry with the pack still in view.
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
- Camera permission screen (shared by all three camera flows); predictive back branches per flow state.
- Progress photo capture with a date-stamped file.

## Exercise & strength

Logging and the strength screen live in `:feature:training`; the diary keeps its burn section,
Profile keeps routine authoring, Progress keeps the history. There is no Training tab or screen —
see `DECISIONS.md`.

- Log a workout: type, duration, MET-estimated burn (editable; stops re-estimating once touched).
- **Describe it instead** — a swap-in panel at the top of the log sheet takes a sentence,
  typed or dictated ("45 minute run along the river"), and fills in the type, the note and the
  duration. The burn stays the app's: the model is never asked for one and the schema has
  nowhere to put one, so a described run and a typed one of the same length price identically
  off the user's own latest weigh-in. Every field is editable after, back steps out of the
  panel one level, and the panel is absent when correcting a logged workout.
- Burned calories credited to the day's budget, with a Profile switch to opt out.
- **Earned calories** — what the credit bought, said in one voice on three surfaces: a
  snackbar the moment a workout saves, a promoted line and its own arc on Home's calorie
  ring, and a rule in the insight card ("Today's activity bought you 320 kcal more than a
  rest day — about a peanut-butter sandwich"). Every one of them goes silent with the
  switch off, on a correction to a past row, and under a 50 kcal floor.
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

An **overview** with a per-subject **page** behind every card — each one a route of its own, so it
draws the full window with no bottom bar and no FAB over its chart. Fifteen subjects in four
groups: Body (Weight · Photos · Measurements) · Nutrition (Food · Water · Fasting · Supplements) ·
Training (Activity · Strength) · Wellbeing (Sleep · Mood · Cycle · Heart · Blood pressure), plus
Badges as a summary row under the grids. Cycle is the one subject a setting can remove entirely.

- Overview: week recap ("Across everything"), one insight card carrying the goal projection, the
  Patterns card, four grouped grids of subject cards — value, a 26dp preview (sparkline, day bars or a photo strip) and
  a trend line — then the Badges row. A subject with nothing logged draws as a dashed "Nothing yet"
  card that still opens its page; a group with nothing tracked collapses to one expandable row.
- Detail page: a toolbar with back and the recap share, hero figure, fact chips, one chart card
  holding its own 1M/3M/6M/1Y range toggle and legend, and stat rows. The range is remembered per subject for the session. Empty subjects get a mascot page
  and no call to action — except Cycle, Blood pressure and Measurements, whose log sheets are on
  the page itself.
- Weight: daily line + 7-day average + dashed goal marker, axis labels pinned to the gridlines,
  goal chip, a BMI chip naming its WHO band, and an insight card carrying the projection and the
  energy check-in — the app's only insight card fed by two sources. Tapping it opens the check-in
  over the page. Under it, **When you weigh in**: once the log holds twelve timed weigh-ins that
  really split into an earlier and a later habit, the two averages and the day count behind each —
  a comparison of the user's own readings, never a claim about the body. Then **Records**: every
  weigh-in in the chart's window, newest first, each row showing its time beside its date and
  opening the log sheet on it to edit the figure or delete it — an imported row shows where it
  came from instead of a delete, and the list pages twenty at a time as you scroll.
- Food: the week's calorie bank (the Home card's figures, unranged — the week is the week), then
  calories + macros against target over the window, the seven nutrients averaged against
  their targets — supplements included, over the same logged-days denominator, with a line
  saying so — then the meal-photo strip — the newest
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
  Under the chart, a **catch-up checklist** — a day stepper back thirty days, and the rows that
  were due on the day it lands on, tapped the way Home's card is tapped. The one Progress subject
  page that writes: Home holds today and Profile holds the list, so a Tuesday nobody ticked has
  nowhere else to be fixed. A day already ticked keeps its own figure, so a row can still read
  "2 of 2" after the supplement dropped to once daily, and a supplement is never offered on a day
  before it was added.
- Water: glasses a day as bars against a dashed line for the profile's current goal, plus the
  window's average, best day and goals hit, and the goal read in the user's own units under the
  chip. Read-only — a glass is logged from Home's card or the diary's row, never from here.
- Fasting: completed fasts as bars on the day each one ended, against a dashed line for the
  profile's current goal, plus the window's average, longest and goals hit. Raising the goal moves
  the line, never a bar.
- Measurements: a table, not a chart — six parts each with their own sparkline row, above them the
  waist-to-height ratio against its published boundary and the fat/lean mass split. Both derived
  cards draw nothing at all rather than a dash when an input is missing. Tapping a row opens the
  add sheet pre-filled with that part; no range toggle, because there is nothing to slice.
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
- Recap: rolling 7/30/365-day summary, shareable as a single-card PNG, and — on the week and
  the month, never the year — a door to the coach carrying that period's question, since
  `get_history` reads a span of at most a month.
- Energy check-in: maintenance calories measured from 28 days of logged intake against the real
  weight trend, with a one-tap adjustment of the calorie target (in the Weight page's insight card,
  opening a full overlay that shows its working).
- Logging sheets: weigh-in (backdatable), measurements, blood pressure, cycle day. All but blood
  pressure carry a **time of day** beside the date, opening at now and set from a clock dialog —
  a reading is not only which day it was taken on. Blood pressure is stamped with the moment Save
  is tapped and has no picker at all.
- Add photo: two routes — a full-screen viewfinder with the gallery beside the shutter, then a
  preview putting a date, a time and an optional weight on the shot before it is saved. Back off
  the preview is a retake. The time shows on the before/after and timelapse labels, not on a grid
  tile, which has no width for it.

## AI (Firebase AI Logic / Gemini)

- Photo food recognition (the food-logging path above).
- Daily insight — one line on Home, cached per day, falls back to three local rules.
- Coach — a chat screen told the day's numbers, which can read the rest of the diary itself:
  `get_day` for any past day's meals, macros, water and activity — plus that day's steps against
  the step goal, and its sleep, mood, completed fast, supplements against what was due, heart rate
  and every blood-pressure reading it holds, where those are tracked, plus the note the user
  wrote about that day — `get_history`
  for a span of up to a month with water, training, steps, sleep, supplements, heart rate, the
  day's mean blood pressure, weigh-ins and body
  measurements (the last two as a change since the reading before, never as a figure), and `get_library` for
  the meals and recipes the user has saved, the foods they log most often and the supplements they
  take with today's count on each.
  History persisted, clearable with a confirm from the top-bar overflow. The answer streams in word
  by word, under the question, which is on screen from the moment it's sent, and may be a short
  list where a list answers better.
- Coach proposals — asked to log something, the coach drafts the rows and the user taps to confirm
  them: foods into today's diary, glasses of water, an activity — whose calorie burn is the
  app's own MET estimate from the user's latest weigh-in, never the model's guess — or today's
  weigh-in, drawn in the unit their profile uses with what it moves by under it, and only ever
  from a figure they volunteered: the coach never asks what they weigh and is never told — or a
  dose of one of their own supplements, matched by exact name and ticked onto today, never one the
  coach suggested. Three more record what the user said about themselves, on the weigh-in's rule:
  **how the day felt** (mood and energy on the same 1–5 scale the card taps out, either column or
  both), **a blood-pressure reading** (drawn with the band `categoryOf()` puts it in — the app's
  label, never the model's), and **one body measurement** (in the unit their profile uses,
  converted only on the write). All three are always today, none of them carries `days_ago` at all,
  and none earns the diary door. A whole meal is
  one card of several rows, each removable before the tap, written to the diary together. A food,
  a glass, an activity or a saved meal can be drafted **into an earlier day** — up to a month back,
  with the day on the card's own title; every row of one draft shares that day, and a weigh-in or
  a supplement is always today. One of
  the user's own saved meals or recipes is drafted by name, with the figures they saved and none
  the model invented. It never writes on its own, and nothing —
  not even the turn that drafted it — is persisted until the tap. Dismissing keeps the answer and
  writes nothing. Every figure a draft will write sits on a white receipt panel inside the card —
  calories big, the three macros under their fixed dots, one cell per row on a multi-row draft with
  a removable ✕ and a total underneath. Removing a row recounts the title, the total, the macro
  legend and the button label, and leaves an undo line inside the card. Taking every row out is not
  a dismissal: the card says so and the confirm goes quiet.
- Coach bands, not coach verdicts — a blood-pressure reading reaches the model **with the AHA band
  `categoryOf()` put it in**, the same label the Blood pressure card shows. The prompt lets it
  repeat that band and forbids it deriving one, calling a reading good or bad, or saying what a
  reading or a heart rate means for anyone's health. That is a doctor's question; the direction
  the numbers moved in is not.
- Coach recommendations — "what should I eat?" is answered from what is left of the day and from
  food that is already theirs: the library tool hands the coach their saved meals, their recipes
  *and* the foods they log most often, at the portions and figures they log them at, and their
  dietary preference is in its instructions. What it picks is drafted as a proposal card, so a
  suggestion is one tap from the diary.
- Coach workouts — "what should I train today?" is answered from the user's own saved routines:
  `get_library` hands the coach each one's lifts and the weekdays it is planned for, so the one on
  today's plan is the one it names, and `start_routine` drafts it as a card showing those lifts. It
  is the one proposal whose Confirm **writes nothing** — the button says "Start it", and the tap
  opens the strength screen seeded with that routine at last week's loads, which the user saves
  themselves. A routine is drafted alone, never beside rows, and the coach can neither invent a
  workout nor add a lift to one.
- Coach fasting — "start my fast" / "I'm breaking my fast" drafts the timer's own transition, the
  one proposal that writes no row: the card shows the goal a start will run to (the profile's) or
  how long the running fast has gone so far, the button says Start it or End it, and the tap flips
  the timer Home's card drives. A start while a fast is already running, or an end while none is,
  fails the turn rather than offering a Confirm that would do nothing. Never backdated, never
  suggested, and it earns no diary door.
- Coach door out — once a confirmed draft has put rows in **today's** diary, an outlined row under
  that answer names where they went ("View it in Breakfast") and switches to the Food tab. It lasts until the next
  question. A weigh-in, a supplement or a backdated draft gets none: the diary opens on today and
  holds neither of the first two, and a door onto the wrong screen or the wrong day is a shrug.
- Coach answer menu — long-press an answer to copy it, share it as text, or ask the same question
  again. Ask again appears on the newest answer only, and is a fresh send rather than a repair.
- Coach question menu — long-press your own question to edit it: the text goes back in the composer,
  unsent, for the user to rephrase and send themselves. Every question in the transcript offers it,
  however old, since nothing is sent and nothing is deleted — the original turn stays put. The
  question still in flight has none; the stop button is already its way back into the field.
- Coach follow-ups — three outlined pills under the newest answer, indented to its text edge,
  picked from the day's own numbers (a protein gap, room left, water short, a weigh-in to compare)
  and falling back to diary questions. Rule-based, not generated: tapping one sends it exactly as an
  opener does.
- Coach empty state — a read-only strip of the three figures the coach is already told about
  (calories, protein and water, each against its goal), a capability line beside the 64dp mascot,
  and four openers as a 2×2 of cards, each with an eyebrow naming the kind of question it stands in
  for: today, a logged day, a span, an opinion. The eyebrow is a label, never part of what is sent.
- Coach transcript — persisted, with a ruled day label at each day boundary, derived at render
  rather than stored. The coach's bubble and the user's are mirrors of each other, so the two read
  apart by shape before colour.
- Coach waiting — the turn in flight gets a real bubble from the moment it is sent, holding a status
  line over three placeholder lines that the first chunk overwrites in place. The mascot never sits
  alone on an empty row.
- Coach offline — a pinned strip under the top bar for as long as the connection is gone, plus a
  bordered notice (no mascot, no bubble) carrying the on-device fallback line under an eyebrow
  saying it came from the diary and not the coach, a Try again and a door to the diary. A failed
  turn is the same notice with one action and no fallback. No red on any of it.
- Coach stop mark — a stopped turn leaves a centred ruled line in the transcript saying so, the same
  shape as a day separator. Nothing is logged and the question is back in the field.
- Coach doors — the diary's day header and the twelve Progress subject pages the coach has tools
  for (weight, measurements, nutrition, water, fasting, supplements, activity, strength, sleep,
  mood, heart, blood pressure) carry an
  "ask the coach" action. It opens the chat with that day's or that subject's question **in the
  field, unsent**, with a chip above the field naming where it came from. Dismissing the chip
  leaves the text.
- Coach stop — the send button becomes a stop button while an answer is streaming. The turn is
  abandoned, nothing is written, and the question goes back into the field.
- Coach voice — a mic inside the chat field dictates the question through the system's own speech
  dialog and fills the field in. It never sends on its own, and it is absent where no recognizer is
  installed — its 48dp slot stays reserved either way, so the composer's geometry never shifts. The keyboard's own Send key sends, under the same rule the button follows: not on a
  blank field, and not while an answer is still streaming.
- Meal ideas (above).
- Talk-to-log (above) — a sentence parsed into several priced diary rows.
- Describe a workout (above) — a sentence parsed into one activity, priced on-device.
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
- One row family across all three "Your stuff" lists — supplements, food library, routines: a
  marker tile, a title-weight name, the figures drawn as data, a one-line contents summary, and
  nothing on the right at all. The card itself opens the row's sheet — Edit on supplements, Rename
  in the library and routines — and Delete lives at the foot of that sheet, below a rule in
  `error`, still asking before anything goes. No swipe, no drag, no multi-select, and nothing on
  any of the three can log, tick or start anything.
- Supplements: name, dose label, times per day, **which weekdays** — the same seven-cell picker the
  routine editor draws, defaulting to every day and refusing to be emptied — and **what one dose
  carries**: the seven graded nutrients, typable behind a "What's in a dose" disclosure that opens
  itself when there is something in it, with a µg/IU toggle on vitamin D because bottles print
  either. A→Z, times-per-day as the row's marker tile, a narrowed schedule spelled out on the row's
  figure line ("twice a day · Mon · Wed · Fri"), a docked bar that never scrolls away carrying Add
  beside **Scan label**, edit and delete. Home's checklist shows only what is due today, and the
  adherence chart prices each day against what was due on it.
- Supplement-label scan — photograph the Supplement Facts panel and a model transcribes it into
  the same edit sheet, seeded: the product name, the serving as the label words it ("2 capsules")
  as the dose, the frequency where the directions state one, and every declared line listed back
  read-only. The four this app grades (vitamin D, calcium, iron, potassium) are kept as figures and
  count toward the day and land in the same typable fields, so a misread digit is corrected rather
  than re-scanned; the rest — vitamin C, B12, zinc, magnesium — are kept as printed text, shown and
  never graded, because there is no field or target for them. The transcript stays as it was read
  even after a correction. Nothing is estimated.
- Food library — your own foods, saved meals and recipes — and routine library: rename and
  delete (neither can log or start anything). The food library adds a persistent search over
  names *and* contents lines, with match highlighting, and sticky counted section headers; My
  foods rows draw their macros in the fixed P/C/F colours.
- Workout routines: each card carries its own **Plan** zone — the weekday picker plus "n days a
  week", or a prompt and seven dashed cells when nothing is set yet.
- Data export / import — JSON, `EXPORT_SCHEMA_VERSION` 21, import is all-or-nothing.
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
NavRail (`BottomNavBar`'s ≥600dp sibling) · NumberFormat (`formatOneDecimal` /
`formatDecimals`, the app's one decimal formatter, `Locale.US` so a field's own parser can read
what it shows) · NumericStepperField · NutrientPanel ·
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

Numbers are the other half and go the other way: every decimal a user sees or types is ASCII,
formatted through `NumberFormat.kt` in `Locale.US`, because the fields that show these figures
parse them back with `toDoubleOrNull()`. `String.keepDigits` accepts a typed `','` and maps it
onto `'.'`, so a comma-locale keyboard's decimal key works. Grouping separators stay
locale-aware — nothing types those back in.

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
  **The label scan is not an exception to this**, and the line is where `OpenFoodFacts.kt` already
  drew it in refusing `nutriments_estimated`: a figure the source *derived* is out, a figure *read
  off a label* is in. Reading a printed panel is transcription, and its prompt forbids inferring,
  completing or recalling anything not visible.
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
