# BUILD_PLAN.md — HealthTrack

Ordered by dependency, not by prototype session number. Each phase is one
Claude Code session. Do not start a phase until the previous one is
confirmed working — later phases assume earlier ones are real, not stubbed.

Phase 0 (below) covers pasting the final `COMPONENTS.md` into `CLAUDE.md`
— don't start Phase 1 without it.

## Phase 0 — Design handoff (Claude Design, not Claude Code)

This happens in Claude Design, as the last step before opening Claude Code.
Do not skip straight to Phase 1 with a mid-session COMPONENTS.md.

Paste this into the Claude Design conversation:

````
This is the last request before handoff to Claude Code. Do NOT build
anything new.

1. Output the final, cumulative COMPONENTS.md across all four sessions —
   deduplicated, sorted by build priority, every deferred item and every
   Claude Code wiring task listed once in one place.

2. Confirm nothing has regressed: trace through onboarding, all four
   tabs, the photo flow, and the log weight / add photo / add
   measurement sheets. Report anything broken.

3. Write a short HANDOFF.md covering:
   - the final file structure (theme.js, appScaffold.js,
     fullScreenState.js, Onboarding/PhotoLogging/Home .dc.html)
   - the localStorage mechanism used for cross-screen state, and a note
     that this is a Claude Design workaround, not a pattern to carry
     into the real app (Claude Code will use Room + Koin instead)
   - what each screen simulates: camera feed, AI analysis delay, chart
     seed data — so Claude Code knows exactly what needs a real
     implementation versus what's already correct as designed
````

Then:
- Export the prototype.
- Paste the final `COMPONENTS.md` into `CLAUDE.md` where the placeholder
  is, and drop `HANDOFF.md` alongside it.

Only after this is done do you open Claude Code and start Phase 1.

## Exact prompts for Claude Code

Three templates: the one-time Phase 1 opener, a reusable per-phase
starter for Phase 2 onward, and an end-of-phase self-audit to run
before moving to the next phase. Fill in `[N]` and `[phase name]`.

### Phase 1 opener (exact — use this verbatim)

````
Read CLAUDE.md and BUILD_PLAN.md in full before doing anything.

Verify the module graph and :core:data access pattern against CLAUDE.md
before writing code — this is settled architecture, not open for
redesign. Flag anything that seems to conflict with it rather than
silently deviating.

Start Phase 1 only. Wait for my confirmation before proceeding to
Phase 2.
````

### Phase [N] opener (template — Phase 2 onward)

````
Read CLAUDE.md and BUILD_PLAN.md again before doing anything — don't
rely on memory of earlier turns in this conversation.

Before building, confirm in a few lines:
1. Which modules/components from prior phases you'll reuse, by name.
2. Anything from Phase [N]'s scope that overlaps something that
   already exists — and why extending it wouldn't work instead of
   building new.

RULES (from CLAUDE.md, unchanged):
- Reuse existing components and repository interfaces. Do not rebuild
  them or fork a second version.
- Never reference AppDatabase, a DAO, or an Entity from a feature
  module — only the repository interface.
- Never hardcode a color, spacing value, or font that isn't in the
  theme tokens.
- Follow the composable file-breakdown and @PreviewLightDark
  conventions for every screen and component this phase touches.

Build ONLY Phase [N] — [phase name] — as scoped in BUILD_PLAN.md. Do
not start the next phase's work even if it seems like a natural
continuation. Propose your approach for anything ambiguous before
writing code, rather than guessing.
````

### End-of-phase self-audit (run before confirming a phase is done)

````
Before I confirm Phase [N] is done, audit your own output. Answer each
with YES/NO plus one line of evidence. Report failures honestly.

1. Quote each bullet from this phase's section in BUILD_PLAN.md, and
   for each one state whether it is fully done, partially done, or not
   done. Partial counts as NO.
2. Does every new screen/component have a @PreviewLightDark, wrapped
   in Surface?
3. Did you introduce any color, spacing, or font value not in
   CLAUDE.md's token tables?
4. Does any feature module reference Room, a DAO, or an Entity
   directly, instead of going through a repository interface? (Should
   be NO everywhere.)
5. Did you duplicate a component or pattern that already existed
   instead of reusing or extending it? Name anything you're unsure
   about.
