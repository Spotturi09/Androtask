# Resume Builder (Android)

A native Android resume builder (Kotlin + Jetpack Compose).
Its main promise is the opposite of what competitors' 1-star reviews complain about:
**your PDF download is always free: no watermark, no paywall and no ad before download.**

## Features

| Area | What's included |
|---|---|
| Sections | Personal details (+ optional photo), Summary, Experience, Education, Skills (with levels), Projects, Certifications, Languages, References, plus a custom section (Interests, Volunteering…) |
| Editing | Autosave, reorder sections, hide sections, reorder/delete items, "one bullet per line" achievements, resume-strength score + checklist |
| Writing help (offline) | Example summaries, achievement bullets and skill suggestions for 8 common roles, plus a list of strong action verbs |
| Templates | Classic, Modern, Minimal (all ATS-friendly single column) and Sidebar (two-column with photo); 8 accent colours; Sans or Serif |
| PDF | Real multi-page A4 PDF with page numbers; automatically tightens spacing when that fits the resume on one page fewer |
| Output | Live preview, Download PDF (system file picker), Share (email, WhatsApp, Drive…) |
| Data | Multiple resumes, duplicate, rename; JSON backup/restore of all resumes; fully offline |
| Monetisation | AdMob adaptive banner on the home screen only |

Template previews: [`docs/templates/`](docs/templates).

## Project layout

```
core/  Pure Kotlin (JVM) — no Android dependency
  model/   Resume data model, completeness score, phrase library
  layout/  Page layout engine + 4 templates → list of draw operations
  data/    JSON file repository, backup/restore
  test/    Model, repository and layout tests; renders each template to PNG
app/   Android app (Compose UI, PDF export via PdfDocument, AdMob)
```

The layout engine is platform-independent. The app draws its output onto an Android
`PdfDocument`. The tests draw the same output onto an AWT image, so templates can be
checked without a device.

## Build & test

```bash
./gradlew :core:test              # model, storage and layout tests (+ PNGs in core/build/template-previews)
./gradlew :app:testDebugUnitTest  # Robolectric UI flow tests
./gradlew :app:assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
```

## Before publishing

1. **Package ID / name:** set `applicationId` in `app/build.gradle.kts` and `app_name` in `app/src/main/res/values/strings.xml`.
2. **AdMob:** replace the Google *test* IDs in `app/build.gradle.kts` (`admobAppId`, `ADMOB_BANNER_ID`) with your own. Set `ADS_ENABLED` to `false` to ship without ads.
3. **Signing:** create a release keystore and a `signingConfig`.
4. **Play Console:** privacy policy (AdMob collects device identifiers), Data safety form, and the ads declaration.

## Roadmap (from competitor research)

- AI rewrite of summary and bullets (through a small backend so the API key isn't in the app)
- Cover letter builder matching the chosen template
- Import from an existing PDF or LinkedIn
- Optional Pro tier (extra templates, AI credits) via Google Play Billing; PDF export stays free
