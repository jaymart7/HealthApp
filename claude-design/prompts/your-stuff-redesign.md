# Claude Design prompt — Your stuff: Supplements, Food library, Workout routines

Paste everything below into Claude Design, together with the three attached screenshots
(Supplements / Food library / Workout routines).

---

Redesign three screens of **FitPulse**, an Android (Kotlin + Jetpack Compose, Material 3) body-
and nutrition-tracking app. They are the three rows under **Your stuff** on the Profile tab, and
screenshots of the shipped versions are attached. They work, and they are the least designed
surfaces in the app: the same grey card repeated down a column, two 44dp icon buttons per row,
a body-size name over a caption, and nothing that says which list you are in or what these
things are *for*.

Give me **one canvas with three artboards at 412×915 (phone)**, plus a dark-mode variant of each,
plus the shared sheets called out at the bottom:

1. **Supplements** — the authored list Home ticks each day
2. **Food library** — my foods, saved meals, recipes
3. **Workout routines** — saved routines and the weekday plan

## Design system (binding — do not invent tokens)

- Material 3, a **fixed** color scheme, no dynamic color. Token values are in
  `claude-design/project/theme.js` (`THEMES.light` / `THEMES.dark`). Use token names only;
  never a raw hex in the markup.
- Type: **Poppins** for Display/Headline/Title, **Inter** for Body/Label. Every number
  (kcal, macros, sets, reps, doses, counts) in **tabular figures**.
- Spacing scale, nothing else: `4 / 8 / 12 / 16 / 24 / 32 / 48`. Screen horizontal padding 16,
  vertical gap between cards 12.
- Macro colors are fixed app-wide and must not be re-assigned: **Protein = `primary`,
  Carbs = `tertiary`, Fat = `secondary`**. `tertiaryContainer` is reserved for AI/insight
  surfaces — do not use it as a card background here.
- **Tap targets are 48dp minimum.** The shipped rows use 44dp icon buttons; that is a known
  defect, not a pattern to match. If a control needs to look smaller, use the split the app's
  stepper already ships: a 48dp touch box around a 40dp visual.
- Both light and dark fully specified.

## Chrome these screens sit in (don't redesign it, do design around it)

- Each screen is a Nav3 route one level above Profile. The **top app bar is drawn for them** —
  a back arrow and the screen title. Don't design a bare in-content title; design the content
  under a top bar that already exists.
- At phone width there is **no bottom nav and no FAB on these routes** — the whole 412×915 below
  the top bar is yours. At ≥840dp the same screen renders as a **detail pane beside Profile**,
  with the nav rail and the docked FAB still on screen, so the layout must survive being
  ~500dp wide and must not assume the full window.
- Both dimensions matter for where a persistent action can live. Supplements needs one; see below.

## What all three share today, and what I want from that

All three are the same component: a `surfaceContainerHighest` rounded card, `bodyMedium` name,
`bodySmall` summary, an optional `labelSmall` contents line, then rename/edit and delete icon
buttons. Three screens, one row, no differentiation.

Design **one row family** — not three unrelated cards, and not one card that ignores what each
list actually holds. I want to see:

- A clear **primary line** (the name) that is typed as a title, not as body text.
- The **figures** (kcal, sets, dose) treated as data, not as a grey caption — this is the one
  place the user audits what they saved.
- The **contents line** ("Greek yogurt, Oats, Black coffee" / "Bench press 3×8, Dip 2×10") kept.
  It exists so a row isn't deleted blind. Single line, ellipsized.
- Destructive and non-destructive actions that don't look identical. Delete currently has the
  same weight as rename, sitting right next to it. Fix that — an overflow, a swipe, a tinted
  icon, your call, but argue it in the artboard.
- Row anatomy specified **once**, with per-screen variants, rather than redrawn three times.

## Screen 1 — Supplements

