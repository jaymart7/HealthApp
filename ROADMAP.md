# ROADMAP.md — what FitPulse is going to do

The planned-feature index, written so a session can implement one section without asking a
question the section should have answered.

`FEATURES.md` is the present tense — if it's listed there it ships. This file is the future
tense, and it never describes something that already exists. A feature **moves out of here and
into `FEATURES.md` when it lands**, and its decisions move into `CLAUDE.md`.

Each spec below names its module and package, says whether it touches the schema or the export
version, pre-argues the design decisions in `CLAUDE.md`'s voice so they don't get re-litigated at
implementation time, lists what it deliberately excludes, and names one runnable check. **If a
spec turns out to be wrong, say so and stop — that is what it's for. Silence is not in the
contract.**

`CLAUDE.md` remains binding. Nothing here overrides a non-negotiable constraint, the module map,
or a decision in its decision log; where a spec changes one, it says so explicitly.

---

## Order

One item left. (The adaptive work has landed, and so has the localization pass that was meant to
precede it — every module now reads its copy from `strings.xml`, so anything written from here on
has a `checkUiLiterals` gate to satisfy. Automatic local backup has landed too; its decisions are
in `CLAUDE.md`.)

| # | Feature | Scope | Schema |
|---|---------|-------|--------|
| 1 | UI test pass + CI gate | infrastructure | — |

---

## 1. UI test pass + CI gate

**What.** Instrumented Compose tests for the flows that would otherwise break silently, and a
GitHub Actions workflow running build + unit tests on push.

**Where.** `app/src/androidTest/`, per-module `src/androidTest/` where a flow lives, new
`.github/workflows/build.yml`.

**Schema / export.** None.

### Decisions

- **The deps are already wired.** `:app` has `ui-test-junit4`, `espresso-core`, `androidx-junit`
  and `ui-test-manifest`; the only instrumented test in the repo is the generated
  `ExampleInstrumentedTest`. Nothing new goes in the version catalog.
- **CI runs `assembleDebug` and `testDebugUnitTest` only — not the instrumented tests.** An
  emulator in CI is a large, slow, flaky dependency for a solo project, and the 59 JVM tests are
  where the derivation logic already lives. Instrumented tests are a local pre-release gate; say
  so in the workflow rather than pretending otherwise.
- **`fdcApiKey` is absent in CI, and the build must still pass.** That is the documented
  degrade-gracefully rule the release signing config also follows — the workflow proves it rather
  than working around it with a secret.
- **Test what has no JVM test and would break silently**, not everything: onboarding's seven
  steps writing a profile, the add-entry sheet's shared `isValid()` across its three log paths,
  the diary's date navigation stopping at today, and predictive back through the photo flow's
  four states. Every one is a decision `CLAUDE.md` argues and no unit test can reach.
- **No new test framework.** No Robolectric, no MockK, no Turbine — the existing tests are plain
  JUnit over pure functions, and a second idiom is a second thing to keep in step.

**Deliberately excluded.** Screenshot/snapshot testing. Coverage thresholds. A CI gate on
instrumented tests. Compose stability CI — the `enforcing-stability-in-ci` skill exists, but that
is a separate decision and not on this roadmap.

**Check.** The workflow itself — it must pass on a clean checkout with no `fdcApiKey`.

---

## Considered and declined

Weighed this round and deferred. These are **not** `FEATURES.md`'s "Deliberately absent" list —
that one is for what was ruled out on principle. Each of these could be reopened; the note says
what would reopen it.

- **Open Food Facts as a second food source.** FoodData Central is a US database. For a `ph.mart`
  app, local dishes and locally-packaged products largely return nothing, and OFF is free,
  keyless and internationally stocked. Reopened by: the search and scan miss rate on real use.
- **Barcode memory (a local product cache).** A scan always hits the network, so the scanner is
  dead offline and a rescan re-spends the app-wide 3600 req/hour key budget. Reopened by: either
  the FDC ceiling or the exposure starting to matter — the proxy already on `CLAUDE.md`'s backlog
  is the neighbouring fix.
- **A second pane for the food diary.** The adaptive work shipped without one: the diary is a
  single scrolling day with no list to put beside it, and its add sheet is an `AppBottomSheet`
  whose panels and form are built for a sheet's scroll. Reopened by: the calendar swap-in
  (`FoodScreenState.calendarOpen`) beside the day, which is the pane that would earn itself.
- **Keeping the analyzed meal photo on the diary entry.** A visual food history. Declined as the
  heaviest of the three: storage growth, downsampling, and an export question the export has
  always answered "no" to for images.
