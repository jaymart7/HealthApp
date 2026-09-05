# Handoff — Food diary redesign (Claude Code brief)

Paste into a session on the FitPulse repo. Design source of truth: `Food Diary Redesign.dc.html`
(artboards `1a`–`1h`) + `FoodDiary.dc.html` (live behaviour). Every value below is an M3 role
token and a dp from the 4/8/12/16/24/32/48 scale. No hexes. No new colour language.

## Scope in one line

Two pinned blocks instead of three (date row absorbs the filter; summary gains a slim scrolled
state), the three logging flows become labelled chips inside the scroll, each meal becomes a
`surfaceContainerLow` card, and Exercise moves out of the meal run into its own outlined block
below a labelled rule with a signed subtotal.

## What is NOT changing — do not touch

- No forward navigation past today. The forward chevron stays present and `enabled = false`.
- The filter still filters **the day's logged entries**. It is not a food search. No network call.
- A section's subtotal is always the whole section's total, even when the filter hides rows.
- Delete is still a swipe with an Undo snackbar. Snackbar must never sit under the FAB.
- Exercise subtotal is still calories *spent*; it raises the day's budget.
- Bottom nav (4 tabs) and the docked "Log food" FAB stay exactly as they are — `AppScaffold`
  is untouched by this work.
- `calcDailyTargets()`, the onboarding hand-off, water persistence, and all overlay/sheet
  destinations (add-entry, save-meal, exercise, calendar, meal ideas) keep their current
  contracts. This is a diary-surface redesign, not a data-layer change.
- Colour scheme files, `Theme.kt`, typography definitions: unchanged.

## Changes per file

### `feature/food/.../ui/diary/FoodScreen.kt`
- The pinned region drops from three children to two: `DiaryDateHeader` and `DiarySummaryBar`.
  The filter field is no longer a sibling — it lives inside `DiaryDateHeader`.
- Hoist the scroll state here and derive `summaryCollapsed`: `true` past 24dp of scroll,
  back to `false` only at `scrollTop == 0`, with an 8dp hysteresis band. Pass it to
  `DiarySummaryBar`. Do not reset it on day change.
- Hoist `filterQuery` and `filterExpanded` here (they were already query-hoisted; add the
  expanded flag). Closing the filter clears the query.
- Bottom content padding on the list: 148dp (nav 104dp + FAB overhang + 24dp).
- Screen horizontal padding stays 16dp; item spacing in the list 12dp.

### `ui/diary/components/DiaryDateHeader.kt`
- One 48dp row, two states.
  - **Collapsed (default):** back chevron 48×48dp `onSurface`; centre label (Poppins
    titleMedium + 18dp `expand_more`, `onSurface` / `onSurfaceVariant`) as a single 48dp
    button opening the calendar sheet; forward chevron 48×48dp `outlineVariant`,
    `enabled = false`; **new** trailing `filter_list` icon button 48×48dp `onSurfaceVariant`.
  - **Expanded:** the same 48dp row holds a 44dp field, radius 12dp, fill
    `surfaceContainerHigh`, 20dp leading `filter_list` in `onSurfaceVariant`, text
    bodyLarge `onSurface`, placeholder "Filter this day's foods…" in `onSurfaceVariant`,
    plus a 48dp `close` button. Crossfade 150ms; request focus on expand.
- New params: `filterExpanded: Boolean`, `onFilterExpandedChange`, `query`, `onQueryChange`.
- The old standalone filter row composable is deleted (see DiaryBody).

### `ui/diary/components/DiarySummaryBar.kt`
- New param `collapsed: Boolean`. Two layouts, crossfade + animated height, 200ms
  emphasized-decelerate.
