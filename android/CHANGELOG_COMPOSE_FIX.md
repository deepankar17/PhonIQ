# Changelog: Android build, Kotlin & Compose

This file is the **handoff log** for Gradle / AGP / Kotlin / Compose Compiler setup and closely related Android Studio build changes. **Gemini, Cursor, Copilot, and humans** should read it before changing tooling so prior decisions stay consistent.

## Summary (initial migration)
The project was migrated to use the **Compose Compiler Gradle Plugin**, which is mandatory starting from Kotlin 2.0. This replaces the previous mechanism where the compiler was bundled with the Kotlin Gradle Plugin or controlled solely by `buildFeatures { compose = true }`.

## Changes Made

### 1. `gradle/libs.versions.toml`
Added the following plugin definitions to ensure version consistency:
```toml
[plugins]
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

### 2. Root `build.gradle.kts`
Added the plugins to the root project to manage versions globally:
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
```

### 3. `app/build.gradle.kts`
Applied the Compose Compiler plugin to the application module:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
```

## Why This Fix is Needed
Starting with **Kotlin 2.0.0**, the Compose compiler is now part of the Kotlin repository. The previous `kotlinCompilerExtensionVersion` in `composeOptions` is **deprecated** and no longer used. Without applying `org.jetbrains.kotlin.plugin.compose`, Gradle sync will fail with the error:
> "Starting in Kotlin 2.0, the Compose Compiler Gradle plugin is required when compose is enabled."

## Instructions for AI Assistants (Cursor / Copilot / Gemini)

### Do not revert (Compose + Kotlin 2.0+)
- **DO NOT** remove `alias(libs.plugins.kotlin.compose)` from the app `plugins` block.
- **DO NOT** remove the matching `kotlin-compose` / `kotlin-android` entries from the version catalog and root `apply false` wiring **unless** you are deliberately migrating to another supported setup—and you **must** document that migration in **Append-only changelog** below.
- **DO NOT** add `composeOptions { kotlinCompilerExtensionVersion = "..." }` back; use the **`composeCompiler { ... }`** DSL when non-default compiler options are required.

### When you MUST update this file (append a new entry)
Any time you change **any** of the following, add a dated bullet under **Append-only changelog** (include what changed, why, and files touched):

- `android/settings.gradle.kts`, root `android/build.gradle.kts`, `android/app/build.gradle.kts`
- `android/gradle/libs.versions.toml`, `android/gradle.properties`, `android/gradle/wrapper/*`
- Compose compiler / Kotlin Gradle plugins, `buildFeatures { compose }`, `composeCompiler { }`, AGP or Gradle major/minor bumps that affect sync
- **Compilation fixes** related to `@Composable` scope, Compose runtime errors, or Kotlin syntax issues that block the build.

Short one-line entries are fine; the goal is so **Gemini (or anyone) can read the latest intent** without re-deriving history from git alone.

---

## Append-only changelog

| Date (UTC) | Author / tool | Summary |
|------------|-----------------|--------|
| _(initial)_ | Android Studio / Gemini | Kotlin 2.0+ Compose Compiler Gradle plugin migration; see sections above. |
| 2026-05-03 | Cursor | Broadened file scope to “Android build, Kotlin & Compose”; added **append-only changelog** table and rules so future Gradle/Compose/plugin edits are recorded here for Gemini / other assistants. |
| 2026-05-04 | Android Studio AI | Fixed `@Composable` invocation errors in `MainActivity.kt`, `MessagesScreen.kt`, `MoneyScreen.kt`, and `PhoneScreen.kt` by replacing `stringResource()` with `LocalContext.current.getString()` in non-composable lambdas. |
| 2026-05-04 | Cursor | Mockup-aligned UI (no Gradle changes): Phone **quick-call** strip; Money **tools** chip row + budget bar + section order per `design/phoniq-mockup-v1.html`; **Settings** full-screen (Personalization + Data & device); **ProtoWireOverlay** for ⋮ wires; Messages **avatar** initials; bottom nav **surfaceContainer** + indicator; `PhoniqSecondary` token; `ShellMenuAction` expanded for Money menu. |
