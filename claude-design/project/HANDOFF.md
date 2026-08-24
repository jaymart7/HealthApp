# HANDOFF.md — HealthTrack prototype → Claude Code

## File structure

- `theme.js` — shared module: `THEMES.light` / `THEMES.dark` (M3-style color tokens), `calcDailyTargets()` (Mifflin–St Jeor BMR → TDEE → goal-adjusted calories + macro split), `ACT_MULT`, `GOAL_ADJ`, unit constants (`CM_PER_IN`, `KG_PER_LB`). Imported by all three screens — single source of truth for colors and health math.
- `appScaffold.js`, `fullScreenState.js` — **not executed imports**. Plain-text reference notes describing two markup patterns (bottom nav + FAB + quick-action sheet; empty/status-state layout) that are hand copy-pasted into every DC that needs them, because DCs can't share arbitrary child markup. Treat these as the canonical spec to diff against, not working code.
- `Onboarding.dc.html` — welcome → goal → basics → activity → dietary → confirm targets. Writes the completed profile to `localStorage['healthtrack_onboarding_profile']`, then navigates to `Home.dc.html`.
- `PhotoLogging.dc.html` — camera capture → simulated AI analysis → confirm/edit → log. Reached from Home's FAB ("Log food") and meal-section "+" buttons via `window.location.href`.
- `Home.dc.html` — the single app shell: bottom nav + FAB + quick-action sheet, and all four tabs (Home, Food, Progress, Profile) in one file, switched by in-memory `state.tab`. Also owns the Log Weight, Add Photo, and Add Measurement bottom sheets.
- `image-slot.js` — drag-and-drop image placeholder component, used for camera/gallery/progress-photo placeholders.

Full component-level detail (every screen, every reusable piece, props, deferred work) is in `COMPONENTS.md`.

## Cross-screen state: localStorage (prototype-only workaround)

Onboarding and Home are separate top-level pages with no shared runtime — the only way to hand data from one to the other in this prototype tool is `localStorage['healthtrack_onboarding_profile']`, written once by Onboarding and read once by Home on mount (`{sex, age, heightCm, weightKg, activity, goal}`). Home falls back to a hardcoded `DEFAULT_PROFILE` if the key is missing.

**This is a Claude Design workaround, not a pattern to carry into the real app.** It exists only because these are static prototype pages with no app-level state container or navigation graph. In the real app, Claude Code should replace this with a proper local database (Room) and dependency injection (Koin) — profile, weight log, photos, and measurements all become persisted entities read reactively, not a single JSON blob shuttled through localStorage at a page boundary.

Everything else — food diary entries, weight log, photo log, measurements — lives only in Home's in-memory React state (`state.foodEntries`, `state.weightData`, `state.photos`, `state.measurementData`), seeded once on mount and mutated by the Log Weight / Add Photo / Add Measurement sheets. None of it persists across a reload; that's expected for this prototype and is exactly the data that becomes real Room-backed storage.

## What's simulated vs. real

**Simulated (needs real implementation in the app):**
- **Camera feed** — CaptureScreen is a static full-bleed placeholder with a framing guide overlay; there is no real camera stream.
- **AI analysis** — AnalyzingScreen shows an indeterminate progress bar and rotating status text, then auto-advances after a fixed 1.8s timeout. No real food-recognition model is called; the "confidence" and identified food on the Confirmation screen are hardcoded per demo path.
- **Barcode scanning** — icon renders and has a pressed state; tapping it does nothing.
- **Chart seed data** — `Home.dc.html`'s `WEIGHT_DATA` (10 hardcoded weigh-ins) and `MEASUREMENT_SEED` (chest/waist/hips only) exist purely to demonstrate the Progress tab's layouts. None of it derives from onboarding answers or real usage.
- **Photo comparison** — static side-by-side only; no draggable before/after slider.
- **Export/Import (Profile)** — this one is real, not simulated: "Export data" downloads an actual JSON file of profile + food entries; a matching import path re-hydrates from that JSON. Worth keeping as a reference for the real export feature, though the real app will export from Room rather than in-memory state.

**Correct as designed (translate as-is, no behavior change needed):**
- All layout, navigation, and visual design across onboarding, the four tabs, and the photo flow.
- `calcDailyTargets()` — the BMR/TDEE/macro-split math is a real, complete formula, not a stub.
- The Log Weight / Add Photo / Add Measurement sheets' UX: shared in-sheet calendar (slides the sheet's own content rather than opening a nested sheet), duplicate-date detection and replace-in-place behavior, unit-aware value entry. Same interaction pattern should be a shared component in the real app rather than three separate implementations of it.
- Goal-relative trend coloring (a rising weight is "on track" green if the user's goal is to build, not lose) — this logic should transfer as-is.

## Known gaps (see COMPONENTS.md → Deferred for the full list)
- Onboarding's target-weight input is never persisted, so Progress's goal marker always uses a hardcoded 75.0kg fallback.
- Profile's Units and Reminders toggles are local UI state only, not wired to anything persistent.
- FAB → PhotoLogging → back to Home is a real page navigation but doesn't round-trip a logged item back into the diary (no shared navigation graph in this prototype).

## Regression check (this session)
Traced onboarding, all four tabs, the photo flow, and the Log Weight / Add Photo / Add Measurement sheets against the current code and a live render. No console errors beyond a benign one-time "hole never resolved" warning from Home's pre-mount render pass (values resolve correctly once mounted, confirmed on screen). Nothing broken.
