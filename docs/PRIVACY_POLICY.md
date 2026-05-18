# Privacy Policy for PhonIQ

**Last Updated:** May 17, 2026

> **Before publishing:** Replace all bracketed placeholders (`[like this]`) with your legal entity name, registered address, and working contact emails. Host the final HTML or Markdown on a **public URL** (e.g. **Firebase Hosting** `https://<your-project-id>.web.app`, or GitHub Pages) and add that URL in **Google Play Console → App content → Privacy policy**.

---

## 1. Introduction

Welcome to **PhonIQ** (“we,” “our,” or “us”). We are committed to protecting your privacy and helping you stay in control of your personal data. This Privacy Policy explains how we collect, use, store, and protect your information when you use our mobile application as your **default Phone (dialer)** and **default SMS** app, and when you use related features such as **on-device spam intelligence**, **call handling**, and **optional financial insights from SMS** (“**Services**”).

This policy is written to align with **Google Play Developer Policies** (including expectations for **sensitive permissions** and **contacts** use), and with the **Digital Personal Data Protection (DPDP) Act, 2023** and evolving **2026** regulatory and platform guidance in **India**. If you are outside India, applicable local laws may also apply.

**Application:** PhonIQ — package name **`com.phoniq.app`**.

---

## 2. Information We Collect and Why (Purpose Limitation)

PhonIQ needs certain **Android permissions** to replace your system dialer and messaging experience. We apply **purpose limitation**: we use data only for the purposes described below.

### A. Default handler & telephony (strictly on-device for logs and message content)

If you set PhonIQ as your **default Phone** or **default SMS** app (or grant the related permissions), we may access the following **on your device**:

| Permission / data | Why we use it |
|-------------------|----------------|
| **Call logs (`READ_CALL_LOG`, `WRITE_CALL_LOG`)** | To show your call history, sync a **local** copy for the in-app recents experience, and support actions you initiate (e.g. return call, delete history where supported). |
| **SMS / MMS (`READ_SMS`, `SEND_SMS`, `RECEIVE_SMS`, `RECEIVE_WAP_PUSH`, `WRITE_SMS`)** | To display conversations, send messages you choose to send, receive inbound SMS, and show a **limited MMS inbox slice** where implemented. |
| **Phone state (`READ_PHONE_STATE`, `CALL_PHONE`, `ANSWER_PHONE_CALLS`, `MANAGE_OWN_CALLS`)** | To place and manage calls in line with the Android **Telecom** framework and your settings. |
| **Contacts (`READ_CONTACTS`, `WRITE_CONTACTS`)** | To show **contact names and photos** from **your** address book, link threads to people you know, and support **contact edits** only when **you** perform them (we do **not** upload your full address book — see **B**). |
| **Microphone (`RECORD_AUDIO`)** | **Only** if you enable **in-call recording** (or similar features): to capture audio for **local** storage as described in-app; not used for advertising. |
| **Notifications (`POST_NOTIFICATIONS`, `USE_FULL_SCREEN_INTENT`)** | To show **incoming call** and **message** notifications as allowed by the OS. |
| **Network (`INTERNET`, `ACCESS_NETWORK_STATE`)** | Declared for **optional, user-directed** or **non-IQ** features (for example, a **manual** backup/export path if offered). **Core dialer, SMS, and on-device “IQ” processing are designed to work without sending your call logs or SMS bodies to us** unless a **separate, clearly labeled** feature says otherwise. |

**Zero-upload guarantee (core IQ):** Your **full call log contents** and **private SMS message bodies** are **not** uploaded to our servers for core functionality, **not sold**, and **not** used for targeted advertising. Default-handler processing for calls and messages (**PhonIQ IQ**) is performed **locally** on your device.

**Money-related SMS:** If you use **Money** or similar features, **transaction-related SMS** may be parsed **on-device** to show summaries, categories, or budgets. That parsing does **not** change the core guarantee: we do **not** upload your SMS library to our servers for this purpose.

### B. Contacts (Google Play contacts expectations)

* We access contacts **locally** to display caller names, enrich threads, and support **your** actions (e.g. add star, edit when you choose).
* We **do not** harvest or upload your **entire** address book to our servers for advertising or to build a social graph.
* Where the OS supports it, picking a **specific** contact uses **Android’s contact picker** or similar system flows rather than bulk export.

### C. Community spam reports & cloud processing (**only if / when enabled**)

Some versions of PhonIQ may offer **optional** community spam reporting (for example, reporting a **specific** number you mark as spam). **The shipping product may or may not include this feature;** where **not** implemented, this section does not apply.

When **that feature is available** and **you actively choose** to submit a report:

* **Only** the **information needed** to process the report (e.g. **normalized phone number** and **report metadata**) may be sent to **secure backend infrastructure** (for example **Google Firebase** — **Firestore** and **Cloud Functions**) configured with access controls.
* We **do not** need — and we instruct implementations **not** to transmit — your **full** contacts, **full** SMS history, or **full** call history for this purpose.
* Aggregated or scored signals may be used to improve **spam awareness** for the community. **App Check** and server-side rules should be used to reduce abuse, consistent with our **[Product / engineering documentation](PROJECT.md)**.

