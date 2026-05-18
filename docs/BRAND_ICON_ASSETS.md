# PhonIQ — Brand, App Icon & Marketing Assets

**Last updated:** 2026-05-17  
**Reference renders:** `design/brand/phoniq-icon-2026/` (exported PNGs from product design)

This document is the **authoritative requirement** for launcher icons, adaptive layers, wordmark, notification/status icon, and Play feature graphic. Implementation in the Android app lives under `android/app/src/main/res/` (`ic_phoniq_launcher_*`, `ic_stat_phoniq`, `mipmap-anydpi-v26`).

---

## Brand palette (icon & wordmark)

| Token | Hex | Use |
|-------|-----|-----|
| **Deep blue** | `#0D47A1` | Gradient start (top-left), wordmark **Phon** |
| **Vivid turquoise** | `#00E5FF` | Gradient end (bottom-right), wordmark **IQ**, trend arrow in mark |
| **Mark on gradient** | `#FFFFFF` | Primary strokes/fills of the “P” symbol on launcher |
| **Notification / status** | White (+ transparency) | Small icon: **alpha only**; no color or gradients |

Legacy UI accents in `PROJECT.md` (e.g. `#6C63FF`) may remain for in-app theme; **store and launcher** should follow this table for consistency.

---

## Step 1 — Standard app icon (1:1)

- **Format:** High-resolution **square** asset (1:1) for stores, press, and design handoff.
- **Content:** Squircle-style rounded square with **linear diagonal gradient** deep blue → turquoise; **centered** stylized **“P”** composed of:
  1. **Three ascending vertical bars** (signal / chart metaphor),
  2. **Handset-shaped loop** forming the bowl of the P,
  3. **Upward trend arrow** (turquoise) inside the loop.
- **Quality:** Clean, flat/shallow depth; balanced padding inside the crop.

---

## Step 2 — Adaptive icon layer set (Android)

- **Background layer:** **108 × 108 dp** fill; **linear gradient** `#0D47A1` (top-left) → `#00E5FF` (bottom-right). Implemented as `drawable/ic_phoniq_launcher_background.xml`.
- **Foreground layer:** **108 × 108 dp**; **white** symbol + **turquoise** arrow on **transparent** outside the artwork. Critical paths must sit in the **~66 dp diameter safe zone** (centered) so OEM masks (squircle, circle, teardrop, rounded rect) do not clip the mark. Implemented as `drawable-nodpi/ic_phoniq_launcher_forground.webp` (bitmap layer; replace placeholder as needed).
- **Manifest:** `mipmap-anydpi-v26/ic_phoniq_launcher.xml` and `ic_phoniq_launcher_round.xml` reference both layers via `<adaptive-icon>`.

---

## Step 3 — Logo wordmark presentation

- **Layout:** Presentation card (e.g. white or neutral background).
- **Icon:** Same gradient + mark as Step 1, **top-center**.
- **Wordmark:** **PhonIQ** — geometric sans-serif:
  - **Phon** — heavier weight (Medium/Bold), color `#0D47A1`,
  - **IQ** — lighter weight (Regular), color `#00E5FF`, aligned on one baseline.
- **Tagline (below wordmark):** `Intelligence. Communication. Finance.` — smaller, **letter-spaced** uppercase, subdued slate gray.

Marketing exports should live in `design/brand/` (see `03-logo-wordmark-card.png`).

---

## Step 4 — Notification / status bar icon

- **Requirement:** **Monochrome white** on **transparent** background; OS uses **alpha** only (colors stripped).
- **Design:** Simplified silhouette of the same “P” (bars + handset loop + arrow) with **enough negative space** so it does not read as a blob at **~24 dp**.
- **Implementation:** `drawable/ic_stat_phoniq.xml` (vector). All notification `setSmallIcon` call sites should use **`R.drawable.ic_stat_phoniq`**, not the full-color launcher.

Optional: generate raster densities (e.g. 48×48 px mdpi) via Android Asset Studio if OEM testing prefers PNGs.

---

## Step 5 — Feature graphic / marketing banner (Play Store)

- **Dimensions:** **1024 × 500 px** (standard Play feature graphic).
- **Background:** Deep blue → vivid turquoise digital gradient; optional subtle **grid** for “data / intelligence.”
- **Top-center:** Combined **icon + wordmark** (same rules as Step 3).
- **Below:** Three **vertical phone mockups** (left → right):

  1. **INTELLIGENT DIALER** — call UI with prominent **suspected spam** / community-style alert.
  2. **SMART MESSAGING** — inbox with **Personal / OTP / Transactions** organization and smart cards.
  3. **MONEY MANAGER** — dashboard with **pie chart** and **trend** visuals from SMS-derived data.

- **Safe zone:** Keep critical text and hero UI inside central **~70%**; outer **~15%** may be cropped by Play or device framing.

Export file name suggestion: `play-feature-graphic-1024x500.png` in `design/brand/` when ready.

---

## Repository layout

```
design/brand/phoniq-icon-2026/
  01-standard-app-icon.png
  02-adaptive-icon-schematic.png
  03-logo-wordmark-card.png
  04-brand-horizontal-card.png
  06-logo-horizontal-mono-white-on-transparent.png  <- PhonIQ icon + wordmark (flat white, transparent BG)
  07-colored-squircle-launcher-gemini.png           <- raster reference only (adaptive foreground uses WebP + ic_phoniq_launcher_background)
  08-favicon-gradient-p.svg                         <- copy of site favicon (gradient “P”); handy vector reference
```

---

## Documentation sync

When the brand team updates any Step **1–5** asset, update this file and, if Android resources change, bump **`docs/PROJECT.md`** (Brand Guidelines) and keep vectors/gradients in sync.
