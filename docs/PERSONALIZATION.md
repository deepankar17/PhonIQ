# PhonIQ — Personalization & Customization Spec
> Version 1.1 | 2026-05-02

Full **mockup done vs planned** tracking (including this personalization surface) lives in `docs/PROJECT.md` → **Mockup requirement checklist**.

## Inspiration
Right Dialer (Goodwy) offers: themes, accent colors, font selection, dialpad padding,
show/hide search bar, messenger shortcuts, swipe gestures, dark/light/auto themes.
PhonIQ goes significantly further across all three pillars.

---

## 1. THEME & APPEARANCE

### 1.1 Color Themes
| Option | Description |
|---|---|
| AMOLED Black | Pure #000000 background — true black for S25 AMOLED |
| Deep Dark | #0A0A0F (default PhonIQ dark) |
| Dark Navy | #0A0F1A navy-tinted dark |
| Dark Forest | #0A1A0F green-tinted dark |
| Dark Wine | #1A0A0F wine-tinted dark |
| Light | #F5F5FA clean white |
| Light Warm | #FAF5F0 warm paper tone |
| Auto | Follows system (Samsung One UI) |

### 1.2 Accent Color Picker
- 20 preset swatches + full custom hex/HSL picker
- Presets: Indigo, Teal, Blue, Purple, Red, Orange, Green, Pink, Yellow, Cyan...
- Applied to: nav bar active icon, FAB, buttons, progress bars, chips, highlights
- Real-time live preview before applying

### 1.3 Material You / Dynamic Color
- Toggle: Use wallpaper-extracted color as primary accent (Android 12+)
- PhonIQ adapts entire UI to wallpaper palette automatically

### 1.4 App Icon Style
- Round, Squircle, Teardrop, Rounded Square, Original
- Icon background color: accent / custom / gradient

### 1.5 Typography
| Setting | Options |
|---|---|
| Font Family | Roboto (default), Inter, Nunito, Poppins, System default |
| Base Font Size | Small (12sp), Normal (14sp), Large (16sp), XL (18sp) |
| Letter Spacing | Tight, Normal, Wide |
| Number Font | Tabular / Proportional (affects call timer + amounts) |

---

## 2. DIALER PERSONALIZATION

### 2.1 Dialpad Style
| Style | Look |
|---|---|
| Classic | Square keys, flat background |
| Rounded | Heavily rounded keys (pill/circle shaped) |
| Minimal | No key background, numbers only |
| iOS-like | White keys, thin separators |
| Material3 | Tonal surface keys with elevation |

### 2.2 Dialpad Settings
- Key size: Compact / Normal / Large / Extra Large
- Show/hide alphabets under numbers (ABC, DEF etc.)
- Haptic feedback: Off / Light / Medium / Strong
- Dial tone: On / Off / Volume slider
- Dialpad position: Always visible below recents / FAB overlay / Swipe up
- Dialpad background: Solid / Blurred / Transparent

### 2.3 Call Screen Personalization
- Background: AMOLED solid / Blurred contact photo / Gradient / Custom image
- Avatar size: Small / Medium / Large / Full screen blur
- Answer style: Tap button / Swipe right (TrueCaller style) / Slide up
- Decline style: Tap button / Swipe left / Slide down
- Action button layout: 2×3 grid / 1×6 row / Arc/circular
- Show call timer: On / Off
- Show call quality indicator: On / Off
- Caller name font size: Normal / Large / XL

### 2.4 Caller ID Display
- Show spam score badge: On / Off
- Spam badge style: Chip / Icon only / Color ring on avatar
- Show "PhonIQ Verified" label: On / Off
- Unknown caller tag: "Unknown" / "?" / "Not in contacts"
- Business caller indicator: Logo icon / Text badge

---

## 3. CONTACT LIST PERSONALIZATION

### 3.1 List Layout
| Layout | Description |
|---|---|
| Comfortable | Large avatar + name + number |
| Compact | Small avatar, tighter spacing |
| Card | Each contact in an elevated card |
| Grid | 2-column avatar grid |

### 3.2 Avatar Style
- Shape: Circle / Squircle / Rounded Square / Hexagon
- Size: Small (32dp) / Medium (44dp) / Large (56dp)
- Fallback: Colored initials / Monogram / Generic icon
- Show/hide contact photos
- Auto-color initials by name hash (consistent per contact)

