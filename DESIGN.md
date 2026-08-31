---
name: FitPulse
description: A cultivated, unhurried Material 3 system for daily body and nutrition tracking on Android.
colors:
  cultivated-green: "#37693D"
  on-cultivated-green: "#FFFFFF"
  cultivated-green-container: "#B8F1B9"
  on-cultivated-green-container: "#1E5027"
  sage-stem: "#516350"
  sage-stem-container: "#D4E8D0"
  on-sage-stem-container: "#3A4B3A"
  rain-barrel-teal: "#39656C"
  rain-barrel-teal-container: "#BDEAF3"
  on-rain-barrel-teal-container: "#1F4D54"
  blight-red: "#BA1A1A"
  greenhouse-paper: "#F7FBF2"
  on-greenhouse-paper: "#181D18"
  cold-frame: "#FFFFFF"
  potting-bench: "#F1F5EC"
  garden-bed: "#EBEFE6"
  turned-soil: "#E5E9E1"
  trellis-grey: "#727970"
  trellis-grey-faint: "#C1C9BE"
  moss-shade: "#424940"
typography:
  display:
    fontFamily: "Poppins, sans-serif"
    fontSize: "57sp"
    fontWeight: 400
    lineHeight: "64sp"
  headline:
    fontFamily: "Poppins, sans-serif"
    fontSize: "24sp"
    fontWeight: 400
    lineHeight: "32sp"
  title:
    fontFamily: "Poppins, sans-serif"
    fontSize: "16sp"
    fontWeight: 500
    lineHeight: "24sp"
  body:
    fontFamily: "Inter, sans-serif"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
  label:
    fontFamily: "Inter, sans-serif"
    fontSize: "12sp"
    fontWeight: 500
    lineHeight: "16sp"
    letterSpacing: "0.5sp"
rounded:
  hairline: "2dp"
  seam: "4dp"
  chip: "8dp"
  field: "12dp"
  panel: "16dp"
  card: "20dp"
  sheet: "28dp"
  pill: "999dp"
spacing:
  xs: "4dp"
  sm: "8dp"
  gap: "12dp"
  md: "16dp"
  lg: "24dp"
  xl: "32dp"
  xxl: "48dp"
components:
  button-primary:
    backgroundColor: "{colors.cultivated-green}"
    textColor: "{colors.on-cultivated-green}"
    typography: "{typography.title}"
    rounded: "{rounded.pill}"
    padding: "0 24dp"
    height: "48dp"
  button-secondary:
    backgroundColor: "transparent"
    textColor: "{colors.cultivated-green}"
    typography: "{typography.title}"
    rounded: "{rounded.pill}"
    padding: "0 24dp"
    height: "48dp"
  button-text:
    backgroundColor: "transparent"
    textColor: "{colors.cultivated-green}"
    rounded: "{rounded.pill}"
    padding: "0 16dp"
    height: "44dp"
  card:
    backgroundColor: "{colors.potting-bench}"
    textColor: "{colors.on-greenhouse-paper}"
    rounded: "{rounded.card}"
    padding: "16dp"
  input-text:
    backgroundColor: "transparent"
    textColor: "{colors.on-greenhouse-paper}"
    typography: "{typography.body}"
    rounded: "{rounded.field}"
    padding: "0 16dp"
    height: "48dp"
  chip-ai:
    backgroundColor: "{colors.rain-barrel-teal-container}"
    textColor: "{colors.on-rain-barrel-teal-container}"
    typography: "{typography.label}"
    rounded: "{rounded.chip}"
    padding: "4dp 8dp"
  chip-ai-on-accent:
    backgroundColor: "{colors.cold-frame}"
    textColor: "{colors.rain-barrel-teal}"
    typography: "{typography.label}"
    rounded: "{rounded.chip}"
    padding: "4dp 8dp"
  card-selectable-selected:
    backgroundColor: "{colors.cultivated-green-container}"
    textColor: "{colors.on-cultivated-green-container}"
    rounded: "{rounded.panel}"
    padding: "16dp"
  nav-item-selected:
    backgroundColor: "{colors.sage-stem-container}"
    textColor: "{colors.on-sage-stem-container}"
    typography: "{typography.label}"
    rounded: "{rounded.panel}"
    padding: "4dp 16dp"
  fab-docked:
    backgroundColor: "{colors.cultivated-green-container}"
    textColor: "{colors.on-cultivated-green-container}"
    rounded: "{rounded.pill}"
    height: "56dp"
