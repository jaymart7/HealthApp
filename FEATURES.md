# FEATURES.md — what FitPulse already does

The shipped-feature index. **Read this before proposing or building a feature** — if it's
listed here it exists, and if it's under "Deliberately absent" it was ruled out on purpose.

This file says *what* exists, one line each. `CLAUDE.md` says *why* and is the binding
architecture reference; nothing here restates it. `ROADMAP.md` says *what next* — full specs
for planned features, and the list of what was weighed and deferred.

---

## Surfaces

**Four tabs** (`:core:navigation`): `HomeRoute` · `FoodRoute` · `ProgressRoute` · `ProfileRoute`.

**Routes above a tab** (own back toolbar, no bottom bar/FAB):
`CoachRoute` · `FoodCaptureRoute(dateEpochDay)` · `BarcodeScanRoute(dateEpochDay)` ·
`VoiceLogRoute(dateEpochDay)` · `RecipeBuilderRoute` ·
`StrengthWorkoutRoute` · `HealthConnectionRoute` · `FoodLibraryRoute` · `RoutinesRoute` ·
`SupplementsRoute` · `HomeLayoutRoute`.

**In-tab overlays** (no route, no second ViewModelStoreOwner): weekly/monthly/yearly recap ·
photo timelapse · photo comparison slider · meal ideas.

**Off-phone surfaces:** Glance home-screen widget (`:app/widget/`) · Wear OS app (`:wear`) ·
Wear tile · WorkManager notifications (`:app/reminder/`).

**Launcher shortcuts** (`@xml/shortcuts`, static): Say what you ate · Log food · Add water ·
Weigh in. The first three of those are the FAB sheet's own rows; water writes a glass and lands
on Home.