6. List anything from this phase's scope you did NOT build.
7. List anything you built that was NOT in this phase's scope.
8. List anything you marked complete but did not actually verify
   yourself (e.g. ran the build, but didn't exercise the feature).

Then give a plain summary: what passes, what fails, what you're unsure
about. Do not start Phase [N+1] until I confirm.
````

## Phase 1 — Project setup

- Add the version catalog block from `CLAUDE.md` to `libs.versions.toml`
  — this includes Navigation 3 **and** `navigationevent` (both artifacts:
  `navigationevent` and `navigationevent-compose`)
- Add Koin, Room 3.0.1, CameraX + exifinterface dependencies
- Add **Firebase AI Logic / Gemini** dependency + `google-services` setup
  — Phase 5 needs it, and discovering a missing Firebase config four
  phases in is expensive. Wire the project, even though no AI call is
  made until Phase 5.
- Add the **Poppins and Inter font files** as resources (or Google Fonts
  provider). `Type.kt` in Phase 2b cannot be wired without them —
  confirm both families load before Phase 2 starts.
- Confirm `Color.kt` is in place and matches the light/dark tables in
  `CLAUDE.md` — do not regenerate it. **If it currently sits in
  `:app`'s `ui/theme/`, move it verbatim into `:core:designsystem`
  (updating only the package declaration) — moving the file is allowed,
  changing any value in it is not.**
- Confirm `minSdk` / `compileSdk` / AGP versions satisfy the Compose and
  `navigationevent` requirements. Flag any conflict before Phase 2
  rather than hitting it mid-build.
- Set up the feature-module skeleton (empty modules, matching the clinic
  app's package convention):

  ```
  :app
  :core:designsystem   — theme, MascotAvatar, buttons, cards
  :core:data           — Room, ALL domains' DAOs/Entities, repository impls
  :core:camera         — CameraX + exifinterface wrapper, shared by food
                          photo logging AND progress photos
  :core:navigation     — Nav3 route registry
  :feature:onboarding
  :feature:home
  :feature:food
  :feature:progress
  :feature:profile
  ```

  Each `:feature:*` module: internal `ui` / `di` packages only — **no
  `data` package**, since all persistence lives in `:core:data` (see
  below). Cross-feature references stay plain Strings/IDs — never a
  direct import of another feature's type.

- **`:core:data` access pattern — verified from the clinic app, replicate
  exactly:**

  1. **Gradle scope enforces it, not convention.** Room
     (`room-runtime`, `room-compiler` via KSP, `androidx-sqlite-bundled`)
     is declared `implementation` — never `api` — in `:core:data`'s
     `build.gradle.kts` only. No feature module's build file references
     Room at all. This is the load-bearing part: verify with a `grep
     room` across every feature module's build file before considering
     this phase done — it should return nothing.
  2. **Public surface = repository interfaces only.** DAOs and `@Entity`
     classes live in `:core:data`'s `<domain>/local/` subpackages —
     e.g. `food/local/FoodDao.kt`, `FoodEntity.kt`. The repository
     interface itself lives at the domain root —
     `:core:data/food/FoodRepository.kt` — with `local/` and `di/` as
     subpackages beneath it. One subpackage per domain: `onboarding/`,
     `food/`, `progress/`, `profile/`. `home` has no subpackage of its
     own — it aggregates from the other three via their repository
     interfaces, same as the clinic app's dashboard does.
  3. **DI is the only wiring point.** Each domain's `di/` module (inside
     `:core:data`) binds the interface (e.g. `FoodRepository`) to its
     Room-backed `FoodRepositoryImpl`, marked `internal`, and hands it
     out via `koinViewModel()`. ViewModels in feature modules take the
     repository interface by constructor injection and never reference
     `AppDatabase`, a DAO, or an Entity directly.

  No further architecture proposal needed for this — it's settled.
  Just replicate it per-domain.
- Confirm `/orbit-mvi-screen-split` and `/navigation-3` skills are
  available and reviewed before Phase 2 starts

**Verify the module graph and `:core:data` access pattern against
`CLAUDE.md` before writing code — this is settled architecture, not
open for redesign. Flag anything that seems to conflict with it rather
than silently deviating.**

## Phase 2 — Design system + navigation shell

Build in this order — each sub-step depends on the one before it. Don't
jump ahead to `AppScaffold` before the icon set and atomic components
exist; it'll just reference placeholders and need rework.

### 2a. Icon set

- Establish the Material Symbols icon set as a single source (a sealed
  set of `ImageVector`s or an icon-name mapping) before any component
  references one. Everything downstream — nav bar tabs, FAB, cards,
  chips — pulls from this, not ad-hoc inline icon lookups.

### 2b. Atomic components → `:core:designsystem`

- Port the theme tokens into `:core:designsystem` (already exist in
  `Color.kt`; wire up `Type.kt` from the type scale in `CLAUDE.md`)
- `MascotAvatar` (5 states), `MascotSpeechBubble`
- `PrimaryButton` / `SecondaryButton` / `TextButton`
- `SelectableCard`, `StepProgressBar`, `AppTextField`,
  `NumericStepperField`, `SegmentedToggle`
- `FullScreenState` — mascot + headline + body + up to two buttons.
  Used by every empty state and by the photo flow's offline/no-food
  states. Build it here, not in whichever phase happens to need it first.
- `AIChip` — with the `default` / `onAccent` variant parameter the
  prototype settled on. Used by the photo confirmation screen (Phase 5)
  and Home's insight card.
- `MacroInputGroup` — three labeled numeric fields (P/C/F) using the
  fixed semantic colors, built on `NumericStepperField`. Used by photo
  confirmation and onboarding Step 5.
- Every component here gets a `@PreviewLightDark` wrapped in `Surface`
  (see "Composable structure & previews" in `CLAUDE.md`) before moving
  to 2c.

### 2c. App structure — nav shell

- `AppScaffold`: bottom nav (4 tabs) + docked top-right FAB + quick-action
  sheet, using Navigation 3 skill for real navigation (this replaces the
  prototype's localStorage/copy-paste workarounds — those existed only
  because Claude Design's DCs couldn't share runtime state)
- Wire the FAB's three actions (Log food / Log weight / Add photo) to real
  destinations, even if those destinations are stubs this phase
- **Predictive back on the quick-action sheet**, using
  `NavigationEventHandler`/`NavigationBackHandler` from
  `androidx.navigationevent` — system back closes the sheet, not the
  screen underneath. Establish this pattern here; Phase 6 reuses it for
  Log weight / Add photo / Add measurement.

**Reuses:** none yet — this phase produces the foundation everything else
depends on. Get it right before moving on.

**Do not treat Phases 3–8 below as one leftover "remaining" step.** Each
stays a separate session with its own reuse list and a stopping point to
verify before the next one starts — collapsing them defeats the purpose
of phasing at all.

## Phase 3 — Onboarding

- 5-step flow + Welcome, per the prototype
- Mifflin–St Jeor calculation as a real, testable function (unit-test the
  safety floor and the male/female formulas separately)
- Persist the completed profile to Room, not to a ViewModel-only memory
  state — this is what Home and Profile read from in later phases
- Back navigation and Step 4 skip, per prototype. Wire system back via
  `NavigationEventHandler` to match the on-screen back button exactly —
  same one-step-back behavior, same data preservation. Don't let system
  back and the on-screen button diverge.

**Reuses:** Phase 2 components entirely. No new shared components expected.

## Phase 4 — Food logging (manual + diary)

- Room entities: food items, meal entries, diary day aggregation
- `FoodItemRow` (Display + Editable variants, single component with a mode
  parameter — the prototype built this correctly, keep that structure)
- Food diary screen: meal sections, swipe-to-delete, sticky summary
- Manual food entry (search stub is fine — barcode is deferred)

**Reuses:** Phase 2 components. Establishes the food Room schema that
Phase 5 (photo logging) and Phase 7 (Home) both read from.

## Phase 5 — Photo food logging (AI)

- Real CameraX capture via `androidx-camera-compose`
- Real Firebase AI Logic / Gemini call replacing the prototype's fixed
  ~1.8s delay — the analyzing state should reflect actual request latency
- Confirmation screen writes to the same Room entities Phase 4 established
- All six prototype states: capture, analyzing, confirmation (+
  low-confidence variant), retry, no-food-detected, offline
- Offline detection is real (network state), not a debug toggle
- **Predictive back differs per state** — implement with
  `NavigationEventHandler`, one handler per state, not one handler for
  the whole flow: from Capture, back exits the flow; from Analyzing, back
  cancels the analysis; from Confirmation, back returns to Capture
  (confirm discard if fields were edited); from Retry/No-food/Offline,
  back exits the flow.

**Reuses:** `MacroInputGroup`, `AIChip`, `FullScreenState` from Phase 2;
`FoodItemRow` (Editable variant) and the Room entities from Phase 4.
Builds no new shared components.

## Phase 6 — Progress

- Room-backed weight and measurement series (chronological, not
  append-only — the prototype implemented backdating correctly, replicate
  that: inserting a weight for a past date must not reorder incorrectly and
  must recompute the 7-day moving average and stat row)
- Weight chart, Photos grid + static side-by-side comparison, Measurements
  list with sparklines
- Log weight / Add photo / Add measurement sheets with working date
  pickers (swap-in-place calendar view, per the prototype's final pattern)

**Reuses:** `SegmentedToggle`, `FullScreenState`, the date-picker pattern
across all three entry sheets — build the date picker once, use it three
times. Also reuse the sheet-level predictive back handler from Phase 2
(closes sheet, not screen) and extend the same pattern to the calendar
swap-in view: back returns to the sheet's fields, one level.

## Phase 7 — Home dashboard

- Reads profile (Phase 3), diary aggregation (Phase 4), and the weight
  and photo series (Phase 6) — no hardcoded persona, no separate
  constants, no stubbed metric cards. This was a real bug in the prototype
  (Home briefly read a hardcoded profile instead of the shared one) —
  don't reintroduce it here.
- Mascot greeting, AI insight card, calorie ring, weight metric card,
  macro bar, progress photo reminder

**Reuses:** everything from Phases 2–6. Every data source this screen
reads already exists — this phase is pure assembly and should require
zero new shared components and zero new Room work. If you find yourself
needing either, something earlier was built incomplete: say so rather
than filling the gap here.

## Phase 8 — Profile / Settings

- Goals, units, reminders, data export (JSON, no photos), about
- All values read from the same Room-backed profile Phase 3 writes —
  no separate hardcoded copies

**Reuses:** everything. Should be the smallest phase.

## Deferred — explicit backlog for later, not silently dropped

Track as separate tickets/issues, not folded into any phase above:

- Barcode scanning
- FAB scroll-collapse behavior
- Photo comparison draggable slider
- Tap-active-tab-to-scroll-to-top
- Medium/high-contrast theme wiring
- Final mascot illustration (replacing the geometric placeholder)
