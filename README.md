# PhonIQ

**Know every call. Track every rupee.**

Offline-first Android app: Phone, Messages, Money — specs and HTML mockup live in this repo; Kotlin app under [`android/`](android/).

| | |
|---|---|
| **Remote** | [github.com/deepankar17/PhonIQ](https://github.com/deepankar17/PhonIQ) |
| **Clone (GitHub CLI)** | `gh repo clone deepankar17/PhonIQ` |
| **Clone (Git)** | `git clone https://github.com/deepankar17/PhonIQ.git` |

- **Min OS:** Android **15** (API **35**); validated on **15 & 16**
- **Stack:** Kotlin, Jetpack Compose, Room — see [`android/README.md`](android/README.md)
- Product + architecture: [`docs/PROJECT.md`](docs/PROJECT.md)
- Personalization UX: [`docs/PERSONALIZATION.md`](docs/PERSONALIZATION.md)
- Interactive mockup: [`design/phoniq-mockup-v1.html`](design/phoniq-mockup-v1.html)

**Privacy:** no ads, no telemetry, **offline-first** core IQ. `INTERNET` may appear in the manifest for optional future/manual flows only (Drive and similar)—**not** for background sync, ads, telemetry, or cloud caller reputation.

> Early development — APIs and structure will evolve. Issues and PRs welcome after a CONTRIBUTING note is added.