If you **never** use community reporting (or the feature is absent), **no such upload** occurs from that path.

---

## 3. How We Use Your Data

We use information **only** as needed to:

1. Provide **core dialer and SMS** functionality you expect from a default app.
2. Show **caller / thread** context and **on-device** spam or trust hints consistent with in-app disclosures.
3. Operate **optional** community spam mechanisms **only when enabled and when you submit reports**, as described in **§2C**.
4. Respond to **support** or **legal** requests where permitted by law.

We **do not** sell your personal data. We **do not** use your SMS or call content for **third-party** advertising.

---

## 4. Data Storage, Security, and Localization

* **On-device data:** Call history mirrors, message indexes, notes, recordings (if enabled), and app settings are stored **on your device** (including **local databases**). They are protected by your **device lock** and OS sandboxing.
* **Cloud data (if used):** If you use features backed by **Firebase** (or similar), we aim to configure services in **`asia-south1` (Mumbai)** or otherwise in **India** where feasible and appropriate for **DPDP** alignment. Exact regions will be confirmed in your deployment and can be updated in this policy.
* **Security:** We use **TLS** (and platform APIs) for data **in transit** where cloud features exist. **At rest**, cloud providers apply their **encryption** standards; sensitive **local** recordings may be stored with **additional app-level protection** as described in-app.
* **Access control:** Cloud data (if any) should be guarded by **Firebase Security Rules**, **Cloud Functions** validation, and **Firebase App Check** (or equivalent) to reduce unauthorized access.

---

## 5. Third-Party Services

* **Google / Firebase:** If community or account features use **Firebase**, processing is subject to **Google’s** terms and privacy notices for those products, in addition to this policy.
* **Google Analytics for Firebase (optional freemium tier):** If we enable **in-app analytics** to measure subscriptions and product usage, the **Google Analytics for Firebase** SDK may collect **pseudonymous** identifiers, **events** (e.g. screen views, custom funnel events), and **purchase-related** signals. **Linked Google Play** may supplement **in-app purchase** telemetry as allowed by Google. We use such data to improve the product and measure **freemium conversions**, **not** to sell data or run third-party ads. Where required, we will obtain **consent** before initialization and describe controls in-app. See **`docs/PROJECT.md` (PRD §8)** for the engineering intent.
* We **do not** integrate **advertising SDKs** or **data brokers** for PhonIQ’s core experience as described here.

---

## 6. Your Rights (India — DPDP and Grievance)

If you are in **India**, you may have rights including (as applicable):

* **Access / confirmation** regarding whether we process your personal data covered by this policy.
* **Correction** of inaccurate **account or profile** data we hold if we operate a login or support identity for cloud features.
* **Erasure / withdrawal** — see **§7**; you may also **revoke default-app roles** or **uninstall** the app to stop local access.
* **Grievance** — contact our **Grievance Officer** (below).

Laws outside India may provide similar rights; contact us and we will respond consistent with applicable law.

---

## 7. Data Retention and Deletion

* **Local data:** Remains on your device until you delete it in-app, clear app storage, or **uninstall** PhonIQ (subject to OS behavior).
* **Cloud data (if any):** Retained only as long as needed for the **stated purpose** (e.g. spam aggregation) and applicable law.
* **Deletion requests:** You may request deletion of **identifiable** data tied to **optional cloud** features by contacting **[Contact Email]** or using an in-app **“Delete my data”** flow when available. We will respond within timelines required by law after verifying your request where appropriate.

---

## 8. Children’s Privacy

PhonIQ is **not** directed at children. We do not knowingly collect children’s personal data. If you believe we have, contact us and we will delete it where required.

---

## 9. Changes to This Privacy Policy

We may update this policy for **legal**, **regulatory**, or **product** reasons. **Material** changes should be communicated via **in-app notice**, **Play listing**, or **email** (if we have one for your account). Continued use after the effective date may constitute acceptance where permitted by law.

---

## 10. Contact Us & Grievance Officer

For privacy questions, data rights requests, or complaints under Indian law:

**Legal / trading name:** **[Your Company / Legal Entity Name]**  

**Grievance Officer / Data Protection contact:** **[Grievance Officer Name]**  

**Email:** **[privacy or support email]**  

**Postal address:** **[Registered address — City, State, PIN, India]**  

For **Google Play** support channels, use the developer contact shown on the store listing once published.

---

## Tips for Google Play Review (Sensitive Permissions & Contacts)

1. **Consistency:** Your **store listing**, **in-app disclosures**, and **this policy** must match **actual behavior** (especially: **no** full contact upload if you claim none).
2. **Prominent disclosure:** Before **default dialer / SMS** or **sensitive** grants, show a **clear, plain-language** screen summarizing **§2A** and any **§2C** cloud path **before** the system permission dialog.
3. **Data Safety form:** Answer Play’s **Data safety** questions to match this policy line-by-line.
4. **Minimize:** Request **WRITE_CONTACTS** (or similar) only if you truly need **write** access; declare **exactly** what leaves the device.

---

*This document is a **draft** for legal review. It does not constitute legal advice.*
