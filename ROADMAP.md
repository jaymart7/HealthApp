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

Cheap and self-contained first; the test/CI gate before the two large mechanical items.
(The adaptive work was item 5 and has landed; localization was meant to precede it so the new
layout code would be written against `stringResource` — it wasn't, so the rail, the two pane
placeholders and the detail header carry literals for that pass to pick up.)

| # | Feature | Scope | Schema |
|---|---------|-------|--------|
| 1 | Custom food library | three modules | none (reuses `favorite_food`) |
| 2 | Automatic local backup | a package move, then `:app` | — |
| 3 | UI test pass + CI gate | infrastructure | — |
| 4 | Localization scaffolding | every module | — |

---

## 1. Custom food library

**What.** Author a food once — name, portion, macros, micronutrients — without having logged it
first; edit it later; find it in food search. Rename and delete from Profile.

**Where.** `:core:data/food/` (`FoodRepository`, `FoodRepositoryImpl`, `local/FavoriteFoodDao.kt`),
`feature/food/ui/search/` and `ui/shared/`, `feature/profile/ui/library/`.

**Schema / export.** **No new table and no migration.** `favorite_food` is already keyed by
`name` and already carries the full nutrition snapshot including fiber, sugar and sodium, with
`isFavorite` as its soft-delete flag. Custom foods stay **out of the export**, for the reason
saved meals, recipes and routines are out — convenience data, not history. No export bump.

### Decisions

- **A custom food is a `favorite_food` row with no diary row behind it.** That table was already
  designed to survive its origin — "carries its own macros so a re-star doesn't depend on the
  original diary row still being there" — so authoring one directly needs no new concept, only a
  write path and an edit path. Resist adding a `custom_food` table: it would be the same columns
  keyed the same way, and two tables answering "what are this food's macros" is how the search
  panel and the suggestion list come to disagree.
- **`name` stays the identity**, case-insensitively, the key `mergeSuggestions` already dedupes
  on. Authoring a food whose name matches an existing favorite **edits that row** rather than
  creating a rival — the primary key already enforces it, and the UI should say so rather than
  letting the write silently win.
- **Custom foods are searchable; recents and favorites are not.** The diary's top field is a
  local filter over logged entries, `searchCommonFoods()` is the food search, and this is
  the first thing the user owns that belongs in the second. That search returns
  `ScannedProduct`s, so a custom food maps to one on the way out — a text search and a barcode
  scan already resolve to the same type, and a third would fork the confirmation screen.
- **The user's own foods lead, and they are drawn with no AI accent.** `COMMON_FOODS` follows,
  deduped against them by the same case-insensitive name key — a custom "Chicken breast" replaces
  the built-in one rather than sitting beside it. Neither half touches the network.
- **Profile → Saved meals & recipes gains the third list; it cannot log.** The division the food
  library already draws: logging needs a meal slot and a day, and Profile has neither. Delete
  asks first — a custom food is user-authored, the saved-meal rule, not the diary's
  swipe-and-undo. Rename is the one column `LibraryRow`'s existing `RenameSheet` already writes.
- **Authoring reuses the add-entry form; it does not get a second one.** `AddEntryForm` already
  holds every field including the micronutrient group and the portion repricing, and its
  `isValid()` is the shared guard three log paths already run through.

**Deliberately excluded.** Per-food targets or grading — fiber, sugar and sodium are reported,
never graded. No barcode attached to a custom food (barcode memory is declined, below). No
export, no sync, no sharing a food between installs.

**Check.** One JVM test that a custom food and a `COMMON_FOODS` entry with the same name collapse
to one result, the custom one winning.

---

## 2. Automatic local backup

**What.** A weekly job writing the existing export JSON to app storage, keeping the last three,
plus making Android's own backup coverage explicit rather than accidental.

**Where.** **First, a move:** the export DTOs, `EXPORT_SCHEMA_VERSION` and the build/parse pair
leave `feature/profile/ui/profile/ProfileExport.kt` (389 lines) for `:core:data/transfer/`,
beside `ImportData` and `DataTransferRepository` which are already there. `ProfileScreen`'s two
SAF launchers and its ViewModel stay in `:feature:profile`. Then a new
`app/src/main/java/ph/mart/healthapp/backup/BackupWorker.kt`, and edits to
`res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml`.

**Schema / export.** No schema change. The move must not change the file format or the version
number — a v15 file written before the move must still import after it.

### Decisions

- **The move is the feature's first step and is not optional.** A WorkManager job lives in `:app`
  because a background job is a system surface, not a screen — the rule reminders and the widget
  both follow — and `:app` reaching into `:feature:profile`'s `ui` package to serialize a backup
  would breach the module map in the one direction it forbids. `transfer/` already exists in
  `:core:data` and already holds the import half; the export half was only ever in
  `:feature:profile` because the screen that triggers it is.
