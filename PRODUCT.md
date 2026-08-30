# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

The primary user is an **everyday habit-builder**: someone getting back into shape who
wants a gentle daily loop — log a meal, drink water, weigh in, keep the streak alive.
They are not a numbers person. They open the app in short bursts, often one-handed,
often mid-meal, and they will abandon a session that asks for precision they don't have.

Deliberate cutting/bulking users are served by the same features (macro accuracy, weight
trend, editable targets) but they are **not** who the primary surfaces are tuned for.
Precision stays available; it never leads.

## Product Purpose

FitPulse tracks two things in one loop: **body** (weight, measurements, progress photos)
and **nutrition** (calories, macros). Success is a user who keeps logging — the streak
is the product's real metric, not the accuracy of any single entry. Everything that makes
logging faster or less punishing is on-mission; everything that makes a day feel like a
grade is off-mission.

## Positioning

Four things a neighboring tracker could not truthfully claim at once:

1. **AI photo food logging.** Point the camera at a plate and it logs. The fastest path
   from meal to entry, with no search-and-scroll.
2. **Body and nutrition in one loop.** Weight, measurements, and photos sit beside
   calories, so food data is always read against actual body change — not in a silo.
3. **Offline-first, no account wall.** All core data works with no network and no sign-up.
   Room on the device is the source of truth; there is no server holding the user's history.
4. **Warm and unjudgmental.** Bibo the mascot, streaks with a grace day, badges earned off
   the best run so a broken streak never un-earns one, and a calorie safety floor that warns
   rather than blocks. The app never scolds.

## Operating Context

- **Short, frequent, in-the-moment sessions.** Logging happens at the table, at the gym,
  on the scale — not at a desk. One-handed reach and glanceability matter more than density.
- **Network is optional and often absent.** AI photo analysis and FoodData Central lookups
  need the network; every one of them degrades to a manual-entry path.
- **The day is the unit.** Home means today. The diary can walk backward through any past
  day but never forward past today — there are no planned meals.
- **Reminders pull the user in** (WorkManager), so the app is often entered cold, from a
  notification, into a specific task.

## Capabilities and Constraints

Confirmed and shipping:

- Onboarding that computes targets from Mifflin–St Jeor (age, sex, height, weight,
  activity, goal), with a 1200/1500 kcal safety floor the user can override past a warning.
- Food diary by meal, with AI photo capture, barcode scanning (ML Kit + FoodData Central),
  manual search, and a local filter over already-logged entries.
- Water, exercise logging (MET-estimated burn, editable, with an opt-out calorie credit),
  weight and body measurements, progress photos with comparison.
- Streaks and badges, derived — no table, no persisted celebration state.
- Profile: units, goals, reminders, data export, appearance (light/dark/system).

Constraints that bind future work:

- **Offline-first.** Room is the source of truth for all core data. Soft delete only.
- **AI requires network** (Firebase AI Logic / Gemini + App Check) and must always have a
  manual fallback.
- **No HTTP client dependency.** FoodData Central goes over `HttpURLConnection` +
  kotlinx.serialization, deliberately.
- **minSdk 24.** Old and small devices are in scope.
- Architecture, module boundaries, theming rules, and the decision log live in `CLAUDE.md`
  and are binding. `:core:data` module boundaries and the frozen `Color.kt` are not
  negotiable by design work.

Explicitly undecided:

- **Accounts and sync.** The welcome screen's "I already have an account" is a deliberate
  no-op; no auth system exists. Whether FitPulse ever gets one is open. Nothing should be
  designed as if a server-side account exists.
- **Monetization.** No pricing, subscription, or paywall exists or has been decided.

## Brand Commitments

- **Name:** FitPulse. **Package:** `ph.mart.healthapp`.
- **Mascot:** Bibo, currently a geometric placeholder illustration used throughout. The
  final illustration is outstanding work; Bibo's presence in the product is not in question.
- **Voice:** warm, plain, second-person, encouraging without cheerleading — "Let's build
  healthy habits together.", "Track your body and nutrition, with Bibo by your side.",
  "What brings you here?" No jargon, no shame, no exclamation-mark energy.
- **Visual system is already committed and load-bearing:** Material 3 with a frozen Material
  Theme Builder palette (light/dark, standard/medium/high contrast), dynamic color disabled,
  Poppins for display/headline/title and Inter for body/label, fixed semantic macro colors
  (Protein = primary, Carbs = tertiary, Fat = secondary), and a 4/8/12/16/24/32/48 spacing
  scale. See `CLAUDE.md` for the full non-negotiable list.

## Evidence on Hand

- **The shipped app is the evidence.** All nine build phases are complete; `BUILD_PLAN.md`
  is kept as history only.
- `claude-design/project/` holds the original Claude Design HTML prototype — the reference
  for layout, copy, and interaction on anything not yet built. Its `COMPONENTS.md` inventory
  is prototype-era; `:core:designsystem` is the truth for what exists.
- **No users, reviews, testimonials, download counts, benchmarks, or press exist.** The app
  has not been released. Future work must not fabricate any of these, and must not imply an
  existing user base in copy or store-facing material.

## Product Principles

1. **Logging must never feel like an exam.** Every flow has a fast, low-precision path;
   accuracy is opt-in. A missed day costs nothing permanent.
2. **Today is the default; the past is browsable, the future isn't.** Surfaces open on now
   and stay one gesture from it.
3. **Offline is the normal case, not the error case.** Network-dependent features announce
   their fallback rather than presenting failure.
4. **Body and food are read together.** Neither pillar gets a surface that pretends the
   other doesn't exist.
5. **Consistency is the brand.** A macro is the same color in every chart in the app; a
   spacing value comes from the scale or doesn't exist. Personality lives in Bibo, copy,
   and motion — never in one-off visual exceptions.

## Accessibility & Inclusion

No formal standard is adopted. Hold to Material 3 and Android platform defaults: system
font scaling, TalkBack labels, minimum touch targets, and correct use of the theme's
contrast schemes (which `AppTheme` already wires from `UiModeManager.getContrast()` on
API 34+). Color is never the sole carrier of meaning — macro colors always ship with a
label. minSdk 24 means small screens and older devices are part of the inclusion picture.
