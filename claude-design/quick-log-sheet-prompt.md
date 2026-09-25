# Prompt — redesign the Quick log bottom sheet

> Paste everything below the line into Claude Design. Source of truth for the current sheet:
> `feature/food/src/main/java/ph/mart/healthapp/feature/food/ui/quicklog/` (`QuickLogSheet.kt`,
> `QuickLogState.kt`, `components/QuickLogConversation.kt`, `components/QuickLogInputBar.kt`).

---

## The ask

Redesign **Quick log**, the bottom sheet that opens from the centre FAB in FitPulse (an Android
fitness + nutrition tracker, Jetpack Compose, Material 3). Produce one phone frame (412 × 915dp)
per state listed under **States**, in **light and dark**, with the keyboard shown wherever it is up.
Keep every behaviour listed under **Behaviour to keep** — the redesign is visual hierarchy, layout
and motion, not a new feature set.

## What the sheet is

One AI text field that reads a sentence about what the user **ate or did** — "two eggs and toast,
then a 30 min run, 3 glasses of water, weighed 72" — and logs all of it from one confirmation.
It is a small conversation, not a form:

1. The user types (or speaks, or attaches a photo of the plate) and sends.
2. If the sentence left out the thing the estimate depends on ("how much rice?", "how long was the
   run?"), the model **asks back** — at most twice — and the user answers in the same field.
3. The model returns **rows**: foods (priced with calories + macros), activities (kcal burned),
   glasses of water, a weigh-in. The user can remove a row, nudge a food's portion, pick the meal
   slot, or type a correction ("make it two cups") which re-reads the whole conversation.
4. **Log** writes everything, the sheet closes, and the app shows a "Logged · Undo" snackbar
   (outside this sheet — not part of the redesign).

It works offline too: the sentence is matched on the phone against the user's own foods and a
built-in list, and every row comes back flagged as a guess.

## Design system — non-negotiable

- **Material 3, fixed palette.** Use only colour *roles* (`primary`, `surfaceContainerLow`,
  `onSurfaceVariant`, …), never a hex. No dynamic colour. Must survive the medium/high-contrast
  schemes, so no role may be used for something it cannot carry.
- **Sheet container** is `surfaceContainerLow` with the standard M3 drag handle, scrim and
  swipe-to-dismiss.
- **AI accent** = `tertiaryContainer` background + `onTertiaryContainer` text. It means "the model
  is talking / the model is unsure", and it is the *only* use of `tertiaryContainer` as a fill.
- **Macros** are always Protein = `primary`, Carbs = `tertiary`, Fat = `secondary`.
- **Type:** Poppins for Display/Headline/Title, Inter for Body/Label. Every number (kcal, grams,
  minutes, kg) uses tabular figures.
- **Spacing:** only `4 / 8 / 12 / 16 / 24 / 32 / 48` dp. 16dp horizontal gutter, 12dp between
  stacked blocks.
- **Touch targets ≥ 48dp**, including every ✕. Every icon-only control has a spoken label.
- The sheet can't hold a lazy list — its content column scrolls as one.

## Anatomy (today)

```
┌──────────────────────────────────────────┐
│               ── drag handle ──           │
│ What did you eat or do?              ✕   │  pinned header (title + close)
├──────────────────────────────────────────┤
│                                          │
│  scrolling content                       │  recents · "You said" · question bubble ·
│                                          │  rows · total · meal-slot chips · message line
│                                          │
├──────────────────────────────────────────┤
│ [            Log            ]            │  pinned bottom bar — rides on the keyboard
│ [photo thumb ✕]                          │
│ ( field ……………………………… 🎤 )  (↑)           │
│ [📷 Photo] [▦ Scan] [✦ Ask coach]         │
└──────────────────────────────────────────┘
```

The bottom bar is **pinned outside the scroll**, so the field and Log sit on the keyboard however
long the review above grows. The header is pinned too, so the close can't scroll away.

## Content inventory (copy is final unless you have a reason)

**Header** — "What did you eat or do?" · close ✕.

**Recent sentences** — up to three sentences that became meals before, as full-width rows (not
pills; two lines each, never ellipsised to one) with a leading clock glyph, filled
`surfaceContainerHigh`. Tap fills the field; it does **not** send.

**"You said" line** — `You said · rice and chicken adobo`, `bodyMedium` `onSurfaceVariant`. Shown
only above a question, so the question reads as a reply.

**Question bubble** — AI accent, 16dp corners, sparkle icon + `bodyLarge` text, e.g.
"About how much rice — one cup or two?"

**Food row** (tappable body, separate ✕) — name, portion ("2 egg", "1 slice"), kcal, and the three
macros in their fixed colours. A row the model was unsure of carries a small AI-accent chip:
`Unsure: a slice` (or `Check this` when it named no reason). Tapping the row expands a **portion
control** under it: the amount as the largest figure, − / + steppers, a unit segmented toggle
(g / oz / serving…), and the caveat "The estimate rescales with the portion." One row open at a time.

**Total line** — `Total · 260 kcal`, only when there is more than one food.

**Activity row** (✕) — "Run", detail "30 min · along the river", value "343 kcal burned".

**Water row** (✕) — "Water", "3 glasses", value "750 ml" (or fl oz in imperial).

**Weight row** (✕) — "Weight", value "72.4 kg" (or lb).

**Meal-slot chips** — Breakfast · Lunch · Dinner · Snacks, single-select, four individually
outlined pills. Only when there is food. Pre-set from the clock, or from the sentence if it named
a meal ("for lunch").

**Message line** — one `bodyMedium` `onSurfaceVariant` sentence under everything:
- "Nothing to log in that one — name a food or an activity."
- "That didn't work. Your words are still there — try again."
- "You're offline, and nothing in that matched your foods or the built-in list. The Food tab's add
  button works offline."
- "Offline — matched from your foods and the built-in list. Check the portions." (sits *under* rows)
- "Camera access is off. Choose a photo from the gallery instead."

**Log** — full-width primary button, only during review. Disabled if every row was removed.

**Attached photo** — 64dp rounded thumbnail + its own ✕, above the field.

**Field** — pill-shaped (24dp corners), `surfaceContainerHighest`, no border, up to 3 lines then
scrolls, mic button inside at the trailing edge (hidden if the device has no speech recogniser but
the slot keeps its width). Keyboard action is Send. Placeholder changes with the state:
- blank: `e.g. "two eggs and toast" or "30 min run"`
- photo attached: `Anything to add? (optional)`
- a question is waiting: `Your answer…`
- rows are showing: `Anything to change?`

**Send / stop circle** — 48dp beside the field. Three looks: disabled (`surfaceContainerHighest`,
`outline` arrow), ready (`primary` fill, `onPrimary` arrow), working (stop square with a 2dp
`primary` progress ring around the circle — still tappable to cancel).

**Chip row under the field** — outlined, never filled (the FAB is the screen's one filled action),
icon + label, 48dp min height, equal widths:
- **Photo** — opens a small menu anchored to it: "Take photo" / "Choose from gallery".
- **Scan** — leaves for the barcode scanner.
- **Ask coach** (sparkle) — closes the sheet and opens the AI coach with everything the user said,
  unsent.

## States

Each is a frame to deliver. "Chips" means the row under the field.

1. **Blank start** — keyboard up (field auto-focused on open), blank placeholder, send circle
   disabled, chips: Photo · Scan. Content area empty. *This is the state seen most — it should
   feel like a composer, not an empty sheet.*
2. **Blank start with recents** — as 1, with the three recent-sentence rows in the content area.
   They vanish the moment anything is typed or a photo is attached.
3. **Typing** — text in the field, send circle ready. Chips: Photo · Scan. Recents gone.
4. **Photo menu open** — the Photo chip's two-item menu.
5. **Photo attached** — thumbnail above the field, placeholder "Anything to add? (optional)", send
   circle ready even with an empty field (a photo alone is a valid first send).
6. **Thinking, first send** — the field has been emptied into the conversation, circle is stop +
   ring, chips gone. Today *nothing* on screen shows what was just sent — see pain points.
7. **Follow-up question** — "You said · …" + question bubble, keyboard up, placeholder
   "Your answer…", send circle disabled until typed. Chips: **Ask coach** alone.
8. **Thinking, answering** — as 6, after an answer; the question bubble has gone.
9. **Review** — keyboard **down**. A realistic mix: two foods (one with `Unsure: a slice`),
   the total line, a run, water, a weigh-in, meal-slot chips, Log pinned above the field,
   placeholder "Anything to change?". Chips: Ask coach.
10. **Review, portion open** — as 9 with the first food's portion control expanded under it.
11. **Review, long** — enough rows that the content scrolls under the pinned header and bar.
12. **Thinking, correcting** — the user typed "make it two slices" on the review: rows stay on
    screen, Log is hidden, circle is stop + ring.
13. **Offline review** — every food flagged, the "Offline — matched…" line under the rows.
14. **Dead end** — nothing found / failed / offline-and-nothing: the sent words are **back in the
    field**, the message line shows, and whatever was on screen before (a question or rows) is
    still there. On a first send the chips are Photo · Scan · **Ask coach** (three across).
15. **Camera denied** — the camera message line after the permission was refused.
16. **Everything removed** — review with every row ✕'d: Log disabled, nothing to confirm.

## Behaviour to keep

- **Back steps before it leaves:** a call in flight → cancel it and hand the words back; a question
  or a review → start over (the first sentence goes back in the field); only a blank start closes
  the sheet. Design the transitions so each step reads as one level up.
- The keyboard is **up** when the sheet lands and **down** when rows arrive; tapping the field
  brings it back for a correction.
- Only one food row's portion is open at a time.
- A food row's tap target is its body; the ✕ is separate and never inside it.
- Photo and Scan disappear once a conversation starts and come back on start-over.
- Ask coach appears only once a conversation has started (a question, rows, or a first send that
  came to nothing).
- Nothing is logged until Log. After Log the sheet just closes.

## Pain points worth solving

- **The conversation disappears while thinking** (states 6 and 8): the field empties and the user
  is left with a spinner ring and nothing that says what is being read.
- The **question bubble and "You said" line** only ever show the latest exchange; after an answer
  the thread is gone. Decide whether it should read as a thread or stay a single-exchange card —
  and say why.
- **Three stacks of outlined rounded rectangles** can meet in review: meal-slot pills in the
  content, Log + field + chips in the bar. The hierarchy between "pick a slot", "confirm" and
  "leave for somewhere else" is weak.
- The **message line** sits at the bottom of the content, far from the field the user's eyes are
  on after a send.
- Rows of four different kinds (food, activity, water, weight) share one plain list with only the
  value column to tell them apart.
- A **✕ per row** is eight identical icons on a long review.

## Deliverables

- The 16 frames, light + dark (32 artboards), 412dp wide.
- One annotated frame calling out every colour role, type style and spacing value used.
- Motion notes for: rows arriving, a question arriving, the portion control expanding, the send
  circle's three looks, and Log appearing/disappearing.
- Anything you add beyond this list, flagged as an addition so it can be argued rather than
  shipped silently.
