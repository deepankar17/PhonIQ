# PhonIQ — Project Master Document
> Last updated: 2026-05-02 (Android: mockup shell parity — unified header, message filters+threads, nav tint)

## Identity
- **App Name:** **PhonIQ** — *Phone* + **IQ** (capital **IQ** is intentional). Technical ids stay lowercased (e.g. `com.phoniq.app`, folder `phoniq/`).
- **Source repository:** [github.com/deepankar17/PhonIQ](https://github.com/deepankar17/PhonIQ) — clone: `gh repo clone deepankar17/PhonIQ` or `git clone https://github.com/deepankar17/PhonIQ.git`
- **Tagline:** Know every call. Track every rupee.
- **Platform:** Android — **minimum OS Android 15 (API 35)**; primary validation on **15 and 16** (no install on API 34 and below). `compileSdk` / `targetSdk` **36** with **AGP 9.2+** / Gradle 9.4.x (Play / platform alignment); `minSdk` remains **35**.
- **Package Name:** com.phoniq.app
- **Trademark Status:** Clear (verified 2026-04-30) — file in India Class 38 + 36
- **Domain:** phoniq.app (register immediately)

---

## Core Philosophy
- 100% offline-first. No INTERNET permission in manifest.
- Google Drive used ONLY for manual backup — never background sync.
- Primary DB: Room (SQLite) on device storage.
- No ads. No telemetry. No data leaves the device.

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
- **Android app (shell + samples):** `android/` — Kotlin, Jetpack Compose, **no `INTERNET` permission**, package **`com.phoniq.app`**, **minSdk 35**; **Shell** matches mockup **unified header**: tab **gradient icon** (Call / Message / Money) + PhonIQ + **center pill search** with **tab-specific placeholders** (numbers / messages / transactions); bottom nav **Phone · Messages · Money** with **accent-forward selected state** (mockup-like pill); **Phone** Recent/Contacts/Favorites (**Material3 `SecondaryTabRow`**), filters, **quick-call** strip, FAB, **recents** stateful (delete-all dialog); **Messages** **filter chips with icons + counts**, **compose FAB**, thread rows with **RCS badge, category pills, typing hint**, sample threads closer to HTML “All” list, **⋮ → Mark all read / Inbox cleaner** sheets; **Money** summary + budget bar, **Money tools** strip, donut + cards; **Money ⋮** item order matches mockup; **global search** placeholder **Search calls, SMS, contacts…**; **⋮** wires + **Settings**. Run from `phoniq/android/` (`android/README.md`).

### Android ↔ HTML mockup — remaining parity (backlog)
Use `design/phoniq-mockup-v1.html` as visual source of truth. Still **not** 1:1: **status bar** pictograms inside app chrome, **backdrop blur** on bottom bar, full **WA thread** (bubbles, ticks, voice, reactions), **Phone** row layout (avatar column + exact typography vs `call-item`), **Money** donut fidelity vs inline SVG, **Settings** density, **OTP countdown** in list, **proto wires** as real flows (insights stats, delete-all bottom sheet, after-call / who-is-this sheets), **contacts** blocked strip + inline search, **Remix** icon set (Material symbols stand in). **Recent list + quick-call strip** sample data and section label now follow the HTML order and scenarios (Track A). Track in issues or continue extending this list when the HTML changes.

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
- [ ] **After-call / end-call sheet:** save contact, note, block, SMS template shortcuts
- [ ] **Communication insights:** lightweight stats screen (top contacts, call time, missed trends) — on-device story only
- [ ] **“Who is this?” sheet:** fuse last call + last SMS + local notes (read-only mock)
- [ ] **Contact merge / cleanup:** duplicate-merge wizard UI
- [ ] **Per-contact policies:** ring/silent, default SIM, ringtone (mock controls)
- [ ] **Backup / export:** manual backup + restore picker (aligns with Drive-only manual backup pillar)
- [ ] **OTP UX:** expiry countdown + one-tap copy affordance in list/thread
- [ ] **SMS hygiene & bill reminders (single slice):** pin / archive promo threads; infer bill due dates from SMS where possible (**credit card, gas, electricity, mobile recharge**, similar); **upcoming reminders list** on Money and/or Messages; mock notification copy (no real push in HTML)
- [ ] **Money — recurring & transfers:** subscription / **recurring payment** detector from parsed SMS; **fund transfers** (standing instructions, periodic IMPS/NEFT patterns) surfaced as **upcoming reminders**; own **separate card view** on Money (distinct from bill-due cards)
- [ ] **Money — salary:** detect salary **credit** lines in bank SMS / transfers; keep them out of “discretionary spend” (or a separate bucket); show a **yearly salary** summary card/section on the Money page (aggregated, on-device heuristics + user confirm)
- [ ] **Money — investments (SMS IQ):** classify investment-related senders (MF/stock/broker/CDSL-NSDL/registrar-style); on-device templates for SIP debits, corporate actions, allotments, etc.; **IQ cards + thread badges** + read-only summaries (user confirm on ambiguous parses; no live market / “verified advisor” claims)
- [ ] **Money — export:** CSV / PDF export sheet from Money overflow
- [ ] **Call recording:** consent copy + toggle + mock recording list/player

### Planned — later / optional
- [ ] **Widgets & shortcuts:** static preview frame or diagram in mockup or doc only

---

## Documentation Sync Rule
- Every change to `design/phoniq-mockup-v1.html` must be followed by updates in relevant docs (minimum: `docs/PROJECT.md`, and `docs/PERSONALIZATION.md` if personalization/UX behavior changed).
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
- **Blocked vs Spam:** spam is warning-only; blocked is enforcement (silence/reject).
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
| DI | Hilt | Standard, ViewModel-friendly |
| Database | Room (SQLite) | Offline-first, type-safe |
| Async | Coroutines + Flow | Reactive SMS updates |
| Call handling | InCallService + TelecomManager | Required to replace default dialer |
| SMS watching | ContentObserver + BroadcastReceiver | Real-time, no polling |
| Charts | Vico (Compose-native) | Best Compose chart library |
| Backup | Google Drive REST API | Manual trigger only |
| Testing | JUnit5 + Robolectric + Compose Test | Parser engine unit-tested |

---

## Permissions
```
READ_CALL_LOG, WRITE_CALL_LOG
READ_CONTACTS, WRITE_CONTACTS
READ_SMS, RECEIVE_SMS, SEND_SMS
RECORD_AUDIO
READ_PHONE_STATE, ANSWER_PHONE_CALLS
FOREGROUND_SERVICE
POST_NOTIFICATIONS
WRITE_EXTERNAL_STORAGE
```
> INTERNET permission: **NOT included**

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
  docs/
    PROJECT.md          <- This file
    ARCHITECTURE.md     <- Detailed technical design
    SMS_PARSER.md       <- Regex corpus + parser logic
    DB_SCHEMA.md        <- Full DB schema with migrations
  design/
    phoniq-mockup-v1.html   <- Interactive UI prototype
    screens/                <- Individual screen exports
    brand/                  <- Logo, colors, typography
  app/                      <- Android Studio project (Phase 1)
```

---

## Brand Guidelines (v1)
- **Primary:** #6C63FF (Indigo-Purple)
- **Secondary:** #00D4AA (Teal-Green)
- **Background:** #0A0A0F (AMOLED Black)
- **Card:** #141420
- **Debit (red):** #FF6B6B
- **Credit (green):** #00D4AA
- **Font:** Roboto (Android system)
- **Logo concept:** Phone receiver icon + IQ/spark symbol

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
| 100% Offline | ❌ | ⚠️ | ❌ | ✅ |
| No Internet Permission | ❌ | ❌ | ❌ | ✅ |
| Call Recording | ✅ | ✅ | ❌ | ✅ |
| Open Source Friendly | ❌ | ❌ | ❌ | ✅ (planned) |
