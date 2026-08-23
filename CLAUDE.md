# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

HealthApp is a bare Android Studio "Empty Activity" Compose project (single `MainActivity`, generated theme files). There is no custom architecture, networking, persistence, or navigation yet — treat any structural decisions as greenfield.

- Package / namespace / applicationId: `ph.mart.healthapp`
- minSdk 24, targetSdk / compileSdk 37
- Kotlin 2.2.10, AGP 9.3.1, Compose BOM 2026.02.01
- Single module: `:app`

## Commands

Run from the repo root using the wrapper (`./gradlew`).

- Build debug APK: `./gradlew assembleDebug`
- Install on connected device/emulator: `./gradlew installDebug`
- Run unit tests (JVM, `app/src/test`): `./gradlew testDebugUnitTest`
  - Single test class: `./gradlew testDebugUnitTest --tests "ph.mart.healthapp.ExampleUnitTest"`
- Run instrumented tests (device/emulator required, `app/src/androidTest`): `./gradlew connectedDebugAndroidTest`
- Full check (lint + unit tests): `./gradlew check`
- Lint only: `./gradlew lintDebug`
- Clean build outputs: `./gradlew clean`

## Architecture

- `app/src/main/java/ph/mart/healthapp/MainActivity.kt` — sole entry point; a `ComponentActivity` that sets Compose content via `setContent` and wraps everything in `HealthAppTheme`.
- `app/src/main/java/ph/mart/healthapp/ui/theme/` — generated Material 3 theme (`Theme.kt`, `Color.kt`, `Type.kt`); no custom design tokens yet.
- Dependency versions are centralized in `gradle/libs.versions.toml` (version catalog) and referenced via `libs.*` aliases in both `build.gradle.kts` files — add new dependencies there rather than hardcoding coordinates in `app/build.gradle.kts`.
- `release` build type currently has code optimization disabled (`optimization { enable = false }` in `app/build.gradle.kts`).