- **`DataTransferRepository` still gets no `exportAll` twin.** That decision holds: reading is a
  set of independent `all*()` calls that cannot leave anything inconsistent, and each already
  lives on its own domain's repository. The worker makes those calls itself and hands them to the
  builder, exactly as `ProfileViewModel` does — the serialization moved, the shape did not.
- **Backups are written to internal storage, not to a user-chosen folder.** SAF needs a picker, a
  picker needs a user, and a user is the thing a background job does not have. `filesDir` is also
  what Android's own backup covers, so one write serves both mechanisms.
- **Three files, rotated oldest-first.** A year of weekly backups is a year of near-identical
  JSON in the app's own storage; three is a bad week, a bad fortnight and a bad month. Mark the
  count with a `ponytail:` comment — a size budget is the upgrade path if the files ever get big
  enough to matter.
- **Android Auto Backup is already on and must be made explicit.** `allowBackup="true"` with
  stock, entirely-commented-out `backup_rules.xml` and `data_extraction_rules.xml` means the Room
  database is being backed up to the user's Drive *by default* — undeclared, untested, and
  silently capped at 25 MB. Write the rules out: include the database and the backup directory,
  exclude the progress-photo images (they are the one thing that can blow the cap, and the export
  has never carried them either). An offline-first app with no account has exactly one answer to
  a lost phone, and leaving it to a default nobody wrote down is not it.
- **The job is not a `Reminder`.** It posts no notification and has no Profile switch: every entry
  in that enum is a nudge whose `ordinal` is a notification id, and a silent backup is neither.
  It is enqueued from `FitPulseApplication` beside the reminder reconciliation, with
  `ExistingPeriodicWorkPolicy.KEEP`.
- **Restore stays manual.** The existing import is all-or-nothing and destructive by design; a job
  that restored on its own is a job that could wipe a good device from a stale file. Profile →
  Data gains a row listing what is on disk and pointing the existing import at it.

**Deliberately excluded.** Cloud backup of our own — no account exists, and nothing may assume
one. Progress photos in the backup. Automatic restore. Encryption: the file sits in app-private
storage, and the manual export already writes plaintext JSON wherever the user points it.

**Check.** A JVM round-trip test that build → parse survives the package move unchanged, plus one
for the rotation keeping exactly the newest three.

---

## 3. UI test pass + CI gate

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

## 4. Localization scaffolding

**What.** Move every user-facing string out of Kotlin and into per-module `strings.xml`. No
translation is added; this is the work that makes one possible.

**Where.** Every module. `app/src/main/res/values/strings.xml` is 3 lines today; the feature
modules and `:core:designsystem` have no `strings.xml` at all. Rough literal counts —
`:feature:food` 432, `:feature:progress` 374, `:feature:profile` 237, `:core:designsystem` 184,
`:feature:home` 131, `:app` 62, `:feature:onboarding` 61, `:wear` 36, `:feature:coach` 27 — are
an over-count including keys and SQL, so expect 800–1000 genuinely user-facing across 419 files.

**Schema / export.** None. Nothing persisted may become a resource — see below.

### Decisions

- **Each module owns its own `strings.xml`.** The module map is the boundary; a central string
  file would be one file every feature edits, and the one place merges collide.
- **Keys are `<module>_<screen>_<thing>`**, flat, no nesting. Enough to grep, not a taxonomy.
- **Composables take `stringResource`, not a `Context`.** Previews resolve resources, so
  `@PreviewLightDark` keeps working with no wrapper change — which makes the previews the
  migration's own smoke test, file by file.
- **What must NOT move: anything persisted or compared.** `QUICK_ADD_NAME` is written into the
  diary and excluded from the recents query by that exact string; `MascotCharacter`,
  `MascotPalette` and `HomeCard` names are stored on `Profile` as strings and parsed back;
  `Reminder.title`/`body` are enum fields on a system surface; Room queries, Data Layer message
  paths and the export DTOs' `@SerialName`s are wire formats. A translated key would rewrite a
  user's Home layout the first time they changed language. **Each of these gets a comment saying
  why it stayed** — otherwise the next pass "finishes the job" and breaks it.
- **The mascot's five state names and the AI prompts stay in Kotlin too.** The prompts are sent
  to a model in English; translating them would change what comes back, not who reads it.
- **`:wear` gets the same treatment**, and its `strings.xml` stays its own — the two APKs update
  independently.
- **One module per commit.** This is the change most likely to be reviewed by skimming, and a
  419-file single diff is unreviewable.

**Deliberately excluded.** Any actual translation. Plurals beyond where a string is already
pluralised in Kotlin today. RTL layout work — `supportsRtl` is already true and untested, and it
is the adaptive work's neighbour rather than this item's. In-app locale switching.

**Check.** `lint` with `HardcodedText` promoted to error for the modules already converted, which
is also what stops the next feature adding literals back.

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