- **Full:** 12dp internal gaps, 8dp bottom padding, 1dp `outlineVariant` divider beneath.
  Remaining figure headlineSmall/Poppins tabular `onSurface`; word "left"/"over" bodyLarge
  `onSurfaceVariant`; consumed line bodySmall tabular `onSurfaceVariant` right-aligned and
  reading `consumed / (goal + burned)`; `MacroBar` at 8dp; legend of three 8dp dots +
  bodySmall in `primary`/`tertiary`/`secondary`; optional micro line bodySmall
  `onSurfaceVariant`, hidden when Fiber/Sugar/Sodium are all zero; **new** exercise credit
  line — 16dp `directions_run` + bodySmall, both `onSurfaceVariant`, "N kcal from exercise
  added to today", hidden when burned = 0.
- **Collapsed (~44dp):** titleLarge figure + bodySmall word, bodySmall consumed line,
  `MacroBar` at 4dp, no legend, no micro line, no credit line.
- Over budget: identical roles and type; only the word flips to "over" and macro fills clamp
  at 100%. **No `error` role anywhere in this composable.**
- Animate the remaining figure as a value count (320ms) and macro fills as width (320ms).

### `ui/diary/components/DiaryBody.kt`
- Insert a new quick-action chip row as the first list item: three equal-weight items, 8dp
  gap, 48dp tall, radius 12dp, 1dp `outlineVariant` border, transparent fill, 20dp icon
  `onSurfaceVariant` + labelLarge `onSurface`. Labels: "Say it" (`mic`), "Scan"
  (`qr_code_scanner`), "Photo" (`photo_camera`). They keep their existing destinations.
  Never filled — the FAB owns the filled treatment.
- Remove the old three bare icon buttons and the filter row from the pinned area.
- Add the same three actions to the FAB quick-action sheet so they stay one tap away once the
  chips scroll off (`QuickActionSheet` in `core/designsystem`-adjacent app shell — sheet rows
  only, no layout change).
- Item order: chips → `DiaryWaterRow` → four `MealSection`s → exercise label/rule →
  `ExerciseSection`. 12dp between items, 24dp above the exercise label row.
- The exercise label row is new: labelSmall uppercase, 0.08em tracking, `onSurfaceVariant`,
  followed by a 1dp `outlineVariant` rule filling the remaining width. Copy: "Adds to today".

### `ui/diary/components/MealSectionHeader.kt`
- Header now sits on a card, so drop any self-drawn divider or background. 48dp tall,
  padding 4dp start / 8dp end.
- Chevron 22dp `onSurfaceVariant`, rotation animated 0° ⇄ −90° over 200ms.
- Name Poppins titleMedium `onSurface`; subtotal bodySmall tabular `onSurfaceVariant`,
  printed only when non-zero.
- Bookmark (`bookmark_add`, 20dp, `onSurfaceVariant`) and add (`add`, 22dp, `onSurface`)
  as 48×48dp icon buttons; bookmark hidden when the section is empty.
- New param `leadingIcon: ImageVector? = null` so `ExerciseSection` can pass
  `directions_run` without a fork, and `subtotalText: String` so the sign can be supplied
  by the caller rather than formatted inside.
- Content description: "{name}, {n} kilocalories, expanded|collapsed, button".

### `ui/diary/components/MealSection.kt`
- Wrap in a `Surface` / `Card`: `surfaceContainerLow`, radius 16dp, no border, no elevation.
- Rows keep 40dp start padding; add 4dp bottom padding inside the card after the last row.
- Expand/collapse animates content height 200ms; no per-row fade.
- Empty-expanded line bodySmall `onSurfaceVariant`, 6dp/14dp padding, 40dp start:
  "Nothing logged here yet." or, when the filter hid the contents, "Hidden by the filter."
  The header subtotal above still shows the real number. **Suppressed when the whole day is
  empty** — there the mascot block is the only line that speaks; the per-section line is for a
  single empty section on an otherwise populated day.

### `ui/exercise/components/ExerciseSection.kt`
- Container is now visually distinct without new colour: transparent on `surface` with a 1dp
  `outlineVariant` border, radius 16dp — the inverse of the meal cards' filled-no-border.
- Header uses `MealSectionHeader` with `leadingIcon = directions_run` and
  `subtotalText = "+$burned kcal"`. No bookmark button.
