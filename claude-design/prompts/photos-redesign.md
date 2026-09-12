# Claude Design prompt — Progress Photos, Compare, Timelapse

Paste everything below into Claude Design, together with the three attached screenshots
(Photos page / Compare photos / Timelapse).

---

Redesign three screens of **FitPulse**, an Android (Kotlin + Jetpack Compose, Material 3) body-
and nutrition-tracking app. Screenshots of the shipped versions are attached — they work, but
they look like a wireframe someone stopped polishing: stacked full-width secondary buttons,
a bare title, no visual weight anywhere.

Give me **one canvas with three artboards at 412×915 (phone)**, plus a dark-mode variant of each:

1. **Photos** — the progress-photo page inside the Progress tab
2. **Compare photos** — the two-photo before/after overlay
3. **Timelapse** — the whole-set player overlay

## Design system (binding — do not invent tokens)

- Material 3, a **fixed** color scheme, no dynamic color. Token values are in
  `claude-design/project/theme.js` (`THEMES.light` / `THEMES.dark`). Use token names only;
  never a raw hex in the markup.
- Type: **Poppins** for Display/Headline/Title, **Inter** for Body/Label. Every number
  (weight, dates, counts, deltas) in **tabular figures**.
- Spacing scale, nothing else: `4 / 8 / 12 / 16 / 24 / 32 / 48`. Screen horizontal padding 16,
  vertical gap between cards 12.
- Trend/delta color: `onSurfaceVariant` neutral, `primary` when on track for the user's goal,
  `error` only when genuinely off track. **Never green-for-loss / red-for-gain** — a rising
  weight is on track for someone bulking.
- Photo frames are portrait **3:4** (`aspectRatio 0.75`), 12dp corners, `surfaceContainerLow`
  behind them while the bitmap decodes.
- Both light and dark must be fully specified. Assume a light-on-dark photo can sit under any
  label — labels need their own scrim/container, not bare text on the image.

## Screen 1 — Photos

Currently: a hero count ("12 shots"), a full-width "Play timelapse" secondary button, then a
3-column grid grouped by month header, each tile square-cropped with a small date chip
bottom-left. Tapping a tile selects it; a 3dp `primary` border marks selection; selecting a
**second** tile immediately opens Compare (selecting a third drops the oldest). A docked FAB
floats over the bottom of the grid.

What it has to keep:
- 3-up grid, month headers, tap-to-select-two, the selection→compare rule.
- Square tiles are fine, but the photos themselves are portrait — show me whether a taller
  tile (3:4) in 3 columns reads better than the square crop.
- "Play timelapse" only appears at **≥2 photos**.
- Bottom content padding clears the docked FAB.

What to fix:
- The selection state is almost invisible until you already know the rule. Design a selection
  affordance that *teaches* it — e.g. a numbered "1 / 2" badge, and a persistent bar or hint
  that says what the second tap will do.
- The hero count + full-width button eat the top of the screen for very little. Consider
  folding the count, the date span ("92 days"), and the timelapse entry into one compact
  header strip.
- Give the month headers more structure (sticky? a count per month?).
- Design the **empty state** too: mascot illustration, "No progress photos yet", one line of
  body. No call to action inside it — Progress reads, it doesn't log.

## Screen 2 — Compare photos

Currently: a title, a before/after slider (older photo full-frame, newer drawn over it clipped
to the right of a draggable vertical divider with a circular handle), each side date-labelled
at the bottom corners, a weight-delta line under the frame, then two side-by-side secondary
buttons: Share and Close.

Keep:
- The drag-to-reveal divider and its handle — that interaction is the point of the screen.
- Date label per side, weight delta when both shots carry a weight (it can be absent).
- Share and Close both reachable.

Fix:
- It reads as a form, not a result. The delta ("−3.2 kg over 92 days") is the headline and
  should be typed like one.
- The frame is width-capped at 3:4 and floats in a tall column — let the photo command the
  screen, with the chrome as overlay rather than a stack of rows beneath it.
- "Close" as a bottom button is wrong for a full-screen overlay — design proper top chrome
  (back/close), and treat Share as the one deliberate action.
- Show the alternate framing too: a **side-by-side** mode next to the slider mode, as a toggle.

## Screen 3 — Timelapse

Currently: a title, one frame (3:4) with a date label bottom-left and weight bottom-right, a
scrub slider, a row with a play/pause icon button and a Slow / Normal / Fast segmented toggle,
then Share + Close side by side. Playback loops; scrubbing pauses it. Speeds are 2/4/8 fps.

Keep:
- Loop playback, scrub-to-pause, three speeds, the per-frame date + weight overlay.

Fix:
- Same problem: a player laid out as a settings form. Design it as a media surface —
  photo dominant, transport controls grouped and overlaid or docked on a single row.
- The scrubber should read as a *timeline*: show where in the date range the current frame
  sits, not just an index. Tick density follows the photo count.
- Speed control shouldn't have equal weight with play/pause. Demote it.
- Show the frame-advance moment: how does one shot become the next without strobing?

## Shared

- Both overlays share one **Share sheet**: a preview of the exact PNG that leaves the app —
  up to 4 frames sampled evenly (first and last always included), a headline like
  "92 days · −3.2 kg", and a brand footer. Design that sheet once, as a fourth artboard, with
  a scrim behind it so it reads in isolation.
- Every label sitting on a photo uses one shared overlay-label component. Specify it once.
- Both overlays are full-screen surfaces *inside* the Progress tab, so the bottom nav and the
  docked FAB stay visible behind/below them — don't design edge-to-edge chrome that assumes
  the tab bar is gone.

## Out of scope

Don't redesign: the Progress overview, the Add-photo sheet, the camera/food-photo flow, the
bottom nav, or the FAB. Don't introduce a new color token, a new spacing value, or a second
typeface.
