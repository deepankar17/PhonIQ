# PhonIQ vs Microsoft SMS Organizer — gap analysis & backlog

> **Research basis:** [SMS Organizer on Google Play](https://play.google.com/store/apps/details?id=com.microsoft.android.smsorganizer), [Microsoft Garage profile](https://www.microsoft.com/en-us/garage/profiles/sms-organizer/), Microsoft India feature stories (trains, offers). Last reviewed **2026-05-16**.  
> **Scope:** Compare **Messages**, **Reminders**, and **Finance** pillars + cross-cutting features. SMS Organizer is **India-market** oriented and **SMS-first** (no full dialer replacement). PhonIQ is **three-pillar** (Phone + Messages + Money), **offline-first**, **minSdk 35**.

## Executive summary

| Area | SMS Organizer (high level) | PhonIQ today | Gap severity |
|------|----------------------------|--------------|--------------|
| **Messages** | ML folders (e.g. Promo / Personal / Txn), starring, translation, voice compose | Rule-based categories, filters + counts, pin/archive threads, RCS/MMS slice, OTP/txn intelligence | **Medium** — promo/ML inbox, per-message star, translation, voice input |
| **Reminders** | **Dedicated tab**; auto reminders from SMS (travel, bills, movies…); **action URLs** (check-in, pay, cab); IRCTC-style train features | Bill/txn **hints** in Money, `MoneyReminderLines`, overflow sheets — **no top-level Reminders hub** | **High** — product shape + extraction breadth + deep links |
| **Finance** | “Passbook” framing, balances/expenses from SMS; **offers/coupons** (incl. web/Bing in org’s product history) | **Money** tab: budgets, categories, donut, exports, salary FY, investments slice | **Low–medium** — unified “accounts passbook” UX; **offers** conflict with offline philosophy unless on-device only |
| **Cross** | Google Drive **auto** backup, spam block, notification reply | Manual DB export/restore wire; spam/block per **IQ spec**; notification reply | **Medium** — optional **manual** encrypted backup story; parity on “block sender” for SMS |

---

## 1. Messages — feature gaps & requirements

### 1.1 Inbox organization
| ID | Requirement | Notes (vs SMS Organizer) | Suggested priority |
|----|-------------|--------------------------|-------------------|
| MSG-1 | **Promotion / offers folder** — reliably separate marketing SMS from personal/transaction | Organizer uses **on-device ML** for Important vs Promotional; PhonIQ uses **rules** — tune “promo” detection + optional **user-defined sender lists** | P1 |
| MSG-2 | **Star / favorite individual messages** (not only thread pin) | Quick access to one SMS inside thread | P2 |
| MSG-3 | **Smarter primary inbox** — “Focused” vs “Other” (or equivalent) | Aligns with ML folders without requiring cloud | P2 |
| MSG-4 | **Voice-to-text** in composer | Organizer highlights mic-driven compose | P2 (optional; uses on-device speech APIs) |

### 1.2 Language & accessibility
| ID | Requirement | Notes | Priority |
|----|-------------|-------|----------|
| MSG-5 | **SMS translation** (e.g. Indian languages ↔ English) | Organizer advertises translation; needs **on-device** model or explicit user-triggered pipeline to match PhonIQ philosophy | P3 |

### 1.3 Social / share flows
| ID | Requirement | Notes | Priority |
|----|-------------|-------|----------|
| MSG-6 | **Forward bill / delegate payment** — share a bill SMS to another contact with **their** reminder | Distinctive Organizer feature | P2 |

### 1.4 Already strong in PhonIQ (keep parity)
- Smart **tabs/filters** with counts (All / Unread / Archived + category filters in mockup spec).
- **OTP** detection, countdown, copy, notification reply.
- **Transaction** parsing and thread-level previews.
- **Full-text search**, archive, bulk actions, **telephony-backed delete** for threads.

---

## 2. Reminders — feature gaps & requirements (**largest structural gap**)

SMS Organizer’s **Reminders tab** is a **central timeline** of inferred and manual tasks. PhonIQ spreads reminders across **Money tools**, **bill hints**, and **wires** — not a first-class hub.

### 2.1 Product shape
| ID | Requirement | Notes | Priority |
|----|-------------|-------|----------|
| REM-1 | **Top-level “Reminders” surface** (4th nav tab *or* **Phone \| Messages \| Reminders \| Money** *or* Messages sub-hub) | Must reconcile with current **three-pillar** brand; could start as **Messages → Reminders** or **Money + Messages unified agenda** | P1 (product decision) |
| REM-2 | **Unified list**: upcoming / overdue / done; snooze; mark complete | Standard reminder UX | P1 |
| REM-3 | **User-created reminders** (time + title + optional link to thread) | Parity with “custom reminder” | P1 |

### 2.2 Auto extraction from SMS (organizer-class coverage)
| ID | Requirement | Notes | Priority |
|----|-------------|-------|----------|
| REM-4 | **Bills & due dates** — stronger templates for utilities, rent, credit cards, BNPL | Overlap with current **bill** category; deepen parsers + **default remind-before** (e.g. 1d / 3d) | P1 |
| REM-5 | **Travel**: flights, buses, hotels — detect PNR / dates / gates where present in SMS | Flight “web check-in” **deep links** are optional (**user tap opens browser/app**); avoid silent network | P2 |
| REM-6 | **Movies / events / doctor** appointments from SMS patterns | Pattern library + locale packs (India-first) | P2 |
| REM-7 | **Train (India)** — PNR status, **live running status**, station alerts | Organizer invested heavily here; **offline timetable** vs **network** for live status must be explicit in spec | P3 (or P2 for India SKU) |

### 2.3 “Smart assist” (actionable reminders)
| ID | Requirement | Notes | Priority |
|----|-------------|-------|----------|
| REM-8 | **Per-reminder actions**: “Pay bill”, “Check flight”, “Open maps”, “Copy PNR” | Implement as **intents + verified URLs**; **no mandatory cloud** | P2 |
| REM-9 | **Offer surfaces from SMS** | Conflicts with **no-telemetry / no routine third-party fetch** — if done, **strictly on-device** parsing only | P3 or “not planned” |

---

## 3. Finance (Money) — feature gaps & requirements

### 3.1 Presentation & mental model
| ID | Requirement | Notes | Priority |
|----|-------------|-------|----------|
| FIN-1 | **Passbook / all accounts** screen — one scrollable view of **every balance + last movement** parsed from SMS | Organizer’s signature “single pane” money view | P1 |
| FIN-2 | **Wallet / card grouping** — explicit **account tiles** (user can merge/rename senders) | Reduces duplicate “Unknown bank” noise | P1 |
| FIN-3 | **Cashflow strip** — “money in vs out this week” at top of Finance | Quick read; can reuse `MoneyIntelligenceSummary` | P2 |

### 3.2 Offers & coupons
| ID | Requirement | Notes | Priority |
|----|-------------|-------|----------|
| FIN-4 | **Deals from SMS** (merchant promos) | On-device only; **no Bing/web scrape** unless user explicitly opens a link | P3 |
| FIN-5 | **Third-party “offers network”** | **Not recommended** for PhonIQ core philosophy | **Out of scope** unless product changes |

### 3.3 Already strong in PhonIQ
- **Budgets**, categories, **donut**, **CSV/PDF export**, **recurring**, **salary FY**, **investment** highlights from txn SMS.
- **Over-budget** notifications (when enabled).

---

## 4. Cross-cutting & platform

| ID | Requirement | Notes | Priority |
|----|-------------|-------|----------|
| X-1 | **Encrypted backup** — user-initiated, optional cloud (Drive) with clear manifest | Organizer: **automatic** Drive backup — PhonIQ should stay **manual + transparent** per `PROJECT.md` | P1 |
| X-2 | **Block SMS sender** integration — align with **system blocked numbers** + local spam list | Organizer: block spam senders | P1 |
| X-3 | **Dark theme / personalization** | PhonIQ: **PersonalizationStore** — continue M3 parity | ongoing |
| X-4 | **Works offline** (core) | Both claim offline; PhonIQ must gate any **optional** online assists behind explicit user action | principle |
| X-5 | **Regional SKU** — India-first parsers (IRCTC, UPI, common billers) | Organizer is India-centric; document **locale packs** for parsers/reminders | P2 |

---

## 5. Suggested implementation phases (todo rollup)

**Phase A — Quick wins (align UX, low architectural risk)**  
- [ ] MSG-1: Harden **promo vs personal** + filter chip / “Offers” subfolder  
- [ ] FIN-1 + FIN-2: **Passbook**-style Money home section (account tiles + last txn)  
- [ ] X-2: **Block sender** flow for SMS aligned with `BlockedNumberContract` / local list  

**Phase B — Reminders MVP (structural)**  
- [ ] REM-1 … REM-3: **Reminders hub** + manual reminders + basic list UX  
- [ ] REM-4: Bill / due-date extraction + default snooze policies  
- [ ] REM-8 (subset): **Open payment app / copy UPI id / open thread** actions  

**Phase C — Depth**  
- [ ] REM-5, REM-6: Travel / event parsers  
- [ ] MSG-2, MSG-4, MSG-6: Star message, voice compose, forward-bill delegate  
- [ ] FIN-3, FIN-4: Cashflow strip; on-deal promo parsing  

**Phase D — India / long-tail**  
- [ ] REM-7: Train PNR / live status (policy: offline cache vs network)  
- [ ] MSG-5: On-device translation  

---

## 6. Non-goals (unless strategy changes)

- Uploading SMS content to Microsoft-/Organizer-style **cloud ML** for classification.  
- **Bing Offers** or routine **third-party** deal feeds.  
- Replacing PhonIQ **Phone** pillar — Organizer does not try to be a full dialer IQ product.

---

## 7. References

- [SMS Organizer — Google Play](https://play.google.com/store/apps/details?id=com.microsoft.android.smsorganizer)  
- [Microsoft Garage — SMS Organizer](https://www.microsoft.com/en-us/garage/profiles/sms-organizer/)  
- [Train journey features (Microsoft Stories India)](https://news.microsoft.com/en-in/features/sms-organizer-india-train-update/)  
- Internal: `docs/PROJECT.md` — PhonIQ pillars, philosophy, shipped parity
