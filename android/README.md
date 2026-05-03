# PhonIQ — Android app

Monorepo root: [github.com/deepankar17/PhonIQ](https://github.com/deepankar17/PhonIQ) (`gh repo clone deepankar17/PhonIQ`).

Kotlin + **Jetpack Compose** shell aligned with `design/phoniq-mockup-v1.html` and `docs/PROJECT.md`.

## Requirements

- Android Studio **Ladybug** (or newer) with **AGP 8.7+** and **JDK 17**
- **Device or emulator: Android 15 (API 35) or newer** — `minSdk` is **35** so PhonIQ does not ship to older OS tiers (15 / 16 market focus). The app still runs on **Android 16** devices.
- **compileSdk / targetSdk:** **35** on this toolchain (AGP 8.x). When you adopt **AGP 9+** / Gradle 9.x, bump to **36** to match Android 16 platform behaviors for Play / policy.

## Open

1. **File → Open** and select this folder: `phoniq/android/`
2. Let Gradle sync finish (Android Studio will fetch the Gradle wrapper if needed).
3. Run the **app** configuration on a device or emulator.

## Current scope (v0.1)

- `com.phoniq.app` application id / namespace
- **No `INTERNET` permission** in the manifest (offline-first pillar)
- **Bottom navigation:** Phone · Messages · Money (placeholder screens)
- Dark **Material 3** colors roughly matching the HTML mockup accent (`#6C63FF`)

## Next implementation steps (suggested order)

1. **Dialer role** — `MANAGE_OWN_CALLS` / default dialer flow, `InCallService`, Room for recents (see `PROJECT.md`).
2. **SMS role** — default SMS app, `Telephony.Sms`, parser pipeline.
3. **Money** — SMS parser module + Room entities matching mock Money flows.

Refer to `../design/phoniq-mockup-v1.html` for UX parity as you replace placeholders.
