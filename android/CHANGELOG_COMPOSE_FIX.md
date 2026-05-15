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
| 2026-05-05 | Cursor | **Money tab — pixel alignment with `design/phoniq-mockup-v1.html`:** gradient hero, 6dp budget bar, 110dp donut, 4-card budget grid, txn styling, tools strip chips; `Color.kt`, `MoneyScreen.kt`, `MoneyToolsStrip.kt`, `strings.xml`. **No Gradle changes.** |
| 2026-05-05 | Cursor | **Phone & Messages mockup parity (`phoniq-mockup-v1.html`):** Recent calls — flat rows (10×16 padding, 44dp gradient avatars, direction icons + meta colors, `PhoniqBorderSoft` dividers), pill **recent-filter-chip** strip (6dp gap, 999 radius), **MockupSectionLabel** for RECENT CALLS; quick strip — 61px columns, 42×14dp avatars, 8/16/2 padding, no card chrome. **FAB** 52dp, 18dp corners, accent→secondary gradient. Messages — **msg-tab** chips (4dp gap, 8×12 padding, count badges), **wa-thread** rows (46dp avatars, online dot, RCS micro-badge, preview + inline pills, dividers), teal **compose** FAB 48dp/16dp gradient. Models: `RecentCall.avatarStart/EndArgb`, `MessageThread` avatar + `showOnlineDot`; `SampleData` gradients; shared `ui/components/MockupSectionLabel.kt`; Money uses same section label. Removed list **OtpListStrip** (OTP remains in thread overlay). **No Gradle changes.** |
| 2026-05-14 | Cursor | **Planned slice — export, bills, policies, backup:** `MoneyExportBottomSheet` (CSV/PDF) from ⋮ + tools; Messages **Bill hygiene** `ModalBottomSheet` (`BillHygiene` kind) with bill threads + `billDueHintLabel()` heuristics; `ContactPoliciesBottomSheet` from contact detail; Settings **Data & device** — SAF `CreateDocument` copy of `phoniq.db` via `LocalDatabaseExport` (WAL checkpoint) + honest **Drive not available** dialog; `docs/PROJECT.md` checkboxes. Files: `MainActivity.kt`, `MessagesOverflowSheets.kt`, `Messages.kt`, `MoneyExportBottomSheet.kt`, `ContactPoliciesBottomSheet.kt`, `ContactDetailOverlay.kt`, `PhoneScreen.kt`, `SettingsOverlay.kt`, `strings.xml`, `LocalDatabaseExport.kt`. **No Gradle changes.** |
| 2026-05-14 | Cursor | **Messages:** opening `ThreadDetailOverlay` now marks all SMS in that `thread_id` read (`SmsDao.markThreadRead` → `SmsRepository` / `MessagesViewModel`); list unread badge + global unread count refresh via existing Room flows. Files: `SmsDao.kt`, `SmsRepository.kt`, `MessagesViewModel.kt`, `ThreadDetailOverlay.kt`. **No Gradle changes.** |
| 2026-05-14 | Cursor | **Compliance + perf hygiene:** `lifecycle-runtime-compose`; `collectAsStateWithLifecycle` in shell + call state + launch routes; `SmsMutex` full sync + receiver **5s** throttle prefs; Messages chips minimum touch target + semantics; settings/contact semantics merges + restore/incoming-copy; docs/README INTERNET alignment. |
| 2026-05-14 | Cursor | **Messages compose FAB:** `ACTION_SENDTO` + `smsto:` with no number resolved to PhonIQ’s own `MainActivity`; `consumeLaunchIntent` skipped blank destinations so nothing visible. FAB now routes **`PhonIQLaunchRouter.offerBlankSmsCompose()` → in-app blank `ThreadDetailOverlay`**; SENDTO/smsto with empty recipient likewise. Files: `PhonIQLaunchRouter.kt`, `MainActivity.kt`, `MessagesScreen.kt`. |
| 2026-05-14 | Cursor | **Dialpad live search:** T9/name + normalized **phone substring** match; 75 ms debounce; filter on `Dispatchers.Default`; matches under composed number in `DialpadContent` (scroll with pad); fullscreen overlay wired with `allContacts` + `recentCalls`. Files: `DialpadContactSearch.kt`, `DialpadSheet.kt`, `FullScreenDialpadOverlay.kt`, `MainActivity.kt`, `strings.xml`. **No Gradle changes.** |
| 2026-05-14 | Cursor | **Contact avatars — list + header scale-up:** ~3-line row alignment (M3-ish): recents **`RecentCallRow`** 44→56dp; messages **`ThreadRow`** 46→56dp; **`ContactDetailHeader`** 72→88dp (+ initials 26→28sp); **`GlobalSearchOverlay`** call/message hit rows 44/46→56dp; **`ThreadChatHeader`** 38→52dp + `titleMedium` initial. **No Gradle changes.** |
| 2026-05-14 | Cursor | **Phone Favorites grid:** star badge clears **Contacts.STARRED** (was opening detail only — whole tile `Surface` click); avatar/name open detail; larger **68dp** avatars + star badge for the 3-column grid only. **`PhoneViewModel.unstarDeviceContact`**, `strings.xml` `cd_favorites_remove_star`. **No Gradle changes.** |
| 2026-05-14 | Cursor | Phone **QuickCallStrip**: tile width `(viewport − 2×16dp − 4×8dp spacing) ÷ 4.5` for ~four full + half-peeking fifth; **56dp** avatars **18sp** (aligned with `RecentCallRow`); `BoxWithConstraints` + symmetric horizontal padding preserves RTL. **No Gradle changes.** |
| 2026-05-15 | Cursor | **Messages UX:** main inbox strip is **All / Unread / Archived** only (removed OTP/txn/bill/delivery/etc. top filters + parsed-txn list + delivery cards); thread rows no longer show category **pills**. **`ThreadDetailOverlay`** adds horizontal **category filter chips** (All + types present in thread only), using `SmsMessageEntity.category` → `messageThreadCategories()` + **`isInvestmentTxnSms`** (`INV` parity); **`Money`** tab deep-links unchanged; Bill hygiene **focus** still opens Messages (no auto Bill filter). **`msg_filter_investment`**. Files: `MessagesScreen.kt`, `ThreadDetailOverlay.kt`, `MainActivity.kt`, `EntityMappers.kt`, `strings.xml`. **No Gradle changes.** |
| 2026-05-15 | Cursor | **Recents vs WhatsApp:** `CallLogRepository` KDoc — unified `CallLog.Calls` only (no private WhatsApp API); broader WhatsApp/Business/JID/ConnectionService detection in `resolveStoredCallChannel`; `dedupeCallsLatestFirst` dedupes by **number + callChannel** so PSTN and WhatsApp can both show. Files: `CallLogRepository.kt`, `PhoneNumbers.kt`. **No Gradle changes.** |
| 2026-05-15 | Cursor | **Phone Recent calls — inbox-style windowing:** `RECENT_CALLS_PAGE_SIZE = 10`, scroll threshold + 120 ms debounce aligned with `MessagesScreen`; pull-to-refresh resets visible window + scroll to top; filter tab still resets via `remember(filter)`. Full list remains `PhoneViewModel.recentCalls` (in-memory); UI slices only. File: `PhoneScreen.kt` (`RecentCallsPanel`). **No Gradle changes.** |
| 2026-05-15 | Cursor | **Messages inbox paging:** LazyColumn threads window **10** initial, **+10** near-end (`snapshotFlow` + debounce); filter strip still resets scroll + window per category; VM `messageThreads` KDoc notes full-thread materialization tech debt vs future Room/Paging. Files: `MessagesScreen.kt`, `MessagesViewModel.kt`. **No Gradle changes.** |
| 2026-05-15 | Cursor | **Removed dead mock wire UI:** deleted unused `ProtoWireOverlay.kt` (hardcoded insights / Priya stats; shell uses real `CommunicationInsightsOverlay`, dialogs, etc.). Dropped unused `ThreadConversationScript`; clarified `ConversationBubble` KDoc. Renamed settings `MockPhoniqToggle` → `PhoniqAccentSwitch`. **` :app:compileDebugKotlin` OK.** |
| 2026-05-15 | Cursor | **Fullscreen dialpad UX:** `FullScreenDialpadOverlay` is a single full-page `Surface` (no half-height scrim split); top app bar + `DialpadContent` with `weight(1f)` so keypad/call FAB are not clipped; `WindowInsets.safeDrawing` retained; dismiss via system back / `Dialog`, toolbar back (removed tap-on-scrim). File: `FullScreenDialpadOverlay.kt`. **No Gradle changes.** |
| 2026-05-15 | Cursor | **Contacts list — one row per device person:** Room still stores one row per `Phone` MIME row; UI groups by `deviceContactId` via `aggregateContactsToRows()` (replaces per-row `toContactRow` + `dedupeForFavoriteDisplay`); `ContactRow.allPhoneNumbers` + `effectivePhoneNumbers()`; detail merges call history across keys; dialpad skips duplicate aggregates. Files: `EntityMappers.kt`, `Calls.kt`, `PhoneScreen.kt`, `ContactDetailOverlay.kt`, `PhoneViewModel.kt`, `DialpadContactSearch.kt`. **No Gradle changes.** |
| 2026-05-15 | Cursor | **Avatar initials optical centering:** Shared `AvatarInitialsText` (`includeFontPadding = false`, `LineHeightStyle`, `TextAlign.Center`, `fillMaxSize`) used for remaining gradient placeholder avatars — **Messages** thread rows, **ThreadDetailOverlay** header, **GlobalSearchOverlay** call/message rows, **Money** `AccountBalanceSection` bank chip. **No Gradle changes.** |
| 2026-05-15 | Cursor | **Removed pull-to-refresh** (`PullToRefreshBox`) from **Phone** + **Messages** to avoid accidental refresh while paginated scrolling; **`runDeviceSync()`** still on **permission grant** (no ⋮/Settings sync entry in app). Files: `MainActivity.kt`, `PhoneScreen.kt`, `MessagesScreen.kt`. |
| 2026-05-15 | Cursor | **SMS thread short codes:** hide reply strip / emoji compose when peer is a heuristic short code or alphanumeric sender ID; info bar + `thread_short_code_no_reply`; `isShortCodeAddress` + `MessageThread.isShortCodeThreadPeer()` in `PhoneNumbers.kt`. Blank FAB compose (`peer` empty) unchanged. Files: `ThreadDetailOverlay.kt`, `PhoneNumbers.kt`, `strings.xml`. **` :app:compileDebugKotlin` OK.** |
| 2026-05-15 | Cursor | **Short-code thread message layout:** when `thread.isShortCodeThreadPeer()` (`blockShortCodeReply`), thread list uses **full-width** bordered `Surface` cards (not left/right chat bubbles) for text/OTP/txn/link-preview/voice/typing; normal threads unchanged. `ConversationBubbleBlock(..., shortCodeFeedLayout)` wired from existing flag. File: `ThreadDetailOverlay.kt`. **` :app:compileDebugKotlin` OK.** |
| 2026-05-15 | Cursor | **SMS thread unread UX:** list badge uses `MessageThread.unreadCount`; `ThreadDetailOverlay` defers `markThreadRead` until after scroll — jump to first unread bubble in loaded page (else bottom), optimistic local `isRead` strip for banner; unread banner under chips + **jump to newest** `SmallFloatingActionButton` when `LazyListState.canScrollForward`. Files: `Messages.kt`, `EntityMappers.kt`, `MessagesScreen.kt`, `ThreadDetailOverlay.kt`, `strings.xml`. **` :app:compileDebugKotlin` OK.** |
| 2026-05-15 | Cursor | **Messages thread row txn badge:** ₹ **subtitleBadge** only when the **latest** SMS in the thread parses as a transaction (`SmsParser` on last message body), not from an older txn in `toMessageThreads()`. **`EntityMappers.kt`**. **No Gradle changes.** |
