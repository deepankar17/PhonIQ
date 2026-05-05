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
| 2026-05-04 | Cursor | Cursor rules: `.cursor/rules/changelog-after-big-changes.mdc` (**alwaysApply**) so substantial repo changes append **`android/CHANGELOG_COMPOSE_FIX.md`**; workspace-parent copy under `personal/.cursor/rules/phoniq-changelog-updates.mdc` (**globs:** `phoniq/**`). |
| 2026-05-02 | Cursor | **Compose compile fix:** removed explicit `import …layout.weight` from `MessagesScreen.kt` (that import resolves to **internal** `RowColumnParentData.weight` and breaks `:app:compileDebugKotlin`). **No Gradle changes.** Also: **Phone** `SecondaryTabRow` + recents from `MainActivity` `mutableStateListOf`; delete-all menu opens **AlertDialog**, clears list, snackbar. Files: `MessagesScreen.kt`, `MainActivity.kt`, `PhoneScreen.kt`. |
| 2026-05-02 | Cursor | **Messages ⋮ → bottom sheets (M3 `ModalBottomSheet`):** **Mark all read** updates shell-hoisted `mutableStateListOf<MessageThread>` (`unread = false`) + snackbar; **Inbox cleaner** sheet + dry-run snackbar. `wireStrings()` returns **null** for those actions. New `MessagesOverflowSheets.kt`; `ShellMenu.kt`, `strings.xml`, `MainActivity.kt`, `MessagesScreen.kt`. **No Gradle changes.** |
| 2026-05-02 | Cursor | **Mockup shell parity (no Gradle):** `PhonIQTopBar` → unified row: **gradient tab icon** + PhonIQ + **pill search** (tab-specific placeholders per HTML); **Messages** filter strip **icons + counts**; **compose FAB**; thread rows **RCS + pills + typing hint**; **`SampleData.messageThreads`** expanded toward HTML “All” list; **Money ⋮** order matches mockup; bottom nav **accent pill / translucent bar**; global search field uses **“Search calls, SMS, contacts…”**. Files: `PhonIQTopBar.kt`, `MessagesScreen.kt`, `Messages.kt`, `SampleData.kt`, `MainActivity.kt`, `GlobalSearchOverlay.kt`, `strings.xml`. |
| 2026-05-02 | Cursor | **Track A — Phone recents vs mockup:** `RecentCall.metaCaption` for mock `call-number` lines; **`SampleData.recentCalls`** 14 rows + **`quickCalls`** 20 entries aligned with `phoniq-mockup-v1.html`; **Recent Calls** section label; **dividers** between rows; duplicate **missed-count** chip suppressed when caption already has `Missed (n)`. Files: `Calls.kt`, `SampleData.kt`, `PhoneScreen.kt`, `strings.xml`. **No Gradle changes.** |
| 2026-05-04 | Cursor | **p10/p12/p15/p16/p18 — Remaining parity items:** **p10** confirmed done (edge-to-edge via `enableEdgeToEdge()` already present). **p12** `SettingsOverlay` expanded: full `PersonalizationBody` with accent swatches (10 colors), theme/dialpad/font/size chip rows, Display+CallScreen+Messages+Money+Privacy toggle sections (13 toggles total); Root now shows "General" group + "Widgets & shortcuts" row. **p15/p16** `ProtoWireOverlay` refactored from title+body strings to `action: ShellMenuAction` directly — CommInsights renders stat rows + chart placeholder, AfterCall renders action rows + footer, WhoIsThis renders context rows + Open thread CTA; all other actions fall through to text copy. `MainActivity` updated to `wireAction: ShellMenuAction?`. **p18** Contacts panel now has rounded `OutlinedTextField` search bar, filtered `SampleData.contacts`, "All Contacts" section label, and `BlockedSection` (Block icon + "3 blocked" + manage button). Files: `SettingsOverlay.kt`, `ProtoWireOverlay.kt`, `MainActivity.kt`, `PhoneScreen.kt`, `strings.xml`. **No Gradle changes.** |
| 2026-05-04 | Cursor | **p17 — OTP countdown + copy in message list:** `MessageThread.otpCode + otpExpiresSeconds`; `SampleData` sets HDFC (3 min) and VFS (8 min) OTP codes; `OtpListStrip` composable (ticking `LaunchedEffect` countdown, auto-dims to "Expired", Copy button with Toast); color-coded `pillColors()` for OTP (teal), TXN (amber), BILL/Overdue (red), Promo (purple). Files: `Messages.kt`, `SampleData.kt`, `MessagesScreen.kt`, `strings.xml`. **No Gradle changes.** |
| 2026-05-04 | Cursor | **p13 — Money screen mockup parity:** `MoneySummary.savingsLabel` (replaces `incomeLabel`); `CategorySpend.emoji + budgetLabel`; `RecentTransaction` model; `SampleData` aligned to mockup values (April 2026, 5 categories, 5 txns); `MoneyScreen` rewritten — `SummaryHeroCard` with teal savings row, `SpendingDonutCard` (5-color donut + legend + center text via `Box` overlay), `BudgetTrackerGrid` (2×2 grid with emoji + colored mini-bars), `RecentTransactions` list. Files: `Money.kt`, `SampleData.kt`, `MoneyScreen.kt`, `strings.xml`. **No Gradle changes.** |
| 2026-05-04 | Cursor | **Track B — Messages thread overlay (`#overlay-sms-thread`):** **`ConversationBubble`** + **`ThreadConversationScript`** (`ThreadConversation.kt`); **`ThreadConversationSamples`** keyed by thread id (`ThreadConversationSamples.kt`); **`MessageThread.peerAddress`** (`Messages.kt`, `SampleData.kt`); full-screen **`ThreadDetailOverlay`** (RCS/SMS bars, bubble variants, OTP/txn cards, composer strip); **`MessagesScreen` / `MainActivity`** `onThreadAction` snackbar wire; strings. **Build note:** concurrent Gradle runs can corrupt Kotlin incremental caches under `app/build/kotlin`; `gradlew --stop`, delete that folder, single `assembleDebug` recovers. |
