# Remix Icon → Material Icons (Compose)

PhonIQ mockups use [Remix Icon](https://remixicon.com/) via `phoniq-mockup-v1.html`. The Android app uses Material Symbols / `androidx.compose.material.icons`. This doc maps **Remix class names** used in the mockup to **suggested `Icons.*` usages** so we can keep visual parity without bundling Remix on Android.

Conventions:

- **Filled vs outlined**: Mockup `-line` ≈ Material **Outlined**; `-fill` ≈ **Filled** when we care about weight.
- **AutoMirrored**: Use `Icons.AutoMirrored.Filled.*` (or `Outlined`) for directional glyphs (back, reply, send) so RTL layouts stay correct.
- **Not in extended set**: Use the closest Material equivalent or a custom vector; noted in notes.

## Bottom navigation (`#phoniq-screen` `.bottom-nav`)

| Remix (mockup) | Where | Material Compose (suggested) |
|----------------|-------|-------------------------------|
| `ri-phone-line` | Phone tab | `Icons.Outlined.Call` or `Icons.AutoMirrored.Outlined.Call` |
| `ri-message-3-line` | Messages tab | `Icons.AutoMirrored.Outlined.Message` |
| `ri-money-rupee-circle-line` | Money tab | `Icons.Outlined.CurrencyRupee` (or `AccountBalance` if rupee unavailable in your icon set) |

## Unified header

| Remix | Where | Material (suggested) |
|-------|--------|----------------------|
| `ri-phone-fill` | App logo chip | `Icons.Filled.Call` |
| `ri-search-line` | Search affordance | `Icons.Outlined.Search` |
| `ri-more-2-fill` | Overflow | `Icons.Filled.MoreVert` |

## Outgoing / active call (`#screen-calling` `.call-actions-grid`)

| Remix | Label | Material (suggested) |
|-------|--------|----------------------|
| `ri-mic-line` | Mute | `Icons.Outlined.Mic` / muted: `Icons.Filled.MicOff` |
| `ri-grid-line` | Keypad | `Icons.Filled.Dialpad` |
| `ri-volume-up-line` | Speaker | `Icons.Outlined.VolumeUp` / on: `Icons.Filled.VolumeUp` or `SpeakerPhone` |
| `ri-pause-line` | Hold | `Icons.Filled.Pause` |
| `ri-record-circle-line` | Record | `Icons.Filled.FiberManualRecord` |
| `ri-message-3-line` | SMS | `Icons.AutoMirrored.Filled.Message` |
| `ri-close-line` | End call | `Icons.Filled.Close` |

## Incoming call screen (`#screen-incoming`)

| Remix | Where | Material (suggested) |
|-------|--------|----------------------|
| `ri-checkbox-circle-fill` | Verified badge | `Icons.Filled.Verified` or `CheckCircle` |
| `ri-phone-fill` / `ri-close-line` | Answer / decline | `Icons.Filled.Call` / `Icons.Filled.CallEnd` (mockup uses filled phone + red close) |

## Dialer (`#dialer-panel-*`)

| Remix | Where | Material (suggested) |
|-------|--------|----------------------|
| `ri-history-line` | History subtab | `Icons.AutoMirrored.Outlined.History` |
| `ri-group-2-line` | People subtab | `Icons.Outlined.People` or `Group` |
| `ri-star-line` | Starred subtab | `Icons.Outlined.Star` / favorite: `Icons.Filled.Star` |
| `ri-grid-line` | Context FAB | `Icons.Outlined.Apps` or `Dialpad` depending on action |
| `ri-delete-back-2-line` | Backspace | `Icons.Outlined.Backspace` |
| `ri-phone-fill` | Call button | `Icons.Filled.Call` |

## Recents & contacts rows

| Remix | Where | Material (suggested) |
|-------|--------|----------------------|
| `ri-arrow-left-down-line` / `ri-arrow-right-up-line` | Call direction | `Icons.Outlined.SouthWest` / `NorthEast` (or `CallReceived` / `CallMade` if using communication icons) |
| `ri-phone-line` | Redial method | `Icons.Outlined.Call` |
| `ri-vidicon-line` | Video / WA video | `Icons.Filled.Videocam` |
| `ri-phone-fill` | Contact row call | `Icons.Filled.Call` |
| `ri-message-3-line` | Contact row message | `Icons.AutoMirrored.Outlined.Message` |
| `ri-add-line` | Add favorite chip | `Icons.Filled.Add` |
| `ri-star-fill` | Favorite badge | `Icons.Filled.Star` |
| `ri-alert-line` / `ri-prohibited-2-line` | Spam / blocked avatar | `Icons.Outlined.Warning` / `Icons.Outlined.Block` |

## Messages inbox

| Remix | Tab / control | Material (suggested) |
|-------|----------------|----------------------|
| `ri-inbox-archive-line` | All | `Icons.Outlined.AllInbox` or `Inbox` |
| `ri-mail-unread-line` | Unread | `Icons.Outlined.MarkEmailUnread` |
| `ri-user-3-line` | Personal | `Icons.Outlined.Person` |
| `ri-money-rupee-circle-line` | Transaction | `Icons.Outlined.CurrencyRupee` |
| `ri-key-2-line` | OTP | `Icons.Outlined.Key` or `VpnKey` |
| `ri-receipt-line` | Bill | `Icons.Outlined.ReceiptLong` |
| `ri-truck-line` | Delivery | `Icons.Outlined.LocalShipping` |
| `ri-flight-takeoff-line` | Travel | `Icons.Outlined.FlightTakeoff` |
| `ri-spam-2-line` | Spam | `Icons.Outlined.Report` or `Block` |
| `ri-chat-new-line` | New message FAB | `Icons.Outlined.Chat` or `Icons.AutoMirrored.Outlined.Chat` |

## Contact detail (`#view-contact` quick actions)

| Remix | Action | Material (suggested) |
|-------|--------|----------------------|
| `ri-phone-fill` | Call | `Icons.Filled.Call` |
| `ri-message-3-line` | Message | `Icons.AutoMirrored.Filled.Message` |
| `ri-edit-line` | Edit | `Icons.Outlined.Edit` |
| `ri-calendar-event-line` | Event | `Icons.Outlined.Event` |
| `ri-user-settings-line` | Settings row | `Icons.Outlined.ManageAccounts` or `Settings` |

## Onboarding / permission (`PermissionScreen`)

| Remix | Where | Material (suggested) |
|-------|--------|----------------------|
| `ri-phone-fill` | Badge (per mockup density) | `Icons.Filled.Phone` or `Icons.Filled.Call` |

## Global search & overlays

| Remix | Where | Material (suggested) |
|-------|--------|----------------------|
| `ri-arrow-left-s-line` | Back | `Icons.AutoMirrored.Filled.ArrowBack` |

## Maintenance

- When adding Remix icons to the HTML mockup, append a row here and the implementing composable.
- Prefer **one** icon style per surface (e.g. all outlined tabs + filled FAB) to match Material guidance.