### 3.3 Sort & Group
- Sort by: First Name / Last Name / Most Called / Recent
- Group by: None / First letter / Label / Frequency band
- Show/hide section headers (A, B, C...)
- Show/hide phone number under name
- Show/hide last call time

### 3.4 Swipe Actions (per contact row)
- Swipe right: Call / Message / Add note (configurable)
- Swipe left: Block / Delete / Star (configurable)

---

## 4. CALL HISTORY PERSONALIZATION

### 4.1 Display
- Group repeated calls: On / Off
- Show call duration in list: On / Off
- Show spam badge in history: On / Off
- Call type icon style: Colored arrow / Badge chip / Dot indicator
- Date/time format: Relative ("2m ago") / Absolute / Both

### 4.2 Colors
- Incoming call color: (default: Teal)
- Outgoing call color: (default: Purple)
- Missed call color: (default: Red)
- All configurable from accent or custom

---

## 5. MESSAGING PERSONALIZATION

### 5.1 Bubble Style
| Style | Look |
|---|---|
| Rounded | Fully rounded (WhatsApp-like) |
| Square | Rectangular with small radius |
| Minimal | No bubble, just text |
| Tailored | Classic SMS bubble with tail |

### 5.2 Bubble Colors
- Sent: accent color / custom
- Received: surface color / custom
- OTP message: highlighted teal border
- Transaction message: highlighted with bank color accent

### 5.3 Typography & Density
- Message font size: Small / Normal / Large
- Thread list density: Compact / Comfortable / Spacious
- Show message preview: 1 line / 2 lines / Off
- Show timestamp on each bubble: Always / On tap / Never

### 5.4 Tab Bar Style
- Tab position: Top / Bottom
- Tab style: Text only / Icons only / Icons + Text
- Tab indicator: Underline / Filled pill / None
- Unread badge style: Dot / Count / None

### 5.5 OTP Customization
- OTP code display size: Normal / Large / XL
- Auto-copy countdown: Show timer / Hide timer
- OTP expiry warning color: configurable

### 5.6 RCS + Advanced Message UI
- Show RCS capability chip in thread list and header: On / Off
- End-to-end encryption status bar for RCS threads: On / Off
- Delivery status indicators: Sent (✓) / Delivered (✓✓) / Read (blue ✓✓)
- Message reactions: Off / Minimal / Full
- Typing indicator style: Dots / Text / Off (mockup: dots in-thread + optional list subtitle)
- Voice note bubble style: Compact / Waveform / Off
- Smart transaction card action: "View in Money" button toggle
- Thread header voice: Opens PhonIQ outgoing calling screen (uses thread identity in mockup)

### 5.7 Thread Filtering Behavior
- Smart tabs can be data-driven from a unified "All" stream (recommended)
- Filters: Personal / OTP / Transactions / Spam via message tags
- Empty state behavior per tab: Show "No messages" label

### 5.8 Live Personalization Controls (Mockup v1)
- Accent color: 10 swatches update nav active state, FAB gradients, tab highlights, message chips, read ticks, bubble sent-tone, and toggle "on" color instantly.
- Theme presets: AMOLED / Deep Dark / Navy / Forest / Wine / Light update app background, card surfaces, border contrast, and text tone live.
- Font family options: Roboto / Nunito / Mono / Serif / System apply app-wide in real time.
- Font size options: Small / Normal / Large / XL scale UI typography in-place for quick readability testing.
- Dialpad style options: Classic / Rounded / Minimal / iOS / Material 3 apply directly to dial keys without reload.
- Avatar shapes: Circle / Squircle / Rounded / Square / Teardrop map to the shared avatar radius token across call list, messages, contact details, and call screens.
- Bubble style modes: Rounded / Tailored / Square / Minimal update bubble corners live; tails are shown only in Tailored mode.
- Toggle switches: every settings toggle supports direct tap flip (on/off) and immediate visual state change.

---

## 6. MONEY MANAGER PERSONALIZATION

### 6.1 Chart Style
- Donut chart / Pie chart / Bar chart / Line graph
- Animate charts on load: On / Off
- Chart color palette: Auto from accent / Manual / Monochrome