- Rows: activity type bodyLarge `onSurface`, "45 min · Morning run" bodySmall
  `onSurfaceVariant`, and for strength a third bodySmall `onSurfaceVariant` line of lifts.
  Calories titleMedium tabular `onSurface`.
- Content description: "Exercise, {n} kilocalories burned, added to today's budget".

### `ui/diary/components/EmptyDiaryDay.kt`
- Now rendered between the Lunch and Dinner cards rather than above all sections; all five
  headers still render, with no subtotals.
- Mascot 48dp **Sleepy**, per the existing empty-diary state — unchanged. 16dp gap, line 1 Poppins titleMedium `onSurface` "Nothing logged yet
  today.", line 2 bodySmall `onSurfaceVariant` with the time-of-day-aware meal suggestion.
- Padding 16dp vertical / 12dp horizontal, card-less.

### `ui/diary/components/DiaryWaterRow.kt`
- Unchanged in behaviour; two visual notes. Label Poppins titleSmall `onSurface`, count
  bodySmall tabular `onSurfaceVariant`. Glasses fill the row width in one flex row with 6dp
  gaps and never wrap to a second line (the current build wraps at 8 glasses — fix this);
  each is a 48dp target with a 26dp icon, filled = `primary` with the icon FILL axis at 1,
  empty = `outlineVariant` FILL 0.

### `core/designsystem` — shared components

- **`MacroBar`** (existing, extend): add a `height: Dp` parameter (8dp / 4dp) and keep the
  frozen role mapping protein=`primary`, carbs=`tertiary`, fat=`secondary`, track
  `surfaceContainerHigh`, fills clamped at 100%. Used by the summary in both states plus
  Home and Profile — extend, do not fork.
- **`FoodItemRow`** (existing, adjust): padding 10dp vertical / 16dp end / 40dp start,
  bodyLarge name, bodySmall secondary line, titleMedium tabular calories. Swipe threshold
  96dp; reveal field `errorContainer` with a `delete` icon in `onErrorContainer` — the only
  use of the error role on this screen. Spring-back 220ms. Add a long-press "Delete"
  context action mirroring the swipe for switch access / TalkBack.
- **`WaterGlassRow`** (existing): the no-wrap + FILL-axis change above belongs here, not in
  the screen.
- **NEW shared component: `SectionCard`** — the `surfaceContainerLow` / radius 16dp / no
  elevation container with an optional 1dp `outlineVariant` outlined variant. Used by the
  four meal sections, the exercise block, and (≥2 screens) Progress's subject cards. Put it
  in `core/designsystem`.
- **NEW shared component: `LabelledActionChip`** — 48dp, radius 12dp, outlined,
  icon + labelLarge. Used by the diary chip row and reusable by Progress's range actions.
  `core/designsystem`.
- **NEW shared component: `SectionRule`** — labelSmall uppercase + 1dp `outlineVariant`
  rule filling the remaining width. Used here for "Adds to today"; a second user exists on
  Profile's grouped settings. `core/designsystem`.
- **Screen-local, do not promote:** the collapsing-summary crossfade logic, the exercise
  credit line, and the empty-day placement rule. All three are diary-specific.

## Behavioural acceptance checks

1. Opening the Food tab shows the full summary; a food row is visible without scrolling.
2. Scrolling 1 row collapses the summary to the slim bar; the remaining figure is never absent.
3. Returning to the top restores the full summary; a scroll-up mid-list does not.
4. Filter icon → field in place; the header height does not change; closing clears the query.
5. Filtering a section to zero rows leaves its subtotal at the real total.
6. Exercise reads as a credit: label + rule above, outlined container, run icon, "+N kcal",
  and the summary restates it. It is never mistaken for a fifth meal.
7. Over budget shows the overage as "N over" (artboard `1e`: "252 over", 2313 / 2061 kcal) with no red, no error surface, no message.
8. Snackbar sits 184dp from the bottom and is never under the FAB.
9. At 200% font scale nothing clips: summary figure and consumed line stack, legend wraps,
  all sections still collapsible.
10. Dark mode: meal cards lift one step from `surface`; the exercise outline stays lighter
   than the card fills.
