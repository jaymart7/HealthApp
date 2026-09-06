# Redesign: FitPulse — Profile screen + a new Settings screen (Android, Jetpack Compose, Material 3)

I'm attaching screenshots of the current screen. I want a visual/UX redesign of it, **and** a
split: some of what's on it today moves to a new second screen called **Settings**.
Ask me questions if anything is ambiguous before producing final artboards.

## The app

FitPulse — an Android health app. Two pillars: body tracking (weight, measurements,
progress photos) and nutrition tracking (calories/macros, AI photo food logging).
Kotlin + Jetpack Compose + Material 3, phone-first, offline-first. There's a friendly
geometric mascot (five characters — Bibo, Pip, Zed, Momo, Sprig — the user picks one)
used sparingly in empty states and greetings.

Profile is the fourth tab. It is **the only settings surface in the app** — everything
configurable lives here, which is exactly the problem. It's one vertically scrolling
column inside the app scaffold (bottom nav bar + a docked FAB bottom-right; both stay
visible on this screen).

## What's on the screen today, top to bottom

The whole screen is built from two primitives: a **section** (an uppercase
`labelMedium` caption over one or more cards) and a **row** (label + optional sublabel
on the left, optional control on the right). Fourteen sections, 24dp apart, in this order:

1. **Title** — a plain `titleLarge` "Profile". No avatar, no name, no stats, no identity
   of any kind. (See "The gap" below.)
2. **Goals** — one card: calorie target as a −/+ stepper with unit suffix "kcal" (shows a
   1200/1500 kcal safety warning inline when nudged below the floor) · divider · "Macro
   split" with a three-segment macro bar and three protein/carbs/fat number fields with
   percentages · divider · "Activity level" read-only ("Sedentary"/"Light"/"Moderate"/
   "Very active"). A second card, **shown only when something has been overridden**:
   "Reset to calculated / Goes back to the targets worked out from your height, weight,
   age and goal".
3. **Units** — one card, a two-option segmented toggle: "Metric (kg, cm)" / "Imperial (lb, in)".
4. **Appearance** — one card, three stacked rows: "Dark mode" + switch · "Buddy" — a row of
   five equal-width mascot avatar cells with names, selected cell filled
   `secondaryContainer` · "Colour" — a row of colour swatch circles, no visible labels.
5. **Home layout** — one nav card: "Cards on Home / Choose which ones show, and in what order".
6. **Water** — one card: "Daily goal · 1.6 L" stepper, unit suffix "glasses".
7. **Fasting** — one card: "Fasting goal · 8h eating window" stepper, unit suffix "hours".
8. **Exercise** — one card: "Daily step goal" stepper ("steps") + a row "Add exercise
   calories / Logged workouts raise the day's budget…" + switch.
9. **Cycle** — one card: "Track your cycle" + switch, with a long privacy sublabel. Off by
   default; off means the Home card and Progress page don't exist.
10. **Reminders** — one card, **eight** switch rows separated by dividers: Meal logging ·
    Weigh-in day · Photo cadence · Water · Fasting goal · Supplements · Training day ·
    Weekly recap. Each has a schedule sublabel ("Every Monday, 8:00 AM"). A permission
    refusal message can appear in `error` colour under the card.
11. **Supplements** — one nav card: "What you take / Your daily list — it shows up on Home".
12. **Library** — two nav cards: "Food library / Your foods, saved meals and recipes" and
    "Workout routines / Set which days you train, rename or delete".
13. **Connections** — one nav card: "Google Health / Import workouts, weigh-ins and sleep;
    send your food and water".
14. **Data** — "Export data (JSON)" · "Import data (JSON)" · zero to three "Restore automatic
    backup / Saved 2 days ago" cards, plus a status line.
15. **About** — "Version 1.0.0 (prototype)" and the line "Estimates based on your inputs,
    not medical advice."

The five nav cards (Home layout, Supplements, Food library, Workout routines, Google Health)
are the only rows that leave the screen. Everything else edits in place — no save button
anywhere, every control writes immediately.

## The gap I most want fixed

**There is nothing personal on the Profile screen.** It is a settings list with the word
"Profile" on top. The app knows the user's sex, age, height, current weight, activity level
and goal (lose/maintain/build) — all collected in onboarding — and **none of it is shown or
editable here**. Activity level is printed read-only inside the Goals card; the rest is
invisible after onboarding. There's also no way to change your goal without reinstalling.

A redesigned Profile should look like a profile: who you are, where you are, what you're
aiming at — with the app's knobs moved out of the way.

## The split I want

Profile keeps **you and your targets**. Settings takes **the app's preferences**.
This is my proposed cut — argue with it if you think it's wrong:

**Stays on Profile** — identity/stats header (new) · Goals · Water · Fasting · Exercise ·
Cycle · Supplements · Food library · Workout routines.

**Moves to Settings** — Units · Appearance (dark mode, buddy, colour) · Home layout ·
Reminders · Connections · Data · About.

Open questions I'd like your recommendation on:
- Is **Units** a profile thing or a settings thing? (It changes every number in the app.)
- Do the three "your stuff" nav rows (Supplements, Food library, Routines) belong on
  Profile, or do they want a third home?
- **Cycle** is a privacy-sensitive on/off that creates surfaces elsewhere — Profile, or Settings?
- **Reminders is eight switches in one card.** Does it stay one flat list on Settings, or
  become its own sub-screen with a nav row?

## How Settings is reached

Settings must be reachable from Profile — I'd expect a gear icon in Profile's top-right, but
propose what you think is right. It cannot become a fifth bottom-nav tab (the bar holds four).
It is a full screen one level above the Profile tab, with its own back arrow and no bottom
nav/FAB — the same shape as the five existing sub-screens.