### 6.2 Transaction List
- Density: Compact / Comfortable
- Amount alignment: Right / Left
- Debit color: Red (default) / Orange / Custom
- Credit color: Green/Teal (default) / Custom
- Category icon style: Emoji / Flat icon / Colored circle
- Show/hide account name on each transaction
- Number format: Indian (1,00,000) / International (100,000)

### 6.3 Summary Card
- Card style: Gradient / Solid / Outlined / Neumorphic
- Show/hide savings row
- Show/hide budget progress bar
- Currency symbol: ₹ / $ / € / Custom

### 6.4 Category Colors
- Each category has a custom color — all editable
- Default palette auto-assigned, fully overridable

---

## 7. NOTIFICATIONS & ALERTS

### 7.1 Incoming Call Notification
- Style: Full screen / Heads-up banner / Mini toast / Silent
- Toast style: Top banner (default) / Bottom card
- Show caller photo in notification: On / Off
- Show spam warning in notification: On / Off
- Flash notification (LED/screen flash): On / Off / Camera flash

### 7.2 Ringtone & Vibration
- Global ringtone selection
- Per-contact custom ringtone
- Vibration pattern: Default / Short / Long / Double pulse / Custom
- Increasing ring volume (starts soft, grows)
- Pocket detection (louder when in pocket)

### 7.3 Budget Alerts
- Over-budget notification: On / Off
- Alert threshold: 75% / 90% / 100% / Custom %
- Alert style: Banner / Badge only / Sound + Banner
- Frequency: Once / Daily / Each transaction

---

## 8. GESTURES & BEHAVIOR

### 8.1 Call Handling
- Answer: Tap / Swipe right / Proximity sensor (bring to ear)
- Decline: Tap / Swipe left / Flip phone face-down
- Power button: End call / Mute ring / Do nothing
- Volume button: Lower ring volume / Mute / Do nothing
- Shake phone: Decline call / Do nothing

### 8.2 Navigation Gestures
- Swipe left/right on call item: configurable action
- Long press contact: Quick actions menu / Call immediately
- Pinch on history list: Compact/expand view
- Pull to refresh: Sync contacts / Reload history

### 8.3 Double Tap
- Double tap status bar: Scroll to top
- Double tap back: Exit app / Last view

---

## 9. QUICK SETTINGS & SHORTCUTS

### 9.1 Quick Dial Shortcuts
- Up to 9 pinned contacts on dialer home
- Shortcut display: Photo + name / Initials / Icon
- Layout: Row / Grid / Carousel

### 9.2 Android Quick Settings Tiles
- Block calls tile (toggle)
- DND mode tile
- Spam filter tile

### 9.3 App Widget
- Missed calls widget (1×1 / 2×1)
- Quick dial widget (4×1 strip of 4 contacts)
- Monthly spend widget (shows ₹ spent / budget)

### 9.4 Lock Screen
- Show missed call on lock screen: On / Off
- Show OTP on lock screen: On / Off (security toggle)
- Show spending alert on lock screen: On / Off

---

## 10. PRIVACY & SECURITY PERSONALIZATION

### 10.1 App Lock
- Biometric lock (fingerprint/face): On / Off
- PIN lock
- Lock trigger: Immediately / After 1 min / After 5 min / On app background

### 10.2 Stealth Mode
- Hide transactions tab: toggle (for shared devices)
- Blur amounts on screenshots
- Incognito call mode (no call log entry)

### 10.3 Data Controls
- Auto-delete call history older than: 30d / 90d / 1yr / Never
- Auto-delete OTPs after: 24h / 7d / Never
- Backup reminder: Weekly / Monthly / Never

---

## Implementation Priority

### Phase 1 (MVP — must have)
- Dark / AMOLED / Light / Auto theme
- 10 accent color presets
- Dialpad style (Classic + Rounded)
- Font size (3 options)
- Swipe actions on contacts
- Call screen background (solid / blurred photo)
- Transaction debit/credit colors

### Phase 2 (Feature complete)
- Full color picker (hex)
- All dialpad styles (5)
- Avatar shape selector
- Bubble style selector
- Chart style selector
- Ringtone per contact
- All gesture settings
- Quick dial shortcuts
- App widget

### Phase 3 (Advanced)
- Material You dynamic color
- Custom font loading
- App icon pack support
- Lock screen widgets
- Stealth mode
- Incognito call mode