---

# Design System: FitPulse

## Overview

**Creative North Star: "The Kitchen Garden"**

FitPulse is cultivated, not engineered. Everything in the palette grows out of green — a deep
forest for what's growing, a muted sage beside it, a pond teal for the water and the weather —
laid on a paper ground that is itself faintly green (#F7FBF2, not white). That off-white is the
system's quietest and most important decision: the app never has the clinical glare of a
medical dashboard, even in its most numeric moments.

The system is unhurried. Cards sit on the ground with 20dp corners and 16dp of interior air,
separated by a 12dp gap, inside 16dp of screen margin — a rhythm that never crowds. Type is a
two-family pairing: **Poppins** carries display, headline, and title with a geometric warmth,
**Inter** carries body and label with the neutrality a number-heavy screen needs. Every numeric
value — calories, weight, macros — renders with tabular figures (`TextStyle.tabularNums`) so
digits don't jitter as they update. Nothing is animated for spectacle; things settle.

Bibo, the default geometric mascot, is where the personality is concentrated: a rounded-square
`primaryContainer` body with dot eyes and a drawn mouth curve, in five states (Idle, Happy,
Celebrating, Sleepy, Thinking) and no other detail at any size. Four other buddies can be picked
in their place; they share his states and his mouth, never his silhouette or his fill. The system deliberately spends
its warmth in one place — the mascot, the copy, and the container fills — and stays disciplined
everywhere else. **`Color.kt` is the normative source for every value listed above:** it is a
frozen Material Theme Builder export carrying light and dark at standard, medium, and high
contrast, and the hexes in this file's frontmatter are a readable mirror of it, never a second
source of truth.

**Key Characteristics:**

- Green-tinted paper ground, never pure white, never a dark-neon black
- Two-family type: Poppins for structure, Inter for substance, tabular figures for all numbers
- Generous and soft: pill buttons, 20dp cards, 16dp interiors, 48dp touch targets
- Depth by tonal surface layering; shadow is reserved for things that genuinely float
- Semantic color is fixed app-wide — a macro is the same color in every chart on every screen
- Personality lives in the mascot and the copy, never in one-off visual exceptions

## Colors

A single green family stretched across roles, with a teal for anything that is water, weather,
or machine-made, and a paper ground that carries a trace of the same green.

### Primary

- **Cultivated Green** (`primary`): the growing thing. Filled buttons, the filled portion of
  step progress, the protein segment of every macro bar, on-track trend arrows, the text cursor.
- **Cultivated Green Container** (`primaryContainer`): Bibo's body, the docked FAB, and the fill
  of a selected `SelectableCard`. Where the app is being friendly rather than instructive.

### Secondary

- **Sage Stem** (`secondary`): the fat segment of every macro bar, and nothing decorative.
- **Sage Stem Container** (`secondaryContainer`): selection indicators — the bottom nav's
  selected pill, the selected chip in a `SegmentedToggle`. The colour of "you are here".

### Tertiary

- **Rain Barrel Teal** (`tertiary`): the carbs segment of every macro bar, and water.
- **Rain Barrel Teal Container** (`tertiaryContainer`): the AI/insight accent — the `AIChip` and
  Home's `AIInsightCard`. This is the **only** place `tertiaryContainer` is used as a card
  background in the entire app.

### Neutral

- **Greenhouse Paper** (`background` / `surface`): the ground every screen sits on.
- **Cold Frame** (`surfaceContainerLowest`): pure white, used only where something must read as
  lifted off an already-tinted surface — the `AIChip`'s on-accent variant.
- **Potting Bench** (`surfaceContainerLow`): the default card and bottom-sheet fill.
- **Garden Bed** (`surfaceContainer`): the bottom navigation bar.
- **Turned Soil** (`surfaceContainerHigh`): the unfilled track of a step progress bar.
- **Trellis Grey** (`outline`): field and secondary-button borders, 1dp.
- **Trellis Grey Faint** (`outlineVariant`) / **Moss Shade** (`onSurfaceVariant`): dividers and
  secondary text.
- **Blight Red** (`error`): field error borders and error text. Reserved — see the rule below.

### Named Rules

**The Fixed Macro Rule.** Protein is `primary`, Carbs is `tertiary`, Fat is `secondary` —
identical in every macro bar, chart, and legend in the app. This is not a per-screen decision,
and colour never carries the meaning alone: a macro colour always ships with its label.

**The One Accent Surface Rule.** `tertiaryContainer` as a card background means "this came from
the AI." It appears on the `AIChip` and the `AIInsightCard` and nowhere else. Spending it on an
ordinary card would erase the only visual signal the app has for machine-generated content.

**The Earned Red Rule.** `error` is for genuine failure and for a trend that is genuinely off
the user's goal — never for a direction (weight going up is not an error), never for a missed
day. Neutral is `onSurfaceVariant`; on-track is `primary`. Direction depends on the user's goal.

**The No Inline Hex Rule.** Every colour reads from `MaterialTheme.colorScheme`. A literal hex
in a component silently breaks the medium- and high-contrast schemes that `AppTheme` swaps in
from `UiModeManager.getContrast()`, and breaks dark mode outright.

## Typography

**Display Font:** Poppins (Google Fonts provider, `displayFontFamily`)
**Body Font:** Inter (Google Fonts provider, `bodyFontFamily`)

**Character:** Poppins is geometric and open — it gives headings and titles a rounded warmth
that matches Bibo without becoming cute. Inter is the workhorse underneath it: neutral, tight
in its metrics, and legible at label sizes where a number needs to be read at a glance. The
pairing is the whole typographic idea; there is no third face and no monospace.

### Hierarchy

The system is Material 3's baseline type scale with the two families mapped onto it —
`AppTypography` overrides only `fontFamily`, never a size or weight.

- **Display** (Poppins, 57/45/36sp): reserved. Not currently used on any screen.
- **Headline** (Poppins, 32/28/24sp): screen-level headings on full-screen moments — onboarding
  step headers, the welcome screen.
- **Title** (Poppins, 22/16/14sp): card headings, `SelectableCard` titles, button labels
  (titleMedium at SemiBold), section headers, the `FullScreenState` heading (titleLarge).
- **Body** (Inter, 16/14/12sp): all prose, field values, list item text. `bodySmall` carries
  secondary and error text.
- **Label** (Inter, 14/12/11sp): the `AIChip`, nav labels, field labels, step counters, and the
  `TextButton` (labelLarge). Uppercase is not used.

### Named Rules

**The Tabular Number Rule.** Any type style rendering a number that updates — calories, weight,
macros, water, streak counts — applies `TextStyle.tabularNums`. Proportional digits make a
counting number twitch as it changes, which reads as instability in a tracker.

**The Role, Not The Size Rule.** Text maps to a type-scale role. There is no hand-picked `sp`
value anywhere in the app, so system font scaling works everywhere without a layout audit.

## Layout

A single-column, vertically-scrolling mobile layout on every screen, under a bottom navigation
bar of four fixed tabs.

- **Screen horizontal padding: 16dp.** Uniform, on every screen.
- **Vertical gap between cards: 12dp.** Uniform.
- **Card interior padding: 16dp.** Bottom sheets pad `16dp` horizontally and `24dp` at the
  bottom, with `12dp` above the drag handle.
- **Spacing scale: 4 / 8 / 12 / 16 / 24 / 32 / 48dp.** No other value exists. A gap that wants
  to be 10 or 18 is a gap that hasn't picked a side.
- **Bottom nav is 80dp tall**; content that scrolls under a docked FAB reserves
  `DockedFabContentPadding` (72dp) so the last row is never trapped behind it.
- **Empty and status states fill the screen** (`FullScreenState`): centred, 16dp between
  elements, 32dp of padding when there are no actions and 24dp when there are.
- **Edge-to-edge with window insets applied.** Onboarding uses `safeDrawingPadding()`; content
  never sits under the status bar, navigation bar, or IME.

### Named Rules

**The Two-Value Rhythm Rule.** 16dp from the screen edge, 12dp between cards. Those two numbers
produce the app's entire vertical rhythm; a screen that invents a third margin will read as
belonging to a different app.

## Elevation & Depth

Depth is conveyed by **Material 3 tonal surface layering**, not by shadow. The five container
tones — `surfaceContainerLowest` → `surfaceContainerHighest` — do the work: a card is a lighter
tone than the ground it sits on, the nav bar is a step up from the content, a progress track is
a step up from the card. This is what lets the whole system invert cleanly into dark mode, where
those tones become steps *up* from near-black (#101510) rather than steps down from paper.

Shadow is permitted only on surfaces that genuinely float above content. In the shipped app that
is exactly one component: the docked FAB, which carries M3's default Level-3 FAB elevation (6dp)
and is the only real drop shadow in the app. Bottom sheets and dialogs may take a shadow if they
ever need one; a resting card may not.

### Named Rules

**The Floating-Only Shadow Rule.** If it scrolls with the content, it has no shadow. If it hovers
over the content — FAB, sheet, dialog — it may. A shadow on a resting card is the tell that
someone reached for depth instead of reaching for the next surface tone.

## Shapes

One continuous family of rounded rectangles, scaled by how much the element wants to be touched.

- **Pill** (`CircleShape` / 999dp): every button, the `SegmentedToggle` track and its selected
  chip, the docked FAB. Anything that is fundamentally an action is fully rounded.
- **28dp**: bottom-sheet top corners only.
- **20dp**: the shared `AppCard` — the app's most-repeated silhouette.
- **16dp**: `SelectableCard`, the bottom nav's selected pill.
- **12dp**: text fields.
- **8dp**: the `AIChip`.
- **4dp / 2dp**: the macro bar and the step progress bar — small enough to read as a seam rather
  than a corner.
- **Mascot**: Bibo's `width / 3`, so his corner radius scales with him and he reads identically
  at 32dp and 96dp. The other four buddies are their own silhouettes, drawn as paths.

Borders are 1dp `outline` (fields, secondary buttons) or 2dp `primary` (a selected
`SelectableCard`). There is no third border weight.

### Named Rules

**The Radius-Follows-Touch Rule.** The bigger and more tappable the thing, the rounder it is:
pill for actions, 20dp for cards, 8dp for chips, 4dp for indicators. A radius that doesn't fit
that ladder is picking a shape for its own sake.

## Motion

Motion is a scale like every other in this system: a fixed set of named durations in
`theme/Motion.kt`, and no call site inventing its own. A duration that wants to be 180 or 340 is
a duration that hasn't picked a side.

- **120ms — Feedback.** A tap acknowledging itself.
- **220ms — State.** A routine state change, and *every* exit. Exits are never slower than the
  entrance that preceded them.
- **300ms — Enter.** Something joining or leaving the layout.
- **450ms — Settle.** Reserved. Exactly one thing in the app is allowed it: Home's calorie ring.

Easings are Material 3's published curves, written out in `Motion.kt` — `Standard`,
`EmphasizedDecelerate` for confident arrivals, `EmphasizedAccelerate` for exits. No bounce, no
elastic; the system is unhurried, and things settle rather than spring.

### Named Rules

**The One Authored Moment Rule.** The calorie ring's sweep is the app's only authored entrance.
Everything else that moves is explaining feedback, a state change, or a spatial relationship. A
second 450ms flourish anywhere would spend the ring's meaning, not add to it.

**The Motion-Marks-Change Rule.** Animate what *changed*, never what merely exists. The badge dot
that just turned earned animates; five badges already earned when the screen opens simply draw.
This is why almost nothing here needs a "have I played this yet" flag — `animateColorAsState` and
`animateFloatAsState` don't animate their first composition, so they only ever animate a change
they witnessed.

**The Numbers Don't Move Rule.** Calories, weight, macros, and streak counts never animate.
`tabularNums` exists so digits stay still while they update; counting them would undo the reason
for the rule. When a figure and its visualisation disagree for half a second, the figure is the
one telling the truth — the ring catches up to the number, never the other way round.

**The Draw-Phase Rule.** An animated value is read inside a `graphicsLayer`, `Canvas`, or
`drawBehind` lambda, never in composition, so a running animation recomposes nothing. Passing a
`State<Float>` down instead of a `Float` is how that gets enforced at the call boundary. The one
sanctioned exception is a leaf `Icon`'s `tint`, which has no draw-phase equivalent.

**The No Loops Rule.** Nothing animates at rest. No idle mascot, no pulsing accent, no shimmer.
Every animation in the app is triggered by a state change and ends.

**The Remove-Animations Rule.** All motion is expressed through Compose's animation APIs, never a
hand-rolled `LaunchedEffect` + `delay`. Android's recomposer carries a `MotionDurationScale` from
`Settings.Global.ANIMATOR_DURATION_SCALE`, so the whole system collapses to instant cuts when the
user turns on **Remove animations** — for free, and only as long as nothing hand-rolls its own
clock.

## Components

### Buttons

- **Shape:** fully rounded pill (`CircleShape`) for all three variants.
- **Primary:** `primary` fill, `onPrimary` text, `titleMedium` at SemiBold, 24dp horizontal
  padding, 48dp minimum height.
- **Secondary:** transparent fill, 1dp `outline` border, `primary` text. Same metrics as primary.
- **Text:** no container, `primary` text, `labelLarge`, 16dp horizontal padding, 44dp min height.
- **Disabled:** 40% opacity via `graphicsLayer` on all three variants — never a grey substitute
  colour, which would break in dark mode and in the contrast schemes.

### Cards

- **`AppCard`** is the shared chrome: `surfaceContainerLow`, 20dp corners, 16dp interior padding,
  full width. Takes an optional `onClick` that uses `Surface`'s own clickable overload so the
  ripple clips to the corners.
- **Colour override** is allowed (the `color` parameter) but in practice only for the AI insight
  surface. A card is `surfaceContainerLow` unless there is a semantic reason it isn't.
- **No shadow, no border.** The tone step is the separation.

### Inputs / Fields

- **`AppTextField`:** 48dp height, 12dp corners, transparent fill, 1dp `outline` border, 16dp
  horizontal padding, `bodyLarge` value text, `labelMedium` `onSurfaceVariant` label above.
- **Error:** the border switches to `error` and a `bodySmall` `error` message appears beneath.
  The field is never colour-filled to signal error.
- **Cursor:** `primary`.

### Chips

- **`AIChip`:** 8dp corners, a 14dp sparkle icon, 4dp/8dp padding, `labelMedium`. Two variants —
  `Default` on `tertiaryContainer` for ordinary surfaces, `OnAccent` on `surfaceContainerLowest`
  with `tertiary` text for placement on a card that is *itself* a `tertiaryContainer`.
- **`SegmentedToggle`:** a pill track in `surfaceContainerLow` with 4dp inset; the selected
  segment is a `secondaryContainer` pill, 40dp tall, `labelLarge`.

### Navigation

- **`BottomNavBar`:** 80dp tall, `surfaceContainer` background, four fixed tabs. Selected tab
  shows a filled icon inside a 16dp-corner `secondaryContainer` pill (16dp/4dp padding) with
  `onSecondaryContainer` label; unselected shows an outlined icon and `onSurfaceVariant` label,
  both at `labelMedium`. The component holds no route knowledge — selection is passed in.
- **`DockedFab`:** extended FAB, `primaryContainer` fill, pill shape, 20dp icon,
  `titleSmall` SemiBold label. One FAB, one primary action, never stacked.

### Sheets

- **`AppBottomSheet`:** 28dp top corners, `surfaceContainerLow` container, standard 32×4dp drag
  handle in `onSurfaceVariant`, 16dp horizontal and 24dp bottom padding, scrim at
  `scrim` @ 32% alpha. Every sheet wires its own back handling so back closes the sheet, not the
  screen beneath it.

### Signature: the mascots (`MascotAvatar`)

A filled body carrying eyes, one accent and a drawn mouth curve, on a face square at 62% of the
body. Five states — Idle, Happy, Celebrating, Sleepy, Thinking — with Celebrating adding two `✦`
glyphs. **No other detail is added at any size.** The mascot is the app's only illustration and
its entire vocabulary; the final illustration is still outstanding and these geometric forms are
the placeholder standing in for it.

Five characters, picked in Profile → Appearance. Each varies on **four axes** — silhouette, fill
pair, eyes and one accent — because two characters differing only in outline read as the same
character badly drawn. What every one shares is the **mouth geometry and the five states**, so a
state reads identically whichever buddy is chosen and no character carries a meaning of its own:

| | Body | Fill / feature | Eyes | Accent |
|---|---|---|---|---|
| **Bibo** (default) | rounded square, radius `width / 3` | `primaryContainer` / `onPrimaryContainer` | round dots | — |
| **Pip** | teardrop — round base tapering to a soft point | `inverseSurface` / `inverseOnSurface` | rings | blush on the cheeks |
| **Zed** | hexagon, flat top and bottom | `surfaceContainerHighest` / `primary` | one visor slot across both eyes | antenna |
| **Momo** | dome — round top, softer base | `primary` / `onPrimary` | tall ovals | two ears |
| **Sprig** | capsule, narrow | `secondary` / `onSecondary` | round dots | stem and leaf |

Pip is the one character that spends its headroom on its own silhouette rather than on an
accent — the taper *is* the thing above its head — and the one whose fill inverts with the theme,
dark on a light scheme and light on a dark one. It deliberately does **not** take
`secondaryContainer`: the picker fills the selected cell with that, and the chosen buddy must not
disappear into it. Zed is the one character whose fill is a neutral and whose features carry the accent rather than
the other way round — a grey chassis with a lit face is what makes it read as a machine. **No
mascot fill takes a `tertiary` or `error` role:** `tertiaryContainer` is the AI accent and
`error` means genuinely off-track.

The pick travels as `LocalMascot`, provided once by `AppTheme`. **`character` is never passed
explicitly outside the picker** — every other caller writes `MascotAvatar(state = …)` and gets
the user's buddy for free. The whole avatar is one `Canvas`, never a clipped `Box`: that is what
lets an antenna or an ear sit *above* the head (`topInset`/`sideInset` carve the headroom, and
Bibo's are zero so it fills its box exactly as it always did) with nothing slicing the
Celebrating sparkles. The picker marks the selected buddy with a `secondaryContainer` cell rather
than an outline, since there is no per-character `Shape` to trace any more.

### Signature: `MacroBar`

An 8dp-tall, 4dp-radius bar split by *calorie* share, not gram share — protein `primary`, carbs
`tertiary`, fat `secondary`, in that order, every time.

## Do's and Don'ts

### Do:

- **Do** read every colour from `MaterialTheme.colorScheme` — the frozen `Color.kt` is the only
  source of values, and the contrast schemes depend on components never bypassing it.
- **Do** reuse `AppCard`, `PrimaryButton`, `AppTextField`, `AIChip`, `AppBottomSheet`, and
  `FullScreenState` rather than restyling a `Surface`. A new component used on two screens
  belongs in `:core:designsystem`; one used on one screen stays in that feature's `ui/components/`.
- **Do** apply `tabularNums` to any style that renders a changing number.
- **Do** keep spacing on the 4/8/12/16/24/32/48 scale, 16dp from screen edges, 12dp between cards.
- **Do** give every screen and component a single `@PreviewLightDark` wrapped in `Surface` inside
  `AppTheme`, and preview the meaningfully different variants (an `AIChip`'s two variants, a
  selected and unselected card). Dialogs and sheets need a scrim-behind-`Surface` wrapper or they
  preview invisible.
- **Do** hold 48×48dp minimum touch targets with at least 8dp between them.

### Don't:

- **Don't** write a hex literal in a component, regenerate `Color.kt`, or enable dynamic colour
  (Material You) — the fixed palette is the identity, and wallpaper-derived colour would dissolve
  the fixed macro assignments.
- **Don't** use `tertiaryContainer` as a card background for anything that isn't AI output.
- **Don't** put a drop shadow on a resting card; reach for the next `surfaceContainer` tone.
- **Don't** reassign a macro colour per screen, or let colour carry the macro meaning without a
  label beside it.
- **Don't** use `error` for a direction of travel, a missed day, or a broken streak.
- **Don't** invent a spacing value, a radius outside the ladder, a third type family, or a third
  border weight.
- **Don't** hand-pick an `sp` size instead of using a type-scale role — it breaks system font
  scaling silently.
- **Don't** add a second FAB, or spend the one FAB on a secondary action.