## What else I want from the redesign

1. **Length.** Fourteen sections is a long scroll of near-identical grey cards with no
   landmarks. Even after the split, both screens need structure someone can scan.
2. **Sameness.** Every section is caption-over-card, so a stepper you'll touch once a
   year looks exactly like the switch you touch weekly. Establish a hierarchy between
   "set once", "adjust occasionally", and "goes somewhere else".
3. **The four goal steppers** (calories, water, fasting, steps) are scattered across four
   separate sections that are conceptually one thing: your daily targets. Consider whether
   they belong together.
4. **The nav rows** — five of them, all identical, none carrying a summary or a count
   (deliberately: a count here can go stale). They read as a dead end rather than a doorway.
5. **The mascot picker.** Five avatars + a swatch row is the one genuinely delightful thing
   on this screen and it's buried at position 4. It's also the closest thing the app has to
   an avatar — use it.
6. **Micro-interactions worth specifying:** a stepper nudge, a switch flip that reveals or
   hides a surface elsewhere (Cycle), the calorie-floor warning appearing, and the
   Profile → Settings transition.

## Hard constraints — please design inside these

- **No accounts, no sign-in, no server sync, no monetization.** There is no user name, no
  email, no photo upload, no "manage subscription". Don't design any of them. The mascot is
  the avatar.
- **Material 3, and the colour palette is frozen.** It's a complete Material Theme Builder
  export (light + dark + medium/high contrast). Work in M3 role tokens — `surface`,
  `surfaceContainerLow/High`, `onSurface`, `onSurfaceVariant`, `primary`, `secondary`,
  `tertiary`, `outlineVariant`, `error`. Don't invent hexes, don't propose a new brand
  palette, no dynamic/Material You colour.
- **Semantic colour is fixed app-wide:** Protein = `primary`, Carbs = `tertiary`,
  Fat = `secondary` — identical in every bar, chart and legend across the app.
  `tertiaryContainer` is reserved for the AI/insight accent and appears at most once per
  screen. `error` is reserved for genuine failure — being over a target is **not** an error.
- **Typography:** Poppins for Display/Headline/Title, Inter for Body/Label. All numeric
  values use tabular figures.
- **Spacing scale is 4 / 8 / 12 / 16 / 24 / 32 / 48 dp only.** Screen horizontal padding
  16dp, 12dp vertical gap between cards. No other values.
- **Touch targets ≥ 48dp**, and everything needs a sensible screen-reader story — including
  the colour swatches, which have no visible label.
- **Light and dark must both be specified**, and the layout must survive large font scales.
- Bottom nav + docked FAB stay on the Profile tab; leave bottom padding clear for them.
  Settings, being one level up, has neither.
- **Tablet/expanded width:** at ≥840dp the Profile tab already draws as a two-pane
  list-detail — the section list on the left, whichever sub-screen is open on the right,
  with a placeholder ("Pick a section") before anything is tapped. Whatever you propose has
  to survive that: a Profile that is mostly a stats header has a thin list pane. Say how it
  behaves at that width.

## Please don't redesign away

These are settled product decisions, not accidents:
- **Nav rows carry no counts or cached state** (no "12 supplements", no "Connected") — a
  number here goes stale and the screen it opens is where counting is honest.
- **No save button.** Every control writes on change.
- **"Reset to calculated" only appears when a target has actually been overridden.**
- Calorie targets are computed live (Mifflin–St Jeor) from the profile; an edit sets an
  override on top, never a second stored copy. The 1200/1500 kcal floor **warns, never blocks**.
- **Cycle's privacy sublabel stays** — that it never leaves the phone is the first question
  the feature raises.
- Backup restore is confirm-first; import is all-or-nothing.
- Reminder switches only turn on once the notification permission is actually granted, and
  a refusal explains itself inline.

## Deliverables

- Artboards, light **and** dark, for: the redesigned Profile (populated) · the new Settings
  screen · any sub-screen you introduce or restructure · the two-pane (≥840dp) Profile ·
  and one state showing the calorie-floor warning.
- Redlines: spacing, type roles, and which M3 colour role each surface/text uses.
- A short written rationale for anything you moved, merged or removed — especially the final
  Profile/Settings cut if it differs from mine.

## Handoff

When the design is done, produce a **handoff brief for Claude Code** that I can paste into a
terminal session on the real repo. It should:
- List the changes per file against this existing structure:
  `feature/profile/src/main/java/ph/mart/healthapp/feature/profile/ui/`, holding
  `ProfileNavigation.kt` (route types + entries) and the flow packages `profile/`, `health/`,
  `library/`, `routine/`, `supplement/`, `layout/`, `shared/`. Today's Profile screen is
  `profile/ProfileScreen.kt` with `profile/components/` holding `SettingsSection.kt`,
  `SettingsRow.kt` and one `Profile*Section.kt` per section listed above.
- Say whether Settings is a new flow package (`ui/settings/`) or stays in `profile/`, and
  whether it needs its own ViewModel — a second ViewModel is what earns a package here.
  Note that a new route also has to join the app's `ProfileDetailRoutes` set, which drives
  both the two-pane metadata and whether the bottom bar/FAB show.
- Name every value as an M3 role token and a dp from the scale above — never a hex.
- Flag anything that would need a **new shared component** in `core/designsystem` (used by
  ≥2 screens) versus a screen-local one.
- Note that every user-facing string is a resource in
  `feature/profile/src/main/res/values/strings.xml`, keyed `profile_<screen>_<thing>` — new
  copy needs new keys, and moved copy keeps its key.
- Be explicit about what is *not* changing, so nothing gets rewritten by accident.