**Window width** (`AppScaffold`, the app's one reader of it): at ≥600dp the bottom bar becomes a
`NavRail` with the FAB in it; at ≥840dp the Progress tab draws its overview beside the open
subject page, and the Profile tab draws its five sub-routes beside Profile as a Nav3 list-detail
scene. Narrower than 600dp is unchanged. Home, the diary and the camera flows stay one pane at
every width.

**Onboarding** is not in the nav graph — `AppRoot` swaps it out once a profile is written.

---

## Onboarding

- Welcome screen with mascot and "I already have an account" (a no-op — no auth exists).
- Basics: age, sex, height, weight, units.
- Activity level; goal (lose / maintain / gain).
- Dietary preference (four-value enum; read only by meal ideas).
- Google Health disclosure + connect step (step 5 of 6, before Confirm).
- Confirm targets: Mifflin–St Jeor calories + 30/40/30 macro split, editable, 1200/1500 kcal
  warn-don't-block floor.

## Home

Fourteen cards, all reorderable/hideable except the pinned greeting and AI insight:

Calories (ring) · Streak · Water · Fasting · Today's workout · Sleep · Steps · Heart rate ·
Blood pressure · Mood · Supplements · Weight · Macros · Progress photo.

- Mascot greeting card — the app's one door to the coach.
- AI insight card (dismissible, with a rules-based offline fallback).
- Card order and visibility edited in Profile → Home layout (drag handle + move up/down a11y
  actions); "Reset to default" restores declaration order.
- Mascot picker: five characters (Bibo, Pip, Zed, Momo, Sprig) × five palettes; blink + breathe
  animation that rests when system animations are off.

## Food diary

- Diary by meal section (breakfast/lunch/dinner/snack), any past day, never forward past today.
- Add-entry sheet: name, calories, protein/carbs/fat, fiber/sugar/sodium, portion + unit.
- Quick add — a bare calorie figure with no name.
- Every numeric field is typable and steppable; changing a portion reprices the whole entry.
- Edit a logged row (supersedes it: soft-delete + insert, keeps its place in the day).
- Swipe to delete with Undo snackbar.
- Local text filter over the day's logged entries.
- Recent-food suggestions with one-tap re-log.
- Favorites — starring a food is also how you author one: it becomes yours.
- Your own foods — save what's in the add-entry form as a food ("Save as my food") without
  logging it first, and it leads every later food search ahead of the built-in list, replacing
  the built-in row of the same name.
- Saved meals — snapshot a diary section, re-log as separate rows.
- Recipes — a saved meal with servings; logs as one priced row.
- Food search — the user's own foods first, then a built-in list of ~120 common foods, offline
  and instant; the empty field lists all of them, eight at a time.
- Talk-to-log — say or type "two eggs, toast and a black coffee" and log the whole meal at
  once, every parsed row editable and repriceable before anything is written. Speech is the
  system's own dialog; typing is the same path.
- Water row in the diary; water goal and glass size configurable.
- Meal ideas — AI suggestions sized to the day's remaining calories, with an offline fallback
  built from the user's own recents and recipes; picking one seeds the add sheet, never logs.

## Camera & barcode

- AI photo food logging: capture → analyze → confirm, with retry, offline and manual-search paths.
- Barcode scanning (ML Kit) → FoodData Central branded lookup, with a `gtinUpc` match check.
- The diary's mic, barcode and camera doors all log to the day being reviewed; the FAB and the
  launcher shortcuts log to today.
- Both viewfinders carry a gallery door and a manual-entry door.
- Camera permission screen; predictive back branches per flow state.
- Progress photo capture with a date-stamped file.

## Exercise & strength

- Log a workout: type, duration, MET-estimated burn (editable; stops re-estimating once touched).
- Burned calories credited to the day's budget, with a Profile switch to opt out.
- Strength workouts: a full set editor (lift name, reps, weight in the user's unit), lift-name
  chips from recent sessions, last-lifted load shown per lift.
- Bodyweight sets (0 kg) are a real value.
- "Repeat last workout" seeds the whole set list.
- Routines: save a session as a routine (modal reps), start one to seed sets at last-lifted loads.
- Training plan: a weekday picker per routine; Home shows today's routine and a week ratio.
- Edit or delete a logged workout (sets re-pointed in the same transaction).

## Progress

An **overview** with a per-subject **detail page** behind every card. Thirteen subjects in four
groups: Body (Weight · Photos · Measurements) · Nutrition (Food · Fasting · Supplements) ·
Training (Activity · Strength) · Wellbeing (Sleep · Mood · Heart · Blood pressure), plus Badges as
a summary row under the grids.

- Overview: week recap ("Across everything"), one insight card carrying the goal projection, four
  grouped grids of subject cards — value, a 26dp preview (sparkline, day bars or a photo strip) and
  a trend line — then the Badges row. A subject with nothing logged draws as a dashed "Nothing yet"
  card that still opens its page; a group with nothing tracked collapses to one expandable row.
- Detail page: back arrow, hero figure, fact chips, one chart card holding its own 1M/3M/6M/1Y
  range toggle and legend, stat rows, and a switcher to the rest of the group. The range is
  remembered per subject for the session. Empty subjects get a mascot page and no call to action.
- Weight: daily line + 7-day average + dashed goal marker, axis labels pinned to the gridlines,
  goal chip, and an insight card carrying the projection and the energy check-in.
- Food: calories + macros against target over the window.
- Activity: two charts — daily steps (imported) and daily burn.
- Strength: volume chart, all-time personal records ranked by estimated 1RM (Epley).
- Photos: grid, before/after comparison slider, timelapse player, share as a PNG strip.
- Body: five measurement sites (chest, waist, hips, arms, thighs), chart + history.
- Mood + energy: two series, separate denominators.
- Sleep / Heart: watch-imported, charted anchored to today.
- Fasting: session hours vs. goal line.
- Supplements: percent-taken per day.
- Blood pressure: floating-bar chart, category label per reading, manual log sheet.
- Badges: seven derived families — streak, days logged, weight moved, workouts, fasts,
  longest fast, photos.
- Recap: rolling 7/30/365-day summary, shareable as a single-card PNG.
- Energy check-in: maintenance calories measured from 28 days of logged intake against the real
  weight trend, with a one-tap adjustment of the calorie target (in the Weight page's insight card,
  opening a full overlay that shows its working).
- Logging sheets: weigh-in (backdatable), measurements, progress photo, blood pressure.

## AI (Firebase AI Logic / Gemini)

- Photo food recognition (the food-logging path above).
- Daily insight — one line on Home, cached per day, falls back to three local rules.
- Coach — a chat screen told the day's numbers and what it doesn't know; history persisted,
  clearable with a confirm.
- Meal ideas (above).
- Talk-to-log (above) — a sentence parsed into several priced diary rows.
- Every AI path degrades to a manual or local-derivation path offline.

## Profile & settings

Sections: Goals · Units · Water · Fasting · Exercise · Reminders · Appearance · Connections ·
Home layout · Supplements · Food library · Workout routines · Data · About.

- Editable calorie and macro targets with "Reset to calculated" — which also undoes a target
  applied from the energy check-in; a manual calorie target reprices the split.
- Metric/imperial toggles; water glass size and daily goal; fasting goal hours; step goal.
- Light / dark / follow-device; mascot character and palette pickers.
- Supplements: name, dose label, times per day; edit and delete.
- Food library — your own foods, saved meals and recipes — and routine library: rename and
  delete (neither can log or start anything).
- Data export / import — JSON, `EXPORT_SCHEMA_VERSION` 15, import is all-or-nothing.

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

AIChip · AppBottomSheet · AppCard · AppTextField · AppTopBar · BadgeDot · BottomNavBar · Buttons ·
CalendarPanel · DateFormat · DiscardConfirmDialog · DockedFab · FoodItemRow · FullScreenState ·
GoalProjectionLine · HealthDisclosurePanel · HomeCardLayout · MacroBar · MacroInputGroup ·
MascotAvatar · MascotSpeechBubble · MicronutrientInputGroup · MicronutrientLegend ·
NumericStepperField · SegmentedToggle · SelectableCard · SheetDatePicker · StepProgressBar ·
WaterGlassRow.

Charts live in `:feature:progress/ui/shared/`: `DayBarChart` (zero-based) and `RangeBarChart`
(floating bars), plus the capture-and-share pair.

---

## Deliberately absent

Ruled out on the record. Don't re-propose without saying why the reasoning has changed —
each one is argued in `CLAUDE.md`.

- **Accounts, sign-in, server sync.** No auth system exists. Nothing may assume one.
- **Monetization.** No pricing, subscription or paywall has been decided.
- **A streak celebration toast.** The badge lighting up is the reward.
- **Mood, sleep, fasting, supplements and steps are not streak domains.** The streak is food,
  water, weigh-in, exercise — adding a fifth rewrites what past runs meant.
- **Badges in the recap.** No badge records when it was earned.
- **Sleep, heart, blood pressure, fasting, supplements in the recap.** Not enough scroll earned.
- **Blood pressure's Google Health scope.** Deliberately not requested. Health Connect reads it
  instead — that permission costs no CASA assessment.
- **Not exported:** saved meals, recipes, routines, `health_link`, coach history, steps, sleep,
  heart, running fasts.
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

`ROADMAP.md` holds the planned features — six specs, in order, each ready to implement.
`CLAUDE.md` → **Backlog** holds the outstanding work (FDC key proxy, Google Health verification
and the unpinned response fields, the final mascot illustration). Inline `ponytail:` comments
mark known ceilings and their upgrade paths.
