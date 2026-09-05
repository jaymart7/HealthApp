# Redesign: FitPulse — Food Diary screen (Android, Jetpack Compose, Material 3)

I'm attaching screenshots of the current screen. I want a visual/UX redesign of it.
Ask me questions if anything is ambiguous before producing final artboards.

## The app

FitPulse — an Android health app. Two pillars: body tracking (weight, measurements,
progress photos) and nutrition tracking (calories/macros, AI photo food logging).
Kotlin + Jetpack Compose + Material 3, phone-first. There's a friendly geometric
mascot ("Bibo") used sparingly in empty states.

The Food tab is the diary: the screen users spend the most time in. It's one
vertically scrolling column inside the app scaffold (bottom nav bar + a docked
"Log food" FAB bottom-right; both stay visible on this screen).

## What's on the screen, top to bottom

**1. Date header** (pinned, above the scroll)
`< [ Today ] >` — chevron left/right step one day. The centre label reads "Today" /
"Yesterday" / "Aug 27, 2026" and is tappable to open a calendar sheet. Forward
chevron is disabled on today (no planned meals — the diary never goes into the future).

**2. Filter row + three quick actions** (pinned)
A text field, placeholder "Filter this day's foods…", plus three 48dp icon buttons
in a row beside it: microphone (say what you ate), barcode (scan a package), camera
(photograph a plate). These three launch separate full-screen flows.

**3. Day summary bar** (pinned — the most important element on the screen)
- Big number + word: "**1584** left" (or "**212** over" when past the goal). Poppins,
  headlineSmall, tabular figures.
- Right-aligned secondary line: "940 / 1941 kcal".
- A single horizontal three-segment macro bar (Protein / Carbs / Fat) that fills as
  the day fills.
- A legend row of three dot+label pairs: "Protein 62/146g", "Carbs 88/194g",
  "Fat 31/65g".
- An optional fourth micronutrient line (Fiber / Sugar / Sodium), hidden when all zero.

**4. Water row** (start of the scroll, card-less, sits in the flow)
Label "Water" left, "3 / 8 · 750 ml" right, and beneath it a row of tappable glass
icons that fill up to the count.

**5. Empty-day state** — when the whole day has nothing on it: the mascot at 48dp
beside two lines, "Nothing logged yet today." / "Dinner is a good place to start."
(the suggested meal is time-of-day aware). Meal headers still render above and below it.

**6. Four meal sections** — Breakfast, Lunch, Dinner, Snacks. Each is:
- A collapsible 48dp header row: chevron (down/right) · meal name · subtotal
  "470 kcal" (printed only when non-zero) · a small circular bookmark button
  ("save this section as a meal", hidden when the section is empty) · a small
  circular "+" button.
- When expanded, its entries. Each entry row is: food name (bodyLarge) with a
  secondary line beneath ("1 cup · P20 C8 F4"), and the calorie figure right-aligned
  (titleMedium, tabular). Rows are indented to clear the header chevron. Tapping a
  row reopens it for editing; swiping it deletes with an Undo snackbar.
- When expanded and empty: one grey line, either "Nothing logged here yet." or,
  if the filter hid its contents, a different line saying so (the subtotal above
  still shows the real number).

**7. Exercise section** — a fifth section with the identical header, but its subtotal
reads "903 kcal **burned**" and it means the opposite of the four above (it raises
the day's calorie budget rather than filling it). Its rows show activity type,
"45 min · Morning run", and for strength workouts a third line listing the lifts.

**8. Overlays** (not part of the artboards unless you want to show one): an add-entry
bottom sheet, a save-meal sheet, an exercise sheet, a calendar sheet, and a
full-screen "meal ideas" overlay.

## What I want from the redesign

1. **Hierarchy.** Three pinned blocks (date, filter+actions, summary) eat a lot of
   the viewport before a single food row appears. Fix that without hiding the day's
   remaining-calorie figure, which is the number people open this screen for.
2. **The five section headers.** They currently read as five near-identical grey
   rows; the exercise one means something different from the other four and barely
   says so. Make the difference legible without a new colour language.
3. **Density and rhythm.** A full day is ~15 rows across five sections. Make it
   scannable.
4. **The three quick-action icons.** Three bare icons beside a text field is
   ambiguous — each launches a different logging flow. Consider a better home for
   them, but they must stay reachable in one tap from the diary and must not be
   confused with the FAB.
5. **The empty day and the barely-started day** — first-run is the most common state.
6. **Micro-interactions worth specifying:** expand/collapse, swipe-to-delete, the
   summary bar animating as a row is logged.

## Hard constraints — please design inside these

- **Material 3, and the colour palette is frozen.** It's a complete Material Theme
  Builder export (light + dark + medium/high contrast). Work in M3 role tokens —
  `surface`, `surfaceContainerLow/High`, `onSurface`, `onSurfaceVariant`, `primary`,
  `secondary`, `tertiary`, `outlineVariant`, `error`. Don't invent hexes, don't
  propose a new brand palette, no dynamic/Material You colour.
- **Semantic colour is fixed app-wide:** Protein = `primary`, Carbs = `tertiary`,
  Fat = `secondary`. Identical in every bar, chart and legend across the app.
  `tertiaryContainer` is reserved for the AI/insight accent and appears at most once
  per screen. `error` is reserved for genuine failure — being over your calories is
  **not** an error and must not read as a scolding.
- **Typography:** Poppins for Display/Headline/Title, Inter for Body/Label. All
  numeric values use tabular figures.
- **Spacing scale is 4 / 8 / 12 / 16 / 24 / 32 / 48 dp only.** Screen horizontal
  padding 16dp, 12dp vertical gap between cards. No other values.
- **Touch targets ≥ 48dp**, and everything needs a sensible screen-reader story.
- **Light and dark must both be specified**, and the layout must survive large font
  scales.
- Bottom nav + docked FAB stay; leave bottom padding clear for them.

## Please don't redesign away

These are settled product decisions, not accidents:
- No forward navigation past today.
- The filter field filters **the day's logged entries** — it is not a food search.
- A section's subtotal is the whole section's, even when the filter hides rows.
- Delete is a swipe with an Undo snackbar (which must never sit under the FAB).
- The exercise subtotal is calories *spent*.

## Deliverables

- Artboards for: the full day (populated), the empty day, a collapsed-sections view,
  and the over-budget summary state — each in light and dark.
- Redlines: spacing, type roles, and which M3 colour role each surface/text uses.
- A short written rationale for anything you moved or removed.

## Handoff

When the design is done, produce a **handoff brief for Claude Code** that I can paste
into a terminal session on the real repo. It should:
- List the changes per file, using this existing structure:
  `feature/food/.../ui/diary/FoodScreen.kt`, and in `ui/diary/components/`:
  `DiaryBody.kt`, `DiaryDateHeader.kt`, `DiarySummaryBar.kt`, `DiaryWaterRow.kt`,
  `MealSection.kt`, `MealSectionHeader.kt`, `EmptyDiaryDay.kt`, plus
  `ui/exercise/components/ExerciseSection.kt`; shared components live in
  `core/designsystem` (`FoodItemRow`, `MacroBar`, `WaterGlassRow`).
- Name every value as an M3 role token and a dp from the scale above — never a hex.
- Flag anything that would need a **new shared component** in `core/designsystem`
  (used by ≥2 screens) versus a screen-local one.
- Be explicit about what is *not* changing, so nothing gets rewritten by accident.