Currently: a scrolling column of rows — name ("Vitamin D") over a summary ("2000 IU · once
daily") — each with edit and delete, then a full-width **Add supplement** primary button at the
bottom of the content (it scrolls away). Empty state is an inline sleepy mascot + one line of
body, deliberately *not* full-screen, because the Add button has to stay reachable.

Keep:
- Name, dose label (free text, optional — "2000 IU", "5 g", "one scoop"), times per day (1–6).
- **No tick, no dose logging here.** A tick belongs to a day and Profile has none; Home's card is
  where a dose is logged. The row must not look tappable-to-complete.
- Add, edit and delete all reachable.

Fix:
- The Add button scrolling away is the main bug. Give the action a fixed home that works at both
  widths — a docked bottom button, a small FAB, or a top-bar action. Show what you picked and
  what it does when the list is 20 rows long.
- Times-per-day is invisible unless it's ≠1 and buried in a grey caption. A supplement taken twice
  daily is materially different from one taken once; show that in the row.
- The list has no order and no grouping. Propose one (by time of day? by name? user-ordered with
  drag handles?) — and if the answer is "none, it's short", say so rather than inventing sections.
- Redesign the **empty state** so it reads as an invitation rather than a truncated screen, while
  keeping the add action on screen. Copy in the app today: "Nothing here yet. Add what you take
  and it shows up on Home each day."

## Screen 2 — Food library

Currently: a lazy list with three plain `labelLarge` section headers — **My foods**, **Saved
meals**, **Recipes** — each followed by its rows, with empty sections omitted entirely. Rows:

- *My foods*: "Mum's adobo" / "420 kcal · 1 serving" / "P 28g · C 12g · F 28g"
- *Saved meals*: "Usual breakfast" / "3 items · 540 kcal" / the item names
- *Recipes*: "Chili" / "180 kcal per serving · makes 4" / the ingredient names

Rename and delete only. Full-screen empty state (sleepy mascot, "Nothing saved yet", one line).

Keep:
- The three sections and their order — My foods first, it is the list the user authored
  deliberately and the one food search leads with.
- Empty sections stay omitted. A section header over nothing is noise.
- **No logging.** Logging needs a meal slot and a day; this screen has neither. Rows are not
  "tap to add".
- The list is unbounded — this screen is the only way to reach saved items past the newest-N
  windows the add-entry sheet shows. It has to work at 200 rows.

Fix:
- The three headers are visually identical to nothing. Give the sections structure: sticky
  headers, a count per section, or a segmented filter at the top — show me which reads better at
  200 rows and say why.
- The three row types carry genuinely different data and currently look the same. Differentiate:
  a food is a *unit price* (kcal per stated portion + macros), a saved meal is a *bundle*, a
  recipe is a *yield*. The macro line especially — P/C/F as bare grey text is the one place in
  the app macros aren't drawn with their fixed colors.
- No search, no filter. At 200 saved items that's the real defect. Design a search affordance
  that fits under an existing top bar (and decide whether it filters across sections or within).
- Rename via a pencil icon is the only edit here, and it's easy to read as "edit this food" —
  which it isn't (a food's fields are corrected by re-saving from the add-entry sheet). Make the
  row say what it can and can't do without a tooltip.
- Empty state copy today: "Nothing saved yet" / "Save a food or a meal from the diary, or build a
  recipe, and it shows up here."

## Screen 3 — Workout routines

Currently: the same rows in a plain scrolling column — "Push day" / "3 lifts · 9 sets · Mon · Wed
· Fri" / "Bench press 3×8, Overhead press 3×8, Dip 2×10" — with rename and delete, and **under
each row, full width, a seven-cell weekday picker**: pill chips `M T W T F S S`, selected =
`secondaryContainer` fill + `primary` 1.5dp border, unselected = `surface` + `outlineVariant`
border, 40dp tall, equal widths. Toggling a day writes the whole mask immediately, no save.
Full-screen empty state. Unscheduled routines read "No days set" in the summary rather than
trailing a blank.

Keep:
- The inline weekday picker and its immediate, save-less toggle. The plan it writes is rendered on
  Home; this is the only place it's authored.
- Seven equal cells, full width, the repeated `M T W T F S S` initials — the full day name rides a
  `contentDescription`, so the initials can stay decorative, but the **selected/unselected states
  must survive a contrast swap without relying on the letter**.
- The days also appear inside the summary line ("Mon · Wed · Fri"). That duplication is
  deliberate-ish but worth questioning: if the picker reads clearly, the summary can drop them.
- No start, no logging — a routine is *started* from the strength screen, not here.

Fix:
- The picker is the most interactive thing on the screen and looks like the least. It's a bordered
  strip glued to the bottom of a grey card. Give the row a structure where the plan belongs to the
  routine rather than hanging off it — and keep the 40dp cells inside 48dp touch.
- "3 lifts · 9 sets · Mon · Wed · Fri" crams three different kinds of fact into one grey line.
  Separate the volume figures from the schedule.
- A routine with no days set is the common case for anything saved before the plan existed. Design
  that state so it points at the picker instead of just saying "No days set".
- Show the **week at a glance**: with 4–6 routines, the user's actual question is "what does my
  week look like", and today they have to read six pickers to answer it. Propose a compact summary
  — a header strip, a week row, something — and mark it clearly as an addition so I can cut it.
- Empty state copy today: "No routines yet" / "Log a strength workout, tap 'Save as routine', and
  it shows up here."

## Shared sheets (design each once, as its own artboard, with a scrim behind it)

1. **Rename sheet** — a bottom sheet titled "Rename", one text field seeded with the current name,
   one full-width primary Save, disabled while blank. Used by food library and routines.
2. **Supplement edit sheet** — the same sheet shape, used for both add and edit: name, dose
   (optional free text), and a read-only ± stepper for times per day (1–6). Full-width Save,
   disabled while the name is blank.
3. **Delete confirm dialog** — one dialog, three bodies. Title is "Delete <name>?"; Delete /
   Keep. The bodies carry the reassurance and must stay legible: "This saved meal is removed for
   good. Anything already logged from it stays in your diary." / "This routine is removed for
   good. Workouts you logged from it stay in your diary." / "It leaves your daily list. Days you
   already ticked keep their record."

All three are used by more than one screen, so specify them once and note which screens draw them.

## Out of scope

Don't redesign: the Profile tab itself, Settings, the add-entry sheet, the strength/workout
screens, Home's supplement card, the bottom nav, the nav rail, or the FAB. Don't introduce a new
color token, a new spacing value, or a second typeface. Don't add a way to log, tick or start
anything from these three screens — that division is deliberate and load-bearing.
