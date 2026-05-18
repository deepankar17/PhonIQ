# PhonIQ — Project Master Document
> Last updated: 2026-05-17 (Android: **adaptive launcher** + **ic_stat_phoniq**; **Firebase Hosting** scaffold — `hosting/` + `firebase.json` (default project id **`phoniq-app`** → `https://phoniq-app.web.app`; **`phoniq` is often taken**); **docs:** [`BRAND_ICON_ASSETS.md`](BRAND_ICON_ASSETS.md); **PRD §8** freemium Analytics/Billing spec. Prior shipped items unchanged.)

## Identity
- **App Name:** **PhonIQ** — *Phone* + **IQ** (capital **IQ** is intentional). Technical ids stay lowercased (e.g. `com.phoniq.app`, folder `phoniq/`).
- **Source repository:** [github.com/deepankar17/PhonIQ](https://github.com/deepankar17/PhonIQ) — clone: `gh repo clone deepankar17/PhonIQ` or `git clone https://github.com/deepankar17/PhonIQ.git`
- **Tagline:** Know every call. Track every rupee.
- **Platform:** Android — **minimum OS Android 15 (API 35)**; primary validation on **15 and 16** (no install on API 34 and below). `compileSdk` / `targetSdk` **36** with **AGP 9.2+** / Gradle 9.4.x (Play / platform alignment); `minSdk` remains **35**.
- **Package Name:** com.phoniq.app
- **Trademark Status:** Clear (verified 2026-04-30) — file in India Class 38 + 36
- **Domain:** phoniq.app (register immediately)
- **Public info site (Firebase Hosting):** Deploy from repo root (`firebase deploy --only hosting`). URLs are **`https://<project-id>.web.app`** and **`https://<project-id>.firebaseapp.com`**. The id **`phoniq`** is **globally taken** on Firebase — use another id in **`.firebaserc`** (default in repo: **`phoniq-app`**) or add a **custom domain** (e.g. `phoniq.app`) in Hosting. See **`hosting/README.md`**.

---

## Core Philosophy
- 100% offline-first core IQ (call log, SMS parse, dialer, spam heuristics): **no network use** for those features.
- **`INTERNET` + `ACCESS_NETWORK_STATE`** may be declared in the app manifest for **optional future manual** flows (e.g. one-shot Google Drive); the app **must not** use them for background sync, ads, telemetry, or third-party reputation APIs. Today, Drive backup is **not** implemented when offline-only is enforced.
- Google Drive (when implemented): **manual trigger only** — never background sync.
- Primary DB: Room (SQLite) on device storage.
- No ads. No telemetry. No routine bulk export of user content off-device without an explicit user action.

---

## Product Requirements Document (PRD) — engineering & Cursor context

Use this section as a **structured product + technical specification** when prompting Cursor (or other AI assistants): it states the *class* of app PhonIQ is, Play-scale patterns you may adopt later, and how that **differs from the current offline-first shipping bar** in **Core Philosophy**. Prefer **Core Philosophy** + **IQ Offline Spam Engine** for what v1 must *not* do without an explicit product decision.

### PRD — document purpose

- **For humans:** single checklist for dialer/SMS/spam/compliance expectations aligned with **2026-era Play policy thinking** (permissions justification, contacts handling, optional cloud).
- **For Cursor:** paste or reference this heading hierarchy so the model keeps **default handlers**, **Room**, **Telecom**, and **privacy constraints** aligned; ask for diffs that touch `AndroidManifest`, `Call*`, `Sms*`, or network code to cite this section.

### 1. Core objectives (PRD framing)

- Ship a **high-performance, privacy-conscious** replacement for the default Android **Phone** and **SMS** apps.
- Provide **caller identification** and **spam intelligence** with a **local-first** path; any **remote reputation** is **optional** and must remain subordinate to **Core Philosophy** unless the product explicitly launches a cloud tier.
- Maintain **Google Play policy awareness**: permission declarations, restricted `READ_SMS` / `READ_CALL_LOG` usage, and **no bulk contact exfiltration**.

**Platform (PhonIQ):** Android **minimum API 35 (Android 15)**; `targetSdk` **36** — stricter than generic “API 29+” PRD templates; update this PRD if `minSdk` changes.

### 2. Functional requirements

#### 2.1 Default handler modules

| Module | PRD expectation | PhonIQ (current direction) |
|--------|-----------------|----------------------------|
| **Phone / dialer** | `InCallService`, optional `CallScreeningService`; DTMF; audio routing | **`PhonIQInCallService`** (`InCallService`), **Telecom**-linked mute/speaker/hold; DTMF via `ToneGenerator`; **`CallScreeningService` not present** — add only if implementing Play-style **pre-call screening** or server-backed scores |
| **SMS messenger** | `SmsReceiver` / MMS; threaded UI; send/receive | **SMS** via broadcast + **Room**; **MMS inbox slice** as `[MMS]` rows; **compose** / **Notification reply** wired |
| **Call logs** | Unified history + spam tagging | **Room** mirror of `CallLog.Calls` + **recents** UI + **spam / trust** badges per **IQ Offline Spam Engine** |

#### 2.2 Spam & caller ID system

| Capability | Generic PRD pattern | PhonIQ |
|------------|--------------------|--------|
| **L1 fast lookup** | Local DB | **Room** (`spam_numbers`, patterns, user trust — see **IQ Offline Spam Engine**) |
| **L2 remote lookup** | Cloud Function, **~2 s timeout**, returns e.g. `spam_score` | **Not part of core IQ** today; **optional future tier** only — requires **explicit** privacy policy, **App Check**, and **no contact scraping** |
| **Community reporting** | Reports via **Cloud Functions** (avoid direct DB poisoning) | **Future**; must send **normalized number + report metadata only**, never contact dumps |
| **Auto-block** | Premium: reject if `spam_score > 80` | **PhonIQ differs:** auto-reject uses **`BlockedNumberContract`** (system block list) in **`shouldAutoRejectIncoming`**; **user-marked spam is warning-only** — see **IQ Offline Spam Engine** |

When implementing a **remote tier**, mirror the PRD’s **2.0 s timeout** and **score threshold** in **call screening** paths; keep **offline** behavior when offline or on timeout (fail open/closed per product choice, documented in app).

### 3. Technical architecture

#### 3.1 Tech stack (PRD vs repo)

| PRD | PhonIQ repo (authoritative) |
|-----|-----------------------------|
| Kotlin + Coroutines + Flow | **Yes** |
| Room | **Yes** |
| Google Analytics for Firebase | **Optional** freemium tier only — conflicts with **Core Philosophy** until consent + policy shipped; see **PRD §8** |
| Firestore + Cloud Functions | **Not required for v1 IQ**; optional later |
| Retrofit/OkHttp to HTTPS endpoints | **When** cloud exists; otherwise avoid background network |
| Hilt | **PRD recommendation** — repo currently uses **manual `ViewModelProvider.Factory`** patterns; migrate to Hilt only if/when agreed |
| `InCallService` | **Yes** (`PhonIQInCallService`) |

#### 3.2 Data tiers (PRD L1 / L2 / L3)

| Tier | PRD | PhonIQ |
|------|-----|--------|
| **L1** | In-memory active session | **`CallStateRepository`** (and related Telecom bridges) |
| **L2** | Local SQLite / top spammer set + contacts | **Room** — call log, SMS, spam keys, Money, etc. |
| **L3** | Remote index (e.g. Firestore via CF) | **Disabled for core IQ**; enable only with **Core Philosophy** update + legal review |

### 4. Compliance & permissions (Google Play–aware)

#### 4.1 Declarations / usage

The manifest must keep **accurate** `uses-permission` and in-app disclosure for sensitive telephony/SMS access. **Current list:** see **Permissions** later in this doc.

PRD-style additions to justify **only if implemented**:

- **`SYSTEM_ALERT_WINDOW`:** for a **non-blocking floating caller-ID overlay** — **not** in repo today; PhonIQ uses **notifications**, **`CallOverlayActivity`**, and in-app in-call UI instead.
- **`CallScreeningService`:** declare and implement when offering pre-answer blocking based on local+remote score.

#### 4.2 Privacy (contacts & India context)

- **No contact scraping / no full contact upload** for reputation or ads (aligns with **Contacts / data policy** expectations). Only **user-initiated** or **minimal identifiers** (e.g. a **reported** spam number) may leave the device if a cloud tier ships.
- **India DPDP / deployment:** if Firebase or other backends are used, document **region** (e.g. **`asia-south1` Mumbai**) in the **Privacy Policy**, provide **data deletion** / account teardown, and tie **retention** to stated purposes.

### 5. Implementation roadmap — **reference prompts for Cursor**

Use as **starting prompts**; substitute **PhonIQ** package (`com.phoniq.app`) and respect **offline-first** unless working on an optional cloud branch.

**Phase 1 — default app setup**

> Generate boilerplate for a **default Android dialer + SMS app**: `Intent` filters for **default dialer** and **default SMS**; first-run / settings flows to request role + permissions. Use **Kotlin**, **Compose**, **minSdk 35**.

**Phase 2 — call screening (only if product enables remote or pre-answer blocking)**

> Implement **`CallScreeningService`**: on incoming call, query **Room** first; on miss, **coroutine + 2 s timeout** to a **Cloud Function** returning `spam_score`; if above threshold, `CallResponse.Builder().setDisallowCall(true)` (or equivalent). **Otherwise** keep PhonIQ’s current **`InCallService`** + local / system-block behavior.

**Phase 3 — caller ID overlay**

> If using **floating overlay**, implement with **`WindowManager`**, **`SYSTEM_ALERT_WINDOW`**, and conservative **touch flags** so **Answer/Decline** stay usable. **Else** extend **`IncomingCallNotification`** / **`CallOverlayActivity`** patterns already in-tree.

### 6. Non-functional requirements (PRD)

- **Latency:** identify caller / spam hint within **~3 s** on inbound paths users perceive (notification / in-call / overlay). Local path should be **sub-second**; remote path must honor **timeout** and degrade gracefully.
- **Battery:** avoid **sync while Doze** unless **charging** or using **official** exempted paths; no tight polling loops on SMS or call log.
- **Security (when cloud exists):** **Firebase App Check** (or equivalent) on **all** callable HTTPS / CF surfaces; no open anonymous reputation writes.

### 7. Observability (recommended)

- Add a **debug / support `Logger`** (file- or buffer-backed, redacted) for **call screening decisions**, **remote timeout**, **service restart**, and **SMS handler** errors so support can distinguish “OS killed service” vs “network missed.” Gate with **BuildConfig.DEBUG** or explicit user opt-in for file export.

### PRD vs PhonIQ — quick reconciliation

| Topic | Default PRD | PhonIQ default |
|-------|-------------|----------------|
| Backend | Firebase assumed | **None required** for IQ |
| Auto-block spam score | > 80 | **System block list** only; spam label ≠ auto-reject |
| `CallScreeningService` | Yes | **Optional / future** |
| Contact upload | Must not | **Must not** (unchanged) |

### 8. Freemium revenue — Firebase (Google Analytics) & Google Play Billing (**optional product tier**)

> **Conflicts with current bar:** **Core Philosophy** states **no telemetry** and no background network for IQ. Shipping **Google Analytics for Firebase** is a **deliberate product / legal** decision: you must add **consent** (where required), update **`docs/PRIVACY_POLICY.md`**, complete Play **Data safety**, and gate the SDK behind an explicit policy. Until then, treat this entire subsection as a **specification only**, not shipped behavior.

Industry practice for **freemium Android** is to use **Firebase** (including **Google Analytics for Firebase**) for funnel and revenue visibility while using **Google Play Billing** for entitlements. Native Android integrates cleanly: Billing Library for purchases, Analytics for events, and **Play ↔ Firebase linking** for automatic IAP telemetry.

#### 8.1 Link Firebase to Google Play (“no-code” IAP signal)

- In **Firebase Console → Project settings → Integrations → Google Play**, **link** the app’s Play listing.
- **Effect:** After linking, Analytics can ingest **`in_app_purchase`** (and related) events with **product ID**, **quantity**, and **revenue** collected by the stack, without custom server code for each purchase (exact set of automatic events evolves with SDK/Play; verify current Firebase & Play docs when implementing).
- **Operational:** Use Firebase’s **In-app purchases** reporting as the primary revenue sanity check alongside Play Console.

#### 8.2 Freemium funnel — required **custom events**

Play linkage covers **conversion outcomes**; it does **not** replace funnel steps. Implement and QA these (names are requirements-level; align with `snake_case` GA4 event naming conventions):

| Event | When to fire |
|-------|----------------|
| **`feature_locked_clicked`** | Free user taps a **premium-gated** control (e.g. auto-block spam upgrade, premium theme pack, paid offline spam DB download). |
| **`paywall_viewed`** | Subscription / upgrade **screen** or sheet is shown. |
| **`checkout_started`** | User taps **Subscribe** / **Continue** and the **Google Play Billing** purchase flow (system sheet) is presented. |

Use these events in Analytics **Explorations** / **funnels** to find drop-off before purchase.

#### 8.3 User properties (segmentation)

- **`user_tier`:** `"free"` at cold start / install attribution; set to `"premium"` (or more granular values) after **verified** purchase state from Billing. If you sell **multiple independent subscriptions**, prefer **boolean or string properties per entitlement** (below) instead of a single ambiguous `"premium"`.
- **Per-product flags (recommended for multi-sub):** e.g. `has_theme_sub`, `has_spam_sub` (boolean), updated whenever entitlements refresh so **every** report can filter “Money engagement among spam-subscribers only” or “crashes among theme-only buyers.”

When implementing, batch property updates with Play Billing **query + purchase token** validation; do not trust client-only flags for security-sensitive gating.

#### 8.4 Audiences, Remote Config, In-App Messaging

- Build **Audiences** from behavior (example: fired **`paywall_viewed`** ≥ 3 times and no `in_app_purchase` in window).
- Use **Remote Config** or **Firebase In-App Messaging** for ethical, disclosed promotions (e.g. time-limited discount on annual plan) **only** with clear UX and policy text.

#### 8.5 Google Play Console — **multiple subscriptions** (example structure)

Google Play supports **several active subscription products** in one app; each product may expose **multiple base plans** (e.g. monthly + yearly) at different prices. Users may hold **one, both, or neither**; entitlements must be checked **independently** in code.

**Example SKUs (replace currency/amounts per market):**

| Subscription product | Grants | Base plans (example) |
|---------------------|--------|----------------------|
| **Product A — “Personalization & themes”** | Premium UI / theme / customization unlocked | Monthly **$1**; Yearly **$10** |
| **Product B — “Spam intelligence pack”** | Advanced / cloud or expanded offline spam corpus, premium auto-block, etc. (product-defined) | Monthly **$2**; Yearly **$15** |

**Implementation notes for engineering / Cursor:**

- **`BillingClient` + ProductDetails:** Query both subscription **product IDs**; purchases are **independent**.
- **Entitlement gates:** Theme settings → verify **Product A** active. Incoming-call spam premium path → verify **Product B** active (or equivalent feature flag from validated purchase).
- **Future bundle:** A third subscription may bundle both at a discount; Play supports **upgrade / replacement** flows — document migrations in-app.
- **Analytics:** Keep Firebase user properties in sync with **actual** entitlements (`has_theme_sub`, `has_spam_sub`) so revenue attribution matches feature usage.

#### 8.6 Reference — enabling Analytics in Firebase

- Video walkthrough (verify still current at implementation time): [How to Integrate Google Analytics with Firebase (Step‑by‑Step) — 2026](https://www.youtube.com/watch?v=7neQzqr3KAo)

---

## Three Pillars

### 1. Dialer (Phone Intelligence)
- Replaces default Samsung dialer
- Caller ID from local contact DB
- Spam detection — IQ local engine (offline-only)
- Call recording (local, encrypted)
- Call notes + tags
- Blacklist / block numbers

### 2. Messages (SMS + RCS Intelligence)
- Replaces default SMS app
- Smart inbox tabs: All / Personal / OTP / Transactions / Spam
- RCS-aware threads with SMS fallback
- WhatsApp-style dense list + thread UI for prototype validation
- OTP auto-detect + one-tap copy (with expiry countdown)
- Full-text SMS search
- SMS linked to dialer contacts

### 3. Money Manager (Financial Intelligence)
- Auto-reads bank/UPI/CC SMS using regex parser engine
- Detects: HDFC, SBI, ICICI, Axis, Kotak, Paytm, PhonePe, GPay, BHIM
- Categorizes: Food, Shopping, Transport, Bills, EMI, Salary, ATM
- Budget tracker with per-category monthly limits
- Running balance per account from SMS history
- Spending analytics (donut chart, monthly bar chart)
- Recurring payment detector
- CSV + PDF export (local / Drive)

---

## Current Mockup Status (design/phoniq-mockup-v1.html)
- Icons: [Remix Icon](https://remixicon.com/) via jsDelivr CDN, `.phoniq-icon` sizing tokens; status-bar pictograms and the Money donut chart remain inline SVG for fidelity
- **Shell:** unified header on Phone / Messages / Money (**PhonIQ** mark, centered search opening global overlay with context placeholder, ⋮ overflow with tab-specific items — each tab’s ⋮ opens **wire overlays** for the relevant `### Planned` flows); bottom nav is **Phone · Messages · Money** only (Settings removed from nav — entry via ⋮ → Settings)
- Bottom nav: Samsung One UI–style bar (blur, safe-area inset, active pill)
- Dialer: Recent/Contact/Favorite subtabs; **Recent filters** (All, Missed, Incoming, Outgoing, Rejected); horizontal quick-call strip (frequency + mode icons); recent-call rows with IQ chips, time/method column, **actionable method buttons** (PSTN redial → outgoing mock; WhatsApp → intent-style handoff in prototype); Favorites as **3-column grid** (names only); **context FAB** switches by tab (keypad / add contact / add favorite); **⋮ menu** opens delete-all, insights, who-is-this, merge, after-call, recording wires
- Contacts panel: favorite chips row, alphabetical groups, inline call/message actions, risk badges, search, blocked section (add contact via bottom FAB on Contacts tab)
- Messages: WhatsApp-inspired list/thread UX, smart filter tabs (All/Unread/Personal/Transaction/OTP/Bill/Delivery/Travel/Spam) with icon + counts; bill rows (BESCOM/Jio/Rent with Due/Overdue badges), delivery tracking rows (Amazon/Swiggy/Flipkart), travel rows (IndiGo BLR→DEL 6E 204 + Ola receipt), plus RCS chip + E2E bar, delivery ticks, reactions, list-row typing hint (Rahul), in-chat typing dots, voice + waveform, corner bubble tails (plain bubbles); **⋮ menu** still opens mark read / inbox cleaner / bill hygiene / OTP wires
- Messages threads: Priya, Rahul, HDFCBK (debit txn + NetBanking OTP card + call header → outgoing call), VM-VFSOTP, PhonePe (Blinkit Money detail)
- Money: summary, donut, categories, budget cards; **Money tools** horizontal chip strip (bill due, recurring, salary FY, investments, export wires); transaction detail flow wired from message actions
- **Settings:** root lists **Personalization** + **Data & device** (backup/restore wire, widgets wire); **Personalization** sub-view holds live theme/accent/dialpad/font/avatar/bubble controls and related toggles
- **Contact detail:** row for **Per-contact call policies** (opens policy wire overlay)
- **Prototype wires:** full-screen overlay (`proto-generic-overlay`) opened from the correct tab’s **⋮ menu**, **Money** “Money tools” chip strip, Settings **Data & device**, or contact-detail policy row — not a separate hub screen
- **Android app (shell + samples):** `android/` — Kotlin, Jetpack Compose, package **`com.phoniq.app`**, **minSdk 35** (manifest may list `INTERNET` for optional manual network features — see **Core Philosophy**); **Shell** matches mockup **unified header**: tab **gradient icon** (Call / Message / Money) + PhonIQ + **center pill search** with **tab-specific placeholders** (numbers / messages / transactions); bottom nav **Phone · Messages · Money** with **accent-forward selected state** (mockup-like pill); **Phone** Recent/Contacts/Favorites (**Material3 `SecondaryTabRow`**), filters, **quick-call** strip, FAB, **recents** stateful (delete-all dialog); **Messages** **filter chips with icons + counts**, **compose FAB**, thread rows with **RCS badge, category pills, typing hint**, sample threads closer to HTML “All” list, **⋮ → Mark all read / Inbox cleaner** sheets; **Money** summary + budget bar, **Money tools** strip, donut + cards; **Money ⋮** item order matches mockup; **global search** placeholder **Search calls, SMS, contacts…**; **⋮** wires + **Settings**. Run from `phoniq/android/` (`android/README.md`).

### Android ↔ HTML mockup — remaining parity (backlog)
Use `design/phoniq-mockup-v1.html` as visual source of truth. Still **not** 1:1: **status bar** pictograms inside app chrome, **backdrop blur** on bottom bar, full **WA thread** (bubbles, ticks, voice, reactions), **Phone** row layout (avatar column + exact typography vs `call-item`), **Money** donut fidelity vs inline SVG, **Settings** density, **contacts** blocked strip polish + inline search, **Remix** icon set (Material symbols stand in). **Shipped in app (not parity gaps):** OTP countdown + copy (inbox + thread), communication insights overlay, who-is-this fused sheet, merge-candidate overlay, after-call templates + recording playback entry, delete-all dialog, local DB export / restore hooks; **incoming notification** ring/silent + custom tone (best-effort per OEM). **Recent list + quick-call strip** sample data and section label follow the HTML order and scenarios (Track A). Track in issues or continue extending this list when the HTML changes.

### Dialer / Messages parity shipped (2026-05-16)
- **Inline SMS composer:** real `BasicTextField` in `ThreadDetailOverlay` → `SmsManager.sendTextMessage` + Telephony `Sent` insert via `SmsRepository.sendSms`; thread refreshes after send; default-SMS-app hint surfaced when not yet default.
- **Telecom-linked in-call:** `CallStateRepository` exposes `isMuted` / `audioRoute` flows + `requestToggleMute` / `requestToggleSpeaker` / `requestHold`; `PhonIQInCallService` mirrors those into `setMuted` / `setAudioRoute` / `Call.hold` / `unhold` and reports back via `onCallAudioStateChanged`.
- **Trust signals:** removed the always-on "Verified" chip on `IncomingCallScreen` per offline trust spec.
- **Recents:** main-column tap opens contact detail; long-press opens row menu (Call, SMS, View contact, Trust, Spam, **Delete from history**) wired to `CallLogRepository.deleteCallsForNumber` (Room + system CallLog provider); added Today / Yesterday / Earlier this week / Older headers driven by `RecentCall.timestampMs`.
- **Dialpad:** long-press `0` → `+`, long-press `1` → voicemail intent, per-key haptic + DTMF tone via `ToneGenerator(STREAM_DTMF)`.
- **Contacts:** sticky letter headers (`stickyHeader`) and floating A–Z rail (`animateScrollToItem` to bucket start index).
- **Contact detail:** Star/unstar (Contacts.STARRED) and Share contact (text/x-vcard via ACTION_SEND chooser) in the hero bar.
- **Messages:** inbox search bar (case-insensitive substring over title / snippet / peer address).
- **Notifications:** SMS notifications carry a `RemoteInput` Reply action; `SmsQuickReplyReceiver` sends via `SmsRepository.sendSms` and updates the notification to `MessagingStyle`.
- **Incoming call:** "Reply with message" expands a chip row of `after_call_sms_templates`; tapping a chip declines and sends the SMS in one step.
- **In-call / incoming identity:** `ActiveCallInfo.deviceContactId` + [`CallContactLookup`](android/app/src/main/java/com/phoniq/app/util/CallContactLookup.kt) (`ContactsContract.PhoneLookup`) resolve contact id and display name; [`PhonIQInCallService`](android/app/src/main/java/com/phoniq/app/telecom/PhonIQInCallService.kt) prefers lookup name, then Telecom `callerDisplayName`, then the dialable number. [`IncomingCallNotification`](android/app/src/main/java/com/phoniq/app/telecom/IncomingCallNotification.kt) builds `Person` with `IconCompat` from the contact photo content URI when id > 0. [`InCallScreen`](android/app/src/main/java/com/phoniq/app/ui/phone/InCallScreen.kt) / [`IncomingCallScreen`](android/app/src/main/java/com/phoniq/app/ui/phone/IncomingCallScreen.kt) use `ContactPhotoAvatar` when a device contact id is known, else gradient + initials.
- **Per-contact policies:** choices in [`ContactPoliciesBottomSheet`](android/app/src/main/java/com/phoniq/app/ui/phone/ContactPoliciesBottomSheet.kt) are saved via [`ContactPoliciesStore`](android/app/src/main/java/com/phoniq/app/util/ContactPoliciesStore.kt). **Preferred SIM** is applied on outgoing calls placed through PhonIQ ([`placeOutgoingTelCall`](android/app/src/main/java/com/phoniq/app/util/DialerIntents.kt) + `TelecomManager.EXTRA_PHONE_ACCOUNT`). **Ring mode + custom ringtone URI** are applied on incoming via [`IncomingCallNotification`](android/app/src/main/java/com/phoniq/app/telecom/IncomingCallNotification.kt) (notification channel sound / silent — **OEM-dependent**; not all devices honor custom sounds the same way).
- **Block vs spam (incoming):** [`shouldAutoRejectIncoming`](android/app/src/main/java/com/phoniq/app/util/CallAutoReject.kt) disconnects only when [`BlockedNumberContract.isBlocked`](https://developer.android.com/reference/android/provider/BlockedNumberContract) is true (user’s **system** blocked list). **User-marked spam stays warning-only** per IQ spec — it does **not** auto-reject.
- **Phone bulk:** Recents + Contacts support **multi-select** (overflow “Select” / long-press on contacts) with bottom bar: delete history, mark spam, open system block list, share numbers; contacts add **bulk star** where `deviceContactId` is known.
- **Messages bulk:** Long-press a thread enters selection mode; bottom bar **mark read**, **archive**, **remove local** (Telephony sync may repopulate).
- **MMS (inbox slice):** [`SmsRepository`](android/app/src/main/java/com/phoniq/app/data/repository/SmsRepository.kt) appends **Telephony `Mms.Inbox`** rows into Room as `[MMS]` / subject placeholder (no image decode / full MMS composer).
- **Money:** [`MoneyScreen`](android/app/src/main/java/com/phoniq/app/ui/money/MoneyScreen.kt) shows **investment-highlight** parsed transactions from [`MoneyViewModel.investmentHighlightTransactions`](android/app/src/main/java/com/phoniq/app/ui/money/MoneyViewModel.kt) alongside existing recurring reminders + FY salary cards.
- **Widget:** [`PhonIQAppWidget`](android/app/src/main/java/com/phoniq/app/widget/PhonIQAppWidget.kt) — tap to open the app (v1 stub).

---

## Mockup requirement checklist
Single place to track **product + UX requirements** against `design/phoniq-mockup-v1.html`. Update checkboxes and notes when the prototype changes.

### In mockup (done)
- [x] Unified header: **PhonIQ** brand, centered search, ⋮ menu; placeholders — Phone *“Search numbers, names”*, Messages *“Search messages”*, Money *“Search transactions”*
- [x] ⋮ menu items — each tab lists **Settings** plus **planned-flow wires** (Phone: delete all, insights, who is this, merge, after-call, recording; Messages: mark read, inbox cleaner, bill hygiene, OTP; Money: export, salary, recurring, investment IQ, bill reminders)
- [x] Global search overlay wired from header; sample results for calls / messages / money
- [x] Bottom navigation: Phone, Messages, Money only
- [x] Settings root + Personalization drill-down (themes, accent, dialpad, fonts, avatar shape, bubble style, feature toggles)
- [x] Dialer subtabs: Recent / Contacts / Favorites
- [x] Recent call filter chips + client-side filter; divider logic when filtered
- [x] Recent scenarios per `Recent Calls Scenario Coverage` (spam, blocked, WA, missed counts, intl, etc.)
- [x] Quick-call strip with mode meta icons
- [x] Favorites 3×N grid; no phone numbers on grid cells
- [x] Context FAB: keypad (Recent), add contact (Contacts), add favorite (Favorites)
- [x] Recent row method icons: PSTN → outgoing mock; WhatsApp voice/video → prototype external handoff
- [x] Remix-safe icons for FAB / decline where font glyphs exist (`ri-grid-line`, `ri-close-line`, etc.)
- [x] **Contextual prototype entry points:** tab ⋮ menus, Money “Money tools” strip, Settings Data & device rows, contact-detail policy row — each opens the **wire overlay** for the matching backlog item (placeholder UI until flows are productized in chrome)

### Planned — next mockup iterations
- [x] **Delete all calls:** **Android:** `AlertDialog` confirm + clear recents + snackbar (mockup asked for bottom sheet — can swap later); empty list after clear
- [x] **Mark all read & Inbox cleaner:** **Android:** `ModalBottomSheet` for each; mark-all clears `unread` on prototype threads; inbox cleaner dry-run + snackbar (full hygiene flows still TBD)
- [x] **After-call / end-call sheet:** **Android:** [`AfterCallSheet`](android/app/src/main/java/com/phoniq/app/ui/phone/AfterCallSheet.kt) — contact, note, block, SMS (**templates** via `string-array`), favourite, who-is-this, recording playback; FlowRow layout on narrow screens; block explainer
- [x] **Communication insights:** **Android:** on-device stats overlay from call log + [`CommunicationInsights`](android/app/src/main/java/com/phoniq/app/data/model/CommunicationInsights.kt)
- [x] **“Who is this?” sheet:** **Android:** fused call + SMS + notes — [`WhoIsThisOverlay`](android/app/src/main/java/com/phoniq/app/ui/shell/WhoIsThisOverlay.kt)
- [x] **Contact merge / cleanup:** **Android:** duplicate-number groups — [`MergeContactsOverlay`](android/app/src/main/java/com/phoniq/app/ui/shell/MergeContactsOverlay.kt)
- [x] **Per-contact policies:** **Android:** `ContactPoliciesBottomSheet` — ring/silent, SIM, ringtone; **SIM choice** applied on PhonIQ-originated `placeCall`; ring/ringtone enforcement **TBD**
- [x] **Backup / export:** **Android:** Settings → Data & device — `ACTION_CREATE_DOCUMENT` export of local `phoniq.db` (WAL checkpoint + copy); Drive backup/restore rows explain that cloud backup/Drive APIs are **not** implemented yet and that **`INTERNET`** in the manifest is for optional future/manual flows only (no fake success)
- [x] **OTP UX:** **Android:** [`OtpCountdownCopyStrip`](android/app/src/main/java/com/phoniq/app/ui/messages/OtpCountdownCopyStrip.kt) in inbox + thread OTP bubbles — [`SmsConversationMapper`](android/app/src/main/java/com/phoniq/app/data/mapper/SmsConversationMapper.kt)
- [x] **SMS hygiene & bill reminders (single slice):** **Android:** Messages ⋮ **Bill hygiene** opens bottom sheet — bill-category threads + light due hints from on-device snippet/pill heuristics + CTA to Bill filter (full pin/archive + Money reminders list still TBD)
- [x] **Money — recurring & transfers:** **Android:** heuristic upcoming lines from parsed debits + transfer gaps in [`buildMoneyReminderLines`](android/app/src/main/java/com/phoniq/app/ui/money/MoneyScheduleHints.kt); **MoneyScreen** “Upcoming (heuristic)” card strip
- [x] **Money — salary:** **Android:** Indian FY salary credit aggregation in [`buildSalaryFySummary`](android/app/src/main/java/com/phoniq/app/ui/money/MoneyScheduleHints.kt) + [`SalaryFySummaryCard`](android/app/src/main/java/com/phoniq/app/ui/money/MoneyScreen.kt)
- [x] **Money — investments (SMS IQ):** **Android:** on-device merchant/category heuristics in [`MoneyIntelligenceSummary`](android/app/src/main/java/com/phoniq/app/data/model/MoneyIntelligenceSummary.kt) + **parsed txn list** on [`MoneyScreen`](android/app/src/main/java/com/phoniq/app/ui/money/MoneyScreen.kt); thread filters already expose investment slice in Messages
- [x] **Money — export:** **Android:** `MoneyExportBottomSheet` from Money ⋮ and Money tools — CSV + PDF via existing `MoneyViewModel` exporters
- [x] **Call recording:** **Android:** Settings toggle + disclosure + [`CallRecordingLibraryOverlay`](android/app/src/main/java/com/phoniq/app/ui/shell/CallRecordingLibraryOverlay.kt) (encrypted `.enc` list + playback); in-call controls via [`PhonIQInCallService`](android/app/src/main/java/com/phoniq/app/telecom/PhonIQInCallService.kt); after-call playback when path available

### Planned — later / optional
- [x] **Widgets & shortcuts:** **Android:** launcher **shortcut** metadata (`shortcuts.xml`) + **PhonIQ home widget** ([`PhonIQAppWidget`](android/app/src/main/java/com/phoniq/app/widget/PhonIQAppWidget.kt)) opens the app; live widget data optional later

---

## Documentation Sync Rule
- Every change to `design/phoniq-mockup-v1.html` must be followed by updates in relevant docs (minimum: `docs/PROJECT.md`, and `docs/PERSONALIZATION.md` if personalization/UX behavior changed).
- If you change **default-handler** behavior, **spam/caller-ID** policy, **optional cloud** plans, **manifest permissions**, or **Play-facing compliance**, update the **Product Requirements Document (PRD)** section in this file and reconcile it with **Core Philosophy** when needed.
- If you change **launcher / wordmark / Play graphics**, update **`docs/BRAND_ICON_ASSETS.md`** and Android `res` (`ic_phoniq_launcher_*`, `ic_stat_phoniq`, `mipmap-anydpi-v26`) together.
- If you enable **freemium billing**, **Firebase Analytics**, or change **subscription SKUs**, update **PRD §8** in this file, **`docs/PRIVACY_POLICY.md`**, and Play **Data safety** together.
- Keep the `Last updated` stamp current in modified docs.
- Treat docs as the source of truth for product behavior and naming.

---

## IQ Offline Spam Engine (Requirement Spec)
- **Scope:** fully offline, no cloud reputation, no API dependency.
- **Output labels in UI:** show only `Likely Spam` and `Blocked` badges; safe/unknown have no badge.
- **Current mockup behavior:** name/number pattern-based classification for demo flows, surfaced in recent calls, contact detail, and call screens.
- **Local-safe signals (planned app logic):** saved contact, favorite contact, user-marked safe.
- **Likely-spam signals (planned app logic):** user-marked spam, local spam list hit, suspicious repeat-caller patterns.
- **Unknown signals:** no strong safe/spam evidence in local store.
- **Blocked vs Spam:** spam is warning-only; blocked is enforcement (silence/reject). **Android:** rejection uses the **system** blocked list ([`BlockedNumberContract`](https://developer.android.com/reference/android/provider/BlockedNumberContract)) in [`shouldAutoRejectIncoming`](android/app/src/main/java/com/phoniq/app/util/CallAutoReject.kt). Spam marks do **not** trigger auto-reject.
- **Trust note:** avoid "verified business" claims in offline mode; no global network reputation wording.

## Recent Calls Scenario Coverage (Requirement Spec)
- **Standard phone call:** incoming and outgoing voice calls with duration marker (e.g., `9m`).
- **WhatsApp video call:** number line shows `WhatsApp`, right-side video icon.
- **WhatsApp audio call:** number line shows `WhatsApp`, right-side phone icon.
- **Missed call:** number/status line shows `Missed`.
- **Missed repeated attempts:** missed marker plus repeated-attempt context in time line.
- **Known missed call:** saved contact can appear as missed; contact identity and avatar remain visible.
- **Missed count rule:** when missed count > 1, render count inline as `Missed (N)`.
- **Likely spam:** card shows `⚠ Likely Spam` chip (warning only).
- **Blocked call:** card shows `🚫 Blocked` chip (enforced rejection state). For auto-rejected attempts, the status line shows `Rejected automatically (N)` (not `Missed (N)`); time stays on the third line.
- **Unknown caller:** no trust badge shown; regular phone icon.
- **Business/helpline style number:** toll-free/short-code pattern rendered as standard phone scenario.
- **International number:** international format with contextual status text.

---

## Tech Stack
| Layer | Choice | Reason |
|---|---|---|
| Language | Kotlin | Modern Android standard |
| UI | Jetpack Compose | Samsung One UI 7 compatible |
| Architecture | Clean Arch + MVVM | Testable, scalable |
| DI | Manual `ViewModelProvider.Factory` (current); Hilt optional | Hilt is a common PRD default; adopt when maintainers agree |
| Database | Room (SQLite) | Offline-first, type-safe |
| Async | Coroutines + Flow | Reactive SMS updates |
| Call handling | InCallService + TelecomManager | Required to replace default dialer |
| SMS watching | ContentObserver + BroadcastReceiver | Real-time, no polling |
| Charts | Vico (Compose-native) | Best Compose chart library |
| Backup | Google Drive REST API | Manual trigger only |
| Testing | JUnit5 + Robolectric + Compose Test | Parser engine unit-tested |

---

## Permissions
Matches `AndroidManifest.xml` (`uses-permission` list; **`WRITE_EXTERNAL_STORAGE` is not** declared):

```
INTERNET, ACCESS_NETWORK_STATE
CALL_PHONE
READ_CONTACTS, WRITE_CONTACTS
READ_CALL_LOG, WRITE_CALL_LOG
READ_SMS, RECEIVE_SMS, RECEIVE_WAP_PUSH, SEND_SMS, WRITE_SMS
RECORD_AUDIO
READ_PHONE_STATE, ANSWER_PHONE_CALLS
FOREGROUND_SERVICE, FOREGROUND_SERVICE_PHONE_CALL
MANAGE_OWN_CALLS
POST_NOTIFICATIONS
USE_FULL_SCREEN_INTENT
```
> **`INTERNET` + `ACCESS_NETWORK_STATE`:** may be present in `AndroidManifest.xml` for **optional manual** network features only (no background sync). Add **`QUERY_ALL_PACKAGES`** or Drive scopes only when implementing Drive. Core dialer/SMS/Money IQ **do not require network**.

---

## Database Schema (v1)

```sql
contacts        (id, name, number, tag, spam_score, notes, avatar_color, created_at)
call_log        (id, contact_id, number, duration_sec, type, timestamp, notes, recording_path)
sms_messages    (id, sender, body, timestamp, category, thread_id, is_transaction, is_otp, is_spam)
transactions    (id, sms_id, amount, txn_type, account_id, merchant, category, date, is_manual)
accounts        (id, bank_name, last4, account_type, balance, last_updated)
budgets         (id, category, monthly_limit, month_year)
spam_numbers    (id, number, source, confidence, reported_count, added_at)
categories      (id, name, icon, color, parent_id, is_system)
otp_log         (id, sms_id, otp_code, sender, expiry_at, was_copied)
```

---

## SMS Parser — Supported Banks & Apps (v1)
HDFC Bank, SBI, ICICI Bank, Axis Bank, Kotak Bank, Yes Bank, IndusInd, PNB,
Paytm, PhonePe, Google Pay (GPay), BHIM, Amazon Pay, Mobikwik

---

## Phase Roadmap

### Phase 1 — MVP (3 months)
- [ ] Project scaffold: Hilt + Room + Navigation + Compose
- [ ] Dialer: keypad, contacts, call history, in-call screen
- [ ] Messenger: inbox, tabs, OTP detection, thread view
- [ ] Money: SMS parser engine (regex), transaction list, categories
- [ ] Settings screen

### Phase 2 — Feature Complete (2 months)
- [ ] Call recording + secure playback
- [ ] Budget tracker + over-budget alerts
- [ ] Spending analytics (Vico charts)
- [ ] Per-account balance tracking
- [ ] CSV + PDF export
- [ ] Google Drive backup/restore
- [ ] Spam corpus refinement

### Phase 3 — AI (Snapdragon 8 Elite NPU)
- [ ] TFLite spam caller model
- [ ] On-device NLP for transaction categories
- [ ] Spending pattern insights
- [ ] Smart contact suggestions
- [ ] Local voice call summary (Whisper-tiny)

---

## Folder Structure (C:\personal\phoniq\)
```
phoniq/
  firebase.json           <- Firebase Hosting config
  .firebaserc             <- Default Firebase project id (see hosting/README — example: phoniq-app)
  hosting/
    README.md             <- Deploy instructions
    public/               <- Static site (index.html, privacy.html)
  docs/
    PROJECT.md              <- This file
    BRAND_ICON_ASSETS.md    <- Launcher, wordmark, notification icon, Play feature graphic
    ARCHITECTURE.md         <- Detailed technical design
    SMS_PARSER.md       <- Regex corpus + parser logic
    DB_SCHEMA.md        <- Full DB schema with migrations
  design/
    phoniq-mockup-v1.html   <- Interactive UI prototype
    brand/
      phoniq-icon-2026/     <- Icon & wordmark reference PNG exports
    screens/                <- Individual screen exports
  android/                  <- Android Studio project
```

---

## Brand Guidelines (v1)

**Full icon, adaptive layers, wordmark, notification icon, and Play feature graphic:** see **[`docs/BRAND_ICON_ASSETS.md`](BRAND_ICON_ASSETS.md)** (Steps 1–5). Reference PNGs: `design/brand/phoniq-icon-2026/`.

### App icon & marketing (store-facing)

- **Icon gradient:** `#0D47A1` (deep blue, top-left) → `#00E5FF` (vivid turquoise, bottom-right).
- **Mark on icon:** White “P” (bars + handset loop) + turquoise trend arrow; centered in **~66 dp** adaptive safe zone on a **108 dp** layer.
- **Wordmark:** **Phon** `#0D47A1` (bolder); **IQ** `#00E5FF` (lighter weight). Tagline: `Intelligence. Communication. Finance.`
- **Status / notification small icon:** White silhouette, transparent background (`ic_stat_phoniq`) — not the full gradient icon.
- **Feature graphic:** 1024 × 500 px; see **BRAND_ICON_ASSETS** for layout and safe zone.

### In-app UI (existing mockup palette)

- **Primary:** #6C63FF (Indigo-Purple)
- **Secondary / credit:** #00D4AA (Teal-Green) — distinct from icon turquoise **#00E5FF**; align chrome over time if desired.
- **Background:** #0A0A0F (AMOLED Black)
- **Card:** #141420
- **Debit (red):** #FF6B6B
- **Font:** Roboto (Android system)

---

## Competitive Positioning
| Feature | TrueCaller | Samsung Dialer | Axio | PhonIQ |
|---|---|---|---|---|
| Caller ID | ✅ Cloud | ✅ Basic | ❌ | ✅ Local |
| Spam Detection | ✅ Cloud | ⚠️ Basic | ❌ | ✅ Offline |
| SMS Smart Inbox | ✅ | ❌ | ❌ | ✅ |
| OTP Manager | ✅ | ❌ | ❌ | ✅ |
| Money Manager | ❌ | ❌ | ✅ | ✅ |
| SMS Transaction Parser | ❌ | ❌ | ✅ | ✅ |
| Offline-first IQ (calls/SMS/Money) | ⚠️ | ⚠️ | ❌ | ✅ |
| INTERNET (declared — manual/optional only; IQ stays offline) | ⚠️ | ⚠️ | ⚠️ | ✅\* |
| Call Recording | ✅ | ✅ | ❌ | ✅ |
| Open Source Friendly | ❌ | ❌ | ❌ | ✅ (planned) |

\* **INTERNET** / **ACCESS_NETWORK_STATE** appear in `AndroidManifest.xml` for optional future/manual flows only (Core Philosophy): no background sync, ads, telemetry, or cloud spam/caller-ID/reputation lookups.
