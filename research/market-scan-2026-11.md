# Play Store Low-Code Clone-Target Scan — November 2026

## Method (what was actually measured)

| Phase | Source | Coverage |
|---|---|---|
| 1. Discovery | Google Play search, US/en, 80 niche keywords × 30 results (`scripts/discover.py`) | 2,058 unique apps |
| 1. Filters | Monetized (ads / IAP / paid) **and** ≥10K installs → 1,281; generic support email (gmail/outlook/mail.ru…) → 369; developer portfolio <10 apps (Play developer page) → **169 indie candidates** | `data/indie_candidates.csv` |
| 2. Revenue | AppGoblin free app overview: MAU, monthly-revenue bucket (`$<10K`, `$10K+`, `$50K+`…), IAP/Ad split, monthly installs (`scripts/ag.py`) | all 169 |
| 3. Stack | AppGoblin "Development Tools SDKs" (from AppGoblin's APK scans: Flutter / React Native / Kotlin / Unity…) | all 169 |
| 4. Gaps | Newest 1–3★ reviews, up to 40 per app, keyword-themed (`scripts/rev.py`) | 33 shortlisted apps |

**Caveats**
- **No APKs were inspected directly.** APKPure (and its download host) and AppBrain sit behind Cloudflare bot checks, and I did not try to get around them. The stack labels come from AppGoblin's SDK detection. Run `scripts/apk_stack_scan.py` on an APK you have pulled legitimately (e.g. `adb pull` from your own device) to confirm before you commit to a build.
- AppGoblin lists "Unity" under development tools for many utility apps. That is almost always the **Unity Ads SDK**, not the Unity engine, so those apps were treated as native.
- Revenue = AppGoblin's bucket plus my own proxy for apps under $10K: **MAU × $0.05–0.15/month** for IAP-only apps (1–3% payers × ~$5 monthly-equivalent). These are directional estimates, not Play Console data.
- No app built with Kodular, App Inventor, Cordova or Capacitor met the ≥10K + revenue bar in this sample. The low-code winners are all **Flutter** or **React Native**.

## Summary Table — verified low-code, revenue-generating, indie

| App & Package ID | Category | Est. Monthly Revenue | Installs | Detected Stack | Rebuild Complexity | Key User Complaint |
|---|---|---|---|---|---|---|
| **Nalabe Shift Work Calendar** `com.nalabe.calendar.workShift` | Productivity | $3K–9K (AG: <$10K; 62K MAU, 100% IAP) | 1.8M | React Native + Firebase | Low | Shift limit suddenly paywalled; purchases not unlocking; can't export PDF |
| **Logo Maker – AI Art Design** `artify.ai.logomaker` | Art & Design | **$10K+** (AG; 279K MAU, 100% IAP) | 4.5M | Flutter + Firebase, Adjust/AppsFlyer | Low–Med (gen-AI API) | "$6.99 a week" weekly sub feels like a scam; output not business-grade |
| **AI Image Generator – CreArt** `ai.aiart.aiartgenerator.creart` | Art & Design | **$10K+** (AG; 180K MAU, 55% IAP / 45% ads) | 3.8M | Flutter + RevenueCat + AdMob | Med (gen-AI API cost) | Credit/"stars" system, forced video ads, price hike, poor prompt adherence |
| **Meal Planner & Macro Tracker** `meal.planner` | Food & Drink | $3K–8K (56K MAU, 100% IAP) | 1.1M | Flutter + Sentry + Firebase | Low | Paywall after onboarding ($10.99); no local backup; no meal-expiry widget |
| **FivePrayer – Pray on Time** `com.fiveprayer.app` | Lifestyle | $6K–10K (128K MAU, 100% IAP) | 1.9M | Flutter + RevenueCat | Low | Installs **declining**; app-lock bypass; few substantive complaints |
| **Kids & Toddlers Learn and Play** `com.kidsworld.learn.music.play` | Education | $5K–10K (102K MAU, 100% IAP) | 5.6M | React Native | Low–Med (content) | 4.18★; content removed (flags); irrelevant lessons |
| **IELTS Practice Band 9** `com.lt.ieltspracticetest` | Education | $2K–7K (47K MAU, 79% IAP) | 3.9M | Flutter + MongoDB | Med (content) | Premium ≈ free tier; "buy points" fatigue; pushes 3rd-party PDF apps |
| **Habit Tracker – TickOff** `app.vinztech.tickit` | Productivity | $1.5K–4.5K (30K MAU) | 298K | Flutter + RevenueCat + OneSignal | Very Low | Only 3 free habits (27/40 bad reviews); sub doesn't carry over Android→iOS |
| **Resume Builder – CV Maker** `com.tapuniverse.resumebuilder` | Business | $1.5K–4.5K (30K MAU) | 169K | Flutter | Low | Crashes; few templates; only 1 free export; no AI help. **Stale 119d, installs flat (+1K/mo)** |
| **Chakra Healing & Meditation** `com.yiqi.chakra.meditation.bowls` | Health & Fitness | $1K–3K (21K MAU; IAP $9.99–69.99) | 250K | Flutter + RevenueCat | Very Low | Paywall hidden behind questionnaire ("deceptive") |

**Native benchmarks in the same niches (not low-code, but they prove demand):**

| App | Stack | Revenue | Weakness |
|---|---|---|---|
| Work Shift Calendar `com.lrhsoft.shiftercalendar` | Kotlin | **$10K+**, 324K MAU, +138K installs/mo | No update for **423 days**; lost data on phone change; Pro "no different from free"; ~100 ad-data partners |
| Shift Work Schedule Calendar `com.dddev.Shift_Work_Calendar` | native + Unity Ads | <$10K, 31K MAU | 4.11★; 21/40 bad reviews say "intrusive loud ads" |

**Excluded:** Ta2 AI Tattoo (Flutter, only 1.3K MAU); Fuel Log (Flutter, 3.1K MAU); Salvia Tarot (Flutter, but live-psychic chat on Agora is a heavy backend); betting-tips apps (policy risk); Cashew (open source and free); large Bible, VPN and wallpaper apps (native and well defended).

---

## Top Target Breakdown

### #1 — Shift Work Calendar niche (attack Nalabe + LRHSoft together)

- **Targets:** [Nalabe Shift Work Calendar](https://play.google.com/store/apps/details?id=com.nalabe.calendar.workShift): solo developer "Nalabe" (nalabe.ru, mail.ru support address), React Native, released Jun 2020, 4.75★ / 45K ratings, 1.8M installs, +33K/mo. [Work Shift Calendar](https://play.google.com/store/apps/details?id=com.lrhsoft.shiftercalendar): LRHSoft (2 apps, gmail support), native, 13.6M installs, **last updated Sep 2025**.
- **Monetization:**
  - Nalabe: IAP only, $4.99–$14.99, no ads. It recently moved shift creation behind a subscription, which is what drives the complaints.
  - LRHSoft: AdMob plus Pro IAP ($0.99–$10.99), 56% ads / 44% IAP, **$10K+/mo**.
  - Together that is about 386K MAU on two apps, from a niche keyword ("shift calendar") that is cheap to rank for.
- **Why it's the #1 target:** demand is proven and growing (+170K installs/mo across both), the leader is stagnant, and the reviews point to the same fixable gaps in every app: backup/sync, honest paywall, Google Calendar export, PDF sharing, no ad spam.
- **MVP rebuild blueprint (≈3–4 weeks solo):**
  - Stack: **FlutterFlow (or Flutter) + Firebase Auth/Firestore + RevenueCat**, home-screen widget via `home_widget`.
  - Core: rotating-pattern editor (4-on-4-off, DuPont, Pitman, 24/48), month view with colour wedges, alarms per shift, overtime/pay tally, PDF/image export, ICS + Google Calendar sync, team share link.
  - Differentiators:
    - **Free automatic cloud backup**, which fixes LRHSoft's #1 data-loss complaint.
    - **Unlimited shifts free.** Pro ($2.99/mo or $14.99 lifetime) adds pay calculation, team calendars, Calendar sync and multiple widgets.
    - No interstitial ads.
    - Import from LRHSoft and Nalabe exports.

### #2 — AI Logo Maker (`artify.ai.logomaker`)

- **Target:** [Logo Maker – AI Art Design](https://play.google.com/store/apps/details?id=artify.ai.logomaker): "Smart AI DEV" (9 apps, outlook.com support, coolsummerdev.com). Flutter, released May 2025, 4.5M installs in 18 months, +97K/mo, 4.48★.
- **Monetization:** 100% IAP, **$10K+/mo** (AppGoblin), $4.99–$79.99 items. Weekly sub of about **$6.99/week**. Adjust and AppsFlyer attribution show it is fed by **paid UA**.
- **Weakness:** 11 of the 40 newest negative reviews are about price ("$6.99 for 1 week", "they hope you forget"). Others say the output is "not for business use" and "not stylish".
- **MVP rebuild blueprint (≈2–3 weeks):**
  - Stack: **FlutterFlow + Firebase Functions → image API** (e.g. Ideogram or Recraft for text-in-logo quality, or SDXL on Replicate), **RevenueCat**, vectorize via Recraft or potrace for SVG export.
  - Differentiators:
    - **Credit packs + annual plan instead of a weekly trap.**
    - Real business deliverables: SVG/PDF, transparent PNG, brand kit (palette, fonts, social sizes), and editable text layers on top of AI icons.
  - Risks:
    - Cost of goods sold (API cost per image) means you need a free-tier cap.
    - The incumbent buys installs, so you have to win on ASO (search "logo maker", "AI logo", "business logo") and on reviews.

### #3 — Meal Planner & Macro Tracker (`meal.planner`)

- **Target:** [Meal Planner & Macro Tracker](https://play.google.com/store/apps/details?id=meal.planner): youmealplan.com (4 apps, gmail support, based in Spain). Flutter, released Aug 2023, 1.1M installs, +36K/mo, 4.82★.
- **Monetization:** 100% IAP, $3.39–$10.99, no ads. Estimated $3K–8K/mo from 56K MAU. The paywall comes **after** the full onboarding, which reviews call a bait-and-switch.
- **Weakness:** no local backup or export, sync/data complaints (7 of 40), no meal-prep expiry tracking or widget, cluttered fonts ("childish", "hard to read").
- **MVP rebuild blueprint (≈3–4 weeks):**
  - Stack: **FlutterFlow + Supabase (or Firestore) + RevenueCat**; nutrition data from the USDA FoodData Central API (free) or Open Food Facts; barcode via `mobile_scanner`.
  - Core: weekly planner, recipe box, auto grocery list, macro targets.
  - Differentiators:
    - **Meal-prep mode** with container expiry dates and a home-screen widget showing what to eat first.
    - Export/backup to Google Drive.
    - Show the price upfront before onboarding.
    - AI "plan my week from what's in my fridge".

**Runners-up worth a weekend build:** TickOff (habit tracker; the free tier is the whole complaint, so ship 10 free habits plus cross-platform restore) and TAPUNIVERSE Resume Builder (stale, flat installs, crashes; add AI bullet rewriting and ATS templates).

---

## Reproduce / extend

```bash
pip install google-play-scraper beautifulsoup4
cd research/scripts
python3 discover.py ../data/apps.json            # edit kws list to add niches
python3 filter.py ../data/apps.json ../data/filtered.json
python3 devcount.py ../data/filtered.json ../data/devcount.json
python3 score.py ../data/filtered.json ../data/devcount.json ../data/ranked.json
python3 ag.py ../data/ranked.json ../data/ag.json  # AppGoblin overview, 2s/request
python3 apk_stack_scan.py some.apk               # local APK framework check
```
