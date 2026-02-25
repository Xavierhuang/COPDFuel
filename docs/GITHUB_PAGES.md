# GitHub Pages – Deploy this site

This folder contains a static site you can serve from GitHub Pages: a landing page and the privacy policy.

## Enable GitHub Pages

1. Push this repo to GitHub (including `docs/index.html` and `docs/privacy.html`).
2. In the repo: **Settings** > **Pages**.
3. Under **Build and deployment** > **Source**, choose **Deploy from a branch**.
4. **Branch:** `main` (or `master`) | **Folder:** `/docs` | **Save**.
5. After a minute or two, the site is live at:
   - **Base URL:** `https://<your-username>.github.io/COPD/`
   - **Privacy policy URL:** `https://<your-username>.github.io/COPD/privacy.html`

## Use the privacy URL in the app and portal

- **Android** (`android/app/build.gradle`):  
  `buildConfigField "String", "PRIVACY_POLICY_URL", "\"https://<your-username>.github.io/COPD/privacy.html\""`
- **Doctor portal** (`.env` or env vars):  
  `VITE_PRIVACY_POLICY_URL=https://<your-username>.github.io/COPD/privacy.html`

Replace `<your-username>` with your GitHub username (or your org name if the repo is under an organization).

## Before publishing the policy

1. Edit `docs/privacy.html` and replace all `[bracketed]` placeholders (date, company name, contact email, address, retention wording).
2. Have a lawyer review the privacy policy before going live.
