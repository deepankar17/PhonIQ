# PhonIQ — Firebase Hosting (info site)

Static site under `hosting/public/`, deployed to **Firebase Hosting** on the free tier.

Default URLs (after deploy):

- `https://<PROJECT_ID>.web.app`
- `https://<PROJECT_ID>.firebaseapp.com`

The project ID **`phoniq`** is **already taken** on Google Cloud/Firebase (you cannot get `phoniq.web.app` with that id). This repo defaults **`.firebaserc`** to **`phoniq-app`**, which yields **`https://phoniq-app.web.app`** once you create a Firebase project with that **exact** id (or change the file to match whatever id you reserve).

**Alternatives if your preferred id is taken:**

- Try **`phoniq-in`**, **`phoniq-io`**, **`get-phoniq`**, etc., and keep **`.firebaserc` in sync**.
- Or buy **`phoniq.app`** and add it under Hosting → **Custom domains** (still use any available project id).

## One-time setup

1. Install the [Firebase CLI](https://firebase.google.com/docs/cli) (`npm install -g firebase-tools`).
2. `firebase login`
3. In [Firebase Console](https://console.firebase.google.com), **Create a project** (or use an existing one). Enable **Hosting** for that project.
4. If the project ID is not `phoniq`, edit **`.firebaserc`** in the repo root and set `"default": "<your-project-id>"`.

## Deploy

From the **`phoniq/`** repository root (where `firebase.json` lives):

```bash
firebase deploy --only hosting
```

## Files

| Path | Purpose |
|------|---------|
| `firebase.json` | Hosting config (`public` → `hosting/public`) |
| `.firebaserc` | Default Firebase project alias |
| `hosting/public/index.html` | Landing page |
| `hosting/public/privacy.html` | Short privacy page + link to full `docs/PRIVACY_POLICY.md` on GitHub |

After you publish the app, add the live site URL to Play Console **Privacy policy** and your store listing.
