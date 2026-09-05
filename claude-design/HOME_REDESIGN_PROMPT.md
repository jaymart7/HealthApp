# Prompt for Claude Design — redesign the FitPulse Home screen

Paste this into Claude Design together with the attached Home screen screenshots.

---

## The ask

Redesign the **Home tab** of FitPulse, a shipping Android app (Kotlin + Jetpack Compose,
Material 3). The screenshots attached are the current Home, scrolled top to bottom. Everything
on them is real and working — this is a **visual and layout redesign of surfaces that already
exist**, not a feature proposal.

Deliver `HomeRedesign.dc.html` in the same project, following the conventions in
`Home.dc.html`: the same app shell (bottom nav + docked FAB + quick-action sheet), the same
token imports from `theme.js`, light **and** dark rendered from those tokens. Show the populated
Home; if a card's alternative state changes the layout materially, show it as a second artboard.

## What Home is

A single vertically scrolling column, 16dp screen padding, 12dp between cards. Two pinned
elements at the top, then a **user-reorderable block of 15 cards** below them.

**Pinned, never moves, never hidden:**
1. **Mascot greeting** — time-of-day greeting, mascot avatar (a small animated geometric buddy),
   and an AI chip that is the app's *only* door to the AI coach.
2. **AI daily insight** — one sentence, dismissible, `tertiaryContainer` background. Absent on
   day one and on days the model has nothing to say. Its collapse-on-dismiss is what stops the
   cards below it jumping, so whatever replaces it must still collapse in place.

**The reorderable block, in default order.** The user can drag these into any order and hide any
of them from Profile → Home layout, so **each card must read correctly in any position and with
any neighbour** — no design that depends on a card sitting first, last, or beside a particular
sibling, and no card that is only legible as half of a pair.

| Card | What it shows | When it's absent |
|---|---|---|
| Calories | Ring: consumed vs. budget, exercise credit | no profile |
| Streak | Day count + earned badges | never |
| Water | Tappable glass row, count vs. goal | never |
| Fasting | Live elapsed timer or a start invitation | never |
| Workout | Today's planned routine + the week's strip | no routine has days set |
| Sleep | Last night's duration + stages | no watch data |
| Steps | Steps vs. goal + kcal credit | no watch data |
| Heart | Day's average and lowest bpm | no watch data |
| Blood pressure | Latest reading + its category | nothing logged ever |
| Mood | Two tappable 1–5 rows (mood, energy) | never |
| Cycle | Cycle day, prediction, tappable flow meter | tracking switched off |
| Supplements | One tappable counter row per supplement | list empty |
| Weight | Trend vs. 7 days ago + goal projection line | never |
| Macros | Protein / carbs / fat bar vs. targets | no profile |
| Progress photo | Days since the last photo + a capture door | never |

## Hard constraints — do not design around these

- **The palette is frozen.** Use only the Material 3 token roles already in `theme.js`
  (`primary`, `secondary`, `tertiary`, their containers, `surface`, `surfaceContainerLow`,
  `outlineVariant`, `error`, …). No new hues, no gradients invented outside the scheme, no
  hardcoded hex. It must survive light, dark, and two higher-contrast schemes.
- **Fixed semantic colours:** Protein = `primary`, Carbs = `tertiary`, Fat = `secondary`,
  everywhere. AI/insight accent = `tertiaryContainer` on `onTertiaryContainer`, and that is the
  **only** card background using it — one such card per screen.
- **Trend arrows:** `onSurfaceVariant` neutral, `primary` on-track, `error` only genuinely
  off-track. Direction depends on the user's goal — never green-for-loss/red-for-gain.
- **Type:** Poppins for display/headline/title, Inter for body/label. Numeric values use tabular
  figures.
- **Spacing scale is 4 / 8 / 12 / 16 / 24 / 32 / 48 only.** 16dp screen padding, 12dp between
  cards. No other values.
- **Cards live under a docked FAB** — leave bottom content padding for it.
- **A card with no data is hidden, never zeroed.** Don't design empty states that report 0 for
  the gated cards above; a Sleep card with no watch simply isn't there.
- **No celebration or congratulation surfaces.** A badge lighting up is the whole reward.
- **Every figure is one the app measured.** No invented averages, no projections beyond the
  weight one, no "score". Ranges and bands may be *named*, never graded.
- Tappable cards must stay tappable in place: water, mood, energy, cycle flow and supplements
  are all logged directly on Home with one tap, and a mis-tap is corrected by the same gesture.
  Nothing may move that interaction behind a sheet or a detail screen.

## What we're hoping the redesign fixes

Say plainly if you disagree with any of these — an argued "leave it" is a useful answer.

1. **Fifteen equal-weight cards is a long, flat scroll.** The day's most-used numbers
   (calories, macros, water) sit in the same visual register as a blood-pressure reading from
   last week. Consider hierarchy, grouping, or density — but any grouping must survive the user
   reordering and hiding cards arbitrarily.
2. **Density.** Several cards spend a full-width card on one number.
3. **Scanability at a glance** — Home should answer "how is today going" before any scrolling.
4. **The pinned pair reads as two separate blocks** at the top; they may want to be one.

## Out of scope

New features, new data, new tabs, changes to the bottom nav or the FAB, and anything that needs
a schema change. If a redesign idea needs data the app doesn't have, name it separately rather
than drawing it.
